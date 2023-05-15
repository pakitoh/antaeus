package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

private const val EXISTING_INVOICE_ID = 201
private const val INVOICE_AMOUNT = 301L
private const val CUSTOMER_ID = 101

class BillingServiceTest {
    private val money = Money(BigDecimal.valueOf(INVOICE_AMOUNT), Currency.EUR)

    @Test
    fun `bill should update invoice status if payment is accepted`() {
        val pendingInvoice = Invoice(EXISTING_INVOICE_ID, CUSTOMER_ID, money, InvoiceStatus.PENDING)
        val paidInvoice = pendingInvoice.copy(status = InvoiceStatus.PAID)
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(pendingInvoice) } returns true
        }
        val dal = mockk<AntaeusDal> {
            every { updateInvoice(EXISTING_INVOICE_ID, paidInvoice) } returns Unit
        }
        val billingService = BillingService(
            paymentProvider = paymentProvider,
            invoiceService = InvoiceService(dal)
        )

        billingService.bill(pendingInvoice)

        verify { dal.updateInvoice(id = EXISTING_INVOICE_ID, updatedInvoice = paidInvoice) }
    }

    @Test
    fun `bill should update invoice status if payment is rejected`() {
        val pendingInvoice = Invoice(EXISTING_INVOICE_ID, CUSTOMER_ID, money, InvoiceStatus.PENDING)
        val rejectedInvoice = pendingInvoice.copy(status = InvoiceStatus.REJECTED)
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(pendingInvoice) } returns false
        }
        val dal = mockk<AntaeusDal> {
            every { updateInvoice(EXISTING_INVOICE_ID, rejectedInvoice) } returns Unit
        }
        val billingService = BillingService(
            paymentProvider = paymentProvider,
            invoiceService = InvoiceService(dal)
        )

        billingService.bill(pendingInvoice)

        verify { dal.updateInvoice(id = EXISTING_INVOICE_ID, updatedInvoice = rejectedInvoice) }
    }

    @Test
    fun `bill should update invoice status if payment throws currency exception`() {
        val pendingInvoice = Invoice(EXISTING_INVOICE_ID, CUSTOMER_ID, money, InvoiceStatus.PENDING)
        val currencyErrorInvoice = pendingInvoice.copy(status = InvoiceStatus.CURRENCY_ERROR)
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(pendingInvoice) } throws CurrencyMismatchException(EXISTING_INVOICE_ID, CUSTOMER_ID)
        }
        val dal = mockk<AntaeusDal> {
            every { updateInvoice(EXISTING_INVOICE_ID, currencyErrorInvoice) } returns Unit
        }
        val billingService = BillingService(
            paymentProvider = paymentProvider,
            invoiceService = InvoiceService(dal)
        )

        billingService.bill(pendingInvoice)

        verify { dal.updateInvoice(id = EXISTING_INVOICE_ID, updatedInvoice = currencyErrorInvoice) }
    }

    @Test
    fun `bill should update invoice status if payment throws customer exception`() {
        val pendingInvoice = Invoice(EXISTING_INVOICE_ID, CUSTOMER_ID, money, InvoiceStatus.PENDING)
        val customerErrorInvoice = pendingInvoice.copy(status = InvoiceStatus.CUSTOMER_ERROR)
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(pendingInvoice) } throws CustomerNotFoundException(CUSTOMER_ID)
        }
        val dal = mockk<AntaeusDal> {
            every { updateInvoice(EXISTING_INVOICE_ID, customerErrorInvoice) } returns Unit
        }
        val billingService = BillingService(
            paymentProvider = paymentProvider,
            invoiceService = InvoiceService(dal)
        )

        billingService.bill(pendingInvoice)

        verify { dal.updateInvoice(id = EXISTING_INVOICE_ID, updatedInvoice = customerErrorInvoice) }
    }

    @Test
    fun `bill should update invoice status if network error sending to payment provider`() {
        val pendingInvoice = Invoice(EXISTING_INVOICE_ID, CUSTOMER_ID, money, InvoiceStatus.PENDING)
        val errorInvoice = pendingInvoice.copy(status = InvoiceStatus.UNEXPECTED_ERROR)
        val paymentProvider = mockk<PaymentProvider> {
            every { charge(pendingInvoice) } throws NetworkException()
        }
        val dal = mockk<AntaeusDal> {
            every { updateInvoice(EXISTING_INVOICE_ID, errorInvoice) } returns Unit
        }
        val billingService = BillingService(
            paymentProvider = paymentProvider,
            invoiceService = InvoiceService(dal)
        )

        billingService.bill(pendingInvoice)

        verify { dal.updateInvoice(id = EXISTING_INVOICE_ID, updatedInvoice = errorInvoice) }
    }
}