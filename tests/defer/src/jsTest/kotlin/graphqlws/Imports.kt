package graphqlws

@JsModule("graphql-ws/lib/use/ws")
@JsNonModule
external object useServer {
  fun useServer(options: dynamic, server: dynamic): dynamic
}
