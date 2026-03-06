#!/bin/bash

# Catalogue Service Test Scripts
# These scripts test the main flow and edge cases of the Catalogue Service

BASE_URL="http://localhost:8080/api/catalogue"

echo "========================================="
echo "CATALOGUE SERVICE TEST SUITE"
echo "========================================="
echo ""

# Color codes for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counter
TESTS_PASSED=0
TESTS_FAILED=0

# Function to print test result
print_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ PASSED${NC}: $2"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗ FAILED${NC}: $2"
        ((TESTS_FAILED++))
    fi
    echo ""
}

echo "========================================="
echo "1. HEALTH CHECK"
echo "========================================="
curl -X GET "$BASE_URL/health"
echo -e "\n"

echo "========================================="
echo "2. GET ALL PRODUCTS (Initial State)"
echo "========================================="
curl -X GET "$BASE_URL/products" | json_pp
echo -e "\n"

echo "========================================="
echo "3. CREATE NEW PRODUCT (Valid)"
echo "========================================="
PRODUCT_RESPONSE=$(curl -s -X POST "$BASE_URL/products" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "iPhone 15 Pro",
    "description": "Latest iPhone with A17 Pro chip",
    "category": "Electronics",
    "startingPrice": 800.00,
    "reservePrice": 1000.00,
    "sellerId": 1,
    "condition": "NEW",
    "quantity": 1,
    "imageUrl": "https://example.com/iphone.jpg"
  }')
echo "$PRODUCT_RESPONSE" | json_pp
PRODUCT_ID=$(echo "$PRODUCT_RESPONSE" | grep -o '"id":[0-9]*' | grep -o '[0-9]*' | head -1)
echo "Created Product ID: $PRODUCT_ID"
echo -e "\n"

echo "========================================="
echo "4. GET PRODUCT BY ID"
echo "========================================="
curl -X GET "$BASE_URL/products/$PRODUCT_ID" | json_pp
echo -e "\n"

echo "========================================="
echo "5. UPDATE PRODUCT"
echo "========================================="
curl -X PUT "$BASE_URL/products/$PRODUCT_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "iPhone 15 Pro Max",
    "description": "Latest iPhone with A17 Pro chip and larger display",
    "startingPrice": 900.00,
    "reservePrice": 1100.00
  }' | json_pp
echo -e "\n"

echo "========================================="
echo "6. ACTIVATE PRODUCT"
echo "========================================="
curl -X POST "$BASE_URL/products/$PRODUCT_ID/activate" | json_pp
echo -e "\n"

echo "========================================="
echo "7. GET ACTIVE PRODUCTS"
echo "========================================="
curl -X GET "$BASE_URL/products/active" | json_pp
echo -e "\n"

echo "========================================="
echo "8. SEARCH PRODUCTS BY KEYWORD"
echo "========================================="
curl -X GET "$BASE_URL/products/search?keyword=iPhone" | json_pp
echo -e "\n"

echo "========================================="
echo "9. GET PRODUCTS BY CATEGORY"
echo "========================================="
curl -X GET "$BASE_URL/products/category/Electronics" | json_pp
echo -e "\n"

echo "========================================="
echo "10. GET PRODUCTS BY SELLER"
echo "========================================="
curl -X GET "$BASE_URL/products/seller/1" | json_pp
echo -e "\n"

echo "========================================="
echo "11. GET PRODUCTS BY PRICE RANGE"
echo "========================================="
curl -X GET "$BASE_URL/products/price-range?min=500&max=1500" | json_pp
echo -e "\n"

echo "========================================="
echo "12. DEACTIVATE PRODUCT"
echo "========================================="
curl -X POST "$BASE_URL/products/$PRODUCT_ID/deactivate" | json_pp
echo -e "\n"

echo "========================================="
echo "ERROR HANDLING TESTS"
echo "========================================="

echo "13. CREATE PRODUCT - Missing Required Fields"
echo "-----------------------------------------"
curl -X POST "$BASE_URL/products" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Incomplete Product"
  }' | json_pp
echo -e "\n"

echo "14. CREATE PRODUCT - Invalid Price (Starting > Reserve)"
echo "-----------------------------------------"
curl -X POST "$BASE_URL/products" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Invalid Product",
    "description": "Test",
    "category": "Test",
    "startingPrice": 1000.00,
    "reservePrice": 500.00,
    "sellerId": 1
  }' | json_pp
echo -e "\n"

echo "15. CREATE PRODUCT - Negative Price"
echo "-----------------------------------------"
curl -X POST "$BASE_URL/products" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Negative Price Product",
    "description": "Test",
    "category": "Test",
    "startingPrice": -100.00,
    "reservePrice": 500.00,
    "sellerId": 1
  }' | json_pp
echo -e "\n"

echo "16. CREATE PRODUCT - Zero Price"
echo "-----------------------------------------"
curl -X POST "$BASE_URL/products" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Zero Price Product",
    "description": "Test",
    "category": "Test",
    "startingPrice": 0.00,
    "reservePrice": 500.00,
    "sellerId": 1
  }' | json_pp
echo -e "\n"

echo "17. GET NON-EXISTENT PRODUCT"
echo "-----------------------------------------"
curl -X GET "$BASE_URL/products/99999" | json_pp
echo -e "\n"

echo "18. UPDATE NON-EXISTENT PRODUCT"
echo "-----------------------------------------"
curl -X PUT "$BASE_URL/products/99999" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Name"
  }' | json_pp
echo -e "\n"

echo "19. DELETE NON-EXISTENT PRODUCT"
echo "-----------------------------------------"
curl -X DELETE "$BASE_URL/products/99999"
echo -e "\n"

echo "20. SEARCH WITH EMPTY KEYWORD"
echo "-----------------------------------------"
curl -X GET "$BASE_URL/products/search?keyword=" | json_pp
echo -e "\n"

echo "21. PRICE RANGE - Invalid (Max < Min)"
echo "-----------------------------------------"
curl -X GET "$BASE_URL/products/price-range?min=1000&max=500" | json_pp
echo -e "\n"

echo "22. PRICE RANGE - Negative Values"
echo "-----------------------------------------"
curl -X GET "$BASE_URL/products/price-range?min=-100&max=500" | json_pp
echo -e "\n"

echo "========================================="
echo "CLEANUP - DELETE TEST PRODUCT"
echo "========================================="
curl -X DELETE "$BASE_URL/products/$PRODUCT_ID"
echo -e "\n"

echo "========================================="
echo "TEST SUITE COMPLETED"
echo "========================================="
echo "Note: Review the responses above to verify correct behavior"
echo "Expected: Valid requests return 200/201, invalid requests return 400/404/409"
