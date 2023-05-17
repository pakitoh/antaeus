/*
    Configures the rest app along with basic exception handling and URL endpoints.
 */

package io.pleo.antaeus.rest

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.pleo.antaeus.core.exceptions.EntityNotFoundException
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class AntaeusRest(
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService,
    private val billingService: BillingService
) : Runnable {

    override fun run() {
        app.start(7000)
    }

    // Set up Javalin rest app
    private val app = Javalin
        .create()
        .apply {
            // InvoiceNotFoundException: return 404 HTTP status code
            exception(EntityNotFoundException::class.java) { _, ctx ->
                ctx.status(404).json( APIError("404", "Entity not found"))
            }
            // Bad invoice status passed as param
            exception(IllegalArgumentException::class.java) { e, ctx ->
                ctx.status(400).json( APIError("400", "Bad request"))
            }
            // Unexpected exception: return HTTP 500
            exception(Exception::class.java) { e, ctx ->
                logger.error(e) { "Internal server error" }
                ctx.status(500).json( APIError("500", "Server error: ${e.message}"))
            }
            // On 404: return message
            error(404) { ctx -> ctx.json( APIError("404", "Not found")) }
        }

    init {
        // Set up URL endpoints for the rest app
        app.routes {
            get("/") {
                it.result("Welcome to Antaeus! see AntaeusRest class for routes")
            }
            path("rest") {
                // Route to check whether the app is running
                // URL: /rest/health
                get("health") {
                    it.json("ok")
                }

                // V1
                path("v1") {
                    path("invoices") {
                        // URL: /rest/v1/invoices
                        get {
                            if(it.queryParams("byStatus").isEmpty()) {
                                it.json(invoiceService.fetchAll())
                            } else {
                                val status = InvoiceStatus.valueOf(it.queryParams("byStatus")[0].toUpperCase())
                                it.json(invoiceService.fetchByStatus(status = status))
                            }
                        }

                        // URL: /rest/v1/invoices/{:id}
                        get(":id") {
                            it.json(invoiceService.fetch(it.pathParam("id").toInt()))
                        }
                    }

                    path("customers") {
                        // URL: /rest/v1/customers
                        get {
                            it.json(customerService.fetchAll())
                        }

                        // URL: /rest/v1/customers/{:id}
                        get(":id") {
                            it.json(customerService.fetch(it.pathParam("id").toInt()))
                        }
                    }

                    path("bills") {
                        post {
                            it.json(billingService.processPending())
                        }
                    }
                }
            }
        }
    }
}
data class APIError ( val error: APIErrorDetails ) {
    constructor(code: String, message: String) : this(APIErrorDetails( code = code, message = message))
}
data class APIErrorDetails ( val code: String, val message: String)

