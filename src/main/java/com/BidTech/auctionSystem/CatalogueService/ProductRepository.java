package com.BidTech.auctionSystem.CatalogueService;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Product Repository - Data Access Layer
 * Design Pattern: Repository Pattern
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // Find by seller
    List<Product> findBySellerId(Long sellerId);
    
    // Find by category
    List<Product> findByCategory(String category);
    
    // Find by status
    List<Product> findByStatus(ProductStatus status);
    
    // Find active products
    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE'")
    List<Product> findAllActiveProducts();
    
    // Find by category and status
    List<Product> findByCategoryAndStatus(String category, ProductStatus status);
    
    // Find by seller and status
    List<Product> findBySellerIdAndStatus(Long sellerId, ProductStatus status);
    
    // Search by name (case-insensitive)
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> searchByName(@Param("keyword") String keyword);
    
    // Search by name or description
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> searchByNameOrDescription(@Param("keyword") String keyword);
    
    // Find products within price range
    @Query("SELECT p FROM Product p WHERE p.startingPrice BETWEEN :minPrice AND :maxPrice " +
           "AND p.status = 'ACTIVE'")
    List<Product> findByPriceRange(@Param("minPrice") BigDecimal minPrice, 
                                    @Param("maxPrice") BigDecimal maxPrice);
    
    // Find products by multiple categories
    @Query("SELECT p FROM Product p WHERE p.category IN :categories AND p.status = 'ACTIVE'")
    List<Product> findByCategories(@Param("categories") List<String> categories);
}
