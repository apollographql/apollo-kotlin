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

fun TypeSpec.resolveNestedTypeNameDuplication(reservedTypeNames: List<String>): TypeSpec {
  fun FieldSpec.overrideType(typeNameOverrideMap: Map<String, String>):FieldSpec = FieldSpec
      .builder(type.overrideTypeName(typeNameOverrideMap).annotated(type.annotations), name)
      .addModifiers(*modifiers.toTypedArray())
      .addAnnotations(annotations)
      .initializer(initializer)
      .build()

  fun MethodSpec.overrideMethodReturnType(typeNameOverrideMap: Map<String, String>): MethodSpec = MethodSpec
      .methodBuilder(name)
      .returns(returnType.overrideTypeName(typeNameOverrideMap).annotated(returnType.annotations))
      .addModifiers(*modifiers.toTypedArray())
      .addCode(code)
      .build()

  fun String.formatUniqueTypeName(reservedTypeNames: List<String>): String {
    val suffix = reservedTypeNames.count { it == this }.let { if (it > 0) "$".repeat(it) else "" }
    return "$this$suffix"
  }

  val typeNameOverrideMap = typeSpecs.map { it.name }
      .map { it to it.formatUniqueTypeName(reservedTypeNames) }.toMap()

  val typeSpecName = name.formatUniqueTypeName(reservedTypeNames.minusElement(name))

  val typeSpecBuilder = if (this.kind == TypeSpec.Kind.INTERFACE) {
    TypeSpec.interfaceBuilder(typeSpecName)
  } else {
    TypeSpec.classBuilder(typeSpecName)
  }
  return typeSpecBuilder
      .addModifiers(*modifiers.toTypedArray())
      .addMethods(methodSpecs.map { it.overrideMethodReturnType(typeNameOverrideMap) })
      .addTypes(typeSpecs.map { typeSpec ->
        typeSpec.resolveNestedTypeNameDuplication(reservedTypeNames + typeSpecs.map { it.name })
      })
      .addFields(fieldSpecs.map { it.overrideType(typeNameOverrideMap) })
      .addSuperinterfaces(superinterfaces)
      .build()
}
