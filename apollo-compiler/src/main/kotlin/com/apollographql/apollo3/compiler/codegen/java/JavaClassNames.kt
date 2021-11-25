package com.apollographql.apollo3.compiler.codegen.java

import com.apollographql.apollo3.compiler.codegen.ClassNames
import com.apollographql.apollo3.compiler.codegen.ClassNames.apolloApiPackageName
import com.apollographql.apollo3.compiler.codegen.ResolverClassName
import com.squareup.javapoet.ClassName

/**
 * A list of constant [ResolverClassName] that don't use `class.name` and therefore survive proguard/R8
 */
internal object JavaClassNames {
  val ObjectType = ClassNames.ObjectType.toJavaPoetClassName()
  val InterfaceType = ClassNames.InterfaceType.toJavaPoetClassName()

  val JsonReader = ClassNames.JsonReader.toJavaPoetClassName()
  val JsonWriter = ClassNames.JsonWriter.toJavaPoetClassName()
  val CustomScalarAdapters = ClassNames.CustomScalarAdapters.toJavaPoetClassName()
  val Optional = ClassNames.Optional.toJavaPoetClassName()
  val Absent = ClassNames.Absent.toJavaPoetClassName()
  val Present = ClassNames.Present.toJavaPoetClassName()
  val Adapter = ClassNames.Adapter.toJavaPoetClassName()
  val CompiledSelection = ClassNames.CompiledSelection.toJavaPoetClassName()
  val CompiledType = ClassNames.CompiledType.toJavaPoetClassName()
  val CompiledNamedType = ClassNames.CompiledNamedType.toJavaPoetClassName()
  val UnionType = ClassNames.UnionType.toJavaPoetClassName()
  val Fragment = ClassNames.Fragment.toJavaPoetClassName()
  val FragmentData = ClassNames.FragmentData.toJavaPoetClassName()
  val Query = ClassNames.Query.toJavaPoetClassName()
  val Mutation = ClassNames.Mutation.toJavaPoetClassName()
  val Subscription = ClassNames.Subscription.toJavaPoetClassName()
  val QueryData = ClassNames.QueryData.toJavaPoetClassName()
  val MutationData = ClassNames.MutationData.toJavaPoetClassName()
  val SubscriptionData = ClassNames.SubscriptionData.toJavaPoetClassName()
  val EnumType = ClassNames.EnumType.toJavaPoetClassName()
  val CustomScalarType = ClassNames.CustomScalarType.toJavaPoetClassName()
  val True = ClassNames.True.toJavaPoetClassName()
  val False = ClassNames.False.toJavaPoetClassName()
  val CompiledArgument = ClassNames.CompiledArgument.toJavaPoetClassName()
  val CompiledVariable = ClassNames.CompiledVariable.toJavaPoetClassName()
  val CompiledCondition = ClassNames.CompiledCondition.toJavaPoetClassName()
  val CompiledField = ClassNames.CompiledField.toJavaPoetClassName()
  val CompiledFieldBuilder = ClassNames.CompiledFieldBuilder.toJavaPoetClassName()
  val CompiledFragment = ClassNames.CompiledFragment.toJavaPoetClassName()
  val CompiledFragmentBuilder = ClassNames.CompiledFragmentBuilder.toJavaPoetClassName()

  /**
   * ClassNames that we don't use in Kotlin because we use extension functions instead
   */
  val CompiledNotNullType = ClassName.get(apolloApiPackageName, "CompiledNotNullType")
  val CompiledListType = ClassName.get(apolloApiPackageName, "CompiledListType")
  val ObjectAdapter  = ClassName.get(apolloApiPackageName, "ObjectAdapter")
  val And = ClassName.get(apolloApiPackageName, "BooleanExpression", "And")
  val Or = ClassName.get(apolloApiPackageName, "BooleanExpression", "Or")
  val Not = ClassName.get(apolloApiPackageName, "BooleanExpression", "Not")
  val BooleanExpressionElement = ClassName.get(apolloApiPackageName, "BooleanExpression", "Element")
  val BTerm = ClassName.get(apolloApiPackageName, "BTerm")
  val BVariable = ClassName.get(apolloApiPackageName, "BVariable")
  val BPossibleTypes = ClassName.get(apolloApiPackageName, "BPossibleTypes")
  val ImmutableMapBuilder = ClassName.get(apolloApiPackageName, "ImmutableMapBuilder")
  val NullableAdapter = ClassName.get(apolloApiPackageName, "NullableAdapter")
  val ListAdapter = ClassName.get(apolloApiPackageName, "ListAdapter")
  val OptionalAdapter = ClassName.get(apolloApiPackageName, "OptionalAdapter")

  val IOException = ClassName.get("java.io", "IOException")

  /**
   * ClassNames for kotlin files turned into java classes
   */
  val Adapters = ClassName.get(apolloApiPackageName, "Adapters")
  val CompiledGraphQL = ClassName.get(apolloApiPackageName, "CompiledGraphQL")
  val BooleanExpressions = ClassName.get(apolloApiPackageName, "BooleanExpressions")
  val Assertions = ClassName.get(apolloApiPackageName, "Assertions")
  val PossibleTypes = ClassName.get(apolloApiPackageName, "PossibleTypes")

  /**
   * ClassNames for builtin Java types
   */
  val String: ClassName = ClassName.get("java.lang", "String")
  val Integer: ClassName = ClassName.get("java.lang", "Integer")
  val Double: ClassName = ClassName.get("java.lang", "Double")
  val Object: ClassName = ClassName.get("java.lang", "Object")
  val Boolean: ClassName = ClassName.get("java.lang", "Boolean")
  val Deprecated = ClassName.get("java.lang", "Deprecated")
  val Override = ClassName.get("java.lang", "Override")

  val List: ClassName = ClassName.get("java.util", "List")
  val Arrays = ClassName.get("java.util", "Arrays")
  val Collections = ClassName.get("java.util", "Collections")
  val IllegalStateException = ClassName.get("java.lang", "IllegalStateException")
}