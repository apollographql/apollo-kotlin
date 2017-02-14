package com.apollographql.android.compiler

import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

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

fun FieldSpec.overrideType(typeNameOverrideMap: Map<String, String>): FieldSpec =
    FieldSpec.builder(type.overrideTypeName(typeNameOverrideMap).annotated(type.annotations), name)
        .addModifiers(*modifiers.toTypedArray())
        .addAnnotations(annotations)
        .initializer(initializer)
        .build()

fun MethodSpec.overrideReturnType(typeNameOverrideMap: Map<String, String>): MethodSpec =
    MethodSpec.methodBuilder(name)
        .returns(returnType.overrideTypeName(typeNameOverrideMap).annotated(returnType.annotations))
        .addModifiers(*modifiers.toTypedArray())
        .addCode(code)
        .build()

fun TypeSpec.withCreator(): TypeSpec {
  return toBuilder()
      .addType(TypeSpec.interfaceBuilder(Util.CREATOR_TYPE_NAME)
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
          .addMethod(MethodSpec
              .methodBuilder(Util.CREATOR_CREATE_METHOD_NAME)
              .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
              .addParameters(
                  methodSpecs.filter { !it.isConstructor }.map {
                    val paramType = it.returnType
                    val paramName = it.name
                    ParameterSpec.builder(paramType, paramName).build()
                  })
              .returns(ClassName.get("", name).annotated(listOf(Annotations.NONNULL)))
              .build())
          .build())
      .build()
}

fun TypeSpec.withCreatorImplementation(): TypeSpec {
  fun List<MethodSpec>.toParameterSpecs() =
      filter { !it.isConstructor }.map {
        val paramType = it.returnType
        val paramName = it.name
        ParameterSpec.builder(paramType, paramName).build()
      }

  fun createMethodCodeBlock(constructorClassName: String, fieldSpecs: List<FieldSpec>) =
      CodeBlock.builder()
          .add("return new \$L(", constructorClassName)
          .add(fieldSpecs
              .filter { !it.modifiers.contains(Modifier.STATIC) }
              .mapIndexed { i, fieldSpec -> CodeBlock.of("\$L\$L", if (i > 0) ", " else "", fieldSpec.name) }
              .fold(CodeBlock.builder(), CodeBlock.Builder::add)
              .build())
          .add(");\n")
          .build()

  fun creatorInitializer(constructorClassName: String, fieldSpecs: List<FieldSpec>) =
      TypeSpec.anonymousClassBuilder("")
          .superclass(ClassName.get("", Util.CREATOR_TYPE_NAME))
          .addMethod(MethodSpec
              .methodBuilder(Util.CREATOR_CREATE_METHOD_NAME)
              .addModifiers(Modifier.PUBLIC)
              .addAnnotation(Override::class.java)
              .addParameters(methodSpecs.toParameterSpecs())
              .returns(ClassName.get("", name).annotated(listOf(Annotations.NONNULL)))
              .addCode(createMethodCodeBlock(constructorClassName, fieldSpecs))
              .build())
          .build()

  return toBuilder()
      .addField(FieldSpec
          .builder(ClassName.get("", Util.CREATOR_TYPE_NAME), Util.CREATOR_TYPE_NAME.toUpperCase())
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
          .initializer("\$L", creatorInitializer(name, fieldSpecs))
          .build())
      .build()
}

fun TypeSpec.withFactory(exclude: List<String> = emptyList(), include: List<String> = emptyList()): TypeSpec {
  return toBuilder()
      .addType(TypeSpec.interfaceBuilder(Util.FACTORY_TYPE_NAME)
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
          .addMethod(
              MethodSpec.methodBuilder(Util.FACTORY_CREATOR_ACCESS_METHOD_NAME)
                  .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                  .returns(ClassName.get("", Util.CREATOR_TYPE_NAME).annotated(listOf(Annotations.NONNULL)))
                  .build())
          .addMethods(typeSpecs
              .map { it.name }
              .filter { it != Util.CREATOR_TYPE_NAME && !exclude.contains(it) }
              .plus(include.map(String::capitalize))
              .map {
                MethodSpec.methodBuilder("${it.decapitalize()}${Util.FACTORY_TYPE_NAME}")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(ClassName.get("", "$it.${Util.FACTORY_TYPE_NAME}").annotated(listOf(Annotations.NONNULL)))
                    .build()
              })
          .build())
      .build()
}

fun TypeSpec.withFactoryImplementation(exclude: List<String> = emptyList(),
    include: List<String> = emptyList()): TypeSpec {
  fun factoryInitializer(typeSpecs: List<TypeSpec>) =
      TypeSpec.anonymousClassBuilder("")
          .superclass(Util.FACTORY_INTERFACE_TYPE)
          .addMethod(MethodSpec
              .methodBuilder(Util.FACTORY_CREATOR_ACCESS_METHOD_NAME)
              .addModifiers(Modifier.PUBLIC)
              .addAnnotation(Override::class.java)
              .returns(ClassName.get("", Util.CREATOR_TYPE_NAME).annotated(listOf(Annotations.NONNULL)))
              .addStatement("return \$L", Util.CREATOR_TYPE_NAME.toUpperCase())
              .build())
          .addMethods(typeSpecs
              .map { it.name }
              .filter { it != Util.CREATOR_TYPE_NAME && !exclude.contains(it) }
              .plus(include.map(String::capitalize))
              .map {
                MethodSpec
                    .methodBuilder("${it.decapitalize()}${Util.FACTORY_TYPE_NAME}")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override::class.java)
                    .returns(ClassName.get("", "$it.${Util.FACTORY_TYPE_NAME}").annotated(listOf(Annotations.NONNULL)))
                    .addStatement("return \$L.\$L", it, Util.FACTORY_TYPE_NAME.toUpperCase())
                    .build()
              })
          .build()

  return toBuilder()
      .addField(FieldSpec
          .builder(Util.FACTORY_INTERFACE_TYPE, Util.FACTORY_TYPE_NAME.toUpperCase())
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
          .initializer("\$L", factoryInitializer(
              typeSpecs.filter { it.name != Util.CREATOR_TYPE_NAME && it.name != Util.FACTORY_TYPE_NAME }))
          .build())
      .build()
}

fun TypeSpec.withValueInitConstructor(): TypeSpec {
  return toBuilder()
      .addMethod(MethodSpec.constructorBuilder()
          .addModifiers(Modifier.PUBLIC)
          .addParameters(fieldSpecs
              .filter { !it.modifiers.contains(Modifier.STATIC) }
              .map { ParameterSpec.builder(it.type, it.name).build() })
          .addCode(fieldSpecs
              .filter { !it.modifiers.contains(Modifier.STATIC) }
              .map { CodeBlock.of("this.\$L = \$L;\n", it.name, it.name) }
              .fold(CodeBlock.builder(), CodeBlock.Builder::add)
              .build())
          .build())
      .build()
}

fun String.toJavaType(): ClassName =
    ClassName.get(substringBeforeLast(delimiter = ".", missingDelimiterValue = ""), substringAfterLast("."))

fun TypeSpec.withToStringImplementation(): TypeSpec {
  fun printFieldCode(fieldIndex: Int, fieldName: String) =
      CodeBlock.builder()
          .let { if (fieldIndex > 0) it.add(" + \", \"\n") else it.add("\n") }
          .indent()
          .add("+ \$S + \$L", "$fieldName=", fieldName)
          .unindent()
          .build()

  fun methodCode() =
      CodeBlock.builder()
          .add("return \$S", "$name{")
          .add(fieldSpecs
              .filter { !it.hasModifier(Modifier.STATIC) }
              .map { it.name }
              .mapIndexed(::printFieldCode)
              .fold(CodeBlock.builder(), CodeBlock.Builder::add)
              .build())
          .add(CodeBlock.builder()
              .indent()
              .add("\n+ \$S;\n", "}")
              .unindent()
              .build())
          .build()

  return toBuilder()
      .addMethod(MethodSpec.methodBuilder("toString")
          .addAnnotation(Override::class.java)
          .addModifiers(Modifier.PUBLIC)
          .returns(java.lang.String::class.java)
          .addCode(methodCode())
          .build())
      .build()
}

fun TypeSpec.withEqualsImplementation(): TypeSpec {
  fun equalsFieldCode(fieldIndex: Int, field: FieldSpec) =
      CodeBlock.builder()
          .let { if (fieldIndex > 0) it.add("\n && ") else it }
          .let {
            if (field.type.isPrimitive) {
              if (field.type == TypeName.DOUBLE) {
                it.add("Double.doubleToLongBits(this.\$L) == Double.doubleToLongBits(that.\$L)",
                    field.name, field.name)
              } else {
                it.add("this.\$L == that.\$L", field.name, field.name)
              }
            } else {
              it.add("((this.\$L == null) ? (that.\$L == null) : this.\$L.equals(that.\$L))", field.name,
                  field.name, field.name, field.name)
            }
          }
          .build()

  fun methodCode(typeJavaClass: ClassName) =
      CodeBlock.builder()
          .beginControlFlow("if (o == this)")
          .addStatement("return true")
          .endControlFlow()
          .beginControlFlow("if (o instanceof \$T)", typeJavaClass)
          .addStatement("\$T that = (\$T) o", typeJavaClass, typeJavaClass)
          .add("return ")
          .add(fieldSpecs
              .filter { !it.hasModifier(Modifier.STATIC) }
              .mapIndexed(::equalsFieldCode)
              .fold(CodeBlock.builder(), CodeBlock.Builder::add)
              .build())
          .add(";\n")
          .endControlFlow()
          .addStatement("return false")
          .build()

  return toBuilder()
      .addMethod(MethodSpec.methodBuilder("equals")
          .addAnnotation(Override::class.java)
          .addModifiers(Modifier.PUBLIC)
          .returns(TypeName.BOOLEAN)
          .addParameter(ParameterSpec.builder(TypeName.OBJECT, "o").build())
          .addCode(methodCode(ClassName.get("", name)))
          .build())
      .build()
}

fun TypeSpec.withHashCodeImplementation(): TypeSpec {
  fun hashCodeFieldCode(field: FieldSpec) =
      CodeBlock.builder()
          .addStatement("h *= 1000003")
          .let {
            if (field.type.isPrimitive) {
              if (field.type == TypeName.DOUBLE) {
                it.addStatement("h ^= Double.valueOf(\$L).hashCode()", field.name)
              } else if (field.type == TypeName.BOOLEAN) {
                it.addStatement("h ^= Boolean.valueOf(\$L).hashCode()", field.name)
              } else {
                it.addStatement("h ^= \$L", field.name)
              }
            } else {
              it.addStatement("h ^= (\$L == null) ? 0 : \$L.hashCode()", field.name, field.name)
            }
          }
          .build()

  fun methodCode() =
      CodeBlock.builder()
          .addStatement("int h = 1")
          .add(fieldSpecs
              .filter { !it.hasModifier(Modifier.STATIC) }
              .map(::hashCodeFieldCode)
              .fold(CodeBlock.builder(), CodeBlock.Builder::add)
              .build())
          .addStatement("return h")
          .build()

  return toBuilder()
      .addMethod(MethodSpec.methodBuilder("hashCode")
          .addAnnotation(Override::class.java)
          .addModifiers(Modifier.PUBLIC)
          .returns(TypeName.INT)
          .addCode(methodCode())
          .build())
      .build()
}

object Util {
  const val CREATOR_TYPE_NAME: String = "Creator"
  const val CREATOR_CREATE_METHOD_NAME: String = "create"
  const val FACTORY_CREATOR_ACCESS_METHOD_NAME: String = "creator"
  const val FACTORY_TYPE_NAME: String = "Factory"
  val FACTORY_INTERFACE_TYPE: ClassName = ClassName.get("", FACTORY_TYPE_NAME)
}