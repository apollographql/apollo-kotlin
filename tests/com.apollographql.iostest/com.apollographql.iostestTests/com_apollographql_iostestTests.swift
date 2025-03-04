import Testing
import shared_framework
@testable import com_apollographql_iostest

struct com_apollographql_iostestTests {

    final class AuthenticationInterceptor: Apollo_runtimeHttpInterceptor {

        func intercept(request: HttpRequest, chain: Apollo_runtimeHttpInterceptorChain) async throws -> HttpResponse {
            throw DefaultApolloException(message: "interceptor error", cause: nil)
            //try await chain.proceed(request: request)
        }
        
        func dispose() {
            // No op
        }
    }

    @Test func example() async throws {
        try MainKt.testInterceptor(interceptor: AuthenticationInterceptor())
    }

}

extension DefaultApolloException: Swift.Error {}
