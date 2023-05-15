package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder

private const val NOT_FOUND_CUSTOMER_ID = 100
private const val CUSTOMER_ID_1 = 101
private const val CUSTOMER_ID_2 = 102

class CustomerServiceTest {
    private val customer1 = Customer(CUSTOMER_ID_1, Currency.EUR)
    private val customer2 = Customer(CUSTOMER_ID_2, Currency.DKK)
    private val dal = mockk<AntaeusDal> {
        every { fetchCustomer(NOT_FOUND_CUSTOMER_ID) } returns null
        every { fetchCustomer(CUSTOMER_ID_1) } returns customer1
        every { fetchCustomer(CUSTOMER_ID_2) } returns customer2
        every { fetchCustomers() } returns listOf(customer1, customer2)
    }
    private val customerService = CustomerService(dal = dal)

    @Test
    fun `fetch should throw exception if customer is not found`() {
        assertThrows<CustomerNotFoundException> {
            customerService.fetch(NOT_FOUND_CUSTOMER_ID)
        }
    }

    @Test
    fun `fetch should return customer if found`() {
        val fetchedCustomer  = customerService.fetch(CUSTOMER_ID_1)

        assertThat(fetchedCustomer, equalTo(customer1))
    }

    @Test
    fun `fetchAll should return all customers`() {
        val fetchedCustomers  = customerService.fetchAll()

        assertThat(fetchedCustomers, containsInAnyOrder(customer1, customer2))
    }
}
