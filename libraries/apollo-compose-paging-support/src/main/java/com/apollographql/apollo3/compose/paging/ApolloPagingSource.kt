package com.apollographql.apollo3.compose.paging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.compose.exception
import com.apollographql.apollo3.compose.tryExecute

@ApolloExperimental
class ApolloPagingSource<Data : Operation.Data, Value : Any>(
    /**
     * The call to fetch the next page, given the response for the current page.
     * This is also used for the initial/refresh call.
     * The response will be null for the initial/refresh call.
     */
    private val appendCall: (response: ApolloResponse<Data>?) -> ApolloCall<Data>,

    /**
     * Whether there is a next page, given the response for the current page.
     */
    private val hasNextPage: (response: ApolloResponse<Data>) -> Boolean,

    /**
     * The call to fetch the previous page, given the response for the current page.
     * Can be null if prepend is not supported.
     */
    private val prependCall: ((response: ApolloResponse<Data>) -> ApolloCall<Data>)? = null,

    /**
     * Whether there is a previous page, given the response for the current page.
     */
    private val hasPreviousPage: (response: ApolloResponse<Data>) -> Boolean = { false },

    /**
     * Extract the list of items from a response.
     * Return [Result.failure] if the response cannot be used (e.g. it contains GraphQL errors).
     */
    private val getItems: (response: ApolloResponse<Data>) -> Result<List<Value>>,
) : PagingSource<ApolloCall<Data>, Value>() {
    override fun getRefreshKey(state: PagingState<ApolloCall<Data>, Value>): ApolloCall<Data>? = null

    override suspend fun load(params: LoadParams<ApolloCall<Data>>): LoadResult<ApolloCall<Data>, Value> {
        val call = when (params) {
            is LoadParams.Refresh -> appendCall(null)
            is LoadParams.Append -> params.key
            is LoadParams.Prepend -> params.key
        }

        val response = call.tryExecute()
        if (response.exception != null) return LoadResult.Error(response.exception!!)
        val itemsResult: Result<List<Value>> = getItems(response)
        if (itemsResult.isFailure) return LoadResult.Error(itemsResult.exceptionOrNull()!!)
        return LoadResult.Page(
            data = itemsResult.getOrThrow(),
            prevKey = prependCall?.let { prependCall -> if (hasPreviousPage(response)) prependCall.invoke(response) else null },
            nextKey = if (hasNextPage(response)) appendCall(response) else null,
        )
    }
}

@ApolloExperimental
fun <Data : Operation.Data, Value : Any> Pager(
    /**
     * The [androidx.paging.Pager] configuration.
     */
    config: PagingConfig,

    /**
     * The call to fetch the next page, given the response for the current page.
     * This is also used for the initial/refresh call.
     * The response will be null for the initial/refresh call.
     */
    appendCall: (response: ApolloResponse<Data>?) -> ApolloCall<Data>,

    /**
     * Whether there is a next page, given the response for the current page.
     */
    hasNextPage: (response: ApolloResponse<Data>) -> Boolean,

    /**
     * The call to fetch the previous page, given the response for the current page.
     * Can be null if prepend is not supported.
     */
    prependCall: ((response: ApolloResponse<Data>) -> ApolloCall<Data>)? = null,

    /**
     * Whether there is a previous page, given the response for the current page.
     */
    hasPreviousPage: (response: ApolloResponse<Data>) -> Boolean = { false },

    /**
     * Extract the list of items from a response.
     * Return [Result.failure] if the response cannot be used (e.g. it contains GraphQL errors).
     */
    getItems: (response: ApolloResponse<Data>) -> Result<List<Value>>,
): Pager<ApolloCall<Data>, Value> {
    return Pager(
        config = config,
        pagingSourceFactory = { ApolloPagingSource(appendCall, hasNextPage, prependCall, hasPreviousPage, getItems) })
}

@ApolloExperimental
@Composable
fun <Data : Operation.Data, Value : Any> rememberAndCollectPager(
    /**
     * The [androidx.paging.Pager] configuration.
     */
    config: PagingConfig,

    /**
     * The call to fetch the next page, given the response for the current page.
     * This is also used for the initial/refresh call.
     * The response will be null for the initial/refresh call.
     */
    appendCall: (response: ApolloResponse<Data>?) -> ApolloCall<Data>,

    /**
     * Whether there is a next page, given the response for the current page.
     */
    hasNextPage: (response: ApolloResponse<Data>) -> Boolean,

    /**
     * The call to fetch the previous page, given the response for the current page.
     * Can be null if prepend is not supported.
     */
    prependCall: ((response: ApolloResponse<Data>) -> ApolloCall<Data>)? = null,

    /**
     * Whether there is a previous page, given the response for the current page.
     */
    hasPreviousPage: (response: ApolloResponse<Data>) -> Boolean = { false },

    /**
     * Extract the list of items from a response.
     * Return [Result.failure] if the response cannot be used (e.g. it contains GraphQL errors).
     */
    getItems: (response: ApolloResponse<Data>) -> Result<List<Value>>,
): LazyPagingItems<Value> {
    val pager = remember { Pager(config, appendCall, hasNextPage, prependCall, hasPreviousPage, getItems) }
    return pager.flow.collectAsLazyPagingItems()
}
