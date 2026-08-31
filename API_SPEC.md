# QUICKBASKET: REST API SPECIFICATION

> **API Standard**: REST over HTTP/1.1  
> **Format**: JSON (`Content-Type: application/json`)  
> **Authentication**: Bearer Token (`Authorization: Bearer <JWT>`)  
> **Error Format**: RFC 7807 `ProblemDetail`  

---

## 1. Endpoints Overview

| Method | Endpoint Path | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/products/search` | Search product offers across quick-commerce platforms | No |
| `GET` | `/api/v1/products/{id}` | Get canonical product details | No |
| `GET` | `/api/v1/products/{id}/offers` | Get live price & stock offers for a product | No |
| `GET` | `/api/v1/products/{id}/price-history` | Get 30-day historical price trend records | No |
| `POST` | `/api/v1/auth/register` | Register new user account | No |
| `POST` | `/api/v1/auth/login` | Authenticate & obtain JWT token | No |
| `GET` | `/api/v1/watchlists` | Get user's watchlisted products | **Yes** |
| `POST` | `/api/v1/watchlists` | Add product to user's watchlist | **Yes** |
| `DELETE`| `/api/v1/watchlists/{id}` | Remove item from watchlist | **Yes** |
| `POST` | `/api/v1/price-alerts` | Create target price alert trigger | **Yes** |
| `DELETE`| `/api/v1/price-alerts/{id}` | Delete price alert trigger | **Yes** |

---

## 2. Endpoint Details

### 2.1 Product Search & Comparison
`GET /api/v1/products/search`

#### Request Parameters:
* `q` (string, required): Product search term (e.g. `Amul Taaza Milk 1L`)
* `lat` (string, optional, default: `12.9716`): Latitude
* `lng` (string, optional, default: `77.5946`): Longitude

#### Success Response (`200 OK`):
```json
{
  "query": "Amul Taaza Milk 1L",
  "totalResults": 3,
  "bestOption": {
    "cheapestPlatformCode": "BLINKIT",
    "cheapestPrice": 54.00,
    "fastestPlatformCode": "ZEPTO",
    "fastestEtaMinutes": 10
  },
  "offers": [
    {
      "platformCode": "BLINKIT",
      "platformName": "Blinkit",
      "price": 54.00,
      "mrp": 56.00,
      "discountPercentage": 3.57,
      "inStock": true,
      "etaMinutes": 14,
      "productUrl": "https://blinkit.com/prn/amul-taaza/prid/123",
      "imageUrl": "https://cdn.blinkit.com/images/amul_taaza.jpg"
    },
    {
      "platformCode": "ZEPTO",
      "platformName": "Zepto",
      "price": 56.00,
      "mrp": 56.00,
      "discountPercentage": 0.00,
      "inStock": true,
      "etaMinutes": 10,
      "productUrl": "https://zeptonow.com/pn/amul-taaza/id/456",
      "imageUrl": "https://cdn.zepto.com/images/amul_taaza.jpg"
    },
    {
      "platformCode": "INSTAMART",
      "platformName": "Swiggy Instamart",
      "price": 55.00,
      "mrp": 56.00,
      "discountPercentage": 1.78,
      "inStock": true,
      "etaMinutes": 12,
      "productUrl": "https://swiggy.com/instamart/item/789",
      "imageUrl": "https://cdn.swiggy.com/images/amul_taaza.jpg"
    }
  ]
}
```

---

### 2.2 Product Price History
`GET /api/v1/products/{id}/price-history?days=30`

#### Request Parameters:
* `days` (integer, optional, default: `30`): Number of historical days to fetch.

#### Success Response (`200 OK`):
```json
{
  "productId": 101,
  "productName": "Amul Taaza Milk 1L",
  "history": [
    {
      "platformCode": "BLINKIT",
      "price": 54.00,
      "mrp": 56.00,
      "inStock": true,
      "recordedAt": "2026-08-31T18:00:00Z"
    },
    {
      "platformCode": "BLINKIT",
      "price": 56.00,
      "mrp": 56.00,
      "inStock": true,
      "recordedAt": "2026-08-25T18:00:00Z"
    }
  ]
}
```

---

### 2.3 User Authentication: Registration
`POST /api/v1/auth/register`

#### Request Body:
```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```

#### Success Response (`201 Created`):
```json
{
  "id": 1,
  "email": "user@example.com",
  "createdAt": "2026-08-31T22:00:00Z"
}
```

---

### 2.4 User Authentication: Login
`POST /api/v1/auth/login`

#### Request Body:
```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```

#### Success Response (`200 OK`):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNzI1MTQxNjAwLCJleHAiOjE3MjUyMjgwMDB9...",
  "type": "Bearer",
  "expiresInSeconds": 86400
}
```

---

## 3. RFC 7807 Error Response Specification

All errors return `Content-Type: application/problem+json`:

### 3.1 Validation Error (`400 Bad Request`)
```json
{
  "type": "https://quickbasket.com/errors/invalid-input",
  "title": "Invalid Request Parameters",
  "status": 400,
  "detail": "Validation failed for request parameters.",
  "instance": "/api/v1/products/search",
  "invalidParams": [
    {
      "name": "q",
      "reason": "Search query parameter 'q' cannot be blank."
    }
  ],
  "timestamp": "2026-08-31T22:05:00Z"
}
```

### 3.2 Third-Party Provider Error (`503 Service Unavailable`)
```json
{
  "type": "https://quickbasket.com/errors/external-provider-unavailable",
  "title": "External Provider Outage",
  "status": 503,
  "detail": "QuickCommerce API provider timed out. Served cached snapshot.",
  "instance": "/api/v1/products/search",
  "timestamp": "2026-08-31T22:05:00Z"
}
```
