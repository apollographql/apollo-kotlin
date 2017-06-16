# Apollo GraphQL Client for Android

[![GitHub license](https://img.shields.io/badge/license-MIT-lightgrey.svg?maxAge=2592000)](https://raw.githubusercontent.com/apollographql/apollo-android/master/LICENSE) [![Get on Slack](https://img.shields.io/badge/slack-join-orange.svg)](http://www.apollostack.com/#slack)
[![Build status](https://travis-ci.org/apollographql/apollo-android.svg?branch=master)](https://travis-ci.org/apollographql/apollo-android)
[![GitHub release](https://img.shields.io/github/tag/apollographql/apollo-android.svg)](https://github.com/apollographql/apollo-android/releases)

Apollo-Android is a GraphQL compliant client that generates Java models from standard GraphQL queries.  These models give you a typesafe API to work with GraphQL servers.  Apollo will help you keep your GraphQL query statements together, organized, and easy to access from Java. Change a query and recompile your project - Apollo code gen will rebuild your data model.  Code generation also allows Apollo to read and unmarshal responses from the network without the need of any reflection (see example generated code below).  Future versions of Apollo-Android will also work with AutoValue and other value object generators.

## Adding Apollo to your Project

The latest Gradle plugin version is 0.3.2.

To use this plugin, add the dependency to your project's build.gradle file:

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.apollographql.apollo:gradle-plugin:0.3.2'
    }
}
```

Latest development changes are available in Sonatype's snapshots repository:

```groovy
buildscript {
  repositories {
    jcenter()
    maven { url 'https://oss.sonatype.org/content/repositories/snapshots/' }
  }
  dependencies {
    classpath 'com.apollographql.apollo:gradle-plugin:0.3.2-SNAPSHOT'
  }
}
```

The plugin can then be applied as follows within your app module's `build.gradle` :

```
apply plugin: 'com.apollographql.android'
```

The Android Plugin must be applied before the Apollo plugin

### Kotlin

If using Apollo in your Kotlin project, make sure to apply the Apollo plugin before your Kotlin plugins within your app module's `build.gradle`:

```
apply plugin: 'com.apollographql.android'
apply plugin: 'kotlin-android'
apply plugin: 'kotlin-android-extensions'
...
```

## Generate Code using Apollo

Follow these steps:

1) Put your GraphQL queries in a `.graphql` file. For the sample project in this repo you can find the graphql file at `apollo-sample/src/main/graphql/com/apollographql/apollo/sample/GithuntFeedQuery.graphql`. 

```
query FeedQuery($type: FeedType!, $limit: Int!) {
  feed(type: $type, limit: $limit) {
    comments {
      ...FeedCommentFragment
    }
    repository {
      ...RepositoryFragment
    }
    postedBy {
      login
    }
  }
}

fragment RepositoryFragment on Repository {
  name
  full_name
  owner {
    login
  }
}

fragment FeedCommentFragment on Comment {
  id
  postedBy {
    login
  }
  content
}
```

Note: There is nothing Android specific about this query, it can be shared with other GraphQL clients as well

2) You will also need to add a schema to the project. In the sample project you can find the schema `apollo-sample/src/main/graphql/com/apollographql/apollo/sample/schema.json`. 

You can find instructions to download your schema using apollo-codegen [HERE](http://dev.apollodata.com/ios/downloading-schema.html)

3) Compile your project to have Apollo generate the appropriate Java classes with nested classes for reading from the network response. In the sample project, a `FeedQuery` Java class is created here `apollo-sample/build/generated/source/apollo/com/apollographql/apollo/sample`.

Note: This is a file that Apollo generates and therefore should not be mutated.


## Consuming Code

You can use the generated classes to make requests to your GraphQL API.  Apollo includes an `ApolloClient` that allows you to edit networking options like pick the base url for your GraphQL Endpoint.

In our sample project, we have the base url pointing to `https://githunt-api.herokuapp.com/graphql`

There is also a #newCall instance method on ApolloClient that can take as input any Query or Mutation that you have generated using Apollo.

```java

apolloClient.newCall(FeedQuery.builder()
                .limit(10)
                .type(FeedType.HOT)
                .build()).enqueue(new ApolloCall.Callback<FeedQuery.Data>() {

            @Override public void onResponse(@Nonnull Response<FeedQuery.Data> dataResponse) {

                final StringBuffer buffer = new StringBuffer();
                for (FeedQuery.Data.Feed feed : dataResponse.data().feed()) {
                    buffer.append("name:" + feed.repository().fragments().repositoryFragment().name());
                    buffer.append(" owner: " + feed.repository().fragments().repositoryFragment().owner().login());
                    buffer.append(" postedBy: " + feed.postedBy().login());
                    buffer.append("\n~~~~~~~~~~~");
                    buffer.append("\n\n");
                }

				// onResponse returns on a background thread. If you want to make UI updates make sure they are done on the Main Thread.
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        TextView txtResponse = (TextView) findViewById(R.id.txtResponse);
                        txtResponse.setText(buffer.toString());
                    }
                });

            }

            @Override public void onFailure(@Nonnull Throwable t) {
                Log.e(TAG, t.getMessage(), t);
            }
        });
             
```

## Custom Scalar Types

Apollo supports Custom Scalar Types like `DateTime` for an example.

You first need to define the mapping in your build.gradle file. This will tell the compiler what type to use when generating the classes.

```gradle
apollo {
    customTypeMapping {
        DateTime = "java.util.Date"
    }
}
```

Then register your custom adapter:

```java
CustomTypeAdapter<Date> dateCustomTypeAdapter = new CustomTypeAdapter<Date>() {
    @Override
    public Date decode(String value) {
        try {
            return ISO8601_DATE_FORMAT.parse(value);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public String encode(Date value) {
        return ISO8601_DATE_FORMAT.format(value);
    }
};

ApolloConverterFactory apolloConverterFactory = new ApolloConverterFactory.Builder()
        .withCustomTypeAdapter(CustomType.DATETIME, dateCustomTypeAdapter)
        .withResponseFieldMappers(ResponseFieldMappers.MAPPERS)
        .build();
```

## Support For Cached Responses

Apollo GraphQL client allows you to cache responses, making it suitable for use even while offline. The client can be configured with 3 levels of caching:

 - **HTTP Response Cache**: For caching raw http responses.
 - **Normalized Disk Cache**: Per node caching of responses in SQL. Persists normalized responses on disk so that they can used after process death. 
 - **Normalized InMemory Cache**: Optimized Guava memory cache for in memory caching as long as the App/Process is still alive.  

### Usage

Raw HTTP Response Cache:
```java
//Directory where cached responses will be stored
File file = new File("/cache/");

//Size in bytes of the cache
int size = 1024*1024;

//Strategy for deciding when the cache becomes stale
EvictionStrategy evictionStrategy = new TimeoutEvictionStrategy(5, TimeUnit.SECONDS);

//Create the http response cache store
ResponseCacheStore cacheStore = new DiskLruCacheStore(file, size); 

//Build the Apollo Client
ApolloClient apolloClient = ApolloClient.builder()
                                    .serverUrl("/")
                                    .httpCache(cacheStore, evictionStrategy)
                                    .okHttpClient(okHttpClient)
                                    .build();
```

Normalized Disk Cache:
```java
//Create the ApolloSqlHelper. Please note that if null is passed in as the name, you will get an in-memory SqlLite database that 
// will not persist across restarts of the app.
ApolloSqlHelper apolloSqlHelper = ApolloSqlHelper.create(context, "db_name");

//Create NormalizedCacheFactory
NormalizedCacheFactory cacheFactory = new SqlNormalizedCacheFactory(apolloSqlHelper);

//Create the cache key resolver
CacheKeyResolver<Map<String, Object>> resolver =  new CacheKeyResolver<Map<String, Object>>() {
          @Nonnull @Override public CacheKey resolve(@Nonnull Map<String, Object> objectSource) {
            String id = (String) objectSource.get("id");
            if (id == null || id.isEmpty()) {
              return CacheKey.NO_KEY;
            }
            return CacheKey.from(id);
          }
        }

//Build the Apollo Client
ApolloClient apolloClient = ApolloClient.builder()
                                    .serverUrl("/")
                                    .normalizedCache(cacheFactory, resolver)
                                    .okHttpClient(okHttpClient)
                                    .build();
```

Normalized InMemory Cache:
```java

//Create NormalizedCacheFactory
NormalizedCacheFactory cacheFactory = new LruNormalizedCacheFactory(EvictionPolicy.builder().maxSizeBytes(10 * 1024).build());

//Create the cache key resolver
CacheKeyResolver<Map<String, Object>> resolver =  new CacheKeyResolver<Map<String, Object>>() {
          @Nonnull @Override public CacheKey resolve(@Nonnull Map<String, Object> objectSource) {
            String id = (String) objectSource.get("id");
            if (id == null || id.isEmpty()) {
              return CacheKey.NO_KEY;
            }
            return CacheKey.from(id);
          }
        }

//Build the Apollo Client
ApolloClient apolloClient = ApolloClient.builder()
                                    .serverUrl("/")
                                    .normalizedCache(cacheFactory, resolver)
                                    .okHttpClient(okHttpClient)
                                    .build();

```

For concrete examples of using response caches, please see the following tests in the [`apollo-integration`](apollo-integration) module:
`CacheTest`, `SqlNormalizedCacheTest`, `LruNormalizedCacheTest`. 

## RxJava Support

Apollo GraphQL client comes with RxJava1 & RxJava2 support. Apollo types such as ApolloCall, ApolloPrefetch & ApolloWatcher can be converted
to their corresponding RxJava1 & RxJava2 Observable types by using wrapper functions provided in RxApollo & Rx2Apollo classes respectively.

### Usage

Converting ApolloCall to a Single:
```java
//Create a query object
EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();

//Create an ApolloCall object
ApolloCall<EpisodeHeroName.Data> apolloCall = apolloClient.newCall(query);

//RxJava1 Single
Single<EpisodeHeroName.Data> single1 = RxApollo.from(apolloCall);

//RxJava2 Single
Single<EpisodeHeroName.Data> single2 = Rx2Apollo.from(apolloCall);
```

Converting ApolloPrefetch to a Completable:
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

Converting ApolloWatcher to an Observable:
```java
//Create a query object
EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();

//Create an ApolloWatcher object
ApolloWatcher<EpisodeHeroName.Data> apolloWatcher = apolloClient.newCall(query).watcher();

//RxJava1 Observable
Observable<EpisodeHeroName.Data> observable1 = RxApollo.from(apolloWatcher);

//RxJava2 Observable
Observable<EpisodeHeroName.Data> observable1 = Rx2Apollo.from(apolloWatcher);
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


For a concrete example of using Rx wrappers for apollo types, checkout the sample app in the [`apollo-sample`](apollo-sample) module.

### Download

RxJava1:

[![Maven Central](https://img.shields.io/maven-central/v/com.apollographql.apollo/apollo-rx-support.svg)](http://repo1.maven.org/maven2/com/apollographql/apollo/apollo-rx-support/)
```gradle
compile 'com.apollographql.apollo:apollo-rx-support:x.y.z'
```

RxJava2:

[![Maven Central](https://img.shields.io/maven-central/v/com.apollographql.apollo/apollo-rx2-support.svg)](http://repo1.maven.org/maven2/com/apollographql/apollo/apollo-rx2-support/)
```gradle
compile 'com.apollographql.apollo:apollo-rx2-support:x.y.z'
```

## License

```
The MIT License (MIT)

Copyright (c) 2016 Meteor Development Group, Inc.
```
