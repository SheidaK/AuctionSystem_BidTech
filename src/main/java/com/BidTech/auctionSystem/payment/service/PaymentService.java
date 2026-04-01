package com.BidTech.auctionSystem.payment.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    public String processPayment(Long auctionId, Long userId, Double amount) {
        Auction auction = auctionRepository.findById(auctionId).orElse(null);

        if (auction == null) {
            return "Error: Auction not found.";
        }

        if (auction.isActive()) {
            return "Error: Auction is still active.";
        }

        if (!userId.equals(auction.getHighestBidderId())) {
            return "Error: Only the winner can pay.";
        }

        if (amount < auction.getHighestBid()) {
            return "Error: Payment amount is less than the winning bid.";
        }

        boolean alreadyPaid = paymentRepository.findAll().stream()
                .anyMatch(p -> p.getAuctionId().equals(auctionId) && "COMPLETED".equals(p.getStatus()));

        if (alreadyPaid) {
            return "Error: Payment already processed for this auction.";
        }

        String transactionId = UUID.randomUUID().toString();
        Payment payment = new Payment(auctionId, userId, amount, transactionId, "COMPLETED");
        paymentRepository.save(payment);

        Map<String, Object> event = new HashMap<>();
        event.put("type", "PaymentCompleted");
        event.put("auctionId", auctionId);
        event.put("transactionId", transactionId);
        rabbitTemplate.convertAndSend("auction.events", "payment.completed", event);

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
        return paymentRepository.findAll().stream()
                .filter(p -> p.getTransactionId().equals(transactionId))
                .findFirst()
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
}
