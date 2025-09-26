//
//  swift_testsTests.swift
//  swift-testsTests
//
//  Created by Martin Bonnin on 26/09/2025.
//

import Testing
import shared_framework
@testable import swift_tests

struct swift_testsTests {

    final class AuthenticationInterceptor: Apollo_runtimeHttpInterceptor {

        func intercept(request: HttpRequest, chain: Apollo_runtimeHttpInterceptorChain) async throws -> HttpResponse {
            throw NSError(domain: "interceptor error", code: 42, userInfo: ["foo": "bar"])
        }
        
        func dispose() {
            // No op
        }
    }

    @Test func example() async throws {
        try MainKt.testInterceptor(interceptor: AuthenticationInterceptor())
    }

}
