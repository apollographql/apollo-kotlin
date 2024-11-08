package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.compiler.codegen.ClassNames
import com.apollographql.apollo.compiler.codegen.ClassNames.apolloApiJsonPackageName
import com.apollographql.apollo.compiler.codegen.ClassNames.apolloApiPackageName
import com.apollographql.apollo.compiler.codegen.ResolverClassName
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
  val Schema = ClassName(ClassNames.apolloAstPackageName, "Schema")
  val ObjectType = ClassNames.ObjectType.toKotlinPoetClassName()
  val ObjectTypeBuilder = ClassNames.ObjectTypeBuilder.toKotlinPoetClassName()
  val InterfaceType = ClassNames.InterfaceType.toKotlinPoetClassName()
  val InterfaceTypeBuilder = ClassNames.InterfaceTypeBuilder.toKotlinPoetClassName()
  val ObjectBuilder = ClassNames.ObjectBuilder.toKotlinPoetClassName()
  val BuilderProperty = ClassNames.BuilderProperty.toKotlinPoetClassName()
  val FakeResolver = ClassNames.FakeResolver.toKotlinPoetClassName()
  val JsonReader = ClassNames.JsonReader.toKotlinPoetClassName()
  val JsonWriter = ClassNames.JsonWriter.toKotlinPoetClassName()
  val CustomScalarAdapters = ClassNames.CustomScalarAdapters.toKotlinPoetClassName()
  val CustomScalarAdaptersBuilder = ClassNames.CustomScalarAdaptersBuilder.toKotlinPoetClassName()
  val Optional = ClassNames.Optional.toKotlinPoetClassName()
  val Absent = ClassNames.Absent.toKotlinPoetClassName()
  val Present = ClassNames.Present.toKotlinPoetClassName()
  val Adapter = ClassNames.Adapter.toKotlinPoetClassName()
  val CompiledSelection = ClassNames.CompiledSelection.toKotlinPoetClassName()
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
  val CustomScalarType = ClassNames.CustomScalarType.toKotlinPoetClassName()
  val True = ClassNames.True.toKotlinPoetClassName()
  val False = ClassNames.False.toKotlinPoetClassName()
  val CompiledArgument = ClassNames.CompiledArgument.toKotlinPoetClassName()
  val CompiledArgumentDefinition = ClassNames.CompiledArgumentDefinition.toKotlinPoetClassName()
  val CompiledArgumentDefinitionBuilder = ClassNames.CompiledArgumentDefinitionBuilder.toKotlinPoetClassName()
  val CompiledVariable = ClassNames.CompiledVariable.toKotlinPoetClassName()
  val JsonNumber = ClassNames.JsonNumber.toKotlinPoetClassName()
  val CompiledCondition = ClassNames.CompiledCondition.toKotlinPoetClassName()
  val CompiledField = ClassNames.CompiledField.toKotlinPoetClassName()
  val CompiledFieldBuilder = ClassNames.CompiledFieldBuilder.toKotlinPoetClassName()
  val CompiledFragment = ClassNames.CompiledFragment.toKotlinPoetClassName()
  val CompiledFragmentBuilder = ClassNames.CompiledFragmentBuilder.toKotlinPoetClassName()
  val DefaultFakeResolver = ClassNames.DefaultFakeResolver.toKotlinPoetClassName()
  val Builder = ClassName("", "Builder")

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
  val AnyAdapter = MemberName(apolloApiPackageName, "AnyAdapter")
  val BooleanAdapter = MemberName(apolloApiPackageName, "BooleanAdapter")
  val DoubleAdapter = MemberName(apolloApiPackageName, "DoubleAdapter")
  val IntAdapter = MemberName(apolloApiPackageName, "IntAdapter")
  val StringAdapter = MemberName(apolloApiPackageName, "StringAdapter")
  val NullableAnyAdapter = MemberName(apolloApiPackageName, "NullableAnyAdapter")
  val NullableBooleanAdapter = MemberName(apolloApiPackageName, "NullableBooleanAdapter")
  val NullableDoubleAdapter = MemberName(apolloApiPackageName, "NullableDoubleAdapter")
  val NullableIntAdapter = MemberName(apolloApiPackageName, "NullableIntAdapter")
  val NullableStringAdapter = MemberName(apolloApiPackageName, "NullableStringAdapter")


  val BuilderScope = ClassName(apolloApiPackageName, "BuilderScope")
  val BuilderFactory = ClassName(apolloApiPackageName, "BuilderFactory")

  val ApolloAdaptableWith = ClassName(ClassNames.apolloAnnotationsPackageName, "ApolloAdaptableWith")
  val ApolloExperimental = ClassName(ClassNames.apolloAnnotationsPackageName, "ApolloExperimental")
  val ApolloPrivateEnumConstructor = ClassName(ClassNames.apolloAnnotationsPackageName, "ApolloPrivateEnumConstructor")

  val JsExport = ClassName("kotlin.js", "JsExport")

  val obj = MemberName(apolloApiPackageName, "obj")
  val catchToResult = MemberName(apolloApiPackageName, "catchToResult")
  val catchToNull = MemberName(apolloApiPackageName, "catchToNull")
  val errorAware = MemberName(apolloApiPackageName, "errorAware")
  val readTypename = MemberName(apolloApiJsonPackageName, "readTypename")
  val buildData = MemberName(apolloApiPackageName, "buildData")
  val assertOneOf = MemberName(apolloApiPackageName, "assertOneOf")
  val missingField = MemberName(apolloApiPackageName, "missingField")
  val FieldResult = ClassNames.FieldResult.toKotlinPoetClassName()
}

internal fun ResolverClassName.toKotlinPoetClassName(): ClassName = ClassName(packageName, simpleNames)
