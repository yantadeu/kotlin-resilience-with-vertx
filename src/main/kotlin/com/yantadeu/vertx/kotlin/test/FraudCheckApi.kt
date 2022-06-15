package com.yantadeu.vertx.kotlin.test

import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.await

class FraudCheckApi(private val vertx: Vertx, private val responses: List<(RoutingContext) -> Unit> = listOf()) {

    private var responseIndex = 0
    private var port = 8090

    private val server by lazy {
        val router = Router.router(vertx)
        router.route().handler {
            if (responses.isEmpty()) {
                println("Received request: Success")
                it.response().end("Success")
            } else {
                responses[responseIndex++](it)
            }
        }

        vertx.createHttpServer().requestHandler(router)
    }

    suspend fun start(): Int = server.listen(port).await().actualPort()

}
