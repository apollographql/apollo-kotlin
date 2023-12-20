# Tests related to `@defer`

Note: on Apple targets, the client is configured to use `StreamingNSURLSessionHttpEngine` which supports chunked
encoding.

## End-to-end tests with Apollo Router

The tests in `DeferWithRouterTest` are not run by default (they are excluded in the gradle conf) because they
expect an instance of [Apollo Router](https://www.apollographql.com/docs/router/) running locally.

They are enabled only when running from the specific `defer-with-router-tests` CI workflow.

To run them locally:

1. Install and run the
   subgraph: `(cd tests/defer/router/subgraphs/computers && npm install && APOLLO_PORT=4001 npm start)&`
2. Run the router: `path/to/router --supergraph tests/defer/router/simple-supergraph.graphqls &`
3. Run the tests: `DEFER_WITH_ROUTER_TESTS=true ./gradlew -p tests :defer:allTests`
