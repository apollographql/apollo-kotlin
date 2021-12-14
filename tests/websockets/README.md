Websocket tests. This project contains 2 kind of tests:

1. Tests agains external servers: because we have no native mockserver that supports websockets, this project contains tests against external servers that are ignored with `@Ignored` by default but can be run manually if needed.
2. JVM-only tests using `sample-server`

