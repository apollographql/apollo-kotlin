package com.apollographql.apollo3.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.normalized.watch
import com.apollographql.apollo3.exception.ApolloException
import kotlinx.coroutines.flow.catch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Returns a State that produces [ApolloResponse]s for this [ApolloCall].
 * Execution happens at first call.
 * The initial value is null, which can be used to mean 'waiting for the first value'.
 * [ApolloResponse.exception] can be used to check for [ApolloException]s thrown during execution.
 * Example:
 * ```
 *     val response by apolloClient.query(MyQuery()).toState()
 *     when {
 *         response == null -> Loading()
 *         response!!.exception != null -> NetworkError(response!!.exception!!)
 *         response!!.hasErrors() -> BackendError(response!!.errors!!)
 *         else -> Screen(response!!.data!!)
 *     }
 * ```
 */
@ApolloExperimental
@Composable
fun <D : Operation.Data> ApolloCall<D>.toState(context: CoroutineContext = EmptyCoroutineContext): State<ApolloResponse<D>?> {
  val responseFlow = remember {
    toFlow()
        .catch { emit(ApolloResponse(this@toState, it as? ApolloException ?: throw it)) }
  }
  return responseFlow.collectAsState(initial = null, context = context)
}

/**
 * Returns a State that produces [ApolloResponse]s by watching this [ApolloCall].
 * The initial value is null, which can be used to mean 'waiting for the first value'.
 * [ApolloResponse.exception] can be used to check for [ApolloException]s thrown during execution.
 * Example:
 * ```
 *     val response by apolloClient.query(MyQuery()).watchAsState()
 *     when {
 *         response == null -> Loading()
 *         response!!.exception != null -> NetworkError(response!!.exception!!)
 *         response!!.hasErrors() -> BackendError(response!!.errors!!)
 *         else -> Screen(response!!.data!!)
 *     }
 * ```
 */
@ApolloExperimental
@Composable
fun <D : Query.Data> ApolloCall<D>.watchAsState(context: CoroutineContext = EmptyCoroutineContext): State<ApolloResponse<D>?> {
  val responseFlow = remember {
    watch()
        .catch { emit(ApolloResponse(this@watchAsState, it as? ApolloException ?: throw it)) }
  }
  return responseFlow.collectAsState(initial = null, context = context)
}
