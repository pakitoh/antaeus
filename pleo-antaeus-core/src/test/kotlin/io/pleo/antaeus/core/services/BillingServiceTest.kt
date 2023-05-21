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
    private val pendingInvoice = Invoice(INVOICE_ID_1, CUSTOMER_ID, money, InvoiceStatus.PENDING)
    private val pendingInvoice2 = Invoice(INVOICE_ID_2, CUSTOMER_ID, money, InvoiceStatus.REJECTED)
    private val paidInvoice = pendingInvoice.copy(status = InvoiceStatus.PAID)
    private val rejectedInvoice = pendingInvoice.copy(status = InvoiceStatus.REJECTED)
    private val rejectedInvoice2 = pendingInvoice2.copy(status = InvoiceStatus.REJECTED)
    private val currencyErrorInvoice = pendingInvoice.copy(status = InvoiceStatus.CURRENCY_MISMATCH)
    private val customerErrorInvoice = pendingInvoice.copy(status = InvoiceStatus.CUSTOMER_NOT_FOUND)
    private val errorInvoice = pendingInvoice.copy(status = InvoiceStatus.ERROR)

    @Test
    fun `bill should send invoice to payment and update invoice status to PAID when payment is accepted`() {
        val paymentProvider = sendInvoiceToPayment(pendingInvoice, true)
        val dal = updateInvoice(paidInvoice)
        val billingService = BillingService(
            paymentProvider = paymentProvider,
            invoiceService = InvoiceService(dal))

        billingService.bill(pendingInvoice)

        verifyInvoiceHasBeenUpdated(dal, pendingInvoice.id, paidInvoice)
    }

    @Test
    fun `bill should send invoice to payment and update invoice status to REJECTED when payment is rejected`() {
        val paymentProvider = sendInvoiceToPayment(pendingInvoice, false)
        val dal = updateInvoice(rejectedInvoice)
        val billingService = BillingService(
            paymentProvider = paymentProvider,
            invoiceService = InvoiceService(dal))

        billingService.bill(pendingInvoice)

        verifyInvoiceHasBeenUpdated(dal, pendingInvoice.id, rejectedInvoice)
    }

    @Test
    fun `bill should send invoice to payment and update invoice status to CURRENCY_MISMATCH when payment throws currency exception`() {
        val paymentProvider = sendInvoiceToPaymentThrows(
            pendingInvoice,
            CurrencyMismatchException(pendingInvoice.id, pendingInvoice.customerId))
        val dal = updateInvoice(currencyErrorInvoice)
        val billingService = BillingService(
            paymentProvider = paymentProvider,
            invoiceService = InvoiceService(dal))

        billingService.bill(pendingInvoice)

        verifyInvoiceHasBeenUpdated(dal, pendingInvoice.id, currencyErrorInvoice)
    }

    @Test
    fun `bill should send invoice to payment and update invoice status to CUSTOMER_NOT_FOUND when payment throws customer exception`() {
        val paymentProvider = sendInvoiceToPaymentThrows(
            pendingInvoice,
            CustomerNotFoundException(pendingInvoice.id))
        val dal = updateInvoice(customerErrorInvoice)
        val billingService = BillingService(
            paymentProvider = paymentProvider,
            invoiceService = InvoiceService(dal)
        )

        billingService.bill(pendingInvoice)

        verifyInvoiceHasBeenUpdated(dal, pendingInvoice.id, customerErrorInvoice)
    }

    @Test
    fun `bill should send invoice to payment and retry when network error sending to payment provider`() {
        val paymentProvider = sendInvoiceToPaymentThrowsAndThen(
            pendingInvoice,
            NetworkException(),
            true)
        val dal = updateInvoice(paidInvoice)
        val billingService = BillingService(
            paymentProvider = paymentProvider,
            invoiceService = InvoiceService(dal)
        )
        billingService.maxRetries = 2

        billingService.bill(pendingInvoice)

        verifyInvoiceHasBeenUpdated(dal, pendingInvoice.id, paidInvoice)
    }

    @Test
    fun `bill should send invoice to payment and update invoice status to ERROR when continue appearing network error until max`() {
        val paymentProvider = sendInvoiceToPaymentThrows(
            pendingInvoice,
            NetworkException())
        val dal = updateInvoice(errorInvoice)
        val billingService = BillingService(
            paymentProvider = paymentProvider,
            invoiceService = InvoiceService(dal)
        )
        billingService.maxRetries = 2

        billingService.bill(pendingInvoice)

        verifyInvoiceHasBeenUpdated(dal, pendingInvoice.id, errorInvoice)
    }

    @Test
    fun `processPending should fetch pending invoices and send them to payment`() {
        val paymentProvider = sendInvoicesToPayment(
            pendingInvoice, true,
            pendingInvoice2, false)
        val dal = fetchInvoicesAndUpdate(InvoiceStatus.PENDING, listOf(pendingInvoice, pendingInvoice2))
        val billingService = BillingService(
            paymentProvider = paymentProvider,
            invoiceService = InvoiceService(dal)
        )

        billingService.processPending()

        verifyInvoiceHasBeenUpdated(dal, pendingInvoice.id, paidInvoice)
        verifyInvoiceHasBeenUpdated(dal, pendingInvoice2.id, rejectedInvoice2)
    }

    @Test
    fun `processRejected should fetch rejected invoices and send them to payment`() {
        val paymentProvider = sendInvoicesToPayment(
            rejectedInvoice, true,
            rejectedInvoice2, false)
        val dal = fetchInvoicesAndUpdate(InvoiceStatus.REJECTED, listOf(rejectedInvoice, rejectedInvoice2))
        val billingService = BillingService(
            paymentProvider = paymentProvider,
            invoiceService = InvoiceService(dal)
        )

        billingService.processRejected()

        verifyInvoiceHasBeenUpdated(dal, rejectedInvoice.id, paidInvoice)
        verifyInvoiceHasBeenUpdated(dal, rejectedInvoice2.id, rejectedInvoice2)
    }


    private fun sendInvoiceToPayment(invoice : Invoice, result : Boolean) : PaymentProvider {
        return mockk<PaymentProvider> {
            every { charge(invoice) } returns result
        }
    }

    private fun sendInvoicesToPayment(invoice1 : Invoice,
                                      result1 : Boolean,
                                      invoice2 : Invoice,
                                      result2 : Boolean) : PaymentProvider {
        return mockk<PaymentProvider> {
            every { charge(invoice1) } returns result1
            every { charge(invoice2) } returns result2
        }
    }

    private fun sendInvoiceToPaymentThrows(invoice : Invoice, exception : Exception) : PaymentProvider {
        return mockk<PaymentProvider> {
            every { charge(invoice) } throws exception
        }
    }

    private fun sendInvoiceToPaymentThrowsAndThen(invoice : Invoice, exception : Exception, result: Boolean) : PaymentProvider {
        return mockk<PaymentProvider> {
            every { charge(invoice) } throws exception andThen result
        }
    }

    private fun updateInvoice(updatedInvoice : Invoice) : AntaeusDal {
        return mockk<AntaeusDal> {
            every { updateInvoice(updatedInvoice.id, updatedInvoice) } just runs
        }
    }

    private fun fetchInvoicesAndUpdate(status: InvoiceStatus, invoices : List<Invoice>) : AntaeusDal {
        return mockk<AntaeusDal> {
            every { fetchInvoicesByStatus(status) } returns invoices
            every { updateInvoice(any(), any()) } just runs
        }

    }

    private fun verifyInvoiceHasBeenUpdated(dal: AntaeusDal, invoiceId: Int, updatedInvoice : Invoice) {
        verify { dal.updateInvoice(id = invoiceId, updatedInvoice = updatedInvoice) }

    }
}