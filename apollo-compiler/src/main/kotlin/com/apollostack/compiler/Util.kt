package com.apollostack.compiler

import com.squareup.javapoet.*
import javax.annotation.Nullable
import javax.lang.model.element.Modifier

fun String.normalizeTypeName() = removeSuffix("!").removeSurrounding("[", "]").removeSuffix("!")

fun TypeName.overrideTypeName(typeNameOverrideMap: Map<String, String>): TypeName {
  if (this is ParameterizedTypeName) {
    val typeArguments = typeArguments.map { it.overrideTypeName(typeNameOverrideMap) }.toTypedArray()
    return ParameterizedTypeName.get(rawType, *typeArguments)
  } else if (this is ClassName) {
    return ClassName.get(packageName(), typeNameOverrideMap[simpleName()] ?: simpleName())
  } else {
    return this
  }
}

fun MethodSpec.overrideMethodReturnType(typeNameOverrideMap: Map<String, String>) =
    MethodSpec.methodBuilder(name)
        .returns(returnType.overrideTypeName(typeNameOverrideMap).annotated(returnType.annotations))
        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
        .build()

fun TypeSpec.resolveNestedTypeNameDuplication(reservedTypeNames: List<String>): TypeSpec {
  fun String.formatUniqueTypeName(reservedTypeNames: List<String>): String {
    val suffix = reservedTypeNames.count { it == this }.let { if (it > 0) "$".repeat(it) else "" }
    return "$this$suffix"
  }

  val typeNameOverrideMap = typeSpecs.map { it.name }
      .map { it to it.formatUniqueTypeName(reservedTypeNames) }.toMap()

  val typeSpecName = name.formatUniqueTypeName(reservedTypeNames.minusElement(name))
  return TypeSpec.interfaceBuilder(typeSpecName)
      .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
      .addMethods(methodSpecs.map { it.overrideMethodReturnType(typeNameOverrideMap) })
      .addTypes(typeSpecs.map { typeSpec ->
        typeSpec.resolveNestedTypeNameDuplication(reservedTypeNames + typeSpecs.map { it.name })
      })
      .build()
}

object JavaPoetUtils {
  val NULLABLE_ANNOTATION: AnnotationSpec = AnnotationSpec.builder(Nullable::class.java).build()
}
