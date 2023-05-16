package io.pleo.antaeus.core.services

import io.mockk.*
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
private const val INVOICE_AMOUNT = 301L

class InvoiceServiceTest {

    private val money = Money(BigDecimal.valueOf(INVOICE_AMOUNT), Currency.EUR)

    @Test
    fun `fetch should throw exception if invoice is not found`() {
        val dal = mockk<AntaeusDal> {
            every { fetchInvoice(NOT_FOUND_INVOICE_ID) } returns null
        }
        val invoiceService = InvoiceService(dal = dal)

        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(NOT_FOUND_INVOICE_ID)
        }
    }

    @Test
    fun `fetch should return invoice if found`() {
        val invoice = Invoice(EXISTING_INVOICE_ID_1, CUSTOMER_ID_1, money, InvoiceStatus.PAID)
        val dal = mockk<AntaeusDal> {
            every { fetchInvoice(EXISTING_INVOICE_ID_1) } returns invoice
        }
        val invoiceService = InvoiceService(dal = dal)

        val fetchedInvoice = invoiceService.fetch(EXISTING_INVOICE_ID_1)

        assertThat(fetchedInvoice, equalTo(invoice))
    }

    @Test
    fun `fetchAll should return all invoices`() {
        val invoice1 = Invoice(EXISTING_INVOICE_ID_1, CUSTOMER_ID_1, money, InvoiceStatus.PAID)
        val invoice2 = Invoice(EXISTING_INVOICE_ID_2, CUSTOMER_ID_1, money, InvoiceStatus.PENDING)
        val dal = mockk<AntaeusDal> {
            every { fetchInvoices() } returns listOf(invoice1, invoice2)
        }
        val invoiceService = InvoiceService(dal = dal)

        val fetchedInvoices = invoiceService.fetchAll()

        assertThat(fetchedInvoices, containsInAnyOrder(invoice1, invoice2))
    }

    @Test
    fun `fetchPending should return all PENDING invoices`() {
        val pendingInvoice = Invoice(EXISTING_INVOICE_ID_2, CUSTOMER_ID_1, money, InvoiceStatus.PENDING)
        val dal = mockk<AntaeusDal> {
            every { fetchPendingInvoices() } returns listOf(pendingInvoice)
        }
        val invoiceService = InvoiceService(dal = dal)

        val pendingInvoices = invoiceService.fetchPending()

        assertThat(pendingInvoices, containsInAnyOrder(pendingInvoice))
    }

    @Test
    fun `update should update invoice in DB`() {
        val invoice = Invoice(EXISTING_INVOICE_ID_2, CUSTOMER_ID_1, money, InvoiceStatus.PENDING)
        val invoiceToUpdate = invoice.copy(status = InvoiceStatus.PAID)
        val dal = mockk<AntaeusDal> {
            every { updateInvoice(EXISTING_INVOICE_ID_2, invoiceToUpdate) } just runs
        }
        val invoiceService = InvoiceService(dal = dal)

        invoiceService.update(id = invoiceToUpdate.id, updatedInvoice = invoiceToUpdate)

        verify { dal.updateInvoice(id = invoiceToUpdate.id, updatedInvoice = invoiceToUpdate) }
    }
}
