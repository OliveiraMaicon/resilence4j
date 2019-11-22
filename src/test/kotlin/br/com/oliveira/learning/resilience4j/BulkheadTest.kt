package br.com.oliveira.learning.resilience4j

import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadConfig
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig
import io.vavr.collection.Stream
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class BulkheadTest {

    companion object {
        private const val NUMBER_OF_PADDLE = 4L
        private const val NUMBER_OF_PEOPLE = 20
    }

    private val logger = LoggerFactory.getLogger(BulkheadTest::class.java)
    private var availablePaddles: AtomicLong = AtomicLong(0)


    @BeforeEach
    fun setUp() {
        availablePaddles = AtomicLong(NUMBER_OF_PADDLE)
    }

    private val paddleNow = Runnable {
        try {
            logger.info("{} paddles left", availablePaddles.decrementAndGet())
            Thread.sleep(100)
        } catch (ex: InterruptedException) {
            logger.error(ex.message!!.toUpperCase())
            Thread.currentThread().interrupt()
        } finally {
            availablePaddles.incrementAndGet()
        }
    }


    private val paddleNowSync = object: Runnable {

        @Synchronized
        override fun run() {
            paddleNow.run()
        }


    }

    @Throws(InterruptedException::class)
    private fun everyBodyInTheBoat(task: Runnable) {
        val service = Executors.newFixedThreadPool(NUMBER_OF_PEOPLE)
        val tasks = Stream.continually(task)
                .map(Executors::callable)
                .take(NUMBER_OF_PEOPLE).toJavaList()
        service.invokeAll(tasks, 500, TimeUnit.MILLISECONDS)
    }

    @Throws(InterruptedException::class)
    @Test
    fun unlimited() {
        everyBodyInTheBoat(paddleNow)
    }

    @Throws(InterruptedException::class)
    @Test
    fun synchronized() {
        everyBodyInTheBoat(paddleNowSync)
    }

    @Throws(InterruptedException::class)
    @Test
    fun threadPoolBulkhead(){
        val threadPoolBulkhead = ThreadPoolBulkhead.of("threads", ThreadPoolBulkheadConfig.custom()
                .coreThreadPoolSize(2)
                .maxThreadPoolSize(NUMBER_OF_PADDLE.toInt())
                .queueCapacity(1)
                .build())

        val withBulkHead = ThreadPoolBulkhead.decorateRunnable(threadPoolBulkhead, paddleNow)

        everyBodyInTheBoat(withBulkHead)
    }

    @Throws(InterruptedException::class)
    @Test
    fun bulkhead(){
        val bulhead =  Bulkhead.of("semaphore", BulkheadConfig.custom()
                .maxConcurrentCalls(NUMBER_OF_PADDLE.toInt())
                .build())
        val withBulkHead = Bulkhead.decorateRunnable(bulhead, paddleNow)

        everyBodyInTheBoat(withBulkHead)

    }
}