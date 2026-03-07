package com.BidTech.auctionSystem.CatalogueService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

/**
 * Product Service - Business Logic Layer
 * Design Patterns Used:
 * 1. Facade Pattern (ProductService) - Encapsulates business logic
 * 2. Facade Pattern - Provides simplified interface to complex subsystem
 */
@Service
public class ProductService {
    
    private final ProductRepository productRepository;
    private final ProductValidator productValidator;
    
    public ProductService(ProductRepository productRepository, ProductValidator productValidator) {
        this.productRepository = productRepository;
        this.productValidator = productValidator;
    }
    
    /**
     * Create a new product
     */
    public ProductDTO createProduct(ProductDTO productDTO) {
        // Validate input
        productValidator.validateProductCreation(productDTO);
        
        // Convert DTO to Entity
        Product product = new Product();
        updateProductFromDTO(product, productDTO);
        
        // Save and return
        Product savedProduct = productRepository.save(product);
        return new ProductDTO(savedProduct);
    }
    
    /**
     * Get product by ID
     */

    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return new ProductDTO(product);
    }
    
    /**
     * Get all products
     */

    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all active products
     */

    public List<ProductDTO> getActiveProducts() {
        return productRepository.findAllActiveProducts().stream()
                .map(ProductDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Get products by seller
     */

    public List<ProductDTO> getProductsBySeller(Long sellerId) {
        return productRepository.findBySellerId(sellerId).stream()
                .map(ProductDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Get products by category
     */

    public List<ProductDTO> getProductsByCategory(String category) {
        return productRepository.findByCategory(category).stream()
                .map(ProductDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Search products by keyword
     */

    public List<ProductDTO> searchProducts(String keyword) {
        return productRepository.searchByNameOrDescription(keyword).stream()
                .map(ProductDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Get products by price range
     */

    public List<ProductDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        productValidator.validatePriceRange(minPrice, maxPrice);
        return productRepository.findByPriceRange(minPrice, maxPrice).stream()
                .map(ProductDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Update product
     */
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        
        // Validate update
        productValidator.validateProductUpdate(product, productDTO);
        
        // Update fields
        updateProductFromDTO(product, productDTO);
        
        // Save and return
        Product updatedProduct = productRepository.save(product);
        return new ProductDTO(updatedProduct);
    }
    
    /**
     * Activate product (make available for auction)
     */
    public ProductDTO activateProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        
        if (!product.canBeListedForAuction()) {
            throw new InvalidProductStateException(
                "Product cannot be activated from current state: " + product.getStatus()
            );
        }
        
        product.activate();
        Product savedProduct = productRepository.save(product);
        return new ProductDTO(savedProduct);
    }
    
    /**
     * Deactivate product
     */
    public ProductDTO deactivateProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        
        product.deactivate();
        Product savedProduct = productRepository.save(product);
        return new ProductDTO(savedProduct);
    }
    
    /**
     * Delete product
     */
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        
        // Only allow deletion of DRAFT or INACTIVE products
        if (product.getStatus() == ProductStatus.IN_AUCTION || 
            product.getStatus() == ProductStatus.SOLD) {
            throw new InvalidProductStateException(
                "Cannot delete product in state: " + product.getStatus()
            );
        }
        
        productRepository.deleteById(id);
    }
    
    /**
     * Update product status (for integration with Auction service)
     */
    public ProductDTO updateProductStatus(Long id, ProductStatus newStatus) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        
        product.setStatus(newStatus);
        Product savedProduct = productRepository.save(product);
        return new ProductDTO(savedProduct);
    }
    
    // Helper method to update product from DTO
    private void updateProductFromDTO(Product product, ProductDTO dto) {
        if (dto.getName() != null) {
            product.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            product.setDescription(dto.getDescription());
        }
        if (dto.getCategory() != null) {
            product.setCategory(dto.getCategory());
        }
        if (dto.getStartingPrice() != null) {
            product.setStartingPrice(dto.getStartingPrice());
        }
        if (dto.getReservePrice() != null) {
            product.setReservePrice(dto.getReservePrice());
        }
        if (dto.getEndDate() != null) {
            product.setEndDate(dto.getEndDate());
        }
        if (dto.getShippingCost() != null) {
            product.setShippingCost(dto.getShippingCost());
        }
        if (dto.getKeywords() != null) {
            product.setKeywords(dto.getKeywords());
        }
        if (dto.getSellerId() != null) {
            product.setSellerId(dto.getSellerId());
        }
        if (dto.getImageUrl() != null) {
            product.setImageUrl(dto.getImageUrl());
        }
        if (dto.getCondition() != null) {
            product.setCondition(dto.getCondition());
        }
        if (dto.getQuantity() != null) {
            product.setQuantity(dto.getQuantity());
        }
        if (dto.getAuctionType() != null) {
            product.setAuctionType(dto.getAuctionType());
        }
    }
}
