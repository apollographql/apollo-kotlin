package com.apollographql.apollo3.compiler.codegen.kotlin

import com.apollographql.apollo3.compiler.codegen.ClassNames
import com.apollographql.apollo3.compiler.codegen.ClassNames.apolloApiTestPackageName
import com.apollographql.apollo3.compiler.codegen.ResolverClassName
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName

/**
 * A list of constant [ResolverClassName] that don't use `class.name` and therefore survive proguard/R8
 */
internal object KotlinClassNames {
  val ObjectType = ClassNames.ObjectType.toKotlinPoetClassName()
  val InterfaceType = ClassNames.InterfaceType.toKotlinPoetClassName()
  val BooleanAdapter = ClassNames.BooleanAdapter.toKotlinPoetClassName()
  val StringAdapter = ClassNames.StringAdapter.toKotlinPoetClassName()
  val IntAdapter = ClassNames.IntAdapter.toKotlinPoetClassName()
  val DoubleAdapter = ClassNames.DoubleAdapter.toKotlinPoetClassName()
  val AnyAdapter = ClassNames.AnyAdapter.toKotlinPoetClassName()
  val JsonReader = ClassNames.JsonReader.toKotlinPoetClassName()
  val JsonWriter = ClassNames.JsonWriter.toKotlinPoetClassName()
  val CustomScalarAdapters = ClassNames.CustomScalarAdapters.toKotlinPoetClassName()
  val Optional = ClassNames.Optional.toKotlinPoetClassName()
  val Absent = ClassNames.Absent.toKotlinPoetClassName()
  val Present = ClassNames.Present.toKotlinPoetClassName()
  val Adapter = ClassNames.Adapter.toKotlinPoetClassName()
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
  val CustomScalarType = ClassNames.CustomScalarType.toKotlinPoetClassName()
  val True = ClassNames.True.toKotlinPoetClassName()
  val False = ClassNames.False.toKotlinPoetClassName()
  val CompiledArgument = ClassNames.CompiledArgument.toKotlinPoetClassName()
  val CompiledVariable = ClassNames.CompiledVariable.toKotlinPoetClassName()
  val CompiledCondition = ClassNames.CompiledCondition.toKotlinPoetClassName()
  val CompiledField = ClassNames.CompiledField.toKotlinPoetClassName()
  val CompiledFieldBuilder = ClassNames.CompiledFieldBuilder.toKotlinPoetClassName()
  val CompiledFragment = ClassNames.CompiledFragment.toKotlinPoetClassName()
  val TestResolver = ClassNames.TestResolver.toKotlinPoetClassName()
  val DefaultTestResolver = ClassNames.DefaultTestResolver.toKotlinPoetClassName()
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
}

fun ResolverClassName.toKotlinPoetClassName(): ClassName = ClassName(packageName, simpleNames)

object KotlinMemberNames {
  val withTestResolver = MemberName(apolloApiTestPackageName, "withTestResolver")
}