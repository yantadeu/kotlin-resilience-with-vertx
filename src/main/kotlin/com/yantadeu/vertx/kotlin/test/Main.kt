package com.yantadeu.vertx.kotlin.test

import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.core.Vertx
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.circuitbreaker.circuitBreakerOptionsOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.*

object Main {
    private suspend fun <T> tryOrPrint(f: suspend () -> T): Unit = try {
        println("Fraud: ${f()}")
    } catch (t: Throwable) {
        println(t)
    }

    private suspend fun successAfterRetry(vertx: Vertx) {
        val responses: List<(RoutingContext) -> Unit> = listOf(
            { ctx -> ctx.response().setStatusCode(500).end() },
            { ctx -> ctx.response().setStatusCode(200).end("false") }
        )
        val server = FraudCheckApi(vertx, responses)
        val port = server.start()

        val options = circuitBreakerOptionsOf(
            maxRetries = 1
        )

        val circuitBreaker = CircuitBreaker.create(UUID.randomUUID().toString(), vertx, options)
        val client = FraudCheckService(vertx, circuitBreaker, "http://localhost:${port}")

        tryOrPrint { client.checkFraud() }
    }

    private suspend fun fallbackOnFailure(vertx: Vertx) {
        val responses: List<(RoutingContext) -> Unit> = listOf(
            { ctx -> ctx.response().setStatusCode(500).end() },
            { ctx -> ctx.response().setStatusCode(500).end() }
        )
        val server = FraudCheckApi(vertx, responses)
        val port = server.start()

        val options = circuitBreakerOptionsOf(
            maxRetries = 1,
            fallbackOnFailure = true
        )

        val circuitBreaker = CircuitBreaker.create(UUID.randomUUID().toString(), vertx, options)
        val client = FraudCheckService(vertx, circuitBreaker, "http://localhost:${port}")

        tryOrPrint { client.checkFraudWithFallback(50) }
    }

    private suspend fun waitForClosedCircuit(vertx: Vertx) {
        val responses: List<(RoutingContext) -> Unit> = listOf(
            { ctx -> ctx.response().setStatusCode(500).end() },
            { ctx -> ctx.response().setStatusCode(200).end("true") }
        )
        val server = FraudCheckApi(vertx, responses)
        val port = server.start()

        val options = circuitBreakerOptionsOf(
            resetTimeout = 5000,
            maxFailures = 1
        )

        val circuitBreaker = CircuitBreaker.create(UUID.randomUUID().toString(), vertx, options)
        val client = FraudCheckService(vertx, circuitBreaker, "http://localhost:${port}")

        tryOrPrint { client.checkFraud() }
        tryOrPrint { client.checkFraud() }
        delay(5000)
        tryOrPrint { client.checkFraud() }
    }

    suspend fun noFallBackOnFailure(vertx: Vertx) {
        val responses: List<(RoutingContext) -> Unit> = listOf(
            { ctx -> ctx.response().setStatusCode(500).end() },
            { _ -> println(" Timing out") },
            { _ -> println(" Timing out") },
            { ctx -> ctx.response().setStatusCode(200).end("true") }
        )
        val server = FraudCheckApi(vertx, responses)
        val port = server.start()

        val options = circuitBreakerOptionsOf(
            fallbackOnFailure = false,
            maxFailures = 1,
            maxRetries = 1,
            resetTimeout = 5000,
            timeout = 2000
        )

        val circuitBreaker = CircuitBreaker.create("kotlin-circuit-breaker", vertx, options)
        val client = FraudCheckService(vertx, circuitBreaker, "http://localhost:${port}")

        tryOrPrint { client.checkFraud() }
        tryOrPrint { client.checkFraud() }
        println("Waiting for circuit to be open again...")
        delay(5000)
        tryOrPrint { client.checkFraud() }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            val vertx = Vertx.vertx()
            try {
                //successAfterRetry(vertx)
                //fallbackOnFailure(vertx)
                //noFallBackOnFailure(vertx)
                waitForClosedCircuit(vertx)
            } finally {
                vertx.close()
            }
        }
    }
}