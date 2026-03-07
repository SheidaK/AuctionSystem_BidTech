#!/bin/bash
echo "BidTech Payment Module Test Script"

echo "1. SUCCESS - Process valid payment"
TRANSACTION_ID=$(curl -s -X POST "http://localhost:8080/api/payments/process?auctionId=1&userId=1&amount=99.99")
echo "Transaction ID generated: $TRANSACTION_ID"

echo -e "\n2. SUCCESS - Check payment status"
curl -v "http://localhost:8080/api/payments/status/$TRANSACTION_ID"

echo -e "\n3. ROBUSTNESS TEST - Negative amount"
curl -v -X POST "http://localhost:8080/api/payments/process?auctionId=1&userId=1&amount=-50"

echo -e "\n4. ROBUSTNESS TEST - Missing parameters"
curl -v -X POST "http://localhost:8080/api/payments/process?auctionId=1"

echo -e "\n End of Payment Tests "
