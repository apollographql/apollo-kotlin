# Thoughts on Android threading and concurrency 

A tentative high level overview of what threads are used and why we came to this.

At a high level, Apollo-Android does multiple things:
* HTTP requests 
* Json serialization (for variables)
* Json deserialization (for data)
* Normalization
* Cache writing
* De-normalization  
* Cache reading
* Plumbing: the work of connecting the above steps together

Most of the above steps need to happen in a background thread as they are potentially CPU expensive (normalization), IO expensive (HTTP) or both (SQLite), except maybe the plumbing that is just passing plates around. That being said, running the plumbing in the main thread means that we have to pay the price of a context switch every time we need to perform an operation. So it makes sense to run the plumbing on the same thread as was previously executing to avoid the context switch.  

## Mutable, shared state that requires synchronization

Some state in the pipeline is shared and requires synchronisation:

* Normalized cache
* Store listeners
* Websocket for subscription
* HTTP2 socket

Earlier versions of Apollo Kotlin used immutable data and the old K/N memory model to handle synchronisation. Newer versions use standard locking mechanisms for this.

## Why coroutines in interceptors?

Mainly for historical reasons. The coroutines APIs are convenient to use in Kotlin, expose a standard cancellation API, and it was decided to use them early in the Kotlin conversion.  

## Sync vs Async HTTP requests

**On iOS**, there isn't much choice as NSURLSession only has an async API. While that could theoretically be made
synchronous using semaphores, it is hard to do so because that would most likely have to be written in Objective-C. Also
that would mean that we pay the context switching price in all cases and also that keeps a thread waiting just doing
nothing so a coroutine is way more efficient there.

**On the JVM**, there are less restrictions. OkHttp has as synchronous API that [has proven to be quite efficient](https://github.com/grpc/grpc-java/issues/6696)/

## Current state

The dispatcher is changed very early in the chain. On the JVM, everything runs synchronously from that dispatcher.

The only thing happening before the dispatcher change is notification of the ApolloIdlingResources that need to happen from the same call stack. ApolloIdlingResource is deprecated and that should be removed when ApolloIdlingResource goes away. 