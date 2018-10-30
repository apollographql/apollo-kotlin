---
title: Mutation
---

Queries are useful to fetch data from a server, but client-server communication may also require sending data to the server. This is where Mutations become handy.
Just like REST, any request might end up causing some side-effects on the server, but by convention it's suggested that one doesn't use GET requests to modify data. GraphQL is similar - technically any query could be implemented to cause a data write.
However, it's useful to establish a convention that any operations that cause writes should be sent explicitly via a *mutation*.

Apollo Android handles GraphQL mutations. Mutations are similar to queries in syntax, the only difference being that you use the keyword `mutation` instead of `query` to indicate that the root fields on this query are going to be performing writes to the backend.

```
mutation UpvotePost($postId: Int!) {
  upvotePost(postId: $postId) {
    votes
  }
}
```

GraphQL mutations represent two things in one query string:

1. The mutation field name with arguments, `upvotePost`, which represents the actual operation to be done on the server
2. The fields you want back from the result of the mutation to update the client: `{ votes }`

The above mutation will upvote a post on the server. The result might be:

```
{
  "data": {
    "upvotePost": {
      "id": "123",
      "votes": 5
    }
  }
}
```

Similar to queries, mutations are represented by instances of generated classes, conforming to the `ApolloMutationCall` interface. Constructor arguments are used to define mutation variables. You pass a mutation object to `ApolloClient#perform(mutation)` to send the mutation to the server, execute it, and receive typed results:

```java
UpvotePostMutation upvotePostMutation = UpvotePostMutation.builder()
    .votes(3)
    .build();

apolloClient
    .mutate(upvotePostMutation)
    .enqueue(
        new ApolloCallback<>(new ApolloCall.Callback<UpvotePost.Data>() {
          @Override public void onResponse(@NotNull Response<UpvotePost.Data> response) {
            Log.i(TAG, response.toString());
          }
        
          @Override public void onFailure(@NotNull ApolloException e) {
            Log.e(TAG, e.getMessage(), e);
          }
        }, uiHandler);
    );
```

<h2 id="fragments-in-mutation-results">Using fragments in mutation results</h2>

In many cases, you'll want to use mutation results to update your UI. Fragments can be a great way of sharing result handling between queries and mutations:

```graphql
mutation UpvotePost($postId: Int!) {
  upvotePost(postId: $postId) {
    ...PostDetails
  }
}
```

```java
apolloClient
    .mutate(upvotePostMutation)
    .enqueue(
        new ApolloCallback<>(new ApolloCall.Callback<UpvotePost.Data>() {
          @Override public void onResponse(@NotNull Response<UpvotePost.Data> response) {
            Log.i(TAG, response.data.upvotePost.fragments.postDetails);
          }
        
          @Override public void onFailure(@NotNull ApolloException e) {
            Log.e(TAG, e.getMessage(), e);
          }
        }, uiHandler)
    );
```

<h2 id="input-objects">Passing input objects</h2>

The GraphQL type system includes [input objects](http://graphql.org/learn/schema/#input-types) as a way to pass complex values to fields. Input objects are often defined as mutation variables, because they give you a compact way to pass in objects to be created:

```graphql
mutation CreateReviewForEpisode($episode: Episode!, $review: ReviewInput!) {
  createReview(episode: $episode, review: $review) {
    stars
    commentary
  }
}
```

```swift
let review = ReviewInput(stars: 5, commentary: "This is a great movie!")
apolloClient
    .mutate(CreateReviewForEpisodeMutation(episode: .jedi, review: review))
```

<h2 id="designing-mutation-results">Designing mutation results</h2>

In GraphQL, mutations can return any type, and that type can be queried just like a regular GraphQL query. So the question is - what type should a particular mutation return?

In most cases, the data available from a mutation result should be the server developer's best guess of the data a client would need to understand what happened on the server. For example, a mutation that creates a new comment on a blog post might return the comment itself. A mutation that reorders an array might need to return the whole array.

<h2 id="next-steps">Next steps</h2>

Learning how to build `Mutation` components to update your data is an important part of developing applications with Apollo Client. Now that you're well-versed in updating data, why not try executing client-side mutations with `apollo-link-state`? Here are some resources we think will help you level up your skills:

- [#125, Fragmented Podcast](http://fragmentedpodcast.com/episodes/125/): Why's and How's about Apollo Android and the entire journey.
- [Caching in Apollo](support-for-cached-responses.md): Dive deep into the Apollo cache and how it's normalized in our advanced guide on caching. Understanding the cache is helpful when writing your mutation's `update` function!
- [Mutation component video by Sara Vieira](https://youtu.be/2SYa0F50Mb4): If you need a refresher or learn best by watching videos, check out this tutorial on `Mutation` components by Sara!