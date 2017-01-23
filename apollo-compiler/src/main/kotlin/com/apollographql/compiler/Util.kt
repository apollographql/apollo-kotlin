package com.apollographql.compiler

import com.squareup.javapoet.*

fun TypeName.overrideTypeName(typeNameOverrideMap: Map<String, String>): TypeName {
  if (this is ParameterizedTypeName) {
    val typeArguments = typeArguments.map { it.overrideTypeName(typeNameOverrideMap) }.toTypedArray()
    return ParameterizedTypeName.get(rawType, *typeArguments)
  } else if (this is ClassName) {
    return ClassName.get(packageName(), typeNameOverrideMap[simpleName()] ?: simpleName())
  } else if (this is WildcardTypeName) {
    return WildcardTypeName.subtypeOf(upperBounds[0].overrideTypeName(typeNameOverrideMap))
  } else {
    return this
  }
}

fun FieldSpec.overrideType(typeNameOverrideMap: Map<String, String>): FieldSpec = FieldSpec
    .builder(type.overrideTypeName(typeNameOverrideMap).annotated(type.annotations), name)
    .addModifiers(*modifiers.toTypedArray())
    .addAnnotations(annotations)
    .initializer(initializer)
    .build()

fun MethodSpec.overrideReturnType(typeNameOverrideMap: Map<String, String>): MethodSpec = MethodSpec
    .methodBuilder(name)
    .returns(returnType.overrideTypeName(typeNameOverrideMap).annotated(returnType.annotations))
    .addModifiers(*modifiers.toTypedArray())
    .addCode(code)
    .build()