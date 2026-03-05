package CatalogueService;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Database initialization with sample data
 * Aligned with M1 specification: items with description, initial price, 
 * auction duration (endDate), shipping cost, and keywords
 */
@Configuration
public class LoadCatalogueDatabase {
    
    private static final Logger log = LoggerFactory.getLogger(LoadCatalogueDatabase.class);
    
    @Bean
    CommandLineRunner initCatalogueDatabase(ProductRepository productRepository) {
        return args -> {
            // Helper to create end dates
            Calendar cal = Calendar.getInstance();
            
            // Sample product 1
            cal.add(Calendar.DAY_OF_MONTH, 7);
            Date endDate1 = cal.getTime();
            Product laptop = new Product(
                "Dell XPS 15 Laptop",
                "High-performance laptop with Intel i7, 16GB RAM, 512GB SSD",
                "Electronics",
                new BigDecimal("800.00"),
                new BigDecimal("1200.00"),
                1L,
                endDate1,
                new BigDecimal("25.00"),
                "laptop, computer, dell, electronics, gaming"
            );
            laptop.setCondition("NEW");
            laptop.setImageUrl("https://example.com/laptop.jpg");
            laptop.setAuctionType("English Auction");
            productRepository.save(laptop);
            log.info("Preloaded: " + laptop);
            
            // Sample product 2
            cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, 5);
            Date endDate2 = cal.getTime();
            Product watch = new Product(
                "Vintage Rolex Watch",
                "Authentic vintage Rolex Submariner from 1980s",
                "Jewelry",
                new BigDecimal("5000.00"),
                new BigDecimal("8000.00"),
                1L,
                endDate2,
                new BigDecimal("15.00"),
                "watch, rolex, jewelry, vintage, luxury"
            );
            watch.setCondition("USED");
            watch.setImageUrl("https://example.com/watch.jpg");
            watch.setAuctionType("English Auction");
            productRepository.save(watch);
            log.info("Preloaded: " + watch);
            
            // Sample product 3
            cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, 10);
            Date endDate3 = cal.getTime();
            Product painting = new Product(
                "Abstract Art Painting",
                "Original abstract painting by local artist, 24x36 inches",
                "Art",
                new BigDecimal("200.00"),
                new BigDecimal("500.00"),
                1L,
                endDate3,
                new BigDecimal("30.00"),
                "art, painting, abstract, canvas, original"
            );
            painting.setCondition("NEW");
            painting.setImageUrl("https://example.com/painting.jpg");
            painting.setAuctionType("English Auction");
            productRepository.save(painting);
            log.info("Preloaded: " + painting);
            
            // Sample product 4 - Active
            cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, 3);
            Date endDate4 = cal.getTime();
            Product camera = new Product(
                "Canon EOS R5 Camera",
                "Professional mirrorless camera with 45MP sensor",
                "Electronics",
                new BigDecimal("2500.00"),
                new BigDecimal("3500.00"),
                1L,
                endDate4,
                new BigDecimal("20.00"),
                "camera, canon, photography, mirrorless, professional"
            );
            camera.setCondition("NEW");
            camera.setImageUrl("https://example.com/camera.jpg");
            camera.setAuctionType("English Auction");
            camera.activate(); // Make this one active
            productRepository.save(camera);
            log.info("Preloaded: " + camera);
            
            // Sample product 5
            cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, 14);
            Date endDate5 = cal.getTime();
            Product book = new Product(
                "First Edition Harry Potter",
                "First edition Harry Potter and the Philosopher's Stone",
                "Books",
                new BigDecimal("1000.00"),
                new BigDecimal("2000.00"),
                1L,
                endDate5,
                new BigDecimal("10.00"),
                "book, harry potter, first edition, collectible, rare"
            );
            book.setCondition("USED");
            book.setImageUrl("https://example.com/book.jpg");
            book.setAuctionType("English Auction");
            productRepository.save(book);
            log.info("Preloaded: " + book);
        };
    }
}
