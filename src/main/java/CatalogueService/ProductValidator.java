package CatalogueService;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

/**
 * Product Validator
 * Design Pattern: Strategy Pattern - Encapsulates validation logic
 * Can be extended with different validation strategies
 */
@Component
public class ProductValidator {
    
    /**
     * Validate product creation
     */
    public void validateProductCreation(ProductDTO productDTO) {
        validateRequiredFields(productDTO);
        validatePrices(productDTO);
        validateCategory(productDTO.getCategory());
        validateQuantity(productDTO.getQuantity());
    }
    
    /**
     * Validate product update
     */
    public void validateProductUpdate(Product existingProduct, ProductDTO updateDTO) {
        // Can't update certain fields if product is in auction or sold
        if (existingProduct.getStatus() == ProductStatus.IN_AUCTION ||
            existingProduct.getStatus() == ProductStatus.SOLD) {
            throw new InvalidProductStateException(
                "Cannot update product in state: " + existingProduct.getStatus()
            );
        }
        
        if (updateDTO.getStartingPrice() != null || updateDTO.getReservePrice() != null) {
            validatePrices(updateDTO);
        }
        
        if (updateDTO.getCategory() != null) {
            validateCategory(updateDTO.getCategory());
        }
        
        if (updateDTO.getQuantity() != null) {
            validateQuantity(updateDTO.getQuantity());
        }
    }
    
    /**
     * Validate required fields
     */
    private void validateRequiredFields(ProductDTO productDTO) {
        if (productDTO.getName() == null || productDTO.getName().trim().isEmpty()) {
            throw new InvalidProductException("Product name is required");
        }
        
        if (productDTO.getCategory() == null || productDTO.getCategory().trim().isEmpty()) {
            throw new InvalidProductException("Product category is required");
        }
        
        if (productDTO.getStartingPrice() == null) {
            throw new InvalidProductException("Starting price is required");
        }
        
        if (productDTO.getReservePrice() == null) {
            throw new InvalidProductException("Reserve price is required");
        }
        
        if (productDTO.getSellerId() == null) {
            throw new InvalidProductException("Seller ID is required");
        }
        
        if (productDTO.getEndDate() == null) {
            throw new InvalidProductException("Auction end date is required");
        }
        
        if (productDTO.getShippingCost() == null) {
            throw new InvalidProductException("Shipping cost is required");
        }
    }
    
    /**
     * Validate prices
     */
    private void validatePrices(ProductDTO productDTO) {
        BigDecimal startingPrice = productDTO.getStartingPrice();
        BigDecimal reservePrice = productDTO.getReservePrice();
        
        if (startingPrice != null && startingPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidProductException("Starting price must be greater than zero");
        }
        
        if (reservePrice != null && reservePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidProductException("Reserve price must be greater than zero");
        }
        
        if (startingPrice != null && reservePrice != null && 
            startingPrice.compareTo(reservePrice) > 0) {
            throw new InvalidProductException(
                "Starting price cannot be greater than reserve price"
            );
        }
    }
    
    /**
     * Validate price range for search
     */
    public void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice == null || maxPrice == null) {
            throw new InvalidProductException("Both min and max prices are required");
        }
        
        if (minPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidProductException("Minimum price cannot be negative");
        }
        
        if (maxPrice.compareTo(minPrice) < 0) {
            throw new InvalidProductException(
                "Maximum price cannot be less than minimum price"
            );
        }
    }
    
    /**
     * Validate category
     */
    private void validateCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            throw new InvalidProductException("Category cannot be empty");
        }
        
        // Add specific category validation if needed
        // For now, accept any non-empty string
    }
    
    /**
     * Validate quantity
     */
    private void validateQuantity(Integer quantity) {
        if (quantity != null && quantity < 1) {
            throw new InvalidProductException("Quantity must be at least 1");
        }
    }
}
