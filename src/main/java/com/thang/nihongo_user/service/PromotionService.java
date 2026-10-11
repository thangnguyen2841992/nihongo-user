package com.thang.nihongo_user.service;
import com.thang.nihongo_user.model.*;
import com.thang.nihongo_user.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.math.*;
import java.util.Locale;
@Service @RequiredArgsConstructor
public class PromotionService {
 private final PromotionRepository promotions;
 private final PromotionRedemptionRepository redemptions;
 public record Discount(String code, BigDecimal originalPrice, BigDecimal discount, BigDecimal payable) {}
 public static String normalize(String value) {
  if(value==null||value.isBlank()) return null;
  String code=value.trim().toUpperCase(Locale.ROOT);
  if(!code.matches("[A-Z0-9_-]{3,32}")) throw bad("Mã giảm giá gồm 3–32 chữ, số, gạch ngang hoặc gạch dưới");
  return code;
 }
 public Discount discount(String userId,Long courseId,BigDecimal price,String input,boolean lock) {
  String code=normalize(input);
  if(code==null) return new Discount(null,price,BigDecimal.ZERO,price);
  Promotion p=(lock?promotions.lockByCode(code):promotions.findByCode(code)).orElseThrow(()->bad("Mã giảm giá không tồn tại"));
  var now=LocalDateTime.now();
  if(!p.isActive()||now.isBefore(p.getStartsAt())||!now.isBefore(p.getEndsAt())||p.getUsed()>=p.getMaxUses()
    ||p.getCourseId()!=null&&!p.getCourseId().equals(courseId)) throw bad("Mã giảm giá hết hạn, hết lượt hoặc không áp dụng cho khóa này");
  if(redemptions.existsByPromotionIdAndUserId(p.getId(),userId)) throw bad("Bạn đã sử dụng mã này");
  BigDecimal discount=price.multiply(BigDecimal.valueOf(p.getPercent())).divide(BigDecimal.valueOf(100),0,RoundingMode.DOWN);
  if(lock) p.setUsed(p.getUsed()+1);
  return new Discount(code,price,discount,price.subtract(discount));
 }
 public void redeem(String userId,Long purchaseId,String code) {
  if(code==null) return;
  Promotion p=promotions.findByCode(code).orElseThrow();
  var r=new PromotionRedemption(); r.setPromotionId(p.getId());r.setUserId(userId);r.setPurchaseId(purchaseId);redemptions.save(r);
 }
 private static ResponseStatusException bad(String text){return new ResponseStatusException(HttpStatus.BAD_REQUEST,text);}
}
