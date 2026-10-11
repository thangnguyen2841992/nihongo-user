package com.thang.nihongo_user.service;
import com.thang.nihongo_user.model.*;
import com.thang.nihongo_user.repository.*;
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
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties={"spring.config.import=","spring.datasource.url=jdbc:h2:mem:commerce_ops;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000","spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect","spring.jpa.show-sql=false"})
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes=AdminCommercePersistenceTest.Config.class)
@Transactional(propagation=Propagation.NOT_SUPPORTED)
class AdminCommercePersistenceTest {
 @TestConfiguration @EntityScan(basePackageClasses=Course.class) @EnableJpaRepositories(basePackageClasses=ICourseRepository.class)
 @Import({CoursePurchaseService.class,PromotionService.class,AdminCommerceService.class}) static class Config {}
 @Autowired CoursePurchaseService purchase;@Autowired AdminCommerceService commerce;
 @Autowired ICourseRepository courses;@Autowired ICoursePackageRepository packages;@Autowired IUserWalletRepository wallets;
 @Autowired CoursePurchaseRepository purchases;@Autowired IWalletTransactionRepository ledger;@Autowired IUserSubscriptionRepository subscriptions;
 @Autowired PromotionRepository promotions;@Autowired PromotionRedemptionRepository redemptions;@Autowired AdminCommerceAuditRepository audits;@Autowired IWalletDepositRepository deposits;
 Long courseId,packageId;String owner;
 @BeforeEach void setup(){
  redemptions.deleteAll();promotions.deleteAll();purchases.deleteAll();subscriptions.deleteAll();ledger.deleteAll();wallets.deleteAll();packages.deleteAll();courses.deleteAll();audits.deleteAll();deposits.deleteAll();
  var c=courses.saveAndFlush(Course.builder().courseName("N3").levelId(3L).active(CourseStatus.ACTIVE).build());courseId=c.getCourseId();
  packageId=packages.saveAndFlush(CoursePackage.builder().course(c).packageName("Tháng").durationDays(30).price(new BigDecimal("10000")).isActive(true).build()).getPackageId();
  owner=wallet(new BigDecimal("50000"));
 }
 String wallet(BigDecimal balance){String id=UUID.randomUUID().toString();var w=new UserWallet();w.setUserId(id);w.setBalance(balance);wallets.saveAndFlush(w);return id;}
 Promotion promo(int uses){var p=new Promotion();p.setCode("N3SALE");p.setCourseId(courseId);p.setPercent(10);p.setMaxUses(uses);p.setActive(true);p.setStartsAt(LocalDateTime.now().minusDays(1));p.setEndsAt(LocalDateTime.now().plusDays(1));return promotions.saveAndFlush(p);}
 @Test void discountAndReplayDebitExactlyOnceAndPersistPriceSnapshot(){
  var p=promo(10);String key=UUID.randomUUID().toString();
  var first=purchase.purchase(owner,courseId,packageId,key,false," n3sale ",new BigDecimal("9000"));
  assertEquals(first.getId(),purchase.purchase(owner,courseId,packageId,key,false,"N3SALE",new BigDecimal("9000")).getId());
  assertEquals(1,purchases.count());assertEquals(1,ledger.count());assertEquals(1,redemptions.count());assertEquals(1,promotions.findById(p.getId()).orElseThrow().getUsed());
  var saved=purchases.findByUserIdAndRequestKey(owner,key).orElseThrow();assertEquals(0,new BigDecimal("10000").compareTo(saved.getOriginalPrice()));assertEquals(0,new BigDecimal("1000").compareTo(saved.getDiscountAmount()));
  assertEquals(0,new BigDecimal("41000").compareTo(wallets.findByUserId(owner).orElseThrow().getBalance()));
  assertThrows(ResponseStatusException.class,()->purchase.purchase(owner,courseId,packageId,key,false,null));
  assertThrows(ResponseStatusException.class,()->purchase.purchase(owner,courseId,packageId,UUID.randomUUID().toString(),true,"N3SALE"));assertEquals(1,ledger.count());
 }
 @Test void insufficientBalanceAndChangedDisplayedPriceDoNotConsumeCouponOrCreateSubscription(){
  var p=promo(1);String poor=wallet(new BigDecimal("100"));
  assertThrows(ResponseStatusException.class,()->purchase.purchase(poor,courseId,packageId,UUID.randomUUID().toString(),false,"N3SALE"));
  assertThrows(ResponseStatusException.class,()->purchase.purchase(owner,courseId,packageId,UUID.randomUUID().toString(),false,"N3SALE",new BigDecimal("8000")));
  assertEquals(0,promotions.findById(p.getId()).orElseThrow().getUsed());assertEquals(0,redemptions.count());assertEquals(0,subscriptions.count());assertEquals(0,ledger.count());
 }
 @Test void simultaneousLastCouponUseAllowsOnlyOneSuccessfulPurchase() throws Exception {
  var p=promo(1);String other=wallet(new BigDecimal("50000"));var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
  try{
   List<Future<Boolean>> results=new ArrayList<>();for(String user:List.of(owner,other))results.add(pool.submit(()->{start.await();try{purchase.purchase(user,courseId,packageId,UUID.randomUUID().toString(),false,"N3SALE");return true;}catch(ResponseStatusException e){return false;}}));start.countDown();
   int success=0;for(var result:results)if(result.get(15,TimeUnit.SECONDS))success++;assertEquals(1,success);assertEquals(1,purchases.count());assertEquals(1,ledger.count());assertEquals(1,promotions.findById(p.getId()).orElseThrow().getUsed());
  }finally{pool.shutdownNow();}
 }
 @Test void revenueSeparatesDiscountedSalesRenewalsAndDeposits(){
  promo(10);purchase.purchase(owner,courseId,packageId,UUID.randomUUID().toString(),false,"N3SALE");purchase.purchase(owner,courseId,packageId,UUID.randomUUID().toString(),true);
  var d=new WalletDeposit();d.setUserId(owner);d.setRequestKey(UUID.randomUUID().toString());d.setAmount(new BigDecimal("30000"));d.setStatus(PaymentStatus.SUCCESS);d.setReviewedAt(LocalDateTime.now());d.setReviewedBy("admin");deposits.saveAndFlush(d);
  var r=commerce.revenue(LocalDate.now(),LocalDate.now());assertEquals(0,new BigDecimal("9000").compareTo(r.sales()));assertEquals(0,new BigDecimal("10000").compareTo(r.renewals()));assertEquals(0,new BigDecimal("30000").compareTo(r.walletDeposits()));assertEquals(2,r.byCourse().size());
  assertThrows(ResponseStatusException.class,()->commerce.revenue(LocalDate.now().minusYears(2),LocalDate.now()));
 }
 @Test void staleCatalogEditCannotOverwritePriceAndEveryCommittedEditIsAudited(){
  var p=packages.findById(packageId).orElseThrow();var input=new AdminCommerceService.PackageInput("Mới",30,new BigDecimal("20000"),true,p.getVersion());
  commerce.savePackage("admin",courseId,packageId,input);assertThrows(ResponseStatusException.class,()->commerce.savePackage("other",courseId,packageId,input));
  assertEquals(0,new BigDecimal("20000").compareTo(packages.findById(packageId).orElseThrow().getPrice()));assertEquals(1,audits.count());assertEquals("admin",audits.findAll().get(0).getActor());
 }
 @Test void expiredAndWrongCourseCouponsAndDisabledPackagesAreRejected(){
  var p=promo(10);p.setEndsAt(LocalDateTime.now().minusMinutes(1));promotions.saveAndFlush(p);
  assertThrows(ResponseStatusException.class,()->commerce.quote(owner,courseId,packageId,"N3SALE"));
  p=promotions.findById(p.getId()).orElseThrow();p.setEndsAt(LocalDateTime.now().plusDays(1));p.setCourseId(courseId+100);promotions.saveAndFlush(p);
  assertThrows(ResponseStatusException.class,()->purchase.purchase(owner,courseId,packageId,UUID.randomUUID().toString(),false,"N3SALE"));
  var pack=packages.findById(packageId).orElseThrow();pack.setIsActive(false);packages.saveAndFlush(pack);
  assertThrows(ResponseStatusException.class,()->commerce.quote(owner,courseId,packageId,"N3SALE"));assertEquals(0,ledger.count());
 }
}
