package com.apollographql.apollo.compiler.codegen.kotlin

import com.apollographql.apollo.api.GraphqlFragment
import com.apollographql.apollo.compiler.ast.AST
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.toMapperFun
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.responseFieldsPropertySpec
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinCodeGen.asTypeName
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

fun KotlinCodeGen.fragmentTypeSpec(fragmentType: AST.FragmentType): TypeSpec {
  return with(fragmentType) {
    TypeSpec.classBuilder(name)
      .addModifiers(KModifier.DATA)
      .addSuperinterface(GraphqlFragment::class.java)
      .addAnnotation(generatedByApolloAnnotation)
      .addAnnotation(suppressWarningsAnnotation)
      .primaryConstructor(primaryConstructorSpec)
      .addProperties(fields.map { field -> field.asPropertySpec(initializer = CodeBlock.of(field.name)) })
      .addType(companionObjectTypeSpec)
      .addTypes(nestedObjects.map { (_, type) -> objectTypeSpec(type) })
      .addFunction(marshallerFunSpec(fields).toBuilder().addModifiers(KModifier.OVERRIDE).build())
      .build()
  }
}

private val AST.FragmentType.primaryConstructorSpec: FunSpec
  get() {
    return FunSpec.constructorBuilder()
      .addParameters(fields.map { field ->
        val typeName = field.type.asTypeName()
        ParameterSpec.builder(
          name = field.name,
          type = if (field.isOptional) typeName.asNullable() else typeName
        ).build()
      })
      .build()
  }

private val AST.FragmentType.companionObjectTypeSpec: TypeSpec
  get() {
    return TypeSpec.companionObjectBuilder()
      .addProperty(responseFieldsPropertySpec(fields))
      .addProperty(PropertySpec.builder("FRAGMENT_DEFINITION", String::class)
        .initializer("%S", definition)
        .build()
      )
      .addProperty(PropertySpec.builder("POSSIBLE_TYPES",
        Array<String>::class.asClassName().parameterizedBy(String::class.asClassName()))
        .initializer(possibleTypes
          .map { CodeBlock.of("%S", it) }
          .joinToCode(prefix = "arrayOf(", separator = ", ", suffix = ")")
        )
        .build()
      )
      .addFunction(fields.toMapperFun(ClassName.bestGuess(name)))
      .build()
  }
