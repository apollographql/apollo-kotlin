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
      .addType(TypeSpec.interfaceBuilder(Util.CREATOR_INTERFACE_NAME)
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
          .addMethod(MethodSpec
              .methodBuilder(Util.CREATOR_METHOD_CREATE)
              .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
              .addParameters(
                  methodSpecs.filter { !it.isConstructor }.map {
                    val paramType = it.returnType
                    val paramName = it.name
                    ParameterSpec.builder(paramType, paramName).build()
                  })
              .returns(ClassName.get("", name))
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
          .superclass(ClassName.get("", Util.CREATOR_INTERFACE_NAME))
          .addMethod(MethodSpec
              .methodBuilder(Util.CREATOR_METHOD_CREATE)
              .addModifiers(Modifier.PUBLIC)
              .addAnnotation(Override::class.java)
              .addParameters(methodSpecs.toParameterSpecs())
              .returns(ClassName.get("", name))
              .addCode(createMethodCodeBlock(constructorClassName, fieldSpecs))
              .build())
          .build()

  return toBuilder()
      .addField(FieldSpec
          .builder(ClassName.get("", Util.CREATOR_INTERFACE_NAME), Util.CREATOR_INTERFACE_NAME.toUpperCase())
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
          .initializer("\$L", creatorInitializer(name, fieldSpecs))
          .build())
      .build()
}

fun TypeSpec.withFactory(): TypeSpec {
  return toBuilder()
      .addType(TypeSpec.interfaceBuilder(Util.FACTORY_INTERFACE_NAME)
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
          .addMethod(MethodSpec
              .methodBuilder(Util.CREATOR_INTERFACE_NAME.decapitalize())
              .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
              .returns(ClassName.get("", Util.CREATOR_INTERFACE_NAME))
              .build())
          .addMethods(typeSpecs
              .filter { it.name != Util.CREATOR_INTERFACE_NAME }
              .map {
                MethodSpec
                    .methodBuilder("${it.name.decapitalize()}${Util.FACTORY_INTERFACE_NAME}")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .returns(ClassName.get("", "${it.name}.${Util.FACTORY_INTERFACE_NAME}"))
                    .build()
              })
          .build())
      .build()
}

fun TypeSpec.withFactoryImplementation(): TypeSpec {
  fun factoryInitializer(typeSpecs: List<TypeSpec>) =
      TypeSpec.anonymousClassBuilder("")
          .superclass(ClassName.get("", Util.FACTORY_INTERFACE_NAME))
          .addMethod(MethodSpec
              .methodBuilder(Util.CREATOR_INTERFACE_NAME.decapitalize())
              .addModifiers(Modifier.PUBLIC)
              .addAnnotation(Override::class.java)
              .returns(ClassName.get("", Util.CREATOR_INTERFACE_NAME))
              .addStatement("return \$L", Util.CREATOR_INTERFACE_NAME.toUpperCase())
              .build())
          .addMethods(typeSpecs
              .map {
                MethodSpec
                    .methodBuilder("${it.name.decapitalize()}${Util.FACTORY_INTERFACE_NAME}")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override::class.java)
                    .returns(ClassName.get("", "${it.name}.${Util.FACTORY_INTERFACE_NAME}"))
                    .addStatement("return \$L.\$L", it.name, Util.FACTORY_INTERFACE_NAME.toUpperCase())
                    .build()
              })
          .build()

  return toBuilder()
      .addField(FieldSpec
          .builder(ClassName.get("", Util.FACTORY_INTERFACE_NAME), Util.FACTORY_INTERFACE_NAME.toUpperCase())
          .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
          .initializer("\$L", factoryInitializer(
              typeSpecs.filter { it.name != Util.CREATOR_INTERFACE_NAME && it.name != Util.FACTORY_INTERFACE_NAME }))
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

object Util {
  const val CREATOR_INTERFACE_NAME: String = "Creator"
  const val CREATOR_METHOD_CREATE: String = "create"
  const val FACTORY_INTERFACE_NAME: String = "Factory"
}