package CatalogueService;

/**
 * Product Status Enum
 * Represents the lifecycle states of a product in the catalogue
 */
public enum ProductStatus {
    DRAFT,          // Product created but not yet listed
    ACTIVE,         // Product is active and available for auction
    INACTIVE,       // Product temporarily removed from listings
    IN_AUCTION,     // Product is currently in an active auction
    SOLD,           // Product has been sold
    ARCHIVED        // Product archived (historical record)
}
