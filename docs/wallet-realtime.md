# Wallet realtime and outcome email

Flow: create PENDING request -> admin verifies bank transfer -> transaction updates deposit, balance/ledger (approval only), and outbox -> publisher sends wallet.events.v1 -> independent Kafka consumers invalidate the user's WebSocket view and enqueue an outcome email.

REJECTED corresponds to CANCELLED in the deposit API. It does not credit the wallet. Request validation errors and network timeouts are not definitive payment failures and must not produce failure emails. This flow still requires manual bank reconciliation; it does not implement a bank webhook.

## Build and run
Install the shared contract before building services, from the workspace root:
```powershell
Push-Location nihongo-user
./mvnw.cmd -f common-events/pom.xml install
Pop-Location
./common-security/mvnw.cmd -f common-security/pom.xml install -DskipTests
./nihongo-user/mvnw.cmd -f nihongo-user/pom.xml test "-Dtest=Wallet*Test"
./notification-service/mvnw.cmd -f notification-service/pom.xml test "-Dtest=WalletNotificationTest"
```

Configure KAFKA_BOOTSTRAP_SERVERS identically on both services and WALLET_EVENTS_TOPIC (default wallet.events.v1). Start MySQL, Kafka, discovery, gateway, nihongo-user and notification-service. Notification-service defaults to the same hosting database as nihongo-user. That server runs MariaDB, so notification-service uses MariaDB Connector/J and a jdbc:mariadb URL. Existing jdbc:mysql URL overrides are accepted through permitMysqlScheme. This avoids the MySQL driver metadata error "Unknown column RESERVED" during Hibernate startup. Override NOTIFICATION_DB_URL, NOTIFICATION_DB_USERNAME and NOTIFICATION_DB_PASSWORD if using a separate database; configure SMTP settings in notification-service. Wallet events/mail must be enabled. Each nihongo-user instance requires a distinct wallet.instance-id if explicitly configured, because its local WebSocket broker needs every event. Set wallet.websocket.allowed-origins to the frontend origin.

The current JPA update setting creates outbox and mail inbox tables; deployments using schema validation need equivalent migrations before startup. Database accounts need access to these tables.

## Delivery behavior
Outbox and wallet changes commit together; Kafka outages do not lose committed events. Email is queued durably before Kafka offsets advance, retried with backoff after SMTP failure, and deduplicated by event/deposit. SMTP acceptance followed by a process crash before the sent marker commits can still produce duplicate email; SMTP does not provide exactly-once delivery. Existing email group ID is preserved, so previously consumed REJECTED events will not automatically be replayed.

WebSocket messages contain event/deposit identifiers and type only. The UI reloads authoritative balance/history, coalesces notices during requests, resyncs on reconnect/focus, and shows connection status. Default scheduled publishing adds approximately one second and email polling up to two seconds under normal load; this is near realtime, not a hard latency guarantee.

## Manual smoke check
1. Log in as user and open /wallet; open /admin/wallet-deposits as admin in a separate authenticated browser.
2. Create a request: admin list updates without reload; balance stays unchanged.
3. Approve with a unique bank reference: user balance/history update, success notice appears, success email arrives.
4. Create another request and reject with a reason: balance stays unchanged, user history/notice update, failure email includes escaped reason.
5. Disconnect/reconnect WebSocket: connection status changes and the page resynchronizes.
6. Stop Kafka or SMTP temporarily: after recovery, queued events/mail are delivered without a second credit.

## Email queue time zone
The consumer supplies an application Instant for received_at and next_attempt_at; Hibernate binds JDBC timestamps in UTC. Do not use database CURRENT_TIMESTAMP for these fields: a hosting session in UTC+7 can postpone new emails by seven hours while the worker compares against UTC.

For diagnosis, inspect sent_at, attempts and next_attempt_at in wallet_mail_inbox. A future next_attempt_at with attempts=0 may indicate legacy clock skew. A non-null sent_at means SMTP accepted the message, not proof that it reached the recipient inbox. Correct only confirmed unsent skewed rows; do not reset delivered messages. Restart notification-service after updating the queue-time code.