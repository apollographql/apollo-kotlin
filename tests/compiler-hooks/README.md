# Compiler hooks examples

In this project you can see examples of how to use the compiler hooks.

A few example implementations of `ApolloCompilerKotlinHooks` are defined in `build.gradle.kts`:

- `DefaultNullValuesHooks` - Adds a default `null` value to data class fields that are nullable
- `TypeNameInterfaceHooks` - Adds a super interface to models that expose `__typename`
- `PrefixNamesKotlinHooks` / `PrefixNamesJavaHooks` - Prefix generated class names with the specified prefix. Shows how
  to make `postProcessFileSpec` and `overrideResolvedType` work together.
- `CapitalizeEnumValuesHooks` - Capitalize generated enum values
- `AddGettersAndSettersHooks` - Add getters and setters to the models

## Gradle plugin

To use `compilerKotlinHooks` you must use `id("com.apollographql.apollo3.external")` instead of the
usual `id("com.apollographql.apollo3")`. This is because the external plugin doesn't relocate the KotlinPoet dependency that the hooks api depends on.
