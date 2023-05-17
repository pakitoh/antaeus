package io.pleo.antaeus.models

enum class InvoiceStatus {
    PENDING,
    PAID,
    REJECTED,
    CURRENCY_MISMATCH,
    CUSTOMER_NOT_FOUND,
    ERROR
}
