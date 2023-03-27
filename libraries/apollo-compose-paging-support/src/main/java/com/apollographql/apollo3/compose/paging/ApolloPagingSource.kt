package com.apollographql.apollo3.compose.paging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation

@ApolloExperimental
class ApolloPagingSource<Data : Operation.Data, Value : Any>(
    /**
     * The call to fetch the next page, given the response for the current page.
     * This is also used for the initial/refresh call.
     * The response will be null for the initial/refresh call.
     * Return null if there is no next page.
     */
    private val appendCall: suspend (response: ApolloResponse<Data>?, loadSize: Int) -> ApolloCall<Data>?,

    /**
     * Count of items after the loaded data, given the response for the current page and the count of loaded items so far.
     * Used to display placeholders.
     * Return [PagingSource.LoadResult.Page.COUNT_UNDEFINED] if the count is unknown.
     */
    private val itemsAfter: suspend (response: ApolloResponse<Data>, loadedItemsCount: Int) -> Int = { _, _ -> LoadResult.Page.COUNT_UNDEFINED },

    /**
     * The call to fetch the previous page, given the response for the current page.
     * Return null if there is no previous page.
     * Can be null if prepend is not supported.
     */
    private val prependCall: (suspend (response: ApolloResponse<Data>, loadSize: Int) -> ApolloCall<Data>?)? = null,

    /**
     * Count of items before the loaded data, given the response for the current page and the count of loaded items so far.
     * Used to display placeholders.
     * Return [PagingSource.LoadResult.Page.COUNT_UNDEFINED] if the count is unknown.
     */
    private val itemsBefore: suspend (response: ApolloResponse<Data>, loadedItemsCount: Int) -> Int = { _, _ -> LoadResult.Page.COUNT_UNDEFINED },

    /**
     * Extract the list of items from a response.
     * Return [Result.failure] if the response cannot be used (e.g. it contains GraphQL errors).
     */
    private val getItems: suspend (response: ApolloResponse<Data>) -> Result<List<Value>>,
) : PagingSource<ApolloCall<Data>, Value>() {
  private var loadedItemsCount = 0

  override fun getRefreshKey(state: PagingState<ApolloCall<Data>, Value>): ApolloCall<Data>? = null

  override suspend fun load(params: LoadParams<ApolloCall<Data>>): LoadResult<ApolloCall<Data>, Value> {
    val call = when (params) {
      is LoadParams.Refresh -> {
        loadedItemsCount = 0
        appendCall(null, params.loadSize)
      }

      is LoadParams.Append -> params.key
      is LoadParams.Prepend -> params.key
    }

    val response = call?.execute() ?: error("appendCall must not return null for the initial/refresh call")
    if (response.exception != null) return LoadResult.Error(response.exception!!)
    val itemsResult: Result<List<Value>> = getItems(response)
    if (itemsResult.isFailure) return LoadResult.Error(itemsResult.exceptionOrNull()!!)
    val data = itemsResult.getOrThrow()
    loadedItemsCount += data.size
    val itemsBefore = if (params.placeholdersEnabled) {
      itemsBefore(response, loadedItemsCount)
    } else {
      LoadResult.Page.COUNT_UNDEFINED
    }
    val itemsAfter = if (params.placeholdersEnabled) {
      itemsAfter(response, loadedItemsCount)
    } else {
      LoadResult.Page.COUNT_UNDEFINED
    }
    return LoadResult.Page(
        data = data,
        prevKey = prependCall?.invoke(response, params.loadSize),
        nextKey = appendCall(response, params.loadSize),
        itemsBefore = itemsBefore,
        itemsAfter = itemsAfter,
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
     * Return null if there is no next page.
     */
    appendCall: suspend (response: ApolloResponse<Data>?, loadSize: Int) -> ApolloCall<Data>?,

    /**
     * Count of items after the loaded data, given the response for the current page and the count of loaded items so far.
     * Used to display placeholders.
     * Return [PagingSource.LoadResult.Page.COUNT_UNDEFINED] if the count is unknown.
     */
    itemsAfter: suspend (response: ApolloResponse<Data>, loadedItemsCount: Int) -> Int = { _, _ -> PagingSource.LoadResult.Page.COUNT_UNDEFINED },

    /**
     * The call to fetch the previous page, given the response for the current page.
     * Return null if there is no previous page.
     * Can be null if prepend is not supported.
     */
    prependCall: (suspend (response: ApolloResponse<Data>, loadSize: Int) -> ApolloCall<Data>?)? = null,

    /**
     * Count of items before the loaded data, given the response for the current page and the count of loaded items so far.
     * Used to display placeholders.
     * Return [PagingSource.LoadResult.Page.COUNT_UNDEFINED] if the count is unknown.
     */
    itemsBefore: suspend (response: ApolloResponse<Data>, loadedItemsCount: Int) -> Int = { _, _ -> PagingSource.LoadResult.Page.COUNT_UNDEFINED },

    /**
     * Extract the list of items from a response.
     * Return [Result.failure] if the response cannot be used (e.g. it contains GraphQL errors).
     */
    getItems: suspend (response: ApolloResponse<Data>) -> Result<List<Value>>,
): Pager<ApolloCall<Data>, Value> {
  return Pager(
      config = config,
      pagingSourceFactory = {
        ApolloPagingSource(
            appendCall = appendCall,
            itemsAfter = itemsAfter,
            prependCall = prependCall,
            itemsBefore = itemsBefore,
            getItems = getItems,
        )
      }
  )
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
     * Return null if there is no next page.
     */
    appendCall: suspend (response: ApolloResponse<Data>?, loadSize: Int) -> ApolloCall<Data>?,

    /**
     * Count of items after the loaded data, given the response for the current page and the count of loaded items so far.
     * Used to display placeholders.
     * Return [PagingSource.LoadResult.Page.COUNT_UNDEFINED] if the count is unknown.
     */
    itemsAfter: suspend (response: ApolloResponse<Data>, loadedItemsCount: Int) -> Int = { _, _ -> PagingSource.LoadResult.Page.COUNT_UNDEFINED },

    /**
     * The call to fetch the previous page, given the response for the current page.
     * Can be null if prepend is not supported.
     * Return null if there is no previous page.
     */
    prependCall: (suspend (response: ApolloResponse<Data>, loadSize: Int) -> ApolloCall<Data>?)? = null,

    /**
     * Count of items before the loaded data, given the response for the current page and the count of loaded items so far.
     * Used to display placeholders.
     * Return [PagingSource.LoadResult.Page.COUNT_UNDEFINED] if the count is unknown.
     */
    itemsBefore: suspend (response: ApolloResponse<Data>, loadedItemsCount: Int) -> Int = { _, _ -> PagingSource.LoadResult.Page.COUNT_UNDEFINED },

    /**
     * Extract the list of items from a response.
     * Return [Result.failure] if the response cannot be used (e.g. it contains GraphQL errors).
     */
    getItems: suspend (response: ApolloResponse<Data>) -> Result<List<Value>>,
): LazyPagingItems<Value> {
  val pager = remember {
    Pager(
        config = config,
        appendCall = appendCall,
        itemsAfter = itemsAfter,
        prependCall = prependCall,
        itemsBefore = itemsBefore,
        getItems = getItems,
    )
  }
  return pager.flow.collectAsLazyPagingItems()
}
