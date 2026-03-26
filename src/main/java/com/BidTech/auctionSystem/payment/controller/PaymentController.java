package com.BidTech.auctionSystem.payment.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.BidTech.auctionSystem.payment.model.Receipt;
import com.BidTech.auctionSystem.payment.service.PaymentService;

/**
 * PaymentController — REST controller for the Payment Service API.
 *
 * <p>All endpoints are prefixed with {@code /api/payments}. This controller
 * delegates all business logic to {@link PaymentService}.
 *
 * <p><b>Endpoints summary:</b>
 * <ul>
 *   <li>{@code POST /api/payments/process} — process a payment for an auction winner</li>
 *   <li>{@code GET  /api/payments/status/{transactionId}} — check payment status</li>
 *   <li>{@code GET  /api/payments/receipt/{paymentId}} — retrieve a receipt</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    /** The service layer containing all payment business logic. */
    @Autowired
    private PaymentService paymentService;

    /**
     * Processes a payment for an auction winner.
     *
     * <p>Example: {@code POST /api/payments/process?auctionId=1&userId=3&amount=150.00}
     *
     * <p>Returns the transaction ID string on success, or an error message if the
     * user is not the winner or the amount is invalid.
     *
     * @param auctionId the ID of the auction being paid for
     * @param userId    the ID of the user making the payment
     * @param amount    the payment amount in dollars
     * @return 200 OK with transaction ID, or 400 Bad Request with error message
     */
    @PostMapping("/process")
    public ResponseEntity<String> processPayment(
            @RequestParam Long auctionId,
            @RequestParam Long userId,
            @RequestParam double amount) {

        String transactionId = paymentService.processPayment(auctionId, userId, amount);

        if (transactionId == null) {
            return ResponseEntity.badRequest().body("Payment failed.");
        }

        return ResponseEntity.ok(transactionId);
    }

    /**
     * Returns all payment records.
     *
     * <p>Example: {@code GET /api/payments}
     *
     * @return 200 OK with a list of all {@link com.BidTech.auctionSystem.payment.model.Payment} records
     */
    @GetMapping
    public ResponseEntity<java.util.List<com.BidTech.auctionSystem.payment.model.Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    /**
     *
     * <p>Example: {@code GET /api/payments/status/1748765337573}
     *
     * @param transactionId the unique transaction ID
     * @return 200 OK with "Payment Status: {status}" string
     */
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<String> getPaymentStatus(@PathVariable String transactionId) {
        String status = paymentService.getPaymentStatus(transactionId);
        return ResponseEntity.ok("Payment Status: " + status);
    }

    /**
     * Generates and returns a receipt for a completed payment.
     *
     * <p>Example: {@code GET /api/payments/receipt/1}
     *
     * @param paymentId the ID of the payment record
     * @return 200 OK with {@link Receipt} JSON, or 404 Not Found if payment doesn't exist
     */
    @GetMapping("/receipt/{paymentId}")
    public ResponseEntity<Receipt> generateReceipt(@PathVariable Long paymentId) {
        Receipt receipt = paymentService.generateReceipt(paymentId);

        if (receipt == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(receipt);
    }
}