# Pagination with the normalized cache

When using the normalized cache, objects are stored in records keyed by the object's id:

Query:

```graphql
query Users {
  allUsers(groupId: 2) {
    id
    name
  }
}
```

Response:

```json
{
  "data": {
    "allUsers": [
      {
        "id": 1,
        "name": "John Smith"
      },
      {
        "id": 2,
        "name": "Jane Doe"
      }
    ]
  }
}
```

Normalized cache:

| Cache Key  | Record                                             |
|------------|----------------------------------------------------|
| QUERY_ROOT | allUsers(groupId: 2): \[ref(user:1), ref(user:2)\] | 
| user:1     | id: 1, name: John Smith                            |
| user:2     | id: 2, name: Jane Doe                              |

The app can watch the `Users()` query and update the UI with the whole list when the data changes.

However with pagination things become less obvious:

Query:

```graphql
query UsersPage($page: Int!) {
  usersPage(groupId: 2, page: $page) {
    id
    name
  }
}
```

Response:

```json
{
  "data": {
    "usersPage": [
      {
        "id": 1,
        "name": "John Smith"
      },
      {
        "id": 2,
        "name": "Jane Doe"
      }
    ]
  }
}
```

Normalized cache:

| Cache Key  | Record                                                       |
|------------|--------------------------------------------------------------|
| QUERY_ROOT | usersPage(groupId: 2, page: 1): \[ref(user:1), ref(user:2)\] |
| user:1     | id: 1, name: John Smith                                      |
| user:2     | id: 2, name: Jane Doe                                        |

After fetching page 2, the cache will look like this:

| Cache Key  | Record                                                                                                                     |
|------------|----------------------------------------------------------------------------------------------------------------------------|
| QUERY_ROOT | usersPage(groupId: 2, page: 1): \[ref(user:1), ref(user:2)\], usersPage(groupId: 2, page: 2): \[ref(user:3), ref(user:4)\] |
| user:1     | id: 1, name: John Smith                                                                                                    |
| user:2     | id: 2, name: Jane Doe                                                                                                      |
| user:3     | id: 3, name: Peter Parker                                                                                                  |
| user:4     | id: 4, name: Bruce Wayne                                                                                                   |

Which query should the app watch to update the UI?

Watching `UsersPage(page = 1)` would only notify changes to the first page.

For the whole list to be reactive you'd need to watch the queries for each page, and update the corresponding segment of the list. While technically possible, this is cumbersome to implement.

You could skip watching altogether and only update the list when scrolling to its end, but that would mean that changes to individual users would not refresh the list.

What we need is having the whole list in a single record, so we can watch a single query.

## Using ApolloStore

The cache can be updated manually using the `ApolloStore` class.

```kotlin
suspend fun fetchAndMergePage(nextPage: Int) {
  // 1. Get the current list from the cache
  val listQuery = UsersPageQuery(page = 1)
  val cacheResponse = apolloClient.query(listQuery).fetchPolicy(FetchPolicy.CacheOnly).execute()

  // 2. Fetch the next page from the network (don't update the cache yet)
  val networkResponse = apolloClient.query(UsersPageQuery(page = nextPage)).fetchPolicy(FetchPolicy.NetworkOnly).execute()

  // 3. Merge the next page with the current list
  val mergedList = cacheResponse.data.usersPage.items + networkResponse.data.usersPage.items
  val dataWithMergedList = networkResponse.data.copy(
      usersPage = networkResponse.data.usersPage.copy(
          items = mergedList
      )
  )

  // 4. Update the cache with the merged list
  withContext(Dispatchers.IO) {
    apolloClient.apolloStore.writeOperation(operation = listQuery, operationData = dataWithMergedList)
  }
}
```

Note that in this simple example, we need to remember the last fetched page, so we can know which page to fetch next. This can be stored in shared preferences for instance.
However in most cases the API can return a "page info" object containing the information needed to fetch the next page, and this can be stored in the cache with the rest of the data.  

## Using the incubating pagination support

### Relay-style pagination

[Relay-style pagination](https://relay.dev/graphql/connections.htm) is a common way of modeling pagination in GraphQL, where fields return `Connection`s that contain a list of `Edges`:

```graphql
type Query {
  usersConnection(first: Int = 10, after: String = null, last: Int = null, before: String = null): UserConnection!
}

type UserConnection {
  pageInfo: PageInfo!
  edges: [UserEdge!]!
}

type PageInfo {
  hasNextPage: Boolean!
  hasPreviousPage: Boolean!
  startCursor: String
  endCursor: String
}

type UserEdge {
  cursor: String!
  node: User!
}

type User {
  id: ID!
  name: String!
}
```

```graphql
query UsersConnection($first: Int, $after: String, $last: Int, $before: String) {
  usersConnection(first: $first, after: $after, last: $last, before: $before) {
    edges {
      cursor
      node {
        name
      }
    }
    pageInfo {
      hasNextPage
      endCursor
    }
  }
}
```

If your schema uses this pagination style, the library supports it out of the box: use the `connectionFields` argument to specify the fields that return a connection:

```graphql
extend type Query @typePolicy(connectionFields: "usersConnection")
```

In Kotlin configure the `ApolloStore` like this, using the generated `Pagination` object:

```kotlin
val apolloStore = ApolloStore(
  normalizedCacheFactory = cacheFactory,
  cacheKeyGenerator = TypePolicyCacheKeyGenerator,
  metadataGenerator = ConnectionMetadataGenerator(Pagination.connectionTypes),
  apolloResolver = FieldPolicyApolloResolver,
  recordMerger = ConnectionRecordMerger
)
```

Query `UsersConnection()` to fetch new pages and update the cache, and watch it to observe the full list.

### Other types of pagination

If your schema uses a different pagination style, you can still use the pagination support, with more configuration needed.

#### Pagination arguments

The `@fieldPolicy` directive has a `paginationArgs` argument that can be used to specify the arguments that should be omitted from the field name.

Going back to the example above with `usersPage`:


```graphql
extend type Query
@fieldPolicy(forField: "usersPage" paginationArgs: "page")
```

(Note: this can also be done programmatically by configuring the `ApolloStore` with a `FieldNameGenerator` implementation.)

With that in place, after fetching the first page, the cache will look like this:

| Cache Key  | Record                                                  |
|------------|---------------------------------------------------------|
| QUERY_ROOT | **usersPage(groupId: 2)**: \[ref(user:1), ref(user:2)\] |
| user:1     | id: 1, name: John Smith                                 |
| user:2     | id: 2, name: Jane Doe                                   |

The field name no longer includes the `page` argument, which means watching `UsersPage(page = 1)` or any page will observe the same list.

Here's what happens when fetching the second page:

| Cache Key  | Record                                              |
|------------|-----------------------------------------------------|
| QUERY_ROOT | usersPage(groupId: 2): \[ref(user:3), ref(user:4)\] |
| user:1     | id: 1, name: John Smith                             |
| user:2     | id: 2, name: Jane Doe                               |
| user:3     | id: 3, name: Peter Parker                           |
| user:4     | id: 4, name: Bruce Wayne                            |

The field containing the first page was overwritten by the second page.

This is because the field name is now the same for all pages and the default merging strategy is to overwrite existing fields with the new value.

#### Record merging

To fix this we need to supply the store with a piece of code that can merge the lists in a sensible way.
This is done by passing a `RecordMerger` to the `ApolloStore` constructor:

```kotlin
object MyFieldMerger : FieldRecordMerger.FieldMerger {
  override fun mergeFields(existing: FieldRecordMerger.FieldInfo, incoming: FieldRecordMerger.FieldInfo): FieldRecordMerger.FieldInfo {
    val existingList = existing.value as List<*>
    val incomingList = incoming.value as List<*>
    val mergedList = existingList + incomingList
    return FieldRecordMerger.FieldInfo(
        value = mergedList,
        metadata = emptyMap()
    )
  }
}

val apolloStore = ApolloStore(
  normalizedCacheFactory = cacheFactory,
  cacheKeyGenerator = TypePolicyCacheKeyGenerator,
  apolloResolver = FieldPolicyApolloResolver,
  recordMerger = FieldRecordMerger(MyFieldMerger), // Configure the store with the custom merger
)
```

With this, the cache will be as expected after fetching the second page:

| Cache Key  | Record                                                                        |
|------------|-------------------------------------------------------------------------------|
| QUERY_ROOT | usersPage(groupId: 2): \[ref(user:1), ref(user:2), ref(user:3), ref(user:4)\] |
| user:1     | id: 1, name: John Smith                                                       |
| user:2     | id: 2, name: Jane Doe                                                         |
| user:3     | id: 3, name: Peter Parker                                                     |
| user:4     | id: 4, name: Bruce Wayne                                                      |

The `RecordMerger` shown above is simplistic: it will always append new items to the end of the existing list.
In a real app, we need to look at the contents of the incoming page and decide if and where to append / insert the items.

To do that it is usually necessary to have access to the arguments that were used to fetch the existing/incoming lists (e.g. the page number), to decide what to do with the new items.
For instance if the existing list is for page 1 and the incoming one is for page 2, we should append.

Fields in records can have arbitrary metadata attached to them, in addition to their value. We'll use this to implement a more capable merging strategy.

#### Metadata

Let's go back to the above example where Relay-style pagination is used.

Configure the `paginationArgs` as seen previously:

```graphql
extend type Query
@fieldPolicy(forField: "usersConnection" paginationArgs: "first,after,last,before")
```

Now let's store in the metadata of each `UserConnection` field the values of the `before` and `after` arguments of the field returning it, as well as the values of the first and last cursor in its list.
This will allow us to insert new pages in the correct position later on.

This is done by passing a `MetadataGenerator` to the `ApolloStore` constructor:

```kotlin
class ConnectionMetadataGenerator : MetadataGenerator {
  @Suppress("UNCHECKED_CAST")
  override fun metadataForObject(obj: ApolloJsonElement, context: MetadataGeneratorContext): Map<String, ApolloJsonElement> {
    if (context.field.type.rawType().name == "UserConnection") {
      obj as Map<String, ApolloJsonElement>
      val edges = obj["edges"] as List<Map<String, ApolloJsonElement>>
      val startCursor = edges.firstOrNull()?.get("cursor") as String?
      val endCursor = edges.lastOrNull()?.get("cursor") as String?
      return mapOf(
          "startCursor" to startCursor,
          "endCursor" to endCursor,
          "before" to context.argumentValue("before"),
          "after" to context.argumentValue("after"),
      )
    }
    return emptyMap()
  }
}
```

However, this cannot work yet.

Normalization will make the `usersConnection` field value be a **reference** to the `UserConnection` record, and not the actual connection. 
Because of this, we won't be able to access its metadata inside the `RecordMerger` implementation.
Furthermore, the `edges` field value will be a list of **references** to the `UserEdge` records which will contain the item's list index in their cache key (e.g. `usersConnection.edges.0`, `usersConnection.edges.1`) which will break the merging logic.

#### Embedded fields

To remediate this, we can configure the cache to skip normalization for certain fields. When doing so, the value will be embedded directly into the record instead of being referenced. 

This is done with the `embeddedFields` argument of the `@typePolicy` directive:

```graphql
# Embed the value of the `usersConnection` field in the record
extend type Query @typePolicy(embeddedFields: "usersConnection")

# Embed the values of the `edges` field in the record
extend type UserConnection @typePolicy(embeddedFields: "edges")
```

(Note: this can also be done programmatically by configuring the `ApolloStore` with an `EmbeddedFieldsProvider` implementation.)


Now that we have the metadata and embedded fields in place, we can implement the `RecordMerger` (simplified for brevity):

```kotlin
object ConnectionFieldMerger : FieldRecordMerger.FieldMerger {
  @Suppress("UNCHECKED_CAST")
  override fun mergeFields(existing: FieldRecordMerger.FieldInfo, incoming: FieldRecordMerger.FieldInfo): FieldRecordMerger.FieldInfo {
    // Get existing field metadata
    val existingStartCursor = existing.metadata["startCursor"]
    val existingEndCursor = existing.metadata["endCursor"]

    // Get incoming field metadata
    val incomingBeforeArgument = incoming.metadata["before"]
    val incomingAfterArgument = incoming.metadata["after"]

    // Get the lists
    val existingList = (existing.value as Map<String, ApolloJsonElement>)["edges"] as List<*>
    val incomingList = (incoming.value as Map<String, ApolloJsonElement>)["edges"] as List<*>

    // Merge the lists
    val mergedList: List<*> = if (incomingAfterArgument == existingEndCursor) {
      // We received the next page: its `after` argument matches the last cursor of the existing list
      existingList + incomingList
    } else if (incomingBeforeArgument == existingStartCursor) {
      // We received the previous page: its `before` argument matches the first cursor of the existing list
      incomingList + existingList
    } else {
      // We received a list which is neither the previous nor the next page.
      // Handle this case by resetting the cache with this page
      incomingList
    }

    val mergedFieldValue = existing.value.toMutableMap()
    mergedFieldValue["edges"] = mergedList
    return FieldRecordMerger.FieldInfo(
        value = mergedFieldValue,
        metadata = mapOf() // Omitted for brevity
    )
  }
}
```

A complete implementation can be found [here](https://github.com/apollographql/apollo-kotlin/blob/main/libraries/apollo-normalized-cache-api-incubating/src/commonMain/kotlin/com/apollographql/apollo3/cache/normalized/api/RecordMerger.kt#L101).
