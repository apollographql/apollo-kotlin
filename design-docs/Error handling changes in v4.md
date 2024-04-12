# Error handling changes in v4

This document describes the changes made around error handling in v4. The purpose is to keep a log of the changes, in a
kind of "decision record" way.

## Broaden the meaning of `ApolloResponse.isFromCache`

In v3, `ApolloResponse.isFromCache` returns `true` if the **data** comes from the cache (implementation:
`cacheInfo?.isCacheHit == true`). A more descriptive name could have been `isCacheHit`.

In v4, since cache misses no longer throw, a response can come from the cache whether the data is present or not.

With that in mind, it makes more sense for `isFromCache` to return `true` if the **response** comes from the cache,
regardless of the data being present or not (new implementation:
`cacheInfo?.isCacheHit == true || exception is CacheMissException`).

Note: this **is** a behavior change for projects that used `emitCacheMisses(true)` in v3:

- cache miss responses in v3: `isFromCache` would return `false` ("data is not a cache hit")
- cache miss responses in v4: `isFromCache` will return `true` ("response is from the cache")

More context in [#5799](https://github.com/apollographql/apollo-kotlin/issues/5799).
