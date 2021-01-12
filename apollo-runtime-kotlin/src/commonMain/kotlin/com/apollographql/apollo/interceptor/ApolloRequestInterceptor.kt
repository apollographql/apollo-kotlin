package com.apollographql.apollo.interceptor

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Operation
import kotlinx.coroutines.flow.Flow

@ApolloExperimental
interface ApolloInterceptorChain {
  val customScalarAdapters: CustomScalarAdapters

  fun <D : Operation.Data> proceed(request: ApolloRequest<D>): Flow<ApolloResponse<D>>

  fun canProceed(): Boolean

}

@ApolloExperimental
interface ApolloRequestInterceptor {
  fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>>
}

@ApolloExperimental
internal class RealInterceptorChain(
    private val interceptors: List<ApolloRequestInterceptor>,
    private val index: Int,
    override val customScalarAdapters: CustomScalarAdapters,
) : ApolloInterceptorChain {

  override fun <D : Operation.Data> proceed(request: ApolloRequest<D>): Flow<ApolloResponse<D>> {
    check(index < interceptors.size)
    return interceptors[index].intercept(
        request,
        RealInterceptorChain(
            interceptors = interceptors,
            index = index + 1,
            customScalarAdapters = customScalarAdapters
        )
    )
  }

  override fun canProceed(): Boolean = index < interceptors.size
}
