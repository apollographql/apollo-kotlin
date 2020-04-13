package com.apollographql.apollo.kmpsample.data

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.kmpsample.GithubRepositoriesQuery
import com.apollographql.apollo.kmpsample.GithubRepositoryCommitsQuery
import com.apollographql.apollo.kmpsample.GithubRepositoryDetailQuery
import okio.Buffer
import okio.toByteString
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLConnection
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.sendSynchronousRequest
import platform.Foundation.setHTTPBody
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import kotlin.coroutines.suspendCoroutine

actual class ApolloCoroutinesService {

  private val url = NSURL(string = BASE_URL)

  actual suspend fun fetchRepositories(query: GithubRepositoriesQuery): Response<GithubRepositoriesQuery.Data> =
      suspendCoroutine {
        doRequest(query)
      }

  actual suspend fun fetchRepositoryDetail(query: GithubRepositoryDetailQuery): Response<GithubRepositoryDetailQuery.Data> =
      suspendCoroutine {
        doRequest(query)
      }

  actual suspend fun fetchCommits(query: GithubRepositoryCommitsQuery): Response<GithubRepositoryCommitsQuery.Data> =
      suspendCoroutine {
        doRequest(query)
      }

  private fun <D : Operation.Data, T, V : Operation.Variables> doRequest(query: Operation<D, T, V>): Response<T> {
    val request = createRequest(query)
    val response = NSURLConnection.sendSynchronousRequest(request, null, null)
    return query.parse(Buffer().write(response!!.toByteString()))
  }

  private fun <D : Operation.Data, T, V : Operation.Variables> createRequest(operation: Operation<D, T, V>): NSMutableURLRequest {
    val requestPayload = NSString.create(string = operation.composeRequestBody().utf8())
    return NSMutableURLRequest(uRL = url).apply {
      setHTTPMethod("POST")
      setValue("application/json", forHTTPHeaderField = "Accept")
      setValue("application/json", forHTTPHeaderField = "Content-Type")
      setValue(operation.operationId(), forHTTPHeaderField = "X-APOLLO-OPERATION-ID")
      setValue(operation.name().name(), forHTTPHeaderField = "X-APOLLO-OPERATION-NAME")
      setValue("bearer ${Companion.GITHUB_KEY}", forHTTPHeaderField = "Authorization")

      setHTTPBody(requestPayload.dataUsingEncoding(NSUTF8StringEncoding))
    }
  }

  companion object {
    private const val GITHUB_KEY = "change_me"
  }
}
