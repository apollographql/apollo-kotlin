package com.apollographql.apollo3.compiler.codegen.kotlin

import com.apollographql.apollo3.compiler.codegen.ClassNames
import com.apollographql.apollo3.compiler.codegen.ClassNames.apolloApiJsonPackageName
import com.apollographql.apollo3.compiler.codegen.ClassNames.apolloApiPackageName
import com.apollographql.apollo3.compiler.codegen.ClassNames.apolloApiTestPackageName
import com.apollographql.apollo3.compiler.codegen.ResolverClassName
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

/**
 * A list of constant symbols from apollo-api referenced from codegen.
 * We don't use reflection so that we can use R8 on the compiler.
 *
 * Symbols can be [ClassName] or [MemberName]
 */
internal object KotlinSymbols {
  val ObjectType = ClassNames.ObjectType.toKotlinPoetClassName()
  val ObjectTypeBuilder = ClassNames.ObjectTypeBuilder.toKotlinPoetClassName()
  val InterfaceType = ClassNames.InterfaceType.toKotlinPoetClassName()
  val InterfaceTypeBuilder = ClassNames.InterfaceTypeBuilder.toKotlinPoetClassName()
  val ObjectBuilder = ClassNames.ObjectBuilder.toKotlinPoetClassName()
  val BuilderProperty = ClassNames.BuilderProperty.toKotlinPoetClassName()
  val FakeResolver = ClassNames.FakeResolver.toKotlinPoetClassName()
  val JsonReader = ClassNames.JsonReader.toKotlinPoetClassName()
  val JsonWriter = ClassNames.JsonWriter.toKotlinPoetClassName()
  val ScalarAdapters = ClassNames.ScalarAdapters.toKotlinPoetClassName()
  val DataSerializeContext = ClassNames.DataSerializeContext.toKotlinPoetClassName()
  val DataDeserializeContext = ClassNames.DataDeserializeContext.toKotlinPoetClassName()
  val ScalarAdaptersBuilder = ClassNames.ScalarAdaptersBuilder.toKotlinPoetClassName()
  val Optional = ClassNames.Optional.toKotlinPoetClassName()
  val Absent = ClassNames.Absent.toKotlinPoetClassName()
  val Present = ClassNames.Present.toKotlinPoetClassName()
  val ApolloAdapter = ClassNames.ApolloAdapter.toKotlinPoetClassName()
  val VariablesAdapter = ClassNames.VariablesAdapter.toKotlinPoetClassName()
  val SerializeVariablesContext = ClassNames.SerializeVariablesContext.toKotlinPoetClassName()
  val ScalarAdapterToApolloAdapter = ClassNames.ScalarAdapterToApolloAdapter.toKotlinPoetClassName()
  val CompiledSelection = ClassNames.CompiledSelection.toKotlinPoetClassName()
  val CompiledType = ClassNames.CompiledType.toKotlinPoetClassName()
  val CompiledNamedType = ClassNames.CompiledNamedType.toKotlinPoetClassName()
  val UnionType = ClassNames.UnionType.toKotlinPoetClassName()
  val Fragment = ClassNames.Fragment.toKotlinPoetClassName()
  val FragmentData = ClassNames.FragmentData.toKotlinPoetClassName()
  val Query = ClassNames.Query.toKotlinPoetClassName()
  val Mutation = ClassNames.Mutation.toKotlinPoetClassName()
  val Subscription = ClassNames.Subscription.toKotlinPoetClassName()
  val QueryData = ClassNames.QueryData.toKotlinPoetClassName()
  val MutationData = ClassNames.MutationData.toKotlinPoetClassName()
  val SubscriptionData = ClassNames.SubscriptionData.toKotlinPoetClassName()
  val EnumType = ClassNames.EnumType.toKotlinPoetClassName()
  val ScalarType = ClassNames.ScalarType.toKotlinPoetClassName()
  val True = ClassNames.True.toKotlinPoetClassName()
  val False = ClassNames.False.toKotlinPoetClassName()
  val CompiledArgument = ClassNames.CompiledArgument.toKotlinPoetClassName()
  val CompiledVariable = ClassNames.CompiledVariable.toKotlinPoetClassName()
  val CompiledCondition = ClassNames.CompiledCondition.toKotlinPoetClassName()
  val CompiledField = ClassNames.CompiledField.toKotlinPoetClassName()
  val CompiledFieldBuilder = ClassNames.CompiledFieldBuilder.toKotlinPoetClassName()
  val CompiledFragment = ClassNames.CompiledFragment.toKotlinPoetClassName()
  val CompiledFragmentBuilder = ClassNames.CompiledFragmentBuilder.toKotlinPoetClassName()
  val TestResolver = ClassNames.TestResolver.toKotlinPoetClassName()
  val DefaultTestResolver = ClassNames.DefaultTestResolver.toKotlinPoetClassName()
  val DefaultFakeResolver = ClassNames.DefaultFakeResolver.toKotlinPoetClassName()
  val MapJsonReader = ClassNames.MapJsonReader.toKotlinPoetClassName()
  val MapBuilder = ClassNames.MapBuilder.toKotlinPoetClassName()
  val StubbedProperty = ClassNames.StubbedProperty.toKotlinPoetClassName()
  val MandatoryTypenameProperty = ClassNames.MandatoryTypenameProperty.toKotlinPoetClassName()

