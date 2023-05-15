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

    @Test
    fun `fetch should throw exception if customer is not found`() {
        val dal = mockk<AntaeusDal> {
            every { fetchCustomer(NOT_FOUND_CUSTOMER_ID) } returns null
        }
        val customerService = CustomerService(dal = dal)

        assertThrows<CustomerNotFoundException> {
            customerService.fetch(NOT_FOUND_CUSTOMER_ID)
        }
    }

    @Test
    fun `fetch should return customer if found`() {
        val customer = Customer(CUSTOMER_ID_1, Currency.EUR)
        val dal = mockk<AntaeusDal> {
            every { fetchCustomer(CUSTOMER_ID_1) } returns customer
        }
        val customerService = CustomerService(dal = dal)

        val fetchedCustomer  = customerService.fetch(CUSTOMER_ID_1)

        assertThat(fetchedCustomer, equalTo(customer))
    }

    @Test
    fun `fetchAll should return all customers`() {
        val customer1 = Customer(CUSTOMER_ID_1, Currency.EUR)
        val customer2 = Customer(CUSTOMER_ID_2, Currency.DKK)
        val dal = mockk<AntaeusDal> {
            every { fetchCustomers() } returns listOf(customer1, customer2)
        }
        val customerService = CustomerService(dal = dal)

        val fetchedCustomers  = customerService.fetchAll()

        assertThat(fetchedCustomers, containsInAnyOrder(customer1, customer2))
    }
}
