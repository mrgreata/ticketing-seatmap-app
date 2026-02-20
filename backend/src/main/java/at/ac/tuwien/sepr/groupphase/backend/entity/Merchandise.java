package at.ac.tuwien.sepr.groupphase.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Entity
@Table(name = "merchandise")
public class Merchandise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 1000)
    @Column(nullable = false, length = 1000)
    private String description;

    @NotNull
    @Size(max = 255)
    @Column(nullable = false)
    private String name;

    @NotNull
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @NotNull
    @Column(nullable = false)
    private Integer rewardPointsPerUnit;

    @NotNull
    @Column(nullable = false)
    private Integer remainingQuantity;

    @NotNull
    @Column(nullable = false)
    private Boolean redeemableWithPoints;

    @Lob
    @Column(name = "image", columnDefinition = "BLOB")
    private byte[] image;

    @Column(name = "image_content_type")
    private String imageContentType;

    @Column
    private Integer pointsPrice;

    @NotNull
    @Column
    private Boolean deleted = false;



    public Merchandise() {
    }

    public Merchandise(String description,
                       String name,
                       BigDecimal unitPrice,
                       Integer remainingQuantity,
                       Integer rewardPointsPerUnit,
                       Boolean redeemableWithPoints,
                       Integer pointsPrice) {
        this.description = description;
        this.name = name;
        this.unitPrice = unitPrice;
        this.remainingQuantity = remainingQuantity;
        this.rewardPointsPerUnit = rewardPointsPerUnit;
        this.redeemableWithPoints = redeemableWithPoints;
        this.pointsPrice = pointsPrice;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getRewardPointsPerUnit() {
        return rewardPointsPerUnit;
    }

    public void setRewardPointsPerUnit(Integer rewardPointsPerUnit) {
        this.rewardPointsPerUnit = rewardPointsPerUnit;
    }

    public Integer getRemainingQuantity() {
        return remainingQuantity;
    }

    public void setRemainingQuantity(Integer remainingQuantity) {
        this.remainingQuantity = remainingQuantity;
    }

    public Boolean getRedeemableWithPoints() {
        return redeemableWithPoints;
    }

    public void setRedeemableWithPoints(Boolean redeemableWithPoints) {
        this.redeemableWithPoints = redeemableWithPoints;
    }

    public Integer getPointsPrice() {
        return pointsPrice;
    }

    public void setPointsPrice(Integer pointsPrice) {
        this.pointsPrice = pointsPrice;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public String getImageContentType() {
        return imageContentType;
    }

    public void setImageContentType(String imageContentType) {
        this.imageContentType = imageContentType;
    }

    public boolean hasImage() {
        return image != null && image.length > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Merchandise merch)) {
            return false;
        }
        return id != null && id.equals(merch.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}