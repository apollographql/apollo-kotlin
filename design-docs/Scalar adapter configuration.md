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
  3 -> date = customScalarAdapters.responseAdapterFor<MyDate>(com.example.type.MyDate.type).nullable().fromJson(reader,
  customScalarAdapters)
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

1. Add the ability to specify an adapter class name in addition to the type class name when declaring the scalar
   mappings in the plugin configuration
2. Allow declaring mappings for built-in scalars (e.g. `ID`)

### User facing API

In the plugin configuration:

```kotlin
apollo {
  mapScalar("ID", "kotlin.Long", "com.apollographql.apollo3.api.LongAdapter")
}
```

We could also have shortcuts for the types for which there’s a built-in adapter:

```kotlin
apollo {
  mapScalarToLong("ID")
  mapScalarToMap("Json")
  ...
}
```

With this, it is no longer necessary to register the adapter at runtime with `addCustomScalarAdapter`.

#### Compatibility

We need to keep the current mechanism (`customScalarsMapping` + `addCustomScalarAdapter`) working of course but let’s
mark them as `@Deprecated` - unless there’s still valid use cases I’m not seeing?

In the plugin configuration, using both the current `customScalarsMapping` and new `mapScalar` should be prohibited to
avoid potential confusion and mistakes.

Similarly, using both the new `mapScalar` (plugin configuration) and the current `addCustomScalarAdapter` (runtime) is
probably a user error which would be nice to detect/report, if possible.

### Code changes

#### 1. Don’t hardcode built-in scalar adapters

In `ResponseAdapter` and `VariablesAdapter` code generation, instead of hardcoding the adapter to use for built-in
scalars, treat them like custom scalar, i.e. look up the adapter in `CustomScalarAdapters`.

This means we need to generate a “Type” class for the built-in scalars, too.

One way to do this is to automatically add all built-in scalar mappings, except if user defined.

So for instance, if the user has not defined any custom scalar, we would have the mapping:

- `“ID”, “kotlin.String”, “com.apollographql.apollo3.api.StringAdapter”` ← automatically added
- `“String”, “kotlin.String”, “com.apollographql.apollo3.api.StringAdapter”` ← automatically added
- etc.

If they have defined a custom scalar:

- `“MyDate”, “com.example.MyDate”, “com.example.MyDateAdapter”`
- `“ID”, “kotlin.String”, “com.apollographql.apollo3.api.StringAdapter”` ← automatically added
- `“String”, “kotlin.String”, “com.apollographql.apollo3.api.StringAdapter”` ← automatically added
- etc.

If they have defined a custom scalar, and also need a specific type / adapter for a built-in scalar:

- `“MyDate”, “com.example.MyDate”, “com.example.MyDateAdapter”`
- `“ID”, “kotlin.Long”, “com.apollographql.apollo3.api.LongAdapter”`
- `“String”, “kotlin.String”, “com.apollographql.apollo3.api.StringAdapter”` ← automatically added
- etc.

#### 2. Generate `CustomScalarAdapters`

The mapping defined in `mapScalar` (including the automatically added ones as discussed above) can be used
to generate an object that looks like this:

```kotlin
package com.example

object ScalarAdapters {
  val customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Builder()
      .add(MyDate.type, com.example.MyDateAdapter())
      .add(ID.type, com.apollographql.apollo3.api.LongAdapter())
      .add(String.type, com.apollographql.apollo3.api.StringAdapter())
      // etc.

      .build()
}
```

A way to use this class with minimum changes would be to require passing it to the `ApolloClient` at runtime (i.e.
`ApolloClient.Builder().customScalarAdapters(ScalarAdapters.customScalarAdapters)`) but requiring this would be
not user-friendly, and error-prone.

Instead, we could in the generated  `ResponseAdapter` / `VariablesAdapter` ignore the `customScalarAdapters` parameter
and use `ScalarAdapters.customScalarAdapters` instead:

```kotlin
public override fun fromJson(reader: JsonReader, customScalarAdapters: CustomScalarAdapters):
    GetDateQuery.Data {
// ...

// Before:
// 3 -> date = customScalarAdapters.responseAdapterFor<MyDate>(com.example.type.MyDate.type).nullable().fromJson(reader,
//              customScalarAdapters)

// After:
  3 -> date = ScalarAdapters.customScalarAdapters.responseAdapterFor<MyDate>(com.example.type.MyDate.type).nullable().fromJson(reader,
               ScalarAdapters.customScalarAdapters)

// ...
```

This new behavior should be enabled only when `mapScalar` is used / keep the current behavior if not.

#### Thoughts

It is a bit odd / clumsy to have an `customScalarAdapters` argument which is not used in the generated code.

Alternatively we could make the switch earlier:

- for `VariableAdapters` this could be in `Executable.serializeVariables` implementations directly
- for `ResponseAdapter` we could define a new `customScalarAdapter(): CustomScalarAdapters?` method on `Executable` and
  generate the implementation like so:

```kotlin
// When mapScalar is used (new behavior)
override fun customScalarAdapters(): CustomScalarAdapters? = ScalarAdapters.customScalarAdapters

// When mapScalar is not used (current behavior)
override fun customScalarAdapters(): CustomScalarAdapters? = null
```

And then in `HttpNetworkTransport` / `WebSocketNetworkTransport` use that instead of the request’s if it’s not `null`.

This may be more elegant but introduces changes in `Executable` (although we could provide a default implementation
returning `null`).


##### Other thoughts:

Naming: `customScalarAdapters` may no longer be a perfect name if it now also include built-in scalars.
