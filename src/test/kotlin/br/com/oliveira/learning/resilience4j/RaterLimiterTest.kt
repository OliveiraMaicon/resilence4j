package br.com.oliveira.learning.resilience4j

import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import io.vavr.control.Try
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.Duration

class RaterLimiterTest {

    private val logger =  LoggerFactory.getLogger(RaterLimiterTest::class.java)

    @Test
    fun overLimit(){

        val raterLimiter = RateLimiter.of("name", RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(2))
                .limitForPeriod(2)
                .timeoutDuration(Duration.ZERO)
                .build())

        val withRateLimiter = RateLimiter.decorateRunnable(raterLimiter, this::evacuate)

        val first =  Try.runRunnable(withRateLimiter)

        val second =  Try.runRunnable(withRateLimiter)

        val thrid =  Try.runRunnable(withRateLimiter)


        assertThat(first.isSuccess).isTrue()
        assertThat(second.isSuccess).isTrue()
        assertThat(thrid.isSuccess).isFalse()
        assertThat(thrid.cause).isInstanceOf(RequestNotPermitted::class.java)

    }


    @Test
    fun timeout(){

        val raterLimiter = RateLimiter.of("name", RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(2))
                .limitForPeriod(1)
                .timeoutDuration(Duration.ofSeconds(2))
                .build())

        val withRateLimiter = RateLimiter.decorateRunnable(raterLimiter, this::evacuate)

        val first =  Try.runRunnable(withRateLimiter)

        val second =  Try.runRunnable(withRateLimiter)

        assertThat(first.isSuccess).isTrue()
        assertThat(second.isSuccess).isTrue()


    }


    private fun evacuate(){
        logger.info("ecavacuated")
    }


}