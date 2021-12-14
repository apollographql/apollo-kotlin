# Thoughts on Android threading and concurrency 

A tentative high level overview of what threads are used and why we came to this. K/N makes the whole thing more complex than what it would have been on the JVM so this document tries to keep track of the various decisions

### Goals
* JVM should be at least as fast as 2.x 
* Minimize number of threads and context changes

### Non-goals
* Atomic cache requests (see the [detailed explanation](#non-goal-atomic-cached-requests) at the end of this document). 
* Multi-threaded coroutines on native.
* JS is out of scope so far. It _should_ be easier since it's single threaded but it's left out for now.
* Linux/Windows is also out of scope. 


At a high level, Apollo-Android does multiple things:
* HTTP requests 
* Json deserialization (for data)
* Json serialization (for variables)
* Normalization
* De-normalization  
* Cache reading
* Cache writing
* Plumbing: the work of connecting the above steps together

Most of the above steps need to happen in a background thread as they are potentially CPU expensive (normalization), IO expensive (HTTP) or both (SQLite), except maybe the plumbing that is just passing plates around. That being said, running the plumbing in the main thread means that we have to pay the price of a context switch every time we need to perform an operation. So it makes sense to run the plumbing on the same thread as was previously executing to avoid the context switch.  

## Mutable, shared state that requires synchronization

If everything were immutable, we could run each request on a separate thread and let them execute concurrently. Unfortunately, there is some state that requires synchronization:

* Normalized cache
* Store listeners
* Websocket IDLE timeout
* HTTP2 connection state with HTTP2 multiplexing, some state is needed there
* ResponseAdapterCache: This currently caches the `ResponseAdapters` so that they don't have to lookup their field `ResponseAdapters`. The fact that this is mutable and that it doesn't work for recursive models encourages to remove that behaviour and look up the custom scalar adapters every time.

**On the JVM**, synchronization is typically done using locks (or read-write locks for better granularity). 

**On native**, that doesn't work as mutable data cannot be touched by multiple threads. Synchronization is usually done using a separate isolated thread as in [Stately](https://github.com/touchlab/Stately) or using the primitive [AtomicReference](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.native.concurrent/-atomic-reference/) (or the higher level [atomicfu](https://github.com/Kotlin/kotlinx.atomicfu) that works in multiplatform code). 

The section below discuss what to use in what situation, starting the bigger constraints.

## Why coroutines in interceptors?

We want to expose a `<Flow>` API so coroutines are definitely a must there. Internally, that isn't required though. Libraries like OkHttp or SqlDelight do most of their work without coroutines and expose a coroutine API at the very last moment, before returning to the user. We decided to go with coroutines in interceptors because:
* it handles cancellation automatically.
* more importantly, it doesn't keep a thread waiting while another operation like an Async HTTP request is executing (more on that [below](#sync-vs-async-http-requests)).

That last point is important. While cancellation could be implemented manually, implementing a state machine that waits on HTTP requests would be way harder and error-prone

## Sync vs Async HTTP requests

**On iOS**, there isn't much choice as NSURLSession only has an async API. While that could theoretically be made synchronous using semaphores, it is hard to do so because that would most likely have to be written in Objective-C. Also that would mean that we pay the context switching price in all cases and also that keeps a thread waiting just doing nothing so a coroutine is way more efficient there. 

The `NSURLSession` takes a queue to dispatch to as a parameter. If we want to use a coroutine to wait on the call, that means the coroutine will have to execute on that queue (because coroutines cannot change thread sunless using `coroutines-mt`, which is out of scope so far). An easy to use queue is the main queue. Doing this means **the request coroutine will run on the main thread on iOS**

**On the JVM**, there are less restrictions. OkHttp has as `synchronous` API that could potentially avoid a context switch. One pitfall is cancellation as it would have to happen from a separate thread but that might actually work. Not reusing the OkHttp threadpool means that it won't be able to be shared with other OkHttp clients but since GraphQL usually connects to a single host, it's not clear what would be shared there.

## Sync vs Async Cache

**On iOS**, here as well, there isn't much choice as the cache is fundamentally mutable and will need to be run from its own thread. The difference with NSURLSession is that we have more control over where the callback happens. We can decide the thread where the work and callback happen. So we're theoretically not limited to main thread (but since HTTP forces the coroutine to run on the main thread, it will happen there.

**On the JVM**, the traditional way to do this would involve ReadWriteLock. ReadWriteLock allow:
* concurrent reads to the DB
* don't switch contexts

On the other hand,

* it's not clear if Android's SQLiteOpenHelper allows concurrent reads
* there's a price to take the lock. It would need to be measured how much it is. In high load, this might also keep threads waiting

**On the JVM**, using the async version would remove all contention on the database. It would also allow to handle cache writes asynchronously as a "Fire & Forget" thing. At this stage it's not clear which one would perform better so it should be configurable. Also we might need to debounce/read the previous value in which case we definitely need the return value.

## Conclusion

There are still questions:
* Can a synchronous OkHttp call be cancelled?
* Does Android allow concurrent SQLite reads?
* Do we need the return value from `cache.write()` or can this be debounced later on?

With all that, the typical flows should be:

* iOS (6 context changes)
  * Callsite (Main) -> CacheRead (Cache) -> Plumbing (Main) -> HTTP (NSURLSession) -> Plumbing (Main) -> CacheWrite (Cache) -> Response (Main) 
* JVM-synccache-asynchttp (4 context changes): 
  * Callsite (Main) -> CacheRead (IO) -> Plumbing (IO) -> HTTP (OkHttp) -> Plumbing (IO) -> CacheWrite (IO) -> Response (Main)
* JVM-synccache-synchttp (2 context changes): 
  * Callsite (Main) -> CacheRead (IO) -> Plumbing (IO) -> HTTP (IO) -> Plumbing (IO) -> CacheWrite (IO) -> Response (Main)
* JVM-asynccache-asynchttp (6 context changes):
    * Callsite (Main) -> CacheRead (Cache) -> Plumbing (IO) -> HTTP (OkHttp) -> Plumbing (IO) -> CacheWrite (Cache) -> Response (Main)
* JVM-asynccache-synchttp (4 context changes):
    * Callsite (Main) -> CacheRead (Cache) -> Plumbing (IO) -> HTTP (IO) -> Plumbing (IO) -> CacheWrite (Cache) -> Response (Main)
    
Note that plumbing above contains potentially not-cheap operations like normalization or serializing variables.


## Appendix-1 Implementation notes

On K/N, the `Stately Isolate` pattern seems to be the way to go. See https://dev.to/touchlab/kotlin-native-isolated-state-50l1 for more details. It has a certain cost and doesn't allow ReadWrite locks for an example so we might want to delegate to something else on the JVM:

``` kotlin
interface SharedState<T> {
  fun write(block: (T) -> Unit)
  fun <R> read(block: (T) -> R): R
  fun dispose()
}

class JvmReadWriteSharedState<T>(producer: () -> T): SharedState<T> {
  private val lock = ReentrantReadWriteLock()
  private val state: T = producer()

  override fun write(block: (T) -> Unit) = lock.write {
    block(state)
  }
  override fun <R> read(block: (T) -> R): R = lock.read {
    block(state)
  }
  override fun dispose() {}
}

class JvmSerialSharedState<T>(producer: () -> T): SharedState<T> {
  private val executor = Executors.newSingleThreadExecutor()
  private val state: T = producer()

  override fun write(block: (T) -> Unit){
    executor.submit { block(state) }
  }
  override fun <R> read(block: (T) -> R): R = executor.submit(
    Callable {
      block(state)
    }
  ).get()

  override fun dispose() {
    executor.shutdown()
  }
}

class NativeSerialSharedState<T>(producer: () -> T): SharedState<T> {
  private val isoState = IsolateState(producer())

  // Could be changed to Fire & Forget
  override fun write(block: (T) -> Unit) = isoState.access { block(it) }
  override fun <R> read(block: (T) -> R): R = isoState.access { block(it) }

  override fun dispose() {
    isoState.shutdown()
  }
}
```



## Appendix-2 Non-goal: Atomic Cached Requests


Apollo Kotlin has no concept of "Atomic request". Launching the same request twice in a row will most likely end up in the request being sent to the network twice even if the first one will ultimately cache it (but this is not guaranteed either):

```kotlin
val response1 = launch {
    // Since "hero" is not in cache, this will go to the network
    apolloClient.query(HeroQuery()).execute()
}
val response2 = launch {
    // This will most likely go to the network even though it's the same request as above
    // If another request is modifying the cache, what is returned depends the timings of the different request
    apolloClient.query(HeroQuery()).execute()
}
```

On the other hand, waiting for one query to complete before launching the next one is guaranteed to have a predictable cache state. Especially if asynchronous cache write is implemented, the second query should wait until the write is written by the first one to read the cache:

```kotlin
val response1 = apolloClient.query(HeroQuery()).execute()
// If no other request is executing and the first one was cached, response2 will return the cached result
val response2 = apolloClient.query(HeroQuery()).execute()
```
