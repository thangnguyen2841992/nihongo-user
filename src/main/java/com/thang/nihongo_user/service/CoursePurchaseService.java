package com.thang.nihongo_user.service;
import com.thang.nihongo_user.model.*;
import com.thang.nihongo_user.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.UUID;
@Service @RequiredArgsConstructor
public class CoursePurchaseService {
 private final IUserWalletRepository wallets;
 private final IUserSubscriptionRepository subscriptions;
 private final ICoursePackageRepository packages;
 private final CoursePurchaseRepository purchases;
 private final IWalletTransactionRepository ledger;
 @Transactional
 public UserSubscription purchase(String userId,Long courseId,Long packageId,String requestKey,boolean renewal) {
  if(userId==null || userId.isBlank()) throw error(HttpStatus.UNAUTHORIZED,"Cần đăng nhập");
  String key;
  try { key=UUID.fromString(requestKey).toString(); } catch(Exception e) { throw error(HttpStatus.BAD_REQUEST,"Mã yêu cầu không hợp lệ"); }
  wallets.ensureWallet(userId);
  UserWallet wallet=wallets.findByUserIdForUpdate(userId).orElseThrow();
  var previous=purchases.findByUserIdAndRequestKey(userId,key);
  if(previous.isPresent()) {
   var p=previous.get();
   if(!p.getCourseId().equals(courseId)||!p.getPackageId().equals(packageId)||p.isRenewal()!=renewal)
    throw error(HttpStatus.CONFLICT,"Mã yêu cầu đã được sử dụng với nội dung khác");
   return subscriptions.findById(p.getSubscriptionId()).orElseThrow();
  }
  if(courseId==null || packageId==null) throw error(HttpStatus.BAD_REQUEST,"Thiếu khóa học hoặc gói học");
  CoursePackage pack=packages.findById(packageId).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Không tìm thấy gói học"));
  if(pack.getCourse()==null || !courseId.equals(pack.getCourse().getCourseId())
    || pack.getCourse().getActive()!=CourseStatus.ACTIVE || !Boolean.TRUE.equals(pack.getIsActive()))
   throw error(HttpStatus.BAD_REQUEST,"Gói học không thuộc khóa học hoặc đã ngừng bán");
  BigDecimal price=pack.getPrice();
  if(price==null||price.signum()<0||price.stripTrailingZeros().scale()>0||price.compareTo(new BigDecimal("9999999999999.99"))>0
    ||pack.getDurationDays()==null||pack.getDurationDays()<1||pack.getDurationDays()>36500)
   throw error(HttpStatus.CONFLICT,"Cấu hình giá hoặc thời hạn gói học không hợp lệ");
  var existing=subscriptions.findByUserIdAndCourseId(userId,courseId);
  if(renewal && existing.isEmpty()) throw error(HttpStatus.CONFLICT,"Chưa đăng ký khóa học");
  if(!renewal && existing.isPresent()) throw error(HttpStatus.CONFLICT,"Khóa học đã đăng ký; hãy dùng gia hạn");
  if(existing.isPresent() && existing.get().getStatus()==SubscriptionStatus.CANCELLED)
   throw error(HttpStatus.CONFLICT,"Đăng ký đã bị hủy; liên hệ quản trị viên");
  if(wallet.getBalance().compareTo(price)<0) throw error(HttpStatus.CONFLICT,"Số dư không đủ. Vui lòng nạp thêm tiền vào ví");
  LocalDateTime now=LocalDateTime.now();
  UserSubscription sub=existing.orElseGet(UserSubscription::new);
  LocalDateTime start=existing.isPresent() && sub.getStatus()==SubscriptionStatus.ACTIVE
      && sub.getExpiredAt()!=null && sub.getExpiredAt().isAfter(now)?sub.getExpiredAt():now;
  sub.setUserId(userId); sub.setCourseId(courseId); sub.setPackageId(packageId);
  sub.setStatus(SubscriptionStatus.ACTIVE); sub.setExpiredAt(start.plusDays(pack.getDurationDays()));
  if(existing.isEmpty()) { sub.setProgress(0); sub.setCreatedAt(now); }
  sub=subscriptions.saveAndFlush(sub);
  BigDecimal before=wallet.getBalance(); wallet.setBalance(before.subtract(price)); wallets.save(wallet);
  CoursePurchase purchase=new CoursePurchase();
  purchase.setUserId(userId); purchase.setRequestKey(key); purchase.setCourseId(courseId); purchase.setPackageId(packageId);
  purchase.setRenewal(renewal); purchase.setSubscriptionId(sub.getId()); purchase.setAmount(price); purchase.setCreatedAt(now);
  purchase=purchases.saveAndFlush(purchase);
  ledger.save(WalletTransaction.builder().walletId(wallet.getWalletId()).transactionType(WalletTransactionType.PURCHASE)
   .amount(price).balanceBefore(before).balanceAfter(wallet.getBalance()).referenceType("COURSE_PURCHASE").referenceId(purchase.getId())
   .description((renewal?"Gia hạn":"Mua")+" khóa học "+courseId).status(WalletTransactionStatus.SUCCESS).build());
  return sub;
 }
 private ResponseStatusException error(HttpStatus status,String message) { return new ResponseStatusException(status,message); }
}
