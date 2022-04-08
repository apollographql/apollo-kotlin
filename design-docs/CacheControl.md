# CacheControl

## Context

In a traditional HTTP API, the semantics of CacheControl are well-defined. Mozilla for an example has a [very detailed documentation about the different cache headers](https://developer.mozilla.org/en-US/docs/Web/HTTP/Caching).

In a GraphQL API, HTTP caching doesn't work as well because the data might overlap between two queries. Because we want the data to be de-duplicated to have a single source of truth, HTTP caching only get us that far.

This document is a working document to add CacheControl to the normalized cache

## Server cache control 

Apollo Server has a [`@cacheControl` directive](https://www.apollographql.com/docs/apollo-server/performance/caching/) for cache control.

```graphql
# A session can be cached 10min
type Session @cacheControl(maxAge: 600) {
  # But dates might change often so they have a 30s expiration
  startDate: Date! @cacheControl(maxAge: 30)
  endDate: Date! @cacheControl(maxAge: 30)
  title: String!
  speaker: Speaker!
}

# Speaker details do not change often so we cache them for 1h
type Speaker @cacheControl(maxAge: 3600) {
  name: String!
  bio: String!
  twitter: String
}
```

The `@cacheControl` directives are analyzed and then exposed in a `Cache-Control` HTTP header. Because there is only one `Cache-Control` HTTP header for the whole query, the most restrictive `maxAge` is used. A query like this:

```graphql
{
  session(id: "42") {
    # Because of startDate, the whole query will only be cached 30s
    startDate
    title
    speaker {
      name
      bio
    }
  }
}
```

will only be cached 30s because it has a field that changes often (`startDate`)

## Client cache control

The same server-side rules could be added client side:

```graphql
extend type Session @cacheControl(maxAge: 600) {
  startDate: Date! @cacheControl(maxAge: 30)
  endDate: Date! @cacheControl(maxAge: 30)
}

extend type Speaker @cacheControl(maxAge: 3600) 
```

Or if introspection ever supports directive usages, it could be retrieved (and cached) at runtime.

## Storing the cache control information

We can think of several fields to store (loosely related to their equivalent HTTP definitions):

- Date: the date the data originated (details TBC: created vs sent vs receive vs other?)
- MaxAge: the maximum age before the data becomes stale
- StaleWhileRevalidate
- ...

Date sounds required in all cases. MaxAge is required if we want to store that information when coming from the server but could be spared if the rules are defined on the client side.

Because any operation can query any field, this information needs to be stored per-field. This is the hard part. Adding one (or several) 64 bit timestamp for each field might double (or worse) the size of the cache. On the other hand a lot of fields will share the same timestamp, opening opportunities for compression (see #possible-optimisations).

## Initial implementation

To keep things simple, it is proposed that the initial implementation will add Date information to every field of every Record and expose a new `fun date(name): Long?`:

```kotlin
class Record {
  val fields: Map<String, Any?>
  fun date(fieldName: String): Long?
}
```

When writing a record, the Date timestamp can be passed in `CacheHeaders` without changing the current API:

```kotlin
abstract fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String>
```

To keep the database size manageable, the Records can be serialized in binary format, skipping the Json overhead and compressing timestamps when the same timestamp is used for multiple fields.

To avoid storing another Long in the DB, it is proposed to define cache control rules in the client. Ideally using directives:

```graphql
extend type Query {
  currentMinute @maxAge(60)
}
extend type Day @maxAge(86400)
```

Programmatic APIs (TBD) should be available as well.

## Benchmarks

Because this touches low level cache things, it's important that we have benchmarks to monitor how the cache is behaving in real life:

- cache size
- read performance
- write performance
- behaviour after a lot of writes

## Possible optimisations

### Compress field names and timestamps

The Records duplicate a lot of information. Typically, field names and timestamps will end up duplicated a lot. In a list, each record stores multiple full utf8 strings for each and every field even if all of them have the same name.

This feels like this could be compressed using an appropriate/TBD data structure.

### Allow partial Record writes

Similarly, updating one field in a Record requires rewriting all the Record. Storing fields separately might help.

### Tweaking memory caches

We currently recommend chaining a memory cache and a SQLite cache. While this works well, this adds a 3rd memory cache:

- OS I/O cache
- SQLite page cache
- Apollo Memory cache

This might ultimately be counterproductive and we could maybe improve performance by removing the number of caches. 