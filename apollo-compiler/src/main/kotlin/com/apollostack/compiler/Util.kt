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

fun TypeSpec.resolveNestedTypeNameDuplication(usedTypeNames: List<String>): TypeSpec {
  val typeNameOverrideMap = typeSpecs.map {
    val typeName = it.name
    val typeNameSuffix = usedTypeNames.count { it == typeName }.let { if (it > 0) "$".repeat(it) else "" }
    typeName to "$typeName$typeNameSuffix"
  }.toMap()

  val typeNameSuffix = usedTypeNames.count { it == name }.let { if (it > 0) "$".repeat(it) else "" }
  return TypeSpec.interfaceBuilder("$name$typeNameSuffix")
      .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
      .addMethods(methodSpecs.map { it.overrideMethodReturnType(typeNameOverrideMap) })
      .addTypes(typeSpecs.map {
        val currentNestedType = it
        it.resolveNestedTypeNameDuplication(
            usedTypeNames + typeSpecs.filter { it != currentNestedType }.map { it.name } + it.name
        )
      })
      .build()
}

object JavaPoetUtils {
  val NULLABLE_ANNOTATION = AnnotationSpec.builder(Nullable::class.java).build()
}
