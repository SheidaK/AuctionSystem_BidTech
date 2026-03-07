package com.BidTech.auctionSystem.payment.service;

import com.BidTech.auctionSystem.payment.model.Payment;
import com.BidTech.auctionSystem.payment.model.Receipt;
import com.BidTech.auctionSystem.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    /**
     * Processes a payment request
     */
    public String processPayment(Long auctionId, Long userId, double amount) {

        if (amount <= 0) {
            return "Negative payment is not accepted.";
        }

        if (!isWinner(userId, auctionId)) {
            return "Please make sure you are the winner.";
        }

        String transactionId = String.valueOf(System.currentTimeMillis());

        Payment payment = new Payment();
        payment.setAuctionId(auctionId);
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setStatus("SUCCESS");
        payment.setTransactionId(transactionId);
        payment.setReceiptUrl("/confirmation/" + transactionId);

        paymentRepository.save(payment);

        return transactionId;
    }

    /**
     * Placeholder winner check
     * In a full system this would call the Auction service
     */
    private boolean isWinner(Long userId, Long auctionId) {
        return true;
    }

    /**
     * Retrieves payment status
     */
    public String getPaymentStatus(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
                .map(Payment::getStatus)
                .orElse("NOT_FOUND");
    }

    /**
     * Generates a receipt for a payment
     */
    public Receipt generateReceipt(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId).orElse(null);

        if (payment == null) {
            return null;
        }

        String shippingAddress = "Shipping address unavailable";

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