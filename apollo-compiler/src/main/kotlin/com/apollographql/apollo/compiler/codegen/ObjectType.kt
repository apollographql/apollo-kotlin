package com.apollographql.apollo.compiler.codegen

import com.apollographql.apollo.api.internal.ResponseFieldMarshaller
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.ast.CodeGenerationAst
import com.apollographql.apollo.compiler.ir.Field
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec

internal fun CodeGenerationAst.ObjectType.typeSpec(
    responseAdapter: TypeName,
    generateAsInternal: Boolean = false,
): TypeSpec {
  return if (kind is CodeGenerationAst.ObjectType.Kind.FragmentDelegate) {
    fragmentDelegateTypeSpec(generateAsInternal)
  } else {
    objectTypeSpec(
        responseAdapter = responseAdapter,
        generateAsInternal = generateAsInternal,
    )
  }
}

private fun CodeGenerationAst.ObjectType.objectTypeSpec(
    responseAdapter: TypeName,
    generateAsInternal: Boolean = false,
): TypeSpec {
  val builder = if (abstract) TypeSpec.interfaceBuilder(name) else TypeSpec.classBuilder(name)
  return builder
      .addSuperinterfaces(implements.map { type -> type.asTypeName() })
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
      .applyIf(!abstract) { addModifiers(KModifier.DATA) }
      .apply { if (description.isNotBlank()) addKdoc("%L\n", description) }
      .applyIf(!abstract) { primaryConstructor(primaryConstructorSpec) }
      .addProperties(
          fields.map { field ->
            field.asPropertySpec(
                initializer = CodeBlock.of(field.name).takeUnless { abstract }
            )
          }
      )
      .apply {
        if (kind is CodeGenerationAst.ObjectType.Kind.Fragment) {
          kind.allPossibleTypes.forEach { type ->
            addFunction(
                FunSpec
                    .builder("as${type.name}")
                    .returns(type.asTypeName().copy(nullable = true))
                    .addStatement("return this as? %T", type.asTypeName())
                    .build()
            )
          }
        }
      }
      .applyIf(!abstract) {
        addFunction(
            FunSpec.builder("marshaller")
                .applyIf(implements.isNotEmpty()) { addModifiers(KModifier.OVERRIDE) }
                .returns(ResponseFieldMarshaller::class)
                .beginControlFlow("return·%T·{·writer·->", ResponseFieldMarshaller::class)
                .addStatement("%T.toResponse(writer,·this)", responseAdapter)
                .endControlFlow()
                .build()
        )
      }
      .applyIf(abstract) {
        addFunction(
            FunSpec.builder("marshaller")
                .addModifiers(KModifier.ABSTRACT)
                .applyIf(implements.isNotEmpty()) { addModifiers(KModifier.OVERRIDE) }
                .returns(ResponseFieldMarshaller::class)
                .build()
        )
      }
      .build()
}

private fun CodeGenerationAst.ObjectType.fragmentDelegateTypeSpec(
    generateAsInternal: Boolean = false
): TypeSpec {
  val delegateTypeRef = (kind as CodeGenerationAst.ObjectType.Kind.FragmentDelegate).fragmentTypeRef
  val delegateFieldTypeName = delegateTypeRef.asTypeName()
  val primaryConstructorSpec = FunSpec.constructorBuilder()
      .apply {
        addParameter(
            ParameterSpec.builder(name = "delegate", type = delegateFieldTypeName).build()
        )
      }
      .build()
  return TypeSpec.classBuilder(name)
      .addSuperinterfaces(implements.minus(delegateTypeRef).map { type -> type.asTypeName() })
      .addSuperinterface(delegateTypeRef.enclosingType!!.asTypeName(), delegate = CodeBlock.of("delegate"))
      .applyIf(generateAsInternal) { addModifiers(KModifier.INTERNAL) }
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
                  name = field.name,
                  type = field.type.asTypeName()
              )
              .applyIf(
                  field.responseName == Field.TYPE_NAME_FIELD.fieldName &&
                      field.type is CodeGenerationAst.FieldType.Scalar.String &&
                      schemaType != null) {
                defaultValue("%S", schemaType)
              }
              .build()
        })
        .build()
  }
