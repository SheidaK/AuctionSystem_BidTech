package com.BidTech.auctionSystem.payment.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.BidTech.auctionSystem.AuctionService.Auction;
import com.BidTech.auctionSystem.AuctionService.AuctionRepository;
import com.BidTech.auctionSystem.IAMService.User;
import com.BidTech.auctionSystem.IAMService.UserRepository;
import com.BidTech.auctionSystem.NotificationListener;
import com.BidTech.auctionSystem.payment.model.Payment;
import com.BidTech.auctionSystem.payment.model.Receipt;
import com.BidTech.auctionSystem.payment.repository.PaymentRepository;

/**
 * PaymentService — business logic for processing payments and generating receipts.
 *
 * <p>This service is responsible for:
 * <ol>
 *   <li>Verifying that the user attempting to pay is the auction winner</li>
 *   <li>Creating a {@link Payment} record with a unique transaction ID</li>
 *   <li>Generating a {@link Receipt} by combining payment data with the user's shipping address</li>
 * </ol>
 *
 * <p>It crosses service boundaries by injecting both {@link AuctionRepository} (to verify
 * the winner) and {@link UserRepository} (to retrieve the shipping address). This works
 * because all services run in the same JVM, but each repository uses its own
 * {@code EntityManagerFactory} pointing to a different SQLite file.
 */
@Service
public class PaymentService {

    @Autowired
    private NotificationListener notificationListener;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /** Repository for persisting and retrieving {@link Payment} entities. */
    @Autowired
    private PaymentRepository paymentRepository;

    /**
     * Repository for retrieving {@link User} entities.
     * Used to get the winner's shipping address for the receipt.
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * Repository for retrieving {@link Auction} entities.
     * Used to verify that the paying user is the auction winner.
     */
    @Autowired
    private AuctionRepository auctionRepository;

    /**
     * Returns all payment records from the database.
     * Used by the UI to display payment history.
     *
     * @return list of all {@link Payment} entities
     */
    public java.util.List<Payment> getAllPayments() {
        // JpaRepository.findAll() is inherited — returns every row in the payments table
        return paymentRepository.findAll();
    }

    /**
     * Processes a payment for an auction winner.
     *
     * <p>Validation steps:
     * <ol>
     *   <li>Amount must be greater than zero</li>
     *   <li>The user must be the auction winner (highest bidder on an ended auction)</li>
     * </ol>
     *
     * @param auctionId the ID of the auction being paid for
     * @param userId    the ID of the user making the payment
     * @param amount    the payment amount in dollars
     * @return the unique transaction ID string on success, or an error message string on failure
     */
    public String processPayment(Long auctionId, Long userId, double amount) {
        if (amount <= 0) {
            return "Negative payment is not accepted.";
        }

        if (!isWinner(userId, auctionId)) {
            return "Please make sure you are the winner.";
        }

        // Prevent duplicate payments — check if this auction has already been paid for.
        // A winning auction should only be paid once. Without this check, clicking
        // "Pay Now" multiple times would create multiple payment records.
        if (paymentRepository.findByAuctionId(auctionId).isPresent()) {
            return "This auction has already been paid for.";
        }

        // Generate a unique transaction ID from the current timestamp
        String transactionId = String.valueOf(System.currentTimeMillis());

        Payment payment = new Payment();
        payment.setAuctionId(auctionId);
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setStatus("SUCCESS");
        payment.setTransactionId(transactionId);
        payment.setReceiptUrl("/confirmation/" + transactionId);

        paymentRepository.save(payment);

        // Publish event
        Map<String, Object> event = new HashMap<>();
        event.put("type", "PaymentCompleted");
        event.put("auctionId", auctionId);
        event.put("userId", userId);
        event.put("transactionId", transactionId);
        event.put("amount", amount);

        rabbitTemplate.convertAndSend(
                "auction.events",
                "payment.completed",
                event
        );

        return transactionId;
    }

    /**
     * Checks whether the given user is the winner of the given auction.
     *
     * <p>A user is the winner if the auction exists, has ended (inactive),
     * and the user's ID matches the auction's {@code highestBidderId}.
     *
     * @param userId    the user ID to check
     * @param auctionId the auction ID to check
     * @return {@code true} if the user is the winner; {@code false} otherwise
     */
    private boolean isWinner(Long userId, Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId).orElse(null);
        return auction != null && userId.equals(auction.getHighestBidderId()) && !auction.isActive();
    }

    /**
     * Returns the status of a payment by its transaction ID.
     *
     * @param transactionId the unique transaction ID
     * @return the payment status string (e.g., "SUCCESS"), or "NOT_FOUND" if no match
     */
    public String getPaymentStatus(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
                .map(Payment::getStatus)
                .orElse("NOT_FOUND");
    }

    /**
     * Generates a receipt for a completed payment.
     *
     * <p>The receipt combines data from the {@link Payment} record with the winner's
     * shipping address from their {@link User} profile.
     *
     * @param paymentId the ID of the payment record
     * @return a {@link Receipt} object, or {@code null} if the payment is not found
     */
    public Receipt generateReceipt(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            return null;
        }

        User user = userRepository.findById(payment.getUserId()).orElse(null);

        // Assemble shipping address from user profile fields
        String shippingAddress = "Address not found";
        if (user != null) {
            shippingAddress = user.getStreetNumber() + " " + user.getStreetName()
                    + ", " + user.getCity() + ", " + user.getPostalCode();
        }

        return new Receipt(
            payment.getId(),
            payment.getUserId(),
            shippingAddress,
            "5-7 business days",
            payment.getAmount(),
            payment.getTransactionId(),
            payment.getStatus()
        );
    }

    /*
    @RabbitListener(queues = RabbitMQConfig.AUCTION_EVENTS_EXCHANGE)
    public void handleAuctionEnded(Map<String, Object> event) {

        if (!"AuctionEnded".equals(event.get("type"))) return;

        Long auctionId = ((Number) event.get("auctionId")).longValue();
        Object winnerObj = event.get("winnerId");
        Double price = ((Number) event.get("finalPrice")).doubleValue();

        System.out.println("\n🔔 AUCTION ENDED NOTIFICATION 🔔");

        if (winnerObj == null) {
            System.out.println("⚠️ Auction " + auctionId + " ended with NO winner.");
            return;
        }

        Long winnerId = ((Number) winnerObj).longValue();

        System.out.println("Auction ID: " + auctionId);
        System.out.println("Winner User ID: " + winnerId);
        System.out.println("Final Price: $" + price);

        System.out.println("👉 All users should be redirected to payment page");
        System.out.println("👉 ONLY User " + winnerId + " can complete payment\n");
    }
     */
}
