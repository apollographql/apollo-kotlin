This server targets to implement the GraphQL subscriptions specs
from https://github.com/enisdenjo/graphql-sse/blob/master/PROTOCOL.md using a jvm server based on ktor library.

Implements in ktor a graphql server that supports subscriptions over SSE.

! Run server

!! From command line

`./gradlew sse-mockserver:run`

check it in a different console with

`curl http://localhost:8080/HelloWorld`

!! Recompile and run server when code changes
Note that to trigger recompile from Intellij tools there's usually a small delay from typing to saving to disk. Usually pressing ctrl+s (or cmd+s) triggers the file saving immediately. 

!!! From terminal You can use 3 terminal windows:

1. `while true ; do curl localhost:8080/HelloWorld ; echo ; date ; sleep 2; done` to see the output

1. `./gradlew -x test -t  :sse-mockserver:build` to recompile when source code changes

1. `./gradlew :sse-mockserver:run` to launch server

Then proceed to modify `KtorServerInteractor.PATH_HELLO_WORLD` and watch the new string in terminal #1. 
After a few seconds the logs for terminal 3 will display the following message
> INFO ktor.application - Application auto-reloaded in 0.002 seconds.

!!! From Intellij

As per instructions from previous section, but instead of launching server from gradle command line:
- open `Main.kt` and click the green arrow
- cancel the execution
- edit the run configuration for `MainKt` and add `-Dio.ktor.development=true` to the VM options.



