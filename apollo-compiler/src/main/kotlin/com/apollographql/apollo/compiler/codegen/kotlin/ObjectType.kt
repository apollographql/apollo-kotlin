package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.GraphqlFragment
import com.apollographql.apollo.api.ResponseFieldMarshaller
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.ast.FieldType
import com.apollographql.apollo.compiler.ast.ObjectType
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.asPropertySpec
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.asTypeName
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.marshallerFunSpec
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.responseFieldsPropertySpec
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.toMapperFun
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

internal fun ObjectType.typeSpec(generateAsInternal: Boolean = false): TypeSpec = when (kind) {
  is ObjectType.Kind.Object -> TypeSpec
      .classBuilder(name)
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .addModifiers(KModifier.DATA)
      .primaryConstructor(primaryConstructorSpec)
      .addProperties(fields.map { it.asPropertySpec(initializer = CodeBlock.of(it.name)) })
      .addProperties(inlineFragmentProperties)
      .addType(TypeSpec.companionObjectBuilder()
          .addProperty(responseFieldsPropertySpec(fields))
          .addFunction(fields.toMapperFun(ClassName(packageName = "", simpleName = name)))
          .build())
      .applyIf(fragmentsType != null) { addType(fragmentsType!!.fragmentsTypeSpec) }
      .addFunction(marshallerFunSpec(fields))
      .addTypes(nestedObjects.map { (_, type) -> type.typeSpec() })
      .build()

  is ObjectType.Kind.InlineFragmentSuper -> TypeSpec
      .interfaceBuilder(name)
      .addFunction(
          FunSpec.builder("marshaller")
              .addModifiers(KModifier.ABSTRACT)
              .returns(ResponseFieldMarshaller::class)
              .build()
      )
      .build()

  is ObjectType.Kind.InlineFragment -> TypeSpec
      .classBuilder(name)
      .addModifiers(KModifier.DATA)
      .primaryConstructor(primaryConstructorSpec)
      .addProperties(fields.map { it.asPropertySpec(initializer = CodeBlock.of(it.name)) })
      .addSuperinterface(kind.superInterface.asTypeName())
      .addType(TypeSpec.companionObjectBuilder()
          .addProperty(responseFieldsPropertySpec(fields))
          .addFunction(fields.toMapperFun(ClassName.bestGuess(name)))
          .addProperty(PropertySpec.builder("POSSIBLE_TYPES",
              Array<String>::class.asClassName().parameterizedBy(String::class.asClassName()))
              .initializer(kind.possibleTypes
                  .map { CodeBlock.of("%S", it) }
                  .joinToCode(prefix = "arrayOf(", separator = ", ", suffix = ")")
              )
              .build()
          )
          .build())
      .addFunction(marshallerFunSpec(fields = fields, override = true))
      .applyIf(fragmentsType != null) { addType(fragmentsType!!.fragmentsTypeSpec) }
      .addTypes(nestedObjects.map { (_, type) -> type.typeSpec() })
      .build()

  is ObjectType.Kind.Fragment -> TypeSpec
      .classBuilder(name)
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .addModifiers(KModifier.DATA)
      .addSuperinterface(GraphqlFragment::class.java)
      .addAnnotation(KotlinCodeGen.suppressWarningsAnnotation)
      .primaryConstructor(primaryConstructorSpec)
      .addProperties(fields.map { field -> field.asPropertySpec(initializer = CodeBlock.of(field.name)) })
      .addType(TypeSpec
          .companionObjectBuilder()
          .addProperty(responseFieldsPropertySpec(fields))
          .addProperty(PropertySpec.builder("FRAGMENT_DEFINITION", String::class)
              .initializer("%S", kind.definition)
              .build()
          )
          .addProperty(PropertySpec.builder("POSSIBLE_TYPES",
              Array<String>::class.asClassName().parameterizedBy(String::class.asClassName()))
              .initializer(kind.possibleTypes
                  .map { CodeBlock.of("%S", it) }
                  .joinToCode(prefix = "arrayOf(", separator = ", ", suffix = ")")
              )
              .build()
          )
          .addFunction(fields.toMapperFun(ClassName.bestGuess(name)))
          .build())
      .applyIf(fragmentsType != null) { addType(fragmentsType!!.fragmentsTypeSpec) }
      .addFunction(marshallerFunSpec(fields, true))
      .addTypes(nestedObjects.map { (_, type) -> type.typeSpec() })
      .build()

}

private val ObjectType.inlineFragmentProperties: List<PropertySpec>
  get() {
    val inlineFragmentField = fields.find { it.type is FieldType.InlineFragment }
    val inlineFragmentFieldType = inlineFragmentField?.type as? FieldType.InlineFragment
    return inlineFragmentFieldType?.fragmentRefs?.map { fragmentRef ->
      val fragmentType = fragmentRef.asTypeName()
      PropertySpec
          .builder(
              name = fragmentRef.name.decapitalize().replace(regex = "\\d+\$".toRegex(), replacement = ""),
              type = fragmentType.copy(nullable = true)
          )
          .initializer("%L as? %T", inlineFragmentField.name, fragmentType)
          .build()
    } ?: emptyList()
  }

private val ObjectType.fragmentsTypeSpec: TypeSpec
  get() {
    return TypeSpec.classBuilder(name)
        .addModifiers(KModifier.DATA)
        .primaryConstructor(primaryConstructorSpec)
        .addProperties(fields.map { field -> field.asPropertySpec(initializer = CodeBlock.of(field.name)) })
        .addFunction(marshallerFunSpec)
        .build()
  }

private val ObjectType.primaryConstructorSpec: FunSpec
  get() {
    return FunSpec.constructorBuilder()
        .addParameters(fields.map { field ->
          val typeName = field.type.asTypeName()
          ParameterSpec.builder(
              name = field.name,
              type = if (field.isOptional) typeName.copy(nullable = true) else typeName
          ).build()
        })
        .build()
  }

private val ObjectType.marshallerFunSpec: FunSpec
  get() {
    return FunSpec.builder("marshaller")
        .returns(ResponseFieldMarshaller::class)
        .beginControlFlow("return %T", ResponseFieldMarshaller::class)
        .addCode(
            fields.map { field ->
              if (field.isOptional) {
                CodeBlock.of("%L?.marshaller()?.marshal(it)", field.name)
              } else {
                CodeBlock.of("%L.marshaller().marshal(it)", field.name)
              }
            }.joinToCode(separator = "\n", suffix = "\n")
        )
        .endControlFlow()
        .build()
  }
