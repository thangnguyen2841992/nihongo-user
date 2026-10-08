# Exercise history

New submissions include a UUID `submissionId`. The frontend freezes the lesson and selected answers on the first submission and reuses them on retry. Starting another attempt generates another UUID. Older clients without an ID remain compatible, but do not receive retry deduplication.

The database enforces uniqueness of `(user_uuid, submission_id)`. The transactional worker saves the result snapshot and updates the wrong-answer notebook together. Concurrent collisions roll back the losing transaction before the submission facade reads the committed result. Reusing a submission ID with different answers or a different lesson returns HTTP 409.

The saved JSON snapshot contains selected answers, the authoritative answer key, and the grading response. History reads these saved values rather than fetching a current answer key. The detail view displays question IDs and answer letters; it does not reconstruct historical question text.

`wrongCount` on new results counts answered questions that were incorrect; `unansweredCount` counts skipped questions. Both affect the score. The grading contract from Staff still includes skipped questions in its wrong count; the user service separates them before saving or returning the result.

Legacy rows have `unansweredCount = null` and no snapshot. Their old wrong count includes skipped questions, so the UI displays an explanatory note and an unknown skipped count rather than inventing a zero. Do not backfill old skipped counts or answer snapshots without authoritative data.

## Schema and rollout

The configured `spring.jpa.hibernate.ddl-auto=update` adds nullable columns `submission_id` (VARCHAR(36)), `unanswered_count` (INTEGER), and `snapshot` (LONGTEXT) to `user_exercise_attempt`, plus unique constraint `uk_attempt_user_submission` on `(user_uuid, submission_id)`. Existing rows are retained. Restart the updated user service to apply the configured schema update before deploying the new frontend. In environments with managed migrations, apply the equivalent additive schema change explicitly and verify the unique constraint exists.

Retries within the same open attempt are deduplicated. Closing/reloading the page discards its pending submission ID; the frontend does not persist unfinished attempts across browser sessions.
