---
title: Kotlin coroutines
---

The Apollo GraphQL client comes with coroutines support with the following extensions:

```kotlin
fun <T> ApolloSubscriptionCall<T>.toFlow(): Flow<Response<T>>
fun <T> ApolloCall<T>.toFlow()
fun <T> ApolloQueryWatcher<T>.toFlow()
fun <T> ApolloSubscriptionCall<T>.toFlow(): Flow<Response<T>>
fun <T> ApolloCall<T>.toDeferred(): Deferred<Response<T>>
fun ApolloPrefetch.toJob(): Job
```

## Including in your project

Add the following `dependency`:

[ ![apollo-coroutines-support](https://img.shields.io/bintray/v/apollographql/android/apollo-coroutines-support.svg?label=apollo-coroutines-coroutines) ](https://bintray.com/apollographql/android/apollo-coroutines-support/_latestVersion)
```gradle
implementation 'com.apollographql.apollo:apollo-coroutines-support:x.y.z'
```
