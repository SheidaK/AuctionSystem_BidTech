package com.bidtech.payment.controller;

import com.bidtech.payment.model.Payment;
import com.bidtech.payment.model.Receipt;
import com.bidtech.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     * Returns true = success
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

    @GetMapping("/status/{transactionId}")
    public ResponseEntity<String> getPaymentStatus(@PathVariable String transactionId) {
        
    	String status = paymentService.getPaymentStatus(transactionId);
        
    	return ResponseEntity.ok("Payment Status: " + status);
    }
    
    @GetMapping("/receipt/{paymentId}")
    public ResponseEntity<Receipt> generateReceipt(@PathVariable Long paymentId) {
        
    	Receipt receipt = paymentService.generateReceipt(paymentId);
        
    	if (receipt == null) {
            return ResponseEntity.notFound().build();
        }
        
    	return ResponseEntity.ok(receipt);
    }
}