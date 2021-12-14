Json is a relatively simple format but there are a few caveats in Apollo Kotlin:

## Fields order

Because the GraphQL execution algorithm specifies the order in which fields are returned, it is possible to "predict" the next name in an object and avoid a few String comparisons. This works most of the times but not when `@include` directives are used so there is a fallback mecanism to String comparison in that case

## Buffering

The `compat` and `operationBased` models duplicate the merge fields and need to read them several times. When this happens, the reader must support the `rewind()` operation 

## Numbers

- Json has arbitrary precision numbers
- GraphQL has Int, Float, custom scalars
- Kotlin has Int, Double, Long, JsonNumber()

Internally, Apollo uses `Map<String, Any?>` in multiple places to represent a Json document:
- Cache `Record`
- WebSocket `Payload`
- Buffered response

Because there is no 1:1 mapping between Json and Kotlin types, the `Map` can contain values in different formats. For an example, a GraphQL Int might be stored in a Map as a Long or an Int or even a String. The caller has to have knowledge about the expected type and the `JsonReader` will coerce to `Int` when `nextInt()` is called. 

This is used with `Long` in particular. Even if `Long` is not in the GraphQL spec, `BufferedSourceJsonReader` can read them without having to allocate a String (because it reads every char and builds the Long as it goes). This way, it is often better to keep the Int as a Long and only convert it back to it when building the model. This is why there is `nextLong()` and `value(Long)` even though it's not in the spec.

Other custom scalars bigger than Long can be read with `nextNumber()` and use a small `JsonNumber` wrapper. It's also possible to call `nextString()` and avoid the wrapper altogether.



