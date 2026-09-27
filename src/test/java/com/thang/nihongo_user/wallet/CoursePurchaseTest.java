package com.thang.nihongo_user.wallet;
import com.thang.nihongo_user.model.*;
import com.thang.nihongo_user.repository.*;
import com.thang.nihongo_user.service.CoursePurchaseService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
@DataJpaTest(properties={
 "spring.datasource.url=jdbc:h2:mem:purchase_test;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
 "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
 "spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
 "spring.jpa.show-sql=false","spring.cloud.discovery.enabled=false","eureka.client.enabled=false"})
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes=CoursePurchaseTest.Config.class)
@Transactional(propagation=Propagation.NOT_SUPPORTED)
class CoursePurchaseTest {
 @TestConfiguration @EntityScan(basePackageClasses=UserWallet.class) @EnableJpaRepositories(basePackageClasses=IUserWalletRepository.class)
 @Import(CoursePurchaseService.class) static class Config {}
 @Autowired CoursePurchaseService service; @Autowired IUserWalletRepository wallets;
 @Autowired ICourseRepository courses; @Autowired ICoursePackageRepository packages;
 @Autowired IUserSubscriptionRepository subscriptions; @Autowired CoursePurchaseRepository purchases;
 @org.springframework.test.context.bean.override.mockito.MockitoSpyBean IWalletTransactionRepository ledger;
 String owner; Long courseId,packageId;
 @BeforeEach void setup(){
  owner=UUID.randomUUID().toString();
  var course=courses.saveAndFlush(Course.builder().courseName("N5").levelId(1L).active(CourseStatus.ACTIVE).build()); courseId=course.getCourseId();
  packageId=packages.saveAndFlush(CoursePackage.builder().course(course).packageName("Month").durationDays(30).price(new BigDecimal("10000")).isActive(true).build()).getPackageId();
  var wallet=new UserWallet();wallet.setUserId(owner);wallet.setBalance(new BigDecimal("25000"));wallets.saveAndFlush(wallet);
 }
 String key(){return UUID.randomUUID().toString();}
 @Test void purchaseAndRenewDebitWithOneActiveSubscription(){
  var first=service.purchase(owner,courseId,packageId,key(),false);assertEquals(SubscriptionStatus.ACTIVE,first.getStatus());
  assertTrue(subscriptions.existsActive(owner,courseId));assertTrue(subscriptions.hasLevelAccess(owner,1L));
  var storedExpiry=subscriptions.findById(first.getId()).orElseThrow().getExpiredAt();
  var renewed=service.purchase(owner,courseId,packageId,key(),true);
  assertEquals(first.getId(),renewed.getId());assertEquals(storedExpiry.plusDays(30),renewed.getExpiredAt());
  assertEquals(0,wallets.findByUserId(owner).orElseThrow().getBalance().compareTo(new BigDecimal("5000")));
  assertEquals(2,ledger.findByWalletIdOrderByCreatedAtDesc(wallets.findByUserId(owner).orElseThrow().getWalletId()).size());
  assertFalse(subscriptions.existsActive(UUID.randomUUID().toString(),courseId));
 }
 @Test void insufficientFundsRollbackRenewalAndLedger(){
  var first=service.purchase(owner,courseId,packageId,key(),false);
  service.purchase(owner,courseId,packageId,key(),true);var expiry=subscriptions.findById(first.getId()).orElseThrow().getExpiredAt();
  assertThrows(ResponseStatusException.class,()->service.purchase(owner,courseId,packageId,key(),true));
  assertEquals(expiry,subscriptions.findById(first.getId()).orElseThrow().getExpiredAt());
  assertEquals(0,wallets.findByUserId(owner).orElseThrow().getBalance().compareTo(new BigDecimal("5000")));
 }
 @Test void wrongCourseDisabledPackageAndChangedIdempotencyPayloadAreRejected(){
  assertThrows(ResponseStatusException.class,()->service.purchase(owner,courseId+100,packageId,key(),false));
  String k=key();service.purchase(owner,courseId,packageId,k,false);
  assertThrows(ResponseStatusException.class,()->service.purchase(owner,courseId,packageId,k,true));
  var p=packages.findById(packageId).orElseThrow();p.setIsActive(false);packages.saveAndFlush(p);
  assertThrows(ResponseStatusException.class,()->service.purchase(owner,courseId,packageId,key(),true));
 }
 @Test void simultaneousRetryDebitsOnlyOnce() throws Exception {
  String k=key();var executor=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
  try {
   Callable<UserSubscription> task=()->{start.await();return service.purchase(owner,courseId,packageId,k,false);};
   var a=executor.submit(task);var b=executor.submit(task);start.countDown();
   assertEquals(a.get(20,TimeUnit.SECONDS).getId(),b.get(20,TimeUnit.SECONDS).getId());
   var wallet=wallets.findByUserId(owner).orElseThrow();
   assertEquals(0,wallet.getBalance().compareTo(new BigDecimal("15000")));
   assertEquals(1,ledger.findByWalletIdOrderByCreatedAtDesc(wallet.getWalletId()).size());
  } finally { executor.shutdownNow(); }
 }
 @Test void ledgerFailureRollsBackDebitAndSubscription(){
  org.mockito.Mockito.doThrow(new IllegalStateException("ledger unavailable")).when(ledger).save(org.mockito.ArgumentMatchers.any(WalletTransaction.class));
  assertThrows(IllegalStateException.class,()->service.purchase(owner,courseId,packageId,key(),false));
  assertTrue(subscriptions.findByUserId(owner).isEmpty());
  assertEquals(0,wallets.findByUserId(owner).orElseThrow().getBalance().compareTo(new BigDecimal("25000")));
 }
 @Test void freshDuplicateRegistrationDoesNotCharge(){
  service.purchase(owner,courseId,packageId,key(),false);
  assertThrows(ResponseStatusException.class,()->service.purchase(owner,courseId,packageId,key(),false));
  assertEquals(0,wallets.findByUserId(owner).orElseThrow().getBalance().compareTo(new BigDecimal("15000")));
 }
}
