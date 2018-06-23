---
title: Queries
---

Fetching data in a simple, predictable way is one of the core features of Apollo Android. In this guide, you'll learn how to Query GraphQL data and use the result in your android application.
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

> By default, Apollo will deliver query results on a background thread. You can provide a handler in `enqueue`, or use [`apollo-android-support`](https://github.com/apollographql/apollo-android/tree/master/apollo-android-support) if you're using the result to update the UI.

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

<h2 id="next-steps">Next steps</h2>

Learning how to build `Query` components to fetch data is one of the most important skills to mastering development with Apollo Client. Now that you're a pro at fetching data, why not try building `Mutation` components to update your data? Here are some resources we think will help you level up your skills:

- [More about queries](https://graphql.org/learn/queries/): Read more about queries directly from the official GraphQL docs.
- [Caching responses](./support-for-cached-responses.md): Learn how to cache responses with apollo-android.
- [Mutations](./mutations.html): Learn how to update data with mutations and when you'll need to update the Apollo cache. For a full list of options, check out the API reference for `Mutation` components.
- [Local state management](./local-state.html): Learn how to query local data with `apollo-link-state`.
- [Pagination](../features/pagination.html): Building lists has never been easier thanks to Apollo Client's `fetchMore` function. Learn more in our pagination tutorial.
- [Query component video by Sara Vieira](https://youtu.be/YHJ2CaS0vpM): If you need a refresher or learn best by watching videos, check out this tutorial on `Query` components by Sara!