package com.BidTech.auctionSystem.payment.model;

/**
 * Receipt — a value object (not persisted) representing a payment receipt.
 *
 * <p>A receipt is generated on demand by
 * {@link com.BidTech.auctionSystem.payment.service.PaymentService#generateReceipt(Long)}
 * by combining data from a {@link Payment} record and the winner's {@link com.BidTech.auctionSystem.IAMService.User}
 * profile (for the shipping address).
 *
 * <p>Unlike {@link Payment}, this class is not a JPA entity — it is constructed
 * in memory and returned directly as a JSON response. It is never stored in the database.
 */
public class Receipt {

    /** The ID of the underlying {@link Payment} record. */
    private Long paymentId;

    /** The user ID of the auction winner who made the payment. */
    private Long winnerUserId;

    /**
     * The shipping address assembled from the winner's user profile.
     * Format: "{streetNumber} {streetName}, {city}, {postalCode}"
     */
    private String shippingAddress;

    /**
     * Estimated shipping time as a human-readable string.
     * Currently hardcoded to "5-7 business days".
     */
    private String shippingTime;

    /** The total amount paid, in dollars. */
    private double totalAmount;

    /** The unique transaction ID from the {@link Payment} record. */
    private String transactionId;

    /** The payment status (e.g., "SUCCESS"). */
    private String status;

    /** Default no-argument constructor. */
    public Receipt() {}

    /**
     * Creates a fully populated receipt.
     *
     * @param paymentId       the ID of the payment record
     * @param winnerUserId    the user ID of the winner
     * @param shippingAddress the assembled shipping address
     * @param shippingTime    the estimated shipping time
     * @param totalAmount     the total amount paid
     * @param transactionId   the unique transaction ID
     * @param status          the payment status
     */
    public Receipt(Long paymentId, Long winnerUserId, String shippingAddress,
                   String shippingTime, double totalAmount, String transactionId, String status) {
        this.paymentId = paymentId;
        this.winnerUserId = winnerUserId;
        this.shippingAddress = shippingAddress;
        this.shippingTime = shippingTime;
        this.totalAmount = totalAmount;
        this.transactionId = transactionId;
        this.status = status;
    }

    /** @return the payment record ID */
    public Long getPaymentId() { return paymentId; }

    /** @return the winner's user ID */
    public Long getWinnerUserId() { return winnerUserId; }

    /** @return the shipping address string */
    public String getShippingAddress() { return shippingAddress; }

    /** @return the estimated shipping time */
    public String getShippingTime() { return shippingTime; }

    /** @return the total amount paid */
    public double getTotalAmount() { return totalAmount; }

    /** @return the unique transaction ID */
    public String getTransactionId() { return transactionId; }

    /** @return the payment status */
    public String getStatus() { return status; }
}