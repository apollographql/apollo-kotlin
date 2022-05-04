

This server targets to implement the GraphQL subscriptions specs from https://github.com/enisdenjo/graphql-sse/blob/master/PROTOCOL.md using a jvm server based on ktor library.


Implements in ktor a graphql server that supports subscriptions over SSE.

! Run server

!! From command line

`./gradlew sse-mockserver:run`

check it in a different console with 

`curl http://localhost:8080/HelloWorld`


!! Recompile and run server when code changes

!!! From terminal 
You can use 3 terminal windows:

1. `while true ; do sleep 2; curl localhost:8080/helloworld ; echo ; date ; done` to see the output

2. `./gradlew -x test -t  :sse-mockserver:build` to recompile when source code changes

3. `./gradlew :sse-mockserver:run` to launch server

Then proceed to modify [KtorServerInteractor.PATH_HELLO_WORLD] and watch the new string in terminal #1.

!!! From Intellij

TODO


