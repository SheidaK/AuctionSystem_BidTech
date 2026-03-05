package CatalogueService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Product Entity - represents items in the auction catalogue
 * Design Pattern: Entity Pattern (Domain Model)
 * 
 * Aligned with Milestone 1 CatalogueAPI specification:
 * - Item with description, initial price, auction duration, shipping cost, keywords
 */
@Entity
@Table(name = "products")
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(length = 2000)
    private String description;
    
    @Column(nullable = false)
    private String category;
    
    @Column(nullable = false)
    private BigDecimal startingPrice;  // Initial price from M1 spec
    
    @Column(nullable = false)
    private BigDecimal reservePrice;
    
    @Column(nullable = false)
    private Date endDate;  // Auction end date from M1 spec
    
    @Column(nullable = false)
    private BigDecimal shippingCost;  // Shipping cost from M1 spec
    
    @Column(length = 500)
    private String keywords;  // Associated keywords from M1 spec
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;
    
    @Column(nullable = false)
    private Long sellerId;
    
    private String imageUrl;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    private String condition; // NEW, USED, REFURBISHED
    
    private Integer quantity;
    
    private String auctionType;  // Type of auction from M1 spec
    
    // Constructors
    public Product() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = ProductStatus.DRAFT;
        this.quantity = 1;
        this.shippingCost = BigDecimal.ZERO;
    }
    
    public Product(String name, String description, String category, 
                   BigDecimal startingPrice, BigDecimal reservePrice, Long sellerId,
                   Date endDate, BigDecimal shippingCost, String keywords) {
        this();
        this.name = name;
        this.description = description;
        this.category = category;
        this.startingPrice = startingPrice;
        this.reservePrice = reservePrice;
        this.sellerId = sellerId;
        this.endDate = endDate;
        this.shippingCost = shippingCost;
        this.keywords = keywords;
    }
    
    // Getters
    public Long getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getCategory() {
        return category;
    }
    
    public BigDecimal getStartingPrice() {
        return startingPrice;
    }
    
    public BigDecimal getReservePrice() {
        return reservePrice;
    }
    
    public Date getEndDate() {
        return endDate;
    }
    
    public BigDecimal getShippingCost() {
        return shippingCost;
    }
    
    public String getKeywords() {
        return keywords;
    }
    
    public ProductStatus getStatus() {
        return status;
    }
    
    public Long getSellerId() {
        return sellerId;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public String getCondition() {
        return condition;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public String getAuctionType() {
        return auctionType;
    }
    
    // Setters
    public void setId(Long id) {
        this.id = id;
    }
    
    public void setName(String name) {
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setCategory(String category) {
        this.category = category;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setReservePrice(BigDecimal reservePrice) {
        this.reservePrice = reservePrice;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setShippingCost(BigDecimal shippingCost) {
        this.shippingCost = shippingCost;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setKeywords(String keywords) {
        this.keywords = keywords;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setStatus(ProductStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setCondition(String condition) {
        this.condition = condition;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void setAuctionType(String auctionType) {
        this.auctionType = auctionType;
        this.updatedAt = LocalDateTime.now();
    }
    
    // Business methods
    public boolean canBeListedForAuction() {
        return this.status == ProductStatus.DRAFT || this.status == ProductStatus.INACTIVE;
    }
    
    public void activate() {
        if (canBeListedForAuction()) {
            this.status = ProductStatus.ACTIVE;
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    public void deactivate() {
        this.status = ProductStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", startingPrice=" + startingPrice +
                ", status=" + status +
                ", sellerId=" + sellerId +
                '}';
    }
}
