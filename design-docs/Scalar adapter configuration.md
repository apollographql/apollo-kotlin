# Scalar mapping and adapter configuration improvements

This outlines thoughts and suggested improvements to the configuration of Scalar adapters,
following [this request](https://github.com/apollographql/apollo-kotlin/issues/3748).

## Current situation

### User facing API

Documentation is [here](https://www.apollographql.com/docs/kotlin/essentials/custom-scalars/).

Custom scalars need:

- declaring the mapping [GraphQL name] → [Kotlin class name], in the Gradle plugin
  configuration (`customScalarsMapping`)
- registering the adapters to use [GraphQL type (generated)] → [adapter instance],
  on `ApolloClient` (`addCustomScalarAdapter`)

### How it works

`Service.kt`:

```kotlin
/**
 * For custom scalar types like Date, map from the GraphQL type to the java/kotlin type.
 *
 * Default value: the empty map
 */
val customScalarsMapping: MapProperty<String, String>
```

Used to generate the wanted type in generated models / adapters (`kotlin.Any` is used by default when no mapping is
specified).

A “Type” class is also generated.

Example:

```kotlin
public class MyDate {
  public companion object {
    public val type: CustomScalarType = CustomScalarType("MyDate", "com.example.MyDate")
  }
}
```

In the generated `ResponseAdapter` and `VariablesAdapter` objects, the registered scalar adapter is found in
the `customScalarAdapters` parameter, like so:

```kotlin
public override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters):
    GetDateQuery.Data {
// ...
  3 -> date = customScalarAdapters.responseAdapterFor<MyDate>(com.example.type.MyDate.type).nullable().fromJson(reader, customScalarAdapters)
// ...
```

(`AnyAdapter` is used by default when no mapping is declared).

Whereas, for **built-in** scalars (e.g. `ID` and `String`), the type and adapter to use are hard-coded in the generated
code:

```kotlin
2 -> id = StringAdapter.fromJson(reader, customScalarAdapters)
```

The hardcoded types / adapters for built-in scalars are
in [`KotlinResolver`](https://github.com/apollographql/apollo-kotlin/blob/main/apollo-compiler/src/main/kotlin/com/apollographql/apollo3/compiler/codegen/kotlin/KotlinResolver.kt#L26)
and [`JavaResolver`](https://github.com/apollographql/apollo-kotlin/blob/main/apollo-compiler/src/main/kotlin/com/apollographql/apollo3/compiler/codegen/java/JavaResolver.kt)
.

The `customScalarAdapters` comes from what is configured on the `ApolloClient` at runtime by the user. It is passed to
requests (in the `executionContext`), and then to the adapters in `HttpNetworkTransport` / `WebSocketNetworkTransport`.

## Proposed changes

1. Add the ability to specify an adapter in addition to the type class name when declaring the scalar
   mappings in the plugin configuration
2. Allow declaring mappings for built-in scalars (e.g. `ID`)

### User facing API

Plugin configuration:

```kotlin
apollo { 
  service("service") {
    mapScalar("MyDate", "com.example.MyDate", "com.example.MyDateAdapter()")

    mapScalar("ID", "kotlin.Long", "com.apollographql.apollo3.api.LongAdapter")

    mapScalar("MyLong", "kotlin.Long")
  }
}
```

`mapScalar`'s signatures:

```kotlin
/**
 * Map a GraphQL scalar type to the Java/Kotlin type.
 * The adapter must be configured at runtime via `ApolloClient.Builder.addCustomScalarAdapter()`.
 *
 * For example: `mapScalar("Date", "com.example.Date")`
 */
fun mapScalar(graphQLName: String, targetName: String)

/**
 * Map a GraphQL scalar type to the Java/Kotlin type and provided adapter expression.
 *
 * For example:
 * - `mapScalar("Date", "com.example.Date", "com.example.DateAdapter")` (an instance property or object)
 * - `mapScalar("Date", "com.example.Date", "com.example.DateAdapter()")` (instantiate the class on the fly)
 */
fun mapScalar(graphQLName: String, targetName: String, expression: String)
```

Let's also have convenience shortcuts for the types for which we have built-in adapters:

```kotlin
apollo {
  service("service") {
    // equivalent to mapScalar("ID", "kotlin.Long", "com.apollographql.apollo3.api.LongAdapter")
    mapScalarToKotlinLong("ID")

    // equivalent to mapScalar("ID", "java.lang.Long", "com.apollographql.apollo3.api.LongAdapter")
    mapScalarToJavaLong("ID")

    // equivalent to mapScalar("Json", "kotlin.Any", "com.apollographql.apollo3.api.AnyAdapter")
    mapScalarToKotlinAny("Json")

    // etc.
  }
}
```

With this, it is no longer necessary (but still possible) to register the adapters at runtime with `addCustomScalarAdapter`.

#### Compatibility

We need to keep the current mechanism (`customScalarsMapping` + `addCustomScalarAdapter`) working of course.

Let's make `customScalarsMapping` call `mapScalar` internally and mark it as deprecated.

### Code changes

#### Use the mapping info to generate the code

In `ResponseAdapter` and `VariablesAdapter` code generation, we now have more cases to handle to reference the scalar adapter to use:

```
if (an Adapter for the scalar is registered with ExpressionAdapterInitializer) {
    Output the adapter with the expression as-is
} else if (an Adapter for the scalar is registered with RuntimeAdapterInitializer) {
    Output the code to lookup the adapter in `customScalarAdapters`
} else if (the scalar is a built-in (e.g. `ID`)) {
    Output the appropriate adapter (same as current behavior)
} else {
    Output the code to lookup the adapter in `customScalarAdapters`
    Note: this is the fallback to current behavior
}
```

Generated code will look like this:

```kotlin
public override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters):
    GetDateQuery.Data {
// ...

// Before:
  0 -> myDate = customScalarAdapters.responseAdapterFor<MyDate>(com.example.type.MyDate.type).nullable().fromJson(reader, customScalarAdapters)
  1 -> id = StringAdapter.fromJson(reader, customScalarAdapters)
  2 -> myLong = customScalarAdapters.responseAdapterFor<Long>(MyLong.type).nullable().fromJson(reader, customScalarAdapters)

// After:
  0 -> myDate = com.example.MyDateAdapter().nullable().fromJson(reader, customScalarAdapters)
  1 -> id = com.apollographql.apollo3.api.LongAdapter.fromJson(reader, customScalarAdapters)
  2 -> myLong = StringAdapter.fromJson(reader, customScalarAdapters)
  
// ...
```

#### Update and store mapping information

A sealed interface is used to account for the ways adapters can be configured:

```kotlin
sealed interface AdapterInitializer

/**
 * The adapter expression will be used as-is (can be an object, a public val, a class instantiation).
 *
 * e.g. `"com.example.MyAdapter"` or `"com.example.MyAdapter()"`.
 */
class ExpressionAdapterInitializer(val expression: String) : AdapterInitializer

/**
 * The adapter instance will be looked up in the [com.apollographql.apollo3.api.CustomScalarAdapters] provided at runtime.
 */
object RuntimeAdapterInitializer : AdapterInitializer
```

Currently, `customScalarsMapping` in `IrBuilder` (and `Options`) is a `Map<String, String>` (GraphQL name -> Java/Kotlin type).

Let's change it to a `Map<String, ScalarInfo>`, with `ScalarInfo` being `data class ScalarInfo(val targetName: String, val adapterInitializer: AdapterInitializer)`.

We need to pass it to `KotlinResolver` / `JavaResolver` because this is where the logic to get the adapter lies (`adapterInitializer()`, `nonNullableAdapterInitializer()`). 

This can be done by passing it to `KotlinCodeGen` / `JavaCodeGen` which instantiate the resolvers.
