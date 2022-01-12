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
  mapScalar("MyDate", "com.example.MyDate", NoArgConstructorAdapterInitializer("com.example.MyDateAdapter"))

  mapScalar("ID", "kotlin.Long", SingletonAdapterInitializer("com.apollographql.apollo3.api.LongAdapter"))
  
  mapScalar("MyLong", "kotlin.Long")
}
```

The second parameter is a sealed interface to account of the different ways to use the given adapter name:

```kotlin
sealed interface AdapterInitializer

// The adapter will be instantiated in the generated code
class SingletonAdapterInitializer(val qualifiedName: String): AdapterInitializer

// The adapter will be used as-is (it's an object or a public val)
class NoArgConstructorAdapterInitializer(val qualifiedName: String): AdapterInitializer

// The adapter will be looked up in the `customScalarAdapters` parameter (same as current behavior)
object RuntimeAdapterInitializer: AdapterInitializer
```

`mapScalar`'s signature:

```kotlin
fun Service.mapScalar(graphQLName: String, targetName: String, adapterInitializer: AdapterInitializer = RuntimeAdapterInitializer)
```

Let's also have convenience shortcuts for the types for which we have built-in adapters:

```kotlin
apollo {
  // equivalent to mapScalar("ID", "kotlin.Long", SingletonAdapterInitializer("com.apollographql.apollo3.api.LongAdapter")
  mapScalarToLong("ID")

  // equivalent of mapScalar("Json", "kotlin.Any", SingletonAdapterInitializer("com.apollographql.apollo3.api.AnyAdapter")
  mapScalarToMap("Json")
  
  // etc.
}
```

With this, it is no longer necessary (but still possible) to register the adapters at runtime with `addCustomScalarAdapter`.

#### Compatibility

We need to keep the current mechanism (`customScalarsMapping` + `addCustomScalarAdapter`) working of course.

Let's make `customScalarsMapping` call `mapScalarToMap` internally and mark it as deprecated.

### Code changes

#### Use the mapping info to generate the code

In `ResponseAdapter` and `VariablesAdapter` code generation, we now have more cases to handle to reference the scalar adapter to use:

```
if (an Adapter for the scalar is registered with NoArgConstructorAdapterInitializer) {
    Output the adapter with "()"
    Note: this is not optimal: they are instantiated each time they are used.
    Instead we can generate fields in a specific object (e.g. `ScalarAdapters`) and use them in the generated code.
} else if (an Adapter for the scalar is registered with SingletonAdapterInitializer) {
    Output the adapter without "()"
} else if (an Adapter for the scalar is registered with RuntimeAdapterInitializer) {
    Output the code to lookup the adapter in `customScalarAdapters`
    Note: if the scalar is built-in, a "Type" class is needed - currently they are not generated.
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

Currently, `customScalarsMapping` in `IrBuilder` (and `Options`) is a `Map<String, String>`.
Let's change it to a `Map<String, ScalarInfo>`, with `ScalarInfo` being `data class ScalarInfo(val targetName: String, val adapterInitializer: AdapterInitializer)`.

We need to pass it to `KotlinResolver` / `JavaResolver` because this is where the logic to get the adapter lies (`adapterInitializer()`, `nonNullableAdapterInitializer()`). 

This can be done by passing it to `KotlinCodeGen` / `JavaCodeGen` which instantiate the resolvers.

#### Remaining points

- Avoid instantiating the no arg constructors adapters at each use
  -> Generate fields in a specific object (e.g. `ScalarAdapters`) and use them in the generated code?
- Need a Type class for built-in scalars (e.g. `ID`) when using `RuntimeAdapterInitializer`
