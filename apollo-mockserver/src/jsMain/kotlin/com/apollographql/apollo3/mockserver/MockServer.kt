package com.apollographql.apollo3.mockserver

actual class MockServer {

  private val responseQueue = mutableListOf<MockResponse>()

  private val server = http.createServer { req, res ->
    println(req)
    res.writeHead(200)
    res.end("Hello, World!")
  }.listen(PORT)

  actual fun url(): String {
    //TODO Use `client.address()` but it might return null, before the server is listening. So this will have to be suspend fun
    return "http://localhost:$PORT"
  }

  init {
    println("MockServer UP")
  }

  actual fun enqueue(mockResponse: MockResponse) {
    responseQueue.add(mockResponse)
  }

  actual fun takeRequest(): MockRecordedRequest {
    TODO("MockServer.takeRequest()")
  }

  actual fun stop() {
    server.close()
    println("MockServer DOWN")
  }

  private companion object {
    const val PORT = 8080
  }
}