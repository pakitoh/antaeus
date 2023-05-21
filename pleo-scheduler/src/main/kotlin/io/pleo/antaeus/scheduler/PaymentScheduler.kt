package io.pleo.antaeus.scheduler

import io.pleo.antaeus.core.services.BillingService
import mu.KotlinLogging
import org.quartz.CronScheduleBuilder
import org.quartz.Job
import org.quartz.JobBuilder
import org.quartz.JobExecutionContext
import org.quartz.Scheduler
import org.quartz.TriggerBuilder
import org.quartz.impl.StdSchedulerFactory
import org.quartz.spi.JobFactory
import org.quartz.spi.TriggerFiredBundle

private val logger = KotlinLogging.logger {}

class PaymentScheduler {

    fun schedulePending(billingService: BillingService, schedule: String) {
        val scheduler = StdSchedulerFactory("pendingInvoicesScheduler.properties").scheduler
        scheduler.setJobFactory(PendingInvoicesJobFactory(billingService));
        val trigger = TriggerBuilder.newTrigger()
            .startNow()
            .withSchedule(CronScheduleBuilder.cronSchedule(schedule))
            .build();
        val jobDetail = JobBuilder.newJob(PendingInvoicesJob::class.java).build()
        scheduler.scheduleJob(jobDetail, trigger)
        scheduler.start()
    }

    fun scheduleRejected(billingService: BillingService, schedule: String) {
        val scheduler = StdSchedulerFactory("rejectedInvoicesScheduler.properties").scheduler
        scheduler.setJobFactory(RejectedInvoicesJobFactory(billingService));
        val trigger = TriggerBuilder.newTrigger()
            .startNow()
            .withSchedule(CronScheduleBuilder.cronSchedule(schedule))
            .build();
        val jobDetail = JobBuilder.newJob(RejectedInvoicesJob::class.java).build()
        scheduler.scheduleJob(jobDetail, trigger)
        scheduler.start()
    }
}

class PendingInvoicesJobFactory(private val billingService: BillingService) : JobFactory {
    override fun newJob(bundle: TriggerFiredBundle?, scheduler: Scheduler?) : Job  =
        PendingInvoicesJob(billingService)
}

class PendingInvoicesJob (private val billingService: BillingService) : Job {
    override fun execute(context: JobExecutionContext?) {
        logger.debug { "Processing pending invoices job launched" }
        billingService.processPending()
    }
}

class RejectedInvoicesJobFactory(private val billingService: BillingService) : JobFactory {
    override fun newJob(bundle: TriggerFiredBundle?, scheduler: Scheduler?) : Job  =
        RejectedInvoicesJob(billingService)
}

class RejectedInvoicesJob (private val billingService: BillingService) : Job {
    override fun execute(context: JobExecutionContext?) {
        logger.debug { "Processing rejected invoices job launched" }
        billingService.processRejected()
    }
}
