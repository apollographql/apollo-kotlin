
# Coroutines Support

The Apollo GraphQL client comes with coroutines support with the following extensions:

```kotlin
fun <T> ApolloCall<T>.toChannel(capacity: Int = Channel.UNLIMITED): Channel<Response<T>>
fun <T> ApolloCall<T>.toDeferred(): Deferred<Response<T>>
fun <T> ApolloSubscriptionCall<T>.toChannel(capacity: Int = Channel.UNLIMITED): Channel<Response<T>>
fun <T> ApolloQueryWatcher<T>.toChannel(capacity: Int = Channel.UNLIMITED): Channel<Response<T>>
fun ApolloPrefetch.toJob(): Job
```

## Including in your project

Add the following `dependency`:

[ ![apollo-coroutines-support](https://img.shields.io/bintray/v/apollographql/android/apollo-coroutines-support.svg?label=apollo-coroutines-coroutines) ](https://bintray.com/apollographql/android/apollo-coroutines-support/_latestVersion)
```gradle
repositories {
    maven {
        // The coroutines artifact is not deployed on jcenter yet
        // See https://github.com/apollographql/apollo-android/issues/1325
        url = uri("http://dl.bintray.com/apollographql/android")
    }
}

implementation 'com.apollographql.apollo:apollo-coroutines-support:x.y.z'
```
