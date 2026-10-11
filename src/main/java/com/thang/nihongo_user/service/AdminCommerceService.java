package com.thang.nihongo_user.service;
import com.thang.nihongo_user.model.*;
import com.thang.nihongo_user.repository.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class AdminCommerceService {
 private final ICourseRepository courses;
 private final ICoursePackageRepository packages;
 private final CoursePurchaseRepository purchases;
 private final IWalletDepositRepository deposits;
 private final PromotionRepository promotions;
 private final PromotionService discounts;
 private final AdminCommerceAuditRepository audits;
 private final EntityManager em;
 private final CourseRefundRepository refunds;
 public record PackageInput(@NotBlank @Size(max=255) String name,@Min(1) @Max(36500) int durationDays,
   @NotNull @DecimalMin("0") @DecimalMax("9999999999999") @Digits(integer=13,fraction=0) BigDecimal price,boolean active,long version) {}
 public record CourseInput(@NotBlank @Size(max=255) String name,@Size(max=255) String description,@NotNull @Positive Long levelId,@NotNull CourseStatus status,long version) {}
 public record Pack(Long id,String name,int durationDays,BigDecimal price,boolean active,long version) {}
 public record Catalog(Long id,String name,String description,Long levelId,CourseStatus status,long version,List<Pack> packages) {}
 public record PromotionInput(@NotBlank String code,Long courseId,@Min(1) @Max(100) int percent,@Min(1) @Max(1000000) int maxUses,
   @NotNull LocalDateTime startsAt,@NotNull LocalDateTime endsAt,boolean active,long version) {}
 public record Sale(Long courseId,String courseName,boolean renewal,long purchases,BigDecimal amount) {}
 public record Revenue(LocalDate from,LocalDate to,BigDecimal sales,BigDecimal renewals,BigDecimal walletDeposits,List<Sale> byCourse,BigDecimal refunds) {}
 public record ReviewAudit(Long id,String actor,String action,String target,String detail,LocalDateTime createdAt) {}
 public static Pageable page(int number,int size){
  if(number<0||size<1||size>100)throw bad("Trang không hợp lệ");return PageRequest.of(number,size);
 }
 public List<Catalog> catalog(){return courses.findAll().stream().map(this::catalog).toList();}
 private Catalog catalog(Course c){var packs=c.getPackages()==null?packages.findByCourse_CourseId(c.getCourseId()):c.getPackages();return new Catalog(c.getCourseId(),c.getCourseName(),c.getCourseDescription(),c.getLevelId(),c.getActive(),c.getVersion(),
  packs.stream().map(p->new Pack(p.getPackageId(),p.getPackageName(),p.getDurationDays(),p.getPrice(),Boolean.TRUE.equals(p.getIsActive()),p.getVersion())).toList());}
 @Transactional public Catalog saveCourse(String actor,Long id,CourseInput input){
  Course c=id==null?new Course():em.find(Course.class,id,LockModeType.PESSIMISTIC_WRITE);
  if(c==null)throw missing();if(id!=null&&c.getVersion()!=input.version())throw conflict("Khóa học đã được sửa; hãy tải lại");
  String before=id==null?"Tạo mới":c.getCourseName()+" / "+c.getActive()+" / cấp độ "+c.getLevelId();
  c.setCourseName(input.name().trim());c.setCourseDescription(input.description());c.setLevelId(input.levelId());c.setActive(input.status());
  c=courses.saveAndFlush(c);audit(actor,id==null?"COURSE_CREATE":"COURSE_UPDATE","course:"+c.getCourseId(),before+" → "+c.getCourseName()+" / "+c.getActive()+" / cấp độ "+c.getLevelId());return catalog(c);
 }
 @Transactional public Catalog savePackage(String actor,Long courseId,Long id,PackageInput input){
  Course c=em.find(Course.class,courseId,LockModeType.PESSIMISTIC_WRITE);if(c==null)throw missing();
  CoursePackage p=id==null?new CoursePackage():packages.lockById(id).orElseThrow(AdminCommerceService::missing);
  if(id!=null&&(!p.getCourse().getCourseId().equals(courseId)||p.getVersion()!=input.version()))throw conflict("Gói không thuộc khóa hoặc đã thay đổi");
  String before=id==null?"Tạo mới":p.getPackageName()+" / "+p.getPrice()+" / "+p.getDurationDays()+" ngày / "+p.getIsActive();
  p.setCourse(c);p.setPackageName(input.name().trim());p.setPrice(input.price());p.setDurationDays(input.durationDays());p.setIsActive(input.active());
  p=packages.saveAndFlush(p);audit(actor,id==null?"PACKAGE_CREATE":"PACKAGE_UPDATE","package:"+p.getPackageId(),before+" → "+p.getPackageName()+" / "+p.getPrice()+" / "+p.getDurationDays()+" ngày / "+p.getIsActive());return catalog(c);
 }
 @Transactional public Promotion savePromotion(String actor,Long id,PromotionInput input){
  String code=PromotionService.normalize(input.code());if(code==null||!input.endsAt().isAfter(input.startsAt()))throw bad("Mã hoặc thời gian áp dụng không hợp lệ");
  if(input.courseId()!=null&&!courses.existsById(input.courseId()))throw missing();
  Promotion p=id==null?new Promotion():em.find(Promotion.class,id,LockModeType.PESSIMISTIC_WRITE);if(p==null)throw missing();
  if(id!=null&&(p.getVersion()!=input.version()||!code.equals(p.getCode())))throw conflict("Mã đã thay đổi; mã ký tự không thể đổi sau khi tạo");
  if(input.maxUses()<p.getUsed())throw bad("Giới hạn không được thấp hơn lượt đã dùng");
  p.setCode(code);p.setCourseId(input.courseId());p.setPercent(input.percent());p.setMaxUses(input.maxUses());p.setStartsAt(input.startsAt());p.setEndsAt(input.endsAt());p.setActive(input.active());
  p=promotions.saveAndFlush(p);audit(actor,id==null?"PROMOTION_CREATE":"PROMOTION_UPDATE","promotion:"+p.getId(),code+" / "+p.getPercent()+"% / "+p.getMaxUses()+" lượt / "+p.isActive()+" / "+p.getStartsAt()+"–"+p.getEndsAt());return p;
 }
 public Page<Promotion> promotions(Pageable page){return promotions.findAllByOrderByIdDesc(page);}
 public PromotionService.Discount quote(String userId,Long courseId,Long packageId,String code){
  CoursePackage p=packages.findById(packageId).orElseThrow(AdminCommerceService::missing);
  if(!p.getCourse().getCourseId().equals(courseId)||p.getCourse().getActive()!=CourseStatus.ACTIVE||!Boolean.TRUE.equals(p.getIsActive()))throw bad("Gói học đã ngừng bán");
  return discounts.discount(userId,courseId,p.getPrice(),code,false);
 }
 public Revenue revenue(LocalDate from,LocalDate to){
  if(from==null||to==null||to.isBefore(from)||java.time.temporal.ChronoUnit.DAYS.between(from,to)>365)throw bad("Chọn khoảng ngày tối đa 366 ngày");
  var rows=purchases.sales(from.atStartOfDay(),to.plusDays(1).atStartOfDay());Map<Long,String> names=new HashMap<>();
  courses.findAllById(rows.stream().map(CoursePurchaseRepository.Sales::getCourseId).distinct().toList()).forEach(c->names.put(c.getCourseId(),c.getCourseName()));
  List<Sale> result=rows.stream().map(r->new Sale(r.getCourseId(),names.getOrDefault(r.getCourseId(),"Khóa "+r.getCourseId()),r.getRenewal(),r.getPurchases(),r.getAmount())).toList();
  return new Revenue(from,to,result.stream().filter(s->!s.renewal()).map(Sale::amount).reduce(BigDecimal.ZERO,BigDecimal::add),result.stream().filter(Sale::renewal).map(Sale::amount).reduce(BigDecimal.ZERO,BigDecimal::add),deposits.approvedAmount(from.atStartOfDay(),to.plusDays(1).atStartOfDay()),result,refunds.total(from.atStartOfDay(),to.plusDays(1).atStartOfDay()));
 }
 public Page<AdminCommerceAudit> auditPage(Pageable page){return audits.findAllByOrderByCreatedAtDescIdDesc(page);}
 public Page<ReviewAudit> depositAudit(Pageable page){return deposits.findByReviewedAtIsNotNullOrderByReviewedAtDescIdDesc(page).map(d->new ReviewAudit(d.getId(),d.getReviewedBy(),"DEPOSIT_"+d.getStatus(),"deposit:"+d.getId(),d.getAmount()+" VND / "+(d.getReviewNote()==null?"":d.getReviewNote()),d.getReviewedAt()));}
 private void audit(String actor,String action,String target,String detail){var a=new AdminCommerceAudit();a.setActor(actor);a.setAction(action);a.setTarget(target);a.setDetail(detail);a.setCreatedAt(LocalDateTime.now());audits.save(a);}
 private static ResponseStatusException missing(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy dữ liệu");}
 private static ResponseStatusException conflict(String text){return new ResponseStatusException(HttpStatus.CONFLICT,text);}
 private static ResponseStatusException bad(String text){return new ResponseStatusException(HttpStatus.BAD_REQUEST,text);}
}
