package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {
    fun bill(invoice: Invoice) {
        var newStatus = sendInvoice(invoice)
        invoiceService.update(invoice.id, invoice.copy(status = newStatus))
    }

    private fun sendInvoice(invoice: Invoice): InvoiceStatus {
        logger.debug { "Requesting payment for invoice ${invoice.id}" }
        try {
            if (paymentProvider.charge(invoice)) {
                logger.debug { "Payment for invoice ${invoice.id} has been ACCEPTED" }
                return InvoiceStatus.PAID
            } else {
                logger.debug { "Payment for invoice ${invoice.id} has been REJECTED" }
                return InvoiceStatus.REJECTED
            }
        } catch (e: CurrencyMismatchException) {
            logger.error { "Wrong currency ${invoice.amount.currency} for invoice ${invoice.id}" }
            return InvoiceStatus.CURRENCY_ERROR
        } catch (e: CustomerNotFoundException) {
            logger.error { "Customer ${invoice.customerId} not found for invoice ${invoice.id}" }
            return InvoiceStatus.CUSTOMER_ERROR
        } catch (e: NetworkException) {
            logger.error { "Unexpected ERROR for invoice ${invoice.id}" }
            return InvoiceStatus.UNEXPECTED_ERROR
        }
    }
}
