package CatalogueService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product REST Controller
 * Design Pattern: Controller Pattern (MVC)
 * Handles HTTP requests and responses following REST principles
 */
@RestController
@RequestMapping("/api/catalogue")
public class ProductController {
    
    private final ProductService productService;
    
    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    
    /**
     * GET /api/catalogue/products - Get all products
     */
    @GetMapping("/products")
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        List<ProductDTO> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }
    
    /**
     * GET /api/catalogue/products/active - Get all active products
     */
    @GetMapping("/products/active")
    public ResponseEntity<List<ProductDTO>> getActiveProducts() {
        List<ProductDTO> products = productService.getActiveProducts();
        return ResponseEntity.ok(products);
    }
    
    /**
     * GET /api/catalogue/products/{id} - Get product by ID
     */
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        ProductDTO product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }
    
    /**
     * POST /api/catalogue/products - Create new product
     */
    @PostMapping("/products")
    public ResponseEntity<ProductDTO> createProduct(@RequestBody ProductDTO productDTO) {
        ProductDTO createdProduct = productService.createProduct(productDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }
    
    /**
     * PUT /api/catalogue/products/{id} - Update product
     */
    @PutMapping("/products/{id}")
    public ResponseEntity<ProductDTO> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductDTO productDTO) {
        ProductDTO updatedProduct = productService.updateProduct(id, productDTO);
        return ResponseEntity.ok(updatedProduct);
    }
    
    /**
     * DELETE /api/catalogue/products/{id} - Delete product
     */
    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * POST /api/catalogue/products/{id}/activate - Activate product
     */
    @PostMapping("/products/{id}/activate")
    public ResponseEntity<ProductDTO> activateProduct(@PathVariable Long id) {
        ProductDTO product = productService.activateProduct(id);
        return ResponseEntity.ok(product);
    }
    
    /**
     * POST /api/catalogue/products/{id}/deactivate - Deactivate product
     */
    @PostMapping("/products/{id}/deactivate")
    public ResponseEntity<ProductDTO> deactivateProduct(@PathVariable Long id) {
        ProductDTO product = productService.deactivateProduct(id);
        return ResponseEntity.ok(product);
    }
    
    /**
     * GET /api/catalogue/products/seller/{sellerId} - Get products by seller
     */
    @GetMapping("/products/seller/{sellerId}")
    public ResponseEntity<List<ProductDTO>> getProductsBySeller(@PathVariable Long sellerId) {
        List<ProductDTO> products = productService.getProductsBySeller(sellerId);
        return ResponseEntity.ok(products);
    }
    
    /**
     * GET /api/catalogue/products/category/{category} - Get products by category
     */
    @GetMapping("/products/category/{category}")
    public ResponseEntity<List<ProductDTO>> getProductsByCategory(@PathVariable String category) {
        List<ProductDTO> products = productService.getProductsByCategory(category);
        return ResponseEntity.ok(products);
    }
    
    /**
     * GET /api/catalogue/products/search?keyword={keyword} - Search products
     */
    @GetMapping("/products/search")
    public ResponseEntity<List<ProductDTO>> searchProducts(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        List<ProductDTO> products = productService.searchProducts(keyword);
        return ResponseEntity.ok(products);
    }
    
    /**
     * GET /api/catalogue/products/price-range?min={min}&max={max} - Get products by price range
     */
    @GetMapping("/products/price-range")
    public ResponseEntity<List<ProductDTO>> getProductsByPriceRange(
            @RequestParam BigDecimal min,
            @RequestParam BigDecimal max) {
        List<ProductDTO> products = productService.getProductsByPriceRange(min, max);
        return ResponseEntity.ok(products);
    }
    
    /**
     * PATCH /api/catalogue/products/{id}/status - Update product status
     * (For internal use by Auction service)
     */
    @PatchMapping("/products/{id}/status")
    public ResponseEntity<ProductDTO> updateProductStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request) {
        try {
            ProductStatus newStatus = ProductStatus.valueOf(request.getStatus());
            ProductDTO product = productService.updateProductStatus(id, newStatus);
            return ResponseEntity.ok(product);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * GET /api/catalogue/health - Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Catalogue Service is running");
    }
}
