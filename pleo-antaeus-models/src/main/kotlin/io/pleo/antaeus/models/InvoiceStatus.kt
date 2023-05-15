package io.pleo.antaeus.models

enum class InvoiceStatus {
    PENDING,
    PAID,
    REJECTED,
    CURRENCY_ERROR,
    CUSTOMER_ERROR,
    UNEXPECTED_ERROR
}
