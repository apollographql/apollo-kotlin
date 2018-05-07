# Apollo GraphQL Client for Android

[![GitHub license](https://img.shields.io/badge/license-MIT-lightgrey.svg?maxAge=2592000)](https://raw.githubusercontent.com/apollographql/apollo-android/master/LICENSE) [![Get on Slack](https://img.shields.io/badge/slack-join-orange.svg)](http://www.apollostack.com/#slack)
[![Build status](https://travis-ci.org/apollographql/apollo-android.svg?branch=master)](https://travis-ci.org/apollographql/apollo-android)
[![GitHub release](https://img.shields.io/github/tag/apollographql/apollo-android.svg)](https://github.com/apollographql/apollo-android/releases)

Apollo-Android is a GraphQL compliant client that generates Java models from standard GraphQL queries.  These models give you a typesafe API to work with GraphQL servers.  Apollo will help you keep your GraphQL query statements together, organized, and easy to access from Java. Change a query and recompile your project - Apollo code gen will rebuild your data model.  Code generation also allows Apollo to read and unmarshal responses from the network without the need of any reflection (see example generated code below).  Future versions of Apollo-Android will also work with AutoValue and other value object generators.

Apollo-Android is designed primarily with Android in mind but you can use it in any java/kotlin app. The android-only parts are in `apollo-android-support` and are only needed to use SQLite as a cache or the android main thread for callbacks.

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE (https://github.com/thlorenz/doctoc) -->
**Table of Contents**

- [Adding Apollo to your Project](#adding-apollo-to-your-project)
  - [Kotlin](#kotlin)
- [Generate Code using Apollo](#generate-code-using-apollo)
- [Consuming Code](#consuming-code)
- [Custom Scalar Types](#custom-scalar-types)
- [Support For Cached Responses](#support-for-cached-responses)
  - [Usage](#usage)
- [RxJava Support](#rxjava-support)
  - [Usage](#usage-1)
- [Gradle Configuration of Apollo Android](#gradle-configuration-of-apollo-android)
  - [Optional Support](#optional-support)
    - [Usage](#usage-2)
  - [Semantic Naming](#semantic-naming)
    - [Usage](#usage-3)
  - [Java Beans Semantic Naming for Accessors](#java-beans-semantic-naming-for-accessors)
    - [Usage](#usage-4)
  - [Explicit Schema location](#explicit-schema-location)
    - [Usage](#usage-5)
  - [Download](#download)
- [License](#license)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Adding Apollo to your Project

The latest Gradle plugin version is [ ![Download](https://api.bintray.com/packages/apollographql/android/apollo-gradle-plugin/images/download.svg) ](https://bintray.com/apollographql/android/apollo-gradle-plugin/_latestVersion)

To use this plugin, add the dependency to your project's build.gradle file:

```groovy
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath 'com.apollographql.apollo:apollo-gradle-plugin:x.y.z'
  }
}

dependencies {
  compile 'com.apollographql.apollo:apollo-runtime:x.y.z'
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
    classpath 'com.apollographql.apollo:apollo-gradle-plugin:0.5.1-SNAPSHOT'
  }
}

dependencies {
  compile 'com.apollographql.apollo:apollo-runtime:0.5.1-SNAPSHOT'
}
```

The plugin can then be applied as follows within your app module's `build.gradle` :

```
apply plugin: 'com.apollographql.android'
```

The Android Plugin must be applied before the Apollo plugin

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

In our sample project, we have the base url pointing to `https://api.githunt.com/graphql/`

There is also a #query && #mutation instance method on ApolloClient that can take as input any Query or Mutation that you have generated using Apollo.

```java
apolloClient.query(
  FeedQuery.builder()
    .limit(10)
    .type(FeedType.HOT)
    .build()
).enqueue(new ApolloCall.Callback<FeedQuery.Data>() {

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
    customTypeMapping['DateTime'] = "java.util.Date"
    customTypeMapping['Currency'] = "java.math.BigDecimal"
}
```

Then register your custom adapter:

```java
CustomTypeAdapter<Date> customTypeAdapter = new CustomTypeAdapter<Date>() {
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

// use on creating ApolloClient
ApolloClient.builder()
  .serverUrl(serverUrl)
  .okHttpClient(okHttpClient)
  .addCustomTypeAdapter(CustomType.DATETIME, customTypeAdapter)
  .build();
```

## Support For Cached Responses

Apollo GraphQL client allows you to cache responses, making it suitable for use even while offline. The client can be configured with 3 levels of caching:

 - **HTTP Response Cache**: For caching raw http responses.
 - **Normalized Disk Cache**: Per node caching of responses in SQL. Persists normalized responses on disk so that they can used after process death. 
 - **Normalized InMemory Cache**: Optimized Guava memory cache for in memory caching as long as the App/Process is still alive.  

### Usage

To enable HTTP Cache support, add the dependency to your project's build.gradle file. The latest version is [[ ![Download](https://api.bintray.com/packages/apollographql/android/apollo-http-cache/images/download.svg) ](https://bintray.com/apollographql/android/apollo-http-cache/_latestVersion)

```groovy
dependencies {
  compile 'com.apollographql.apollo:apollo-http-cache:x.y.z'
}
```

Raw HTTP Response Cache:

```java
//Directory where cached responses will be stored
File file = new File("/cache/");

//Size in bytes of the cache
int size = 1024*1024;

//Create the http response cache store
DiskLruHttpCacheStore cacheStore = new DiskLruCacheStore(file, size); 

//Build the Apollo Client
ApolloClient apolloClient = ApolloClient.builder()
  .serverUrl("/")
  .httpCache(new ApolloHttpCache(cacheStore))
  .okHttpClient(okHttpClient)
  .build();

apolloClient
  .query(
    FeedQuery.builder()
      .limit(10)
      .type(FeedType.HOT)
      .build()
  )
  .httpCachePolicy(HttpCachePolicy.CACHE_FIRST)
  .enqueue(new ApolloCall.Callback<FeedQuery.Data>() {

    @Override public void onResponse(@Nonnull Response<FeedQuery.Data> dataResponse) {
      ...
    }

    @Override public void onFailure(@Nonnull Throwable t) {
      ...
    }
  }); 
```

**IMPORTANT:** Caching is provided only for `query` operations. It isn't available for `mutation` operations.

There are four available cache policies `HttpCachePolicy`:

- `CACHE_ONLY` - Fetch a response from the cache only, ignoring the network. If the cached response doesn't exist or is expired, then return an error.
- `NETWORK_ONLY` - Fetch a response from the network only, ignoring any cached responses.
- `CACHE_FIRST` - Fetch a response from the cache first. If the response doesn't exist or is expired, then fetch a response from the network.
- `NETWORK_FIRST` - Fetch a response from the network first. If the network fails and the cached response isn't expired, then return cached data instead.

For `CACHE_ONLY`, `CACHE_FIRST` and `NETWORK_FIRST` policies you can define the timeout after what cached response is treated as expired and will be evicted from the http cache, `expireAfter(expireTimeout, timeUnit)`.`

Normalized Disk Cache:
```java
//Create the ApolloSqlHelper. Please note that if null is passed in as the name, you will get an in-memory SqlLite database that 
// will not persist across restarts of the app.
ApolloSqlHelper apolloSqlHelper = ApolloSqlHelper.create(context, "db_name");

//Create NormalizedCacheFactory
NormalizedCacheFactory cacheFactory = new SqlNormalizedCacheFactory(apolloSqlHelper);

//Create the cache key resolver, this example works well when all types have globally unique ids.
CacheKeyResolver resolver =  new CacheKeyResolver() {
 @Nonnull @Override
   public CacheKey fromFieldRecordSet(@Nonnull ResponseField field, @Nonnull Map<String, Object> recordSet) {
     return formatCacheKey((String) recordSet.get("id"));
   }
 
   @Nonnull @Override
   public CacheKey fromFieldArguments(@Nonnull ResponseField field, @Nonnull Operation.Variables variables) {
     return formatCacheKey((String) field.resolveArgument("id", variables));
   }
 
   private CacheKey formatCacheKey(String id) {
     if (id == null || id.isEmpty()) {
       return CacheKey.NO_KEY;
     } else {
       return CacheKey.from(id);
     }
   }
};

//Build the Apollo Client
ApolloClient apolloClient = ApolloClient.builder()
  .serverUrl("/")
  .normalizedCache(cacheFactory, resolver)
  .okHttpClient(okHttpClient)
  .build();
```

Normalized In-Memory Cache:
```java

//Create NormalizedCacheFactory
NormalizedCacheFactory cacheFactory = new LruNormalizedCacheFactory(EvictionPolicy.builder().maxSizeBytes(10 * 1024).build());

//Build the Apollo Client
ApolloClient apolloClient = ApolloClient.builder()
  .serverUrl("/")
  .normalizedCache(cacheFactory, resolver)
  .okHttpClient(okHttpClient)
  .build();

```

Chaining Caches:
You can use both an memory cache and sql cache, with a cache chain. Reads will read from the first cache
hit in the chain. Writes will propagate down the entire chain.

```java

NormalizedCacheFactory sqlCacheFactory = new SqlNormalizedCacheFactory(apolloSqlHelper)
NormalizedCacheFactory memoryFirstThenSqlCacheFactory = new LruNormalizedCacheFactory(
  EvictionPolicy.builder().maxSizeBytes(10 * 1024).build()
).chain(sqlCacheFactory);

```

For concrete examples of using response caches, please see the following tests in the [`apollo-integration`](apollo-integration) module:
`CacheTest`, `SqlNormalizedCacheTest`, `LruNormalizedCacheTest`. 

## RxJava Support

Apollo GraphQL client comes with RxJava1 & RxJava2 support. Apollo types such as ApolloCall, ApolloPrefetch & ApolloWatcher can be converted
to their corresponding RxJava1 & RxJava2 Observable types by using wrapper functions provided in RxApollo & Rx2Apollo classes respectively.

### Usage

Converting ApolloCall to a Observable:
```java
//Create a query object
EpisodeHeroName query = EpisodeHeroName.builder().episode(Episode.EMPIRE).build();

//Create an ApolloCall object
ApolloCall<EpisodeHeroName.Data> apolloCall = apolloClient.query(query);

//RxJava1 Observable
Observable<EpisodeHeroName.Data> observable1 = RxApollo.from(apolloCall);

//RxJava2 Observable
Observable<EpisodeHeroName.Data> observable2 = Rx2Apollo.from(apolloCall);
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
ApolloWatcher<EpisodeHeroName.Data> apolloWatcher = apolloClient.query(query).watcher();

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

##  Gradle Configuration of Apollo Android
Apollo Android comes with logical defaults that will work for the majority of use cases, below you will find additional configuration that will add Optional Support & Semantic Query Naming.

### Optional Support
By default Apollo-Android will return `null` when a graph api returns a `null` field.  Apollo allows you to configure the generated code to instead use a Guava `Optional<T>` or a shaded`Apollo Optional<T>` rather than simply returning the scalar value or null.

#### Usage

```java
apollo {
  nullableValueType = "apolloOptional"  //use one or the other
  nullableValueType = "guavaOptional"   //use one or the other
}
```

### Semantic Naming
By default Apollo-Android expects queries to be written as follows:
```Query someQuery{....}```
alternatively you can turn on Semantic Naming which will allow you to define queries without the Query suffix:
```Query some{....}```

With Semantic Naming enabled you will still see a SomeQuery.java generated same as the first query above.

#### Usage 

```java
apollo {
  useSemanticNaming = false
}
```

### Java Beans Semantic Naming for Accessors
By default, the generated classes have accessor methods whose names are identical to the name of the Schema field.

```query Foo { bar }```

results in a class signature like:

```
class Foo {
    public Bar bar() { ... }
}
```

Alternatively, turning on Java Beans Semantic Naming will result in those methods being pre-pended with `get` or `is`:

```
class Foo {
    public Bar getBar() { ... }
}
```

#### Usage
```groovy
apollo {
  useJavaBeansSemanticNaming = true
}
```

### Explicit Schema location
By default Apollo-Android tries to lookup GraphQL schema file in `/graphql` folder, the same folder where all your GraphQL queries are stored. 
For example, if query files are located at `/src/main/graphql/com/example/api` then the schema file should be placed in the same location `/src/main/graphql/com/example/api`. Relative path of schema file to `/src/main/graphql` root folder defines the package name for generated models, in our example the package name of generated models will be `com.example.api`.

Alternatively, you can explicitly provide GraphQL schema file location and package name for generated models:

#### Usage

```groovy
apollo {
  schemaFilePath = "/path_to_schema_file/my-schema.json"
  outputPackageName = "com.my-example.graphql.api"
}
```

### Use system pre-installed `apollo-codegen`
By default Apollo will enable gradle plugin that installs Node-JS and downloads `apollo-codegen` module into your project's build directory. If you already have Node-JS and `apollo-codegen` module installed on your computer, you can enable Apollo to use it and skip these steps. Apollo will fallback to default behaviour if verification of pre-installed version of `apollo-codegen` fails.          

#### Usage
To enable usage of pre-installed `apollo-codegen` module, set gradle system property `apollographql.useGlobalApolloCodegen` (for example in `gradle.properties` file):
```properties
systemProp.apollographql.useGlobalApolloCodegen=true
```

### Download

RxJava1:

[ ![Download](https://api.bintray.com/packages/apollographql/android/apollo-rx-support/images/download.svg) ](https://bintray.com/apollographql/android/apollo-rx-support/_latestVersion)
```gradle
compile 'com.apollographql.apollo:apollo-rx-support:x.y.z'
```

RxJava2:

[ ![Download](https://api.bintray.com/packages/apollographql/android/apollo-rx2-support/images/download.svg) ](https://bintray.com/apollographql/android/apollo-rx2-support/_latestVersion)

```gradle
compile 'com.apollographql.apollo:apollo-rx2-support:x.y.z'
```

## License

```
The MIT License (MIT)

Copyright (c) 2017 Meteor Development Group, Inc.
```
