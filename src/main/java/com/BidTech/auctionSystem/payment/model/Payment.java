package com.BidTech.auctionSystem.payment.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Payment — JPA entity representing a completed payment transaction.
 *
 * <p>A payment record is created when the auction winner successfully pays for
 * the item they won. The record stores the auction ID, user ID, amount paid,
 * transaction status, and a unique transaction ID.
 *
 * <p>This entity is persisted in {@code PAYMENT.db} via the
 * {@code paymentEntityManagerFactory} configured in
 * {@link com.BidTech.auctionSystem.config.PaymentDbConfig}.
 *
 * <p>Only the auction winner (verified by checking
 * {@code auction.getHighestBidderId()}) can create a payment record.
 */
@Entity
@Table(name = "payments")
public class Payment {

    /** Auto-generated primary key for this payment record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The ID of the auction this payment is for.
     * References an {@link com.BidTech.auctionSystem.AuctionService.Auction} entity.
     */
    private Long auctionId;

    /**
     * The ID of the user who made this payment.
     * Must match the auction's {@code highestBidderId} (winner verification).
     */
    private Long userId;

    /** The amount paid in dollars. Must be greater than zero. */
    private double amount;

    /**
     * The payment status. Currently always {@code "SUCCESS"} for saved records.
     * Failed payments are not persisted.
     */
    private String status;

    /**
     * A unique transaction identifier generated from {@code System.currentTimeMillis()}.
     * Used to look up payment status and generate receipts.
     */
    private String transactionId;

    /**
     * A URL path to the receipt for this payment.
     * Format: {@code /confirmation/{transactionId}}
     */
    private String receiptUrl;

    /** Default no-argument constructor required by JPA. */
    public Payment() {}

    /**
     * Main constructor to create a new payment record.
     * @param auctionId     The ID of the auction being paid for
     * @param userId        The ID of the winner making the payment
     * @param amount        The winning bid amount
     * @param transactionId The unique ID generated for this transaction
     * @param status        The status of the payment
     */
    public Payment(Long auctionId, Long userId, double amount, String transactionId, String status) {
        this.auctionId = auctionId;
        this.userId = userId;
        this.amount = amount;
        this.transactionId = transactionId;
        this.status = status;
    }

    /** @return the payment's database ID */
    public Long getId() { return id; }

    /** @param id the database ID to set */
    public void setId(Long id) { this.id = id; }

    /** @return the auction ID this payment is for */
    public Long getAuctionId() { return auctionId; }

    /** @param auctionId the auction ID to set */
    public void setAuctionId(Long auctionId) { this.auctionId = auctionId; }

    /** @return the user ID who made this payment */
    public Long getUserId() { return userId; }

    /** @param userId the user ID to set */
    public void setUserId(Long userId) { this.userId = userId; }

    /** @return the amount paid in dollars */
    public double getAmount() { return amount; }

    /** @param amount the amount to set */
    public void setAmount(double amount) { this.amount = amount; }

    /** @return the payment status string */
    public String getStatus() { return status; }

    /** @param status the status to set */
    public void setStatus(String status) { this.status = status; }

    /** @return the unique transaction ID */
    public String getTransactionId() { return transactionId; }

    /** @param transactionId the transaction ID to set */
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    /** @return the receipt URL path */
    public String getReceiptUrl() { return receiptUrl; }

    /** @param receiptUrl the receipt URL to set */
    public void setReceiptUrl(String receiptUrl) { this.receiptUrl = receiptUrl; }
}
