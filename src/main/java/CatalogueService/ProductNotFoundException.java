package CatalogueService;

/**
 * Exception thrown when a product is not found
 */
public class ProductNotFoundException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    public ProductNotFoundException(Long id) {
        super("Could not find product with id: " + id);
    }
    
    public ProductNotFoundException(String message) {
        super(message);
    }
}
