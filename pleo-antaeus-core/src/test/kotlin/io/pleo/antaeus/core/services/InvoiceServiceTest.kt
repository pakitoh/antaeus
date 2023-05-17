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
    private val pendingInvoice = Invoice(EXISTING_INVOICE_ID_1, CUSTOMER_ID_1, money, InvoiceStatus.PENDING)
    private val paidInvoice = Invoice(EXISTING_INVOICE_ID_2, CUSTOMER_ID_1, money, InvoiceStatus.PAID)
    private val invoiceToUpdate = pendingInvoice.copy(status = InvoiceStatus.PAID)

    @Test
    fun `fetch should throw exception when invoice is not found`() {
        val dal = fetchInvoice(NOT_FOUND_INVOICE_ID, null)
        val invoiceService = InvoiceService(dal = dal)

        assertThrows<InvoiceNotFoundException> {
            invoiceService.fetch(NOT_FOUND_INVOICE_ID)
        }
    }

    @Test
    fun `fetch should return invoice when found`() {
        val dal = fetchInvoice(paidInvoice.id, paidInvoice)
        val invoiceService = InvoiceService(dal = dal)

        val fetchedInvoice = invoiceService.fetch(paidInvoice.id)

        assertThat(fetchedInvoice, equalTo(paidInvoice))
    }

    @Test
    fun `fetchAll should return all invoices`() {
        val dal = fetchInvoices(listOf(paidInvoice, pendingInvoice))
        val invoiceService = InvoiceService(dal = dal)

        val fetchedInvoices = invoiceService.fetchAll()

        assertThat(fetchedInvoices, containsInAnyOrder(paidInvoice, pendingInvoice))
    }

    @Test
    fun `fetchByStatus pending should return all PENDING invoices`() {
        val dal = fetchInvoicesByStatus(InvoiceStatus.PENDING, listOf(pendingInvoice))
        val invoiceService = InvoiceService(dal = dal)

        val pendingInvoices = invoiceService.fetchByStatus(InvoiceStatus.PENDING)

        assertThat(pendingInvoices, containsInAnyOrder(pendingInvoice))
    }

    @Test
    fun `fetchByStatus paid should return all PAID invoices`() {
        val dal = fetchInvoicesByStatus(InvoiceStatus.PAID, listOf(paidInvoice))
        val invoiceService = InvoiceService(dal = dal)

        val pendingInvoices = invoiceService.fetchByStatus(InvoiceStatus.PAID)

        assertThat(pendingInvoices, containsInAnyOrder(paidInvoice))
    }

    @Test
    fun `update should update invoice in DB`() {
        val dal = updateInvoice(invoiceToUpdate)
        val invoiceService = InvoiceService(dal = dal)

        invoiceService.update(id = invoiceToUpdate.id, updatedInvoice = invoiceToUpdate)

        verify { dal.updateInvoice(id = invoiceToUpdate.id, updatedInvoice = invoiceToUpdate) }
    }

    private fun fetchInvoice(id: Int, resul: Invoice?) : AntaeusDal {
        return mockk<AntaeusDal> {
            every { fetchInvoice(id) } returns resul
        }
    }

    private fun fetchInvoices(resul: List<Invoice>) : AntaeusDal {
        return mockk<AntaeusDal> {
            every { fetchInvoices() } returns resul
        }
    }

    private fun fetchInvoicesByStatus(status: InvoiceStatus, resul: List<Invoice>) : AntaeusDal {
        return mockk<AntaeusDal> {
            every { fetchInvoicesByStatus(status) } returns resul
        }
    }

    private fun updateInvoice(updatedInvoice : Invoice) : AntaeusDal {
        return mockk<AntaeusDal> {
            every { updateInvoice(updatedInvoice.id, updatedInvoice) } just runs
        }
    }
}
