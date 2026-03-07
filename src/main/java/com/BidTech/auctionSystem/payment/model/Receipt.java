package com.BidTech.auctionSystem.payment.model;

public class Receipt {

    private Long paymentId;
    private Long winnerUserId;
    private String shippingAddress;
    private String shippingTime;
    private double totalAmount;
    private String transactionId;
    private String status;

    public Receipt() {}

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

    public Long getWinnerUserId() {
        return winnerUserId;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public String getShippingTime() {
        return shippingTime;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getStatus() {
        return status;
    }

    public Long getPaymentId() {
        return paymentId;
    }
}