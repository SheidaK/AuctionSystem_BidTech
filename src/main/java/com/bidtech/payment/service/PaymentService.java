package com.bidtech.payment.service;

import com.bidtech.payment.model.Payment;
import com.bidtech.payment.model.Receipt;
import com.bidtech.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.bidtech.*;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AuctionRepository auctionRepository;

    /**
     * Returns boolean: true = success, false = not winner or error
     * Validates that the user is the winner before processing payment
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
     * Winner check (placeholder for now)
     */
    private boolean isWinner(Long userId, Long auctionId) {
    	
        Auction auction = auctionRepository.findById(auctionId).orElse(null);
        
        return auction != null && userId.equals(auction.getHighestBidderId()) && !auction.isActive();
    }

    public String getPaymentStatus(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
                .map(Payment::getStatus)
                .orElse("NOT_FOUND");
    }
    
    public Receipt generateReceipt(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            return null;
        }

        User user = userRepository.findById(payment.getUserId()).orElse(null);
        
        String shippingAddress = "Address not found";
        
        if (user != null) {
            shippingAddress = user.getStreetNumber() + " " + user.getStreetName() + ", " + user.getCity() + ", " + user.getPostalCode();
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