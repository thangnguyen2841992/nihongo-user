package com.thang.nihongo_user.model;
import jakarta.persistence.*;
import lombok.*;
@Entity @Getter @Setter @Table(name="promotion_redemption",uniqueConstraints=@UniqueConstraint(columnNames={"promotionId","userId"}))
public class PromotionRedemption {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private Long promotionId;
 @Column(nullable=false,length=36) private String userId;
 @Column(nullable=false,unique=true) private Long purchaseId;
}
