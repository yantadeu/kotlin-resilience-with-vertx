package com.yantadeu.vertx.kotlin.test

import io.vertx.circuitbreaker.CircuitBreaker
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import java.util.function.Function

class FraudCheckService(
    private val vertx: Vertx,
    private val circuitBreaker: CircuitBreaker,
    private val apiUrl: String
) {

    private val client: WebClient = WebClient.create(vertx)
    private val handlerFactory = CoroutineHandlerFactory(vertx.dispatcher())

    suspend fun checkFraud(): Boolean {
        val response = circuitBreaker.execute(
            handlerFactory.create(
                handler = {
                    println("Requesting checkFraud...")
                    val response = client.getAbs(apiUrl).send().await()
                    Pair(response, response.bodyAsString().toBoolean())
                },
                failure = {
                    when (it.statusCode()) {
                        200 -> {
                            println(" Success (200)")
                            null
                        }
                        else -> {
                            println(" Failure (${it.statusCode()})")
                            "Status code was ${it.statusCode()}"
                        }
                    }
                }
            )
        )
        return response.await()
    }

    suspend fun checkFraudWithFallback(totalPrice: Int): Any {
        val handlerFactory = CoroutineHandlerFactory(vertx.dispatcher())
        val response = circuitBreaker.executeWithFallback(
            handlerFactory.create(
                handler = {
                    println("Requesting checkFraudWithFallback...")
                    val response = client.getAbs(apiUrl).send().await()
                    Pair(response, response.bodyAsString().toBoolean())
                },
                failure = {
                    when (it.statusCode()) {
                        200 -> {
                            println(" Success (200)")
                            null
                        }
                        else -> {
                            println(" Failure (${it.statusCode()})")
                            "Status code was ${it.statusCode()}"
                        }
                    }
                }
            ),
            Function {
                totalPrice > 100
            }
        )
        return response.await()
    }
}