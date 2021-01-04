package com.apollographql.apollo.compiler.backend.codegen

import com.apollographql.apollo.api.internal.ResponseFieldMarshaller
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

internal fun CodeGenerationAst.ObjectType.typeSpec(): TypeSpec {
  return if (kind is CodeGenerationAst.ObjectType.Kind.FragmentDelegate) {
    fragmentDelegateTypeSpec()
  } else {
    objectTypeSpec()
  }
}

private fun CodeGenerationAst.ObjectType.objectTypeSpec(): TypeSpec {
  val builder = if (abstract)
    TypeSpec.interfaceBuilder(name.escapeKotlinReservedWord())
  else
    TypeSpec.classBuilder(name.escapeKotlinReservedWord())
  return builder
      .addSuperinterfaces(implements.map { type -> type.asTypeName() })
      .applyIf(!abstract && this.fields.isNotEmpty()) { addModifiers(KModifier.DATA) }
      .apply { if (description.isNotBlank()) addKdoc("%L\n", description) }
      .applyIf(!abstract) { primaryConstructor(primaryConstructorSpec) }
      .addProperties(
          fields.map { field ->
            field.asPropertySpec(
                initializer = CodeBlock.of(field.name.escapeKotlinReservedWord()).takeUnless { abstract }
            )
          }
      )
      .addTypes(
          this.nestedObjects
              .map { nestedObject -> nestedObject.objectTypeSpec() }
      )
      .applyIf(this.fragmentAccessors.isNotEmpty()) {
        addType(
            TypeSpec.companionObjectBuilder()
                .addFunctions(
                    this@objectTypeSpec.fragmentAccessors.map { accessor ->
                      FunSpec
                          .builder(accessor.name.escapeKotlinReservedWord())
                          .receiver(typeRef.asTypeName())
                          .returns(accessor.typeRef.asTypeName().copy(nullable = true))
                          .addStatement("return this as? %T", accessor.typeRef.asTypeName())
                          .build()

                    }
                )
                .build()
        )
      }
      .build()
}

private fun CodeGenerationAst.ObjectType.fragmentDelegateTypeSpec(): TypeSpec {
  val delegateTypeRef = (kind as CodeGenerationAst.ObjectType.Kind.FragmentDelegate).fragmentTypeRef
  val delegateFieldTypeName = delegateTypeRef.asTypeName()
  val primaryConstructorSpec = FunSpec.constructorBuilder()
      .apply {
        addParameter(
            ParameterSpec.builder(name = "delegate", type = delegateFieldTypeName).build()
        )
      }
      .build()
  return TypeSpec.classBuilder(name.escapeKotlinReservedWord())
      .addSuperinterfaces(implements.minus(delegateTypeRef).map { type -> type.asTypeName() })
      .addSuperinterface(delegateTypeRef.enclosingType!!.asTypeName(), delegate = CodeBlock.of("delegate"))
      .addModifiers(KModifier.DATA)
      .apply { if (description.isNotBlank()) addKdoc("%L\n", description) }
      .addProperty(
          PropertySpec.builder(name = "delegate", type = delegateFieldTypeName)
              .initializer("delegate")
              .build()
      )
      .primaryConstructor(primaryConstructorSpec)
      .build()
}

private val CodeGenerationAst.ObjectType.primaryConstructorSpec: FunSpec
  get() {
    return FunSpec.constructorBuilder()
        .addParameters(fields.map { field ->
          ParameterSpec
              .builder(
                  name = field.name.escapeKotlinReservedWord(),
                  type = field.type.asTypeName()
              )
              .apply {
                if (field.responseName == "__typename" &&
                    field.type is CodeGenerationAst.FieldType.Scalar.String &&
                    this@primaryConstructorSpec.schemaTypename != null
                ) {
                  defaultValue("%S", this@primaryConstructorSpec.schemaTypename)
                }
              }
              .build()
        })
        .build()
  }
