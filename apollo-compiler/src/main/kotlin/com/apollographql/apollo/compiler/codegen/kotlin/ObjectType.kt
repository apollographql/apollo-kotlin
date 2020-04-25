package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.GraphqlFragment
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.ast.FieldType
import com.apollographql.apollo.compiler.ast.ObjectType
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.asPropertySpec
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.asTypeName
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.createMapperFun
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.marshallerFunSpec
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.responseFieldsPropertySpec
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.toMapperFun
import com.apollographql.apollo.compiler.ir.Field
import com.squareup.kotlinpoet.*

internal fun ObjectType.typeSpec(generateAsInternal: Boolean = false): TypeSpec = when (kind) {
  is ObjectType.Kind.Object -> TypeSpec
      .classBuilder(name)
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .addModifiers(KModifier.DATA)
      .apply { if (description.isNotBlank()) addKdoc("%L\n", description) }
      .primaryConstructor(primaryConstructorSpec)
      .addProperties(fields.map { it.asPropertySpec(initializer = CodeBlock.of(it.name)) })
      .addType(TypeSpec.companionObjectBuilder()
          .addProperty(responseFieldsPropertySpec(fields))
          .addFunction(fields.toMapperFun(ClassName("", name)))
          .addFunction(ClassName("", name).createMapperFun())
          .build())
      .applyIf(fragmentsType != null) { addType(fragmentsType!!.fragmentsTypeSpec(generateAsInternal)) }
      .addFunction(fields.marshallerFunSpec(thisRef = name))
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
      .apply { if (description.isNotBlank()) addKdoc("%L\n", description) }
      .primaryConstructor(primaryConstructorSpec)
      .addProperties(fields.map { it.asPropertySpec(initializer = CodeBlock.of(it.name)) })
      .addSuperinterface(kind.superInterface.asTypeName())
      .addType(TypeSpec.companionObjectBuilder()
          .addProperty(responseFieldsPropertySpec(fields))
          .addFunction(fields.toMapperFun(ClassName("", name)))
          .addFunction(ClassName("", name).createMapperFun())
          .build())
      .addFunction(fields.marshallerFunSpec(override = true, thisRef = name))
      .applyIf(fragmentsType != null) { addType(fragmentsType!!.fragmentsTypeSpec(generateAsInternal)) }
      .addTypes(nestedObjects.map { (_, type) -> type.typeSpec() })
      .build()

  is ObjectType.Kind.Fragment -> TypeSpec
      .classBuilder(name)
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .addModifiers(KModifier.DATA)
      .apply { if (description.isNotBlank()) addKdoc("%L\n", description) }
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
          .addFunction(fields.toMapperFun(ClassName("", name)))
          .addFunction(ClassName("", name).createMapperFun())
          .build())
      .applyIf(fragmentsType != null) { addType(fragmentsType!!.fragmentsTypeSpec(generateAsInternal)) }
      .addFunction(fields.marshallerFunSpec(override = true, thisRef = name))
      .addTypes(nestedObjects.map { (_, type) -> type.typeSpec() })
      .build()

}

private fun ObjectType.fragmentsTypeSpec(generateAsInternal: Boolean = false): TypeSpec {
  return TypeSpec
      .classBuilder(name)
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .addModifiers(KModifier.DATA)
      .primaryConstructor(primaryConstructorSpec)
      .addProperties(fields.map { it.asPropertySpec(initializer = CodeBlock.of(it.name)) })
      .addType(TypeSpec.companionObjectBuilder()
          .addProperty(responseFieldsPropertySpec(fields))
          .addFunction(fields.toMapperFun(ClassName("", name)))
          .addFunction(ClassName("", name).createMapperFun())
          .build())
      .addFunction(fields.marshallerFunSpec(thisRef = name))
      .addTypes(nestedObjects.map { (_, type) -> type.typeSpec() })
      .build()
}

private val ObjectType.primaryConstructorSpec: FunSpec
  get() {
    return FunSpec.constructorBuilder()
        .addParameters(fields.map { field ->
          val typeName = field.type.asTypeName()
          ParameterSpec
              .builder(
                  name = field.name,
                  type = if (field.isOptional) typeName.copy(nullable = true) else typeName
              )
              .applyIf(field.schemaName == Field.TYPE_NAME_FIELD.fieldName && field.type == FieldType.Scalar.String) {
                defaultValue("%S", schemaTypeName)
              }
              .build()
        })
        .build()
  }
