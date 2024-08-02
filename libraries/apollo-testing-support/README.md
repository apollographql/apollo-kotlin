# Module apollo-testing-support

`apollo-testing-support` contains:

* `QueueTestNetworkTransport` and `MapTestNetworkTransport` for testing without a mock server.
* a set of helper functions used for Apollo tests. They were never really intended to become public and will be removed in a future version. These symbols are marked as deprecated. If you are using them, copy paste them in your project.

See ["Mocking GraphQL responses"](https://www.apollographql.com/docs/kotlin/testing/mocking-graphql-responses) for how to use test network transports.
