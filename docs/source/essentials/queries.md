---
title: Queries
---

Fetching data in a simple, predictable way is one of the core features of Apollo Client. In this guide, you'll learn how to Query GraphQL data and use the result in your application.
You'll also learn how Apollo-android Client simplifies your data management code by tracking different error states for you.

This page assumes some familiarity with building GraphQL queries. If you'd like a refresher, we recommend [reading this guide](http://graphql.org/learn/queries/) and practicing [running queries in GraphiQL](https://graphql.github.io/swapi-graphql/).
Since Apollo Client queries are just standard GraphQL, anything you can type into the GraphiQL query explorer can also be put into `.graphql` files in your project.

The following examples assume that you've already set up Apollo Client for your Android/Java application. Read our [getting started](./get-started.html) guide if you need help with either of those steps.

> All code snippets are taken from the apollo-sample project and can be found [here](https://github.com/apollographql/apollo-android/tree/master/apollo-sample).

Apollo-android takes a schema and a set of `.graphql` files and uses these to generate code you can use to execute queries and access typed results.

> All `.graphql` files in your project (or the subset you specify as input to `apollo-codegen` if you customize the script you define as the code generation build phase) will be combined and treated as one big GraphQL document. That means fragments defined in one `.graphql` file are available to all other `.graphql` files for example, but it also means operation names and fragment names have to be unique and you will receive validation errors if they are not.

<h2 id="creating-queries">Creating queries</h2>

Queries are represented as instances of generated classes conforming to the `GraphQLQuery` protocol. Constructor arguments can be used to define query variables if needed.
You pass a query object to `ApolloClient#query(query)` to send the query to the server, execute it, and receive results.

For example, if you define a query called `FeedQuery`:

```graphql
query FeedQuery($type: FeedType!, $limit: Int!) {
  feedEntries: feed(type: $type, limit: $limit) {
    id
    repository {
      name
      full_name
      owner {
        login
      }
    }
    postedBy {
      login
    }
  }
}
```

Here, `query` is the operation type and `FeedQuery` is the operation name.
Apollo-android will generate a `FeedQuery` class that you can construct (with variables) and pass to `ApolloClient#query(query)`:

```java
apolloClient().query(feedQuery)
        .enqueue(new ApolloCallback<>(new ApolloCall.Callback<FeedQuery.Data>() {
          @Override public void onResponse(@NotNull Response<FeedQuery.Data> response) {
            Log.i(TAG, response.toString());
          }

          @Override public void onFailure(@NotNull ApolloException e) {
            Log.e(TAG, e.getMessage(), e);
          }
        }, uiHandler));
```

> By default, Apollo will deliver query results on a background, so you have to provide a handler in enqueue if you're using the result to update the UI.

The `ApolloCall.Callback` also provides error handling methods for request parsing failed, network error and request cancelled, amongst others.

In addition to the `data` property, `response` contains an `errors` list with GraphQL errors (for more on this, see the sections on [error handling](https://facebook.github.io/graphql/#sec-Error-handling) and the [response format](https://facebook.github.io/graphql/#sec-Response-Format) in the GraphQL specification).

<h2 id="typed-query-results">Typed query results</h2>

Query results are defined as nested immutable classes that at each level only contain the properties defined in the corresponding part of the query definition. 
This means the type system won't allow you to access fields that are not actually fetched by the query, even if they *are* part of the schema.

For example, given the following schema:

```graphql
enum Episode { NEWHOPE, EMPIRE, JEDI }

interface Character {
  id: String!
  name: String!
  friends: [Character]
  appearsIn: [Episode]!
 }

 type Human implements Character {
   id: String!
   name: String!
   friends: [Character]
   appearsIn: [Episode]!
   height(unit: LengthUnit = METER): Float
 }

 type Droid implements Character {
   id: String!
   name: String!
   friends: [Character]
   appearsIn: [Episode]!
   primaryFunction: String
}
```

And the following query:

```graphql
query HeroAndFriendsNames($episode: Episode) {
  hero(episode: $episode) {
    name
    friends {
      name
    }
  }
}
```

You can fetch results and access data using the following code:

```java
final HeroAndFriendsNames heroAndFriendsQuery = HeroAndFriendsNames.builder()
    .episode(NEWHOPE)
    .build();

apolloClient().query(heroAndFriendsQuery)
    .enqueue(new ApolloCallback<>(new ApolloCall.Callback<HeroAndFriendsNames.Data>() {
      @Override public void onResponse(@NotNull Response<HeroAndFriendsNames.Data> response) {
        Log.i(TAG, response.toString());
      }

      @Override public void onFailure(@NotNull ApolloException e) {
        Log.e(TAG, e.getMessage(), e);
      }
    }, uiHandler));
}
```

Because the above query won't fetch `appearsIn`, this property is not part of the returned result type and cannot be accessed here.

<h2 id="cache-policy">Support For Cached Responses</h2>

As explained in more detail in [the section on watching queries](watching-queries.html), Apollo-android client allows you to keep a client-side cache of query results, making it suitable for use even while offline.
The client can be configured with 3 levels of caching:

- **HTTP Response Cache**: For caching raw http responses.
- **Normalized Disk Cache**: Per node caching of responses in SQL. Persists normalized responses on disk so that they can used after process death. 
- **Normalized InMemory Cache**: Optimized Guava memory cache for in memory caching as long as the App/Process is still alive.  

#### Usage

To enable HTTP Cache support, add the dependency to your project's build.gradle file.

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

    @Override public void onResponse(@NotNull Response<FeedQuery.Data> dataResponse) {
      Log.i(TAG, response.toString());
    }

    @Override public void onFailure(@NotNull Throwable t) {
      Log.e(TAG, e.getMessage(), e);
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
 @NotNull @Override
   public CacheKey fromFieldRecordSet(@NotNull ResponseField field, @NotNull Map<String, Object> recordSet) {
     return formatCacheKey((String) recordSet.get("id"));
   }
 
   @NotNull @Override
   public CacheKey fromFieldArguments(@NotNull ResponseField field, @NotNull Operation.Variables variables) {
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
<h2 id="next-steps">Next steps</h2>

Learning how to build `Query` components to fetch data is one of the most important skills to mastering development with Apollo Client. Now that you're a pro at fetching data, why not try building `Mutation` components to update your data? Here are some resources we think will help you level up your skills:

- [More about queries](https://graphql.org/learn/queries/): Read more about queries directly from the official GraphQL docs. 
- [Mutations](./mutations.html): Learn how to update data with mutations and when you'll need to update the Apollo cache. For a full list of options, check out the API reference for `Mutation` components.
- [Local state management](./local-state.html): Learn how to query local data with `apollo-link-state`.
- [Pagination](../features/pagination.html): Building lists has never been easier thanks to Apollo Client's `fetchMore` function. Learn more in our pagination tutorial.
- [Query component video by Sara Vieira](https://youtu.be/YHJ2CaS0vpM): If you need a refresher or learn best by watching videos, check out this tutorial on `Query` components by Sara!