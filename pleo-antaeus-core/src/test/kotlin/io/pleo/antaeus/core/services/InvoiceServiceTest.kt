package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import java.math.BigDecimal

private const val NOT_FOUND_INVOICE_ID = 200
private const val EXISTING_INVOICE_ID_1 = 201
private const val EXISTING_INVOICE_ID_2 = 202
private const val CUSTOMER_ID_1 = 101
private const val CUSTOMER_ID_2 = 102
private const val INVOICE_AMOUNT_1 = 301L

class InvoiceServiceTest {
    private val money = Money(BigDecimal.valueOf(INVOICE_AMOUNT_1), Currency.EUR)
    private val invoice1 = Invoice(EXISTING_INVOICE_ID_1, CUSTOMER_ID_1, money, InvoiceStatus.PAID)
    private val invoice2 = Invoice(EXISTING_INVOICE_ID_2, CUSTOMER_ID_1, money, InvoiceStatus.PENDING)
    private val dal = mockk<AntaeusDal> {
        every { fetchInvoice(NOT_FOUND_INVOICE_ID) } returns null
        every { fetchInvoice(EXISTING_INVOICE_ID_1) } returns invoice1
        every { fetchInvoices() } returns listOf(invoice1, invoice2)
        every { fetchPendingInvoices() } returns listOf(invoice2)
    }
    private val invoiceService = InvoiceService(dal = dal)

    @Test
    fun `fetch should throw exception if invoice is not found`() {
        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(NOT_FOUND_INVOICE_ID)
        }
    }

    @Test
    fun `fetch should return invoice if found`() {
        val fetchedInvoice = invoiceService.fetch(EXISTING_INVOICE_ID_1)

        assertThat(fetchedInvoice, equalTo(invoice1))
    }

    @Test
    fun `fetchAll should return all invoices`() {
        val fetchedInvoices = invoiceService.fetchAll()

        assertThat(fetchedInvoices, containsInAnyOrder(invoice1, invoice2))
    }

    @Test
    fun `fetchPending should return all PENDING invoices`() {
        val pendingInvoices = invoiceService.fetchPending()

        assertThat(pendingInvoices, containsInAnyOrder(invoice2))
    }
}
