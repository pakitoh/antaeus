package io.pleo.antaeus.models

enum class InvoiceStatus {
    PENDING,
    PROCESSING,
    PAID,
    REJECTED,
    CURRENCY_MISMATCH,
    CUSTOMER_NOT_FOUND,
    ERROR
}
