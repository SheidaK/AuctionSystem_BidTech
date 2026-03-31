package com.BidTech.auctionSystem.payment.repository;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.BidTech.auctionSystem.payment.model.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionId(String transactionId);

    /** Check if a payment already exists for a given auction — prevents duplicate payments */
    Optional<Payment> findByAuctionId(Long auctionId);
}