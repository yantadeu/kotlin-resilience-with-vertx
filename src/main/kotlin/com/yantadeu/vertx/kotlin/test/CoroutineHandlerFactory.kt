package com.yantadeu.vertx.kotlin.test

import io.vertx.core.Handler
import io.vertx.core.Promise
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class CoroutineHandlerFactory(override val coroutineContext: CoroutineDispatcher) : CoroutineScope {

    fun <F, R> create(
        handler: suspend () -> Pair<F, R>,
        failure: suspend (F) -> String? = { null }
    ): Handler<Promise<R>> =
        Handler {
            launch(coroutineContext) {
                try {
                    val res = handler()
                    val potentialFailure = failure(res.first)
                    if (potentialFailure != null) {
                        it.fail(potentialFailure)
                    } else {
                        it.complete(res.second)
                    }
                } catch (e: Exception) {
                    it.fail(e)
                }
            }
        }

    fun <T> create(handler: suspend () -> T): Handler<Promise<T>> =
        Handler {
            launch(coroutineContext) {
                try {
                    it.complete(handler())
                } catch (e: Exception) {
                    it.fail(e)
                }
            }
        }

}