package com.apollographql.apollo3.compiler.codegen

import com.squareup.kotlinpoet.ClassName

/**
 * A list of constant [ResolverClassName] that don't use `class.name` and therefore survive proguard/R8
 */
internal object ClassNames {
  const val apolloApiPackageName = "com.apollographql.apollo3.api"
  const val apolloAnnotationsPackageName = "com.apollographql.apollo3.annotations"
  const val apolloApiJsonPackageName = "$apolloApiPackageName.json"
  const val apolloApiTestPackageName = "$apolloApiPackageName.test"

  val ObjectType = ResolverClassName(apolloApiPackageName, "ObjectType")
  val InterfaceType = ResolverClassName(apolloApiPackageName, "InterfaceType")

  val JsonReader = ResolverClassName(apolloApiJsonPackageName, "JsonReader")
  val JsonWriter = ResolverClassName(apolloApiJsonPackageName, "JsonWriter")
  val CustomScalarAdapters = ResolverClassName(apolloApiPackageName, "CustomScalarAdapters")
  val Optional = ResolverClassName(apolloApiPackageName, "Optional")
  val Absent = ResolverClassName(apolloApiPackageName, "Optional", "Absent")
  val Present = ResolverClassName(apolloApiPackageName, "Optional", "Present")
  val Adapter = ResolverClassName(apolloApiPackageName, "Adapter")
  val CompiledSelection = ResolverClassName(apolloApiPackageName, "CompiledSelection")
  val CompiledType = ResolverClassName(apolloApiPackageName, "CompiledType")
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
  val CompiledArgument = ResolverClassName(apolloApiPackageName, "CompiledArgument")
  val CompiledVariable = ResolverClassName(apolloApiPackageName, "CompiledVariable")
  val CompiledCondition = ResolverClassName(apolloApiPackageName, "CompiledCondition")
  val CompiledField = ResolverClassName(apolloApiPackageName, "CompiledField")
  val CompiledFieldBuilder = ResolverClassName(apolloApiPackageName, "CompiledField", "Builder")
  val CompiledFragment = ResolverClassName(apolloApiPackageName, "CompiledFragment")
  val CompiledFragmentBuilder = ResolverClassName(apolloApiPackageName, "CompiledFragment", "Builder")
  val TestResolver = ResolverClassName(apolloApiTestPackageName, "TestResolver")
  val DefaultTestResolver = ResolverClassName(apolloApiTestPackageName, "DefaultTestResolver")
  val MapJsonReader = ResolverClassName(apolloApiJsonPackageName, "MapJsonReader")
  val MapBuilder = ResolverClassName(apolloApiTestPackageName, "MapBuilder")
  val StubbedProperty = ResolverClassName(apolloApiTestPackageName, "StubbedProperty")
  val MandatoryTypenameProperty = ResolverClassName(apolloApiTestPackageName, "MandatoryTypenameProperty")

  val ApolloExperimental = ClassName(apolloAnnotationsPackageName, "ApolloExperimental")
}