  /**
   * Kotlin class names
   */
  val Boolean = ClassName("kotlin", "Boolean")
  val Int = ClassName("kotlin", "Int")
  val String = ClassName("kotlin", "String")
  val Double = ClassName("kotlin", "Double")
  val Any = ClassName("kotlin", "Any")
  val Deprecated = ClassName("kotlin", "Deprecated")
  val Unit = ClassName("kotlin", "Unit")

  val List = ClassName("kotlin.collections", "List")
  val Map = ClassName("kotlin.collections", "Map")
  val Array = ClassName("kotlin", "Array")
  val Set = ClassName("kotlin.collections", "Set")

  val MapOfStringToNullableAny = Map.parameterizedBy(String, Any.copy(nullable = true))

  val Suppress = ClassName("kotlin", "Suppress")
  val OptIn = ClassName("kotlin", "OptIn")
  val JvmOverloads = ClassName("kotlin.jvm", "JvmOverloads")

  /**
   * Adapters
   */
  val AnyApolloAdapter = MemberName(apolloApiPackageName, "AnyApolloAdapter")
  val BooleanApolloAdapter = MemberName(apolloApiPackageName, "BooleanApolloAdapter")
  val DoubleApolloAdapter = MemberName(apolloApiPackageName, "DoubleApolloAdapter")
  val IntApolloAdapter = MemberName(apolloApiPackageName, "IntApolloAdapter")
  val StringApolloAdapter = MemberName(apolloApiPackageName, "StringApolloAdapter")
  val NullableAnyApolloAdapter = MemberName(apolloApiPackageName, "NullableAnyApolloAdapter")
  val NullableBooleanApolloAdapter = MemberName(apolloApiPackageName, "NullableBooleanApolloAdapter")
  val NullableDoubleApolloAdapter = MemberName(apolloApiPackageName, "NullableDoubleApolloAdapter")
  val NullableIntApolloAdapter = MemberName(apolloApiPackageName, "NullableIntApolloAdapter")
  val NullableStringApolloAdapter = MemberName(apolloApiPackageName, "NullableStringApolloAdapter")


  val BuilderScope = ClassName(apolloApiPackageName, "BuilderScope")
  val BuilderFactory = ClassName(apolloApiPackageName, "BuilderFactory")

  val ApolloAdaptableWith = ClassName(ClassNames.apolloAnnotationsPackageName, "ApolloAdaptableWith")
  val ApolloExperimental = ClassName(ClassNames.apolloAnnotationsPackageName, "ApolloExperimental")

}

internal fun ResolverClassName.toKotlinPoetClassName(): ClassName = ClassName(packageName, simpleNames)

internal object KotlinMemberNames {
  val withTestResolver = MemberName(apolloApiTestPackageName, "withTestResolver")
  val obj = MemberName(apolloApiPackageName, "obj")
  val readTypename = MemberName(apolloApiJsonPackageName, "readTypename")
  val buildData = MemberName(apolloApiPackageName, "buildData")
  val GlobalBuilder = MemberName(apolloApiPackageName, "GlobalBuilder")
}
