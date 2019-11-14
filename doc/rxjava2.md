# RxJava2 support
The Apollo GraphQL client comes with RxJava2 support.

Apollo types can be converted to RxJava2 `Observable` *types* using wrapper functions `Rx2Apollo.from(...)`.

Conversion is done according to the following table:

| Apollo type |  RxJava2 type|
| :--- | :--- |
| `ApolloCall<T>` | `Observable<Response<T>>` |
| `ApolloSubscriptionCall<T>` | `Observable<Response<T>>` |
| `ApolloQueryWatcher<T>` | `Observable<Response<T>>` |
| `ApolloStoreOperation<T>` | `Single<T>` |
| `ApolloPrefetch` | `Completable` |

## Including in your project

Add the following `dependency`:

[ ![apollo-rx2-support](https://img.shields.io/bintray/v/apollographql/android/apollo-rx2-support.svg?label=apollo-rx2-support) ](https://bintray.com/apollographql/android/apollo-rx2-support/_latestVersion)
```gradle
// RxJava2 support
implementation 'com.apollographql.apollo:apollo-rx2-support:x.y.z'
```

## Usage examples

### Converting `ApolloCall` to an `Observable`:
```java
//Create a query object
EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();

//Create an ApolloCall object
ApolloCall<EpisodeHeroName.Data> apolloCall = apolloClient.query(query);

//RxJava1 Observable
Observable<Response<EpisodeHeroName.Data>> observable1 = RxApollo.from(apolloCall);

//RxJava2 Observable
Observable<Response<EpisodeHeroName.Data>> observable2 = Rx2Apollo.from(apolloCall);
```

### Converting `ApolloPrefetch` to a `Completable`:
```java
//Create a query object
EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();

//Create an ApolloPrefetch object
ApolloPrefetch<EpisodeHeroName.Data> apolloPrefetch = apolloClient.prefetch(query);

//RxJava1 Completable
Completable completable1 = RxApollo.from(apolloPrefetch);

//RxJava2 Completable
Completable completable2 = Rx2Apollo.from(apolloPrefetch);
```

Also, don't forget to dispose of your Observer/Subscriber when you are finished:
```java
Disposable disposable = Rx2Apollo.from(query).subscribe();

//Dispose of your Observer when you are done with your work
disposable.dispose();
```
As an alternative, multiple Disposables can be collected to dispose of at once via `CompositeDisposable`:
```java
CompositeDisposable disposable = new CompositeDisposable();
disposable.add(Rx2Apollo.from(call).subscribe());

// Dispose of all collected Disposables at once
disposable.clear();
```


For a concrete example of using Rx wrappers for apollo types, checkout the sample app in the [`apollo-sample`](samples/apollo-sample) module.
