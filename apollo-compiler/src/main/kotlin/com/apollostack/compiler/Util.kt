package com.apollostack.compiler

import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

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

fun MethodSpec.overrideMethodReturnType(typeNameOverrideMap: Map<String, String>): MethodSpec =
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
      .addFields(fieldSpecs)
      .addSuperinterfaces(superinterfaces)
      .build()
}

fun TypeSpec.convertToPOJO(vararg modifiers: Modifier): TypeSpec {
  if (kind != TypeSpec.Kind.INTERFACE) {
    throw IllegalArgumentException("can't convert non-interface class to POJO")
  }
  return TypeSpec.classBuilder(name)
      .addModifiers(*modifiers)
      .addSuperinterfaces(this.superinterfaces)
      .addFields(methodSpecs.map {
        FieldSpec
            .builder(it.returnType, it.name)
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .build()
      })
      .addMethod(MethodSpec
          .constructorBuilder()
          .addModifiers(Modifier.PUBLIC)
          .addParameters(methodSpecs.map { ParameterSpec.builder(it.returnType, it.name).build() })
          .addCode(methodSpecs.map { CodeBlock.of("this.\$L = \$L;\n", it.name, it.name) }
              .fold(CodeBlock.builder(), CodeBlock.Builder::add).build())
          .build())
      .addMethods(methodSpecs.map {
        MethodSpec
            .methodBuilder(it.name)
            .returns(it.returnType)
            .addModifiers(Modifier.PUBLIC)
            .addStatement("return this.\$L", it.name)
            .build()
      })
      .addFields(fieldSpecs)
      .addTypes(typeSpecs.map { it.convertToPOJO(Modifier.PUBLIC, Modifier.STATIC) })
      .build()
}
