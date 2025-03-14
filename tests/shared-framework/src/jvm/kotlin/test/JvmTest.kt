package test

import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.exception.DefaultApolloException
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import testInterceptor
import kotlin.test.Test

class JvmTest {
  @Test
  fun throwingInInterceptorIsExposedInResponse() {
    testInterceptor(object : HttpInterceptor {
      override suspend fun intercept(
          request: HttpRequest,
          chain: HttpInterceptorChain,
      ): HttpResponse {
        throw Exception("interceptor error")
      }
    })
  }
}
