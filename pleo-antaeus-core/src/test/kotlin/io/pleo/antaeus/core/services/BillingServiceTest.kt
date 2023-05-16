package io.pleo.antaeus.core.services

import io.mockk.*
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

private const val INVOICE_ID_1 = 201
private const val INVOICE_ID_2 = 202
private const val INVOICE_AMOUNT = 301L
private const val CUSTOMER_ID = 101

class BillingServiceTest {
    private val money = Money(BigDecimal.valueOf(INVOICE_AMOUNT), Currency.EUR)

    @Test
    fun `bill should update invoice status if payment is accepted`() {
        val pendingInvoice = Invoice(INVOICE_ID_1, CUSTOMER_ID, money, InvoiceStatus.PENDING)
        val paidInvoice = pendingInvoice.copy(status = InvoiceStatus.PAID)
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(pendingInvoice) } returns true
        }
        val dal = mockk<AntaeusDal> {
            every { updateInvoice(INVOICE_ID_1, paidInvoice) } just runs
        }
        val billingService = BillingService(
            paymentProvider = paymentProvider,
            invoiceService = InvoiceService(dal)
        )

        billingService.bill(pendingInvoice)

        verify { dal.updateInvoice(id = INVOICE_ID_1, updatedInvoice = paidInvoice) }
    }

    @Test
    fun `bill should update invoice status if payment is rejected`() {
        val pendingInvoice = Invoice(INVOICE_ID_1, CUSTOMER_ID, money, InvoiceStatus.PENDING)
        val rejectedInvoice = pendingInvoice.copy(status = InvoiceStatus.REJECTED)
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(pendingInvoice) } returns false
        }
        val dal = mockk<AntaeusDal> {
            every { updateInvoice(INVOICE_ID_1, rejectedInvoice) } just runs
        }
        val billingService = BillingService(
            paymentProvider = paymentProvider,
            invoiceService = InvoiceService(dal)
        )

        billingService.bill(pendingInvoice)

        verify { dal.updateInvoice(id = INVOICE_ID_1, updatedInvoice = rejectedInvoice) }
    }

    @Test
    fun `bill should update invoice status if payment throws currency exception`() {
        val pendingInvoice = Invoice(INVOICE_ID_1, CUSTOMER_ID, money, InvoiceStatus.PENDING)
        val currencyErrorInvoice = pendingInvoice.copy(status = InvoiceStatus.CURRENCY_ERROR)
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(pendingInvoice) } throws CurrencyMismatchException(INVOICE_ID_1, CUSTOMER_ID)
        }
        val dal = mockk<AntaeusDal> {
            every { updateInvoice(INVOICE_ID_1, currencyErrorInvoice) } just runs
        }
        val billingService = BillingService(
            paymentProvider = paymentProvider,
            invoiceService = InvoiceService(dal)
        )

        billingService.bill(pendingInvoice)

        verify { dal.updateInvoice(id = INVOICE_ID_1, updatedInvoice = currencyErrorInvoice) }
    }

    @Test
    fun `bill should update invoice status if payment throws customer exception`() {
        val pendingInvoice = Invoice(INVOICE_ID_1, CUSTOMER_ID, money, InvoiceStatus.PENDING)
        val customerErrorInvoice = pendingInvoice.copy(status = InvoiceStatus.CUSTOMER_ERROR)
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(pendingInvoice) } throws CustomerNotFoundException(CUSTOMER_ID)
        }
        val dal = mockk<AntaeusDal> {
            every { updateInvoice(INVOICE_ID_1, customerErrorInvoice) } just runs
        }
        val billingService = BillingService(
            paymentProvider = paymentProvider,
            invoiceService = InvoiceService(dal)
        )

        billingService.bill(pendingInvoice)

        verify { dal.updateInvoice(id = INVOICE_ID_1, updatedInvoice = customerErrorInvoice) }
    }

    @Test
    fun `bill should retry update invoice status if network error sending to payment provider`() {
        val pendingInvoice = Invoice(INVOICE_ID_1, CUSTOMER_ID, money, InvoiceStatus.PENDING)
        val paidInvoice = pendingInvoice.copy(status = InvoiceStatus.PAID)
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(pendingInvoice) } throws NetworkException() andThen true
        }
        val dal = mockk<AntaeusDal> {
            every { updateInvoice(any(), any()) } just runs
        }
        val billingService = BillingService(
            paymentProvider = paymentProvider,
            invoiceService = InvoiceService(dal)
        )
        billingService.maxAttempts = 2

        billingService.bill(pendingInvoice)

        verify { dal.updateInvoice(id = INVOICE_ID_1, updatedInvoice = paidInvoice) }
    }

    @Test
    fun `bill should update invoice status if continue appearing network error when exhausted max retries sending to payment provider`() {
        val pendingInvoice = Invoice(INVOICE_ID_1, CUSTOMER_ID, money, InvoiceStatus.PENDING)
        val errorInvoice = pendingInvoice.copy(status = InvoiceStatus.UNEXPECTED_ERROR)
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(pendingInvoice) } throws NetworkException()
        }
        val dal = mockk<AntaeusDal> {
            every { updateInvoice(INVOICE_ID_1, errorInvoice) } just runs
        }
        val billingService = BillingService(
            paymentProvider = paymentProvider,
            invoiceService = InvoiceService(dal)
        )
        billingService.maxAttempts = 2

        billingService.bill(pendingInvoice)

        verify { dal.updateInvoice(id = INVOICE_ID_1, updatedInvoice = errorInvoice) }
    }
}