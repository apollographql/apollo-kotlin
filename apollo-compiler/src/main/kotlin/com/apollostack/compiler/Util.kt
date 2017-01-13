package com.apollostack.compiler

import com.squareup.javapoet.*

fun String.normalizeTypeName() = removeSuffix("!").removeSurrounding("[", "]").removeSuffix("!")

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

fun buildTypeNameOverrideMap(reservedTypeNames: List<String>): Map<String, String> {
  fun String.formatUniqueTypeName(reservedTypeNames: List<String>): String {
    val suffix = reservedTypeNames.count { it == this }.let { if (it > 0) "$".repeat(it) else "" }
    return "$this$suffix"
  }
  return reservedTypeNames.distinct().associate {
    it to it.formatUniqueTypeName(reservedTypeNames)
  }
}

fun MethodSpec.overrideReturnType(typeNameOverrideMap: Map<String, String>): MethodSpec = MethodSpec
      .methodBuilder(name)
      .returns(returnType.overrideTypeName(typeNameOverrideMap).annotated(returnType.annotations))
      .addModifiers(*modifiers.toTypedArray())
      .addCode(code)
      .build()

fun TypeSpec.overrideName(typeNameOverrideMap: Map<String, String>): TypeSpec {
  val typeSpecBuilder = if (this.kind == TypeSpec.Kind.INTERFACE) {
    TypeSpec.interfaceBuilder(typeNameOverrideMap[name] ?: name)
  } else {
    TypeSpec.classBuilder(typeNameOverrideMap[name] ?: name)
  }
  return typeSpecBuilder
      .addModifiers(*modifiers.toTypedArray())
      .addMethods(methodSpecs.filter { it.isConstructor })
      .addMethods(methodSpecs.filter { !it.isConstructor }.map { it.overrideReturnType(typeNameOverrideMap) })
      .addTypes(typeSpecs)
      .addFields(fieldSpecs.map { it.overrideType(typeNameOverrideMap) })
      .addSuperinterfaces(superinterfaces)
      .build()
}