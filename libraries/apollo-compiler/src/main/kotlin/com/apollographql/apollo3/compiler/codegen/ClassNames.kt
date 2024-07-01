package com.apollographql.apollo.compiler.codegen

/**
 * A list of constant [ResolverClassName] that don't use `class.name` and therefore survive proguard/R8
 */
internal object ClassNames {
  const val apolloExecutionPackageName: String = "com.apollographql.apollo.execution"
  const val apolloAstPackageName: String = "com.apollographql.apollo.ast"
  const val apolloApiPackageName = "com.apollographql.apollo.api"
  const val apolloAnnotationsPackageName = "com.apollographql.apollo.annotations"
  const val apolloApiJsonPackageName = "$apolloApiPackageName.json"
  private const val jetbrainsAnnotationsPackageName = "org.jetbrains.annotations"

  val ObjectType = ResolverClassName(apolloApiPackageName, "ObjectType")
  val InterfaceType = ResolverClassName(apolloApiPackageName, "InterfaceType")
  val ObjectTypeBuilder = ResolverClassName(apolloApiPackageName, "ObjectType", "Builder")
  val InterfaceTypeBuilder = ResolverClassName(apolloApiPackageName, "InterfaceType", "Builder")

  val ObjectBuilder = ResolverClassName(apolloApiPackageName, "ObjectBuilder")
  val BuilderProperty = ResolverClassName(apolloApiPackageName, "BuilderProperty")
  val JsonReader = ResolverClassName(apolloApiJsonPackageName, "JsonReader")
  val JsonWriter = ResolverClassName(apolloApiJsonPackageName, "JsonWriter")
  val CustomScalarAdapters = ResolverClassName(apolloApiPackageName, "CustomScalarAdapters")
  val Input = ResolverClassName(apolloApiPackageName, "Input")
  val CustomScalarAdaptersBuilder = ResolverClassName(apolloApiPackageName, "CustomScalarAdapters", "Builder")
  val Optional = ResolverClassName(apolloApiPackageName, "Optional")
  val Absent = ResolverClassName(apolloApiPackageName, "Optional", "Absent")
  val Present = ResolverClassName(apolloApiPackageName, "Optional", "Present")
  val Adapter = ResolverClassName(apolloApiPackageName, "Adapter")
  val CompiledSelection = ResolverClassName(apolloApiPackageName, "CompiledSelection")
  val CompiledNamedType = ResolverClassName(apolloApiPackageName, "CompiledNamedType")
  val UnionType = ResolverClassName(apolloApiPackageName, "UnionType")
  val Fragment = ResolverClassName(apolloApiPackageName, "Fragment")
  val FragmentData = ResolverClassName(apolloApiPackageName, "Fragment", "Data")
  val Query = ResolverClassName(apolloApiPackageName, "Query")
  val Mutation = ResolverClassName(apolloApiPackageName, "Mutation")
  val Subscription = ResolverClassName(apolloApiPackageName, "Subscription")
  val QueryData = ResolverClassName(apolloApiPackageName, "Query", "Data")
  val MutationData = ResolverClassName(apolloApiPackageName, "Mutation", "Data")
  val SubscriptionData = ResolverClassName(apolloApiPackageName, "Subscription", "Data")
  val EnumType = ResolverClassName(apolloApiPackageName, "EnumType")
  val CustomScalarType = ResolverClassName(apolloApiPackageName, "CustomScalarType")
  val True = ResolverClassName(apolloApiPackageName, "BooleanExpression", "True")
  val False = ResolverClassName(apolloApiPackageName, "BooleanExpression", "False")
  val CompiledArgument = ResolverClassName(apolloApiPackageName, "CompiledArgument", "Builder")
  val CompiledArgumentDefinition = ResolverClassName(apolloApiPackageName, "CompiledArgumentDefinition")
  val CompiledArgumentDefinitionBuilder = ResolverClassName(apolloApiPackageName, "CompiledArgumentDefinition", "Builder")
  val CompiledVariable = ResolverClassName(apolloApiPackageName, "CompiledVariable")
  val JsonNumber = ResolverClassName(apolloApiJsonPackageName, "JsonNumber")
  val CompiledCondition = ResolverClassName(apolloApiPackageName, "CompiledCondition")
  val CompiledField = ResolverClassName(apolloApiPackageName, "CompiledField")
  val CompiledFieldBuilder = ResolverClassName(apolloApiPackageName, "CompiledField", "Builder")
  val CompiledFragment = ResolverClassName(apolloApiPackageName, "CompiledFragment")
  val CompiledFragmentBuilder = ResolverClassName(apolloApiPackageName, "CompiledFragment", "Builder")
  val DefaultFakeResolver = ResolverClassName(apolloApiPackageName, "DefaultFakeResolver")
  val FakeResolver = ResolverClassName(apolloApiPackageName, "FakeResolver")
  val JetBrainsNullable = ResolverClassName(jetbrainsAnnotationsPackageName, "Nullable")
  val JetBrainsNonNull = ResolverClassName(jetbrainsAnnotationsPackageName, "NotNull")
  val FieldResult = ResolverClassName(apolloApiPackageName, "FieldResult")
}
