package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.infra.MAX_ATTEMPTS
import io.pleo.antaeus.core.infra.Retry
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}


class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {

    var maxRetries : Int = MAX_ATTEMPTS

    fun bill(invoice: Invoice) {
        var newStatus = sendInvoiceWithRetry(invoice)
        invoiceService.update(invoice.id, invoice.copy(status = newStatus))
    }

    private fun sendInvoiceWithRetry(invoice: Invoice): InvoiceStatus {
        return Retry(maxRetries)
            .withRetry(
                ::sendInvoice,
                invoice,
                ::isRetryNeeded
            )
    }

    private fun isRetryNeeded(status: InvoiceStatus) = status == InvoiceStatus.ERROR

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
            return InvoiceStatus.CURRENCY_MISMATCH
        } catch (e: CustomerNotFoundException) {
            logger.error { "Customer ${invoice.customerId} not found for invoice ${invoice.id}" }
            return InvoiceStatus.CUSTOMER_NOT_FOUND
        } catch (e: NetworkException) {
            logger.error { "Unexpected ERROR for invoice ${invoice.id}" }
            return InvoiceStatus.ERROR
        }
    }

    fun processPending() {
        logger.info { "Starting to request the payment of pending invoices" }
        invoiceService
            .fetchByStatusAndUpdate(InvoiceStatus.PENDING)
            .forEach { bill(it) }
    }

    fun processRejected() {
        logger.info { "Starting to request the payment of rejected invoices" }
        invoiceService
            .fetchByStatusAndUpdate(InvoiceStatus.REJECTED)
            .forEach { bill(it) }
    }
}
