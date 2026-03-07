package com.BidTech.auctionSystem.CatalogueService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Product Data Transfer Object
 * Design Pattern: DTO Pattern (Data Transfer Object)
 * Used to decouple API representation from domain model
 * 
 * Aligned with M1 CatalogueAPI specification
 */
public class ProductDTO {
    
    private Long id;
    private String name;
    private String description;
    private String category;
    private BigDecimal startingPrice;  // Initial price
    private BigDecimal reservePrice;
    private Date endDate;  // Auction end date
    private BigDecimal shippingCost;  // Shipping cost
    private String keywords;  // Associated keywords
    private String status;
    private Long sellerId;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String condition;
    private Integer quantity;
    private String auctionType;  // Type of auction
    
    // Constructors
    public ProductDTO() {}
    
    public ProductDTO(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.description = product.getDescription();
        this.category = product.getCategory();
        this.startingPrice = product.getStartingPrice();
        this.reservePrice = product.getReservePrice();
        this.endDate = product.getEndDate();
        this.shippingCost = product.getShippingCost();
        this.keywords = product.getKeywords();
        this.status = product.getStatus().name();
        this.sellerId = product.getSellerId();
        this.imageUrl = product.getImageUrl();
        this.createdAt = product.getCreatedAt();
        this.updatedAt = product.getUpdatedAt();
        this.condition = product.getCondition();
        this.quantity = product.getQuantity();
        this.auctionType = product.getAuctionType();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public BigDecimal getStartingPrice() {
        return startingPrice;
    }
    
    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
    }
    
    public BigDecimal getReservePrice() {
        return reservePrice;
    }
    
    public void setReservePrice(BigDecimal reservePrice) {
        this.reservePrice = reservePrice;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Long getSellerId() {
        return sellerId;
    }
    
    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getCondition() {
        return condition;
    }
    
    public void setCondition(String condition) {
        this.condition = condition;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public Date getEndDate() {
        return endDate;
    }
    
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }
    
    public BigDecimal getShippingCost() {
        return shippingCost;
    }
    
    public void setShippingCost(BigDecimal shippingCost) {
        this.shippingCost = shippingCost;
    }
    
    public String getKeywords() {
        return keywords;
    }
    
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }
    
    public String getAuctionType() {
        return auctionType;
    }
    
    public void setAuctionType(String auctionType) {
        this.auctionType = auctionType;
    }
}
