package com.apollographql.apollo3.compiler.backend.codegen

import com.apollographql.apollo3.api.Fragment
import com.apollographql.apollo3.api.ResponseField
import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo3.compiler.escapeKotlinReservedWord
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import kotlin.reflect.KClass

internal fun CodeGenerationAst.FragmentType.interfaceTypeSpec(): TypeSpec {
  return TypeSpec
      .interfaceBuilder((this.interfaceType.name).escapeKotlinReservedWord())
      .addAnnotation(suppressWarningsAnnotation)
      .applyIf(this.description.isNotBlank()) { addKdoc("%L\n", this@interfaceTypeSpec.description) }
      .addProperties(
          this.interfaceType.fields
              .filter {
                it.type is CodeGenerationAst.FieldType.Object ||
                    (it.type is CodeGenerationAst.FieldType.Array && it.type.leafType is CodeGenerationAst.FieldType.Object) ||
                    !it.override
              }
              .map { field -> field.asPropertySpec() }
      )
      .addTypes(
          this.interfaceType.nestedObjects.map { nestedObject ->
            nestedObject.typeSpec(generateFragmentsAsInterfaces = true)
          }
      )
      .addType(
          TypeSpec.companionObjectBuilder()
              .addProperty(PropertySpec.builder("FRAGMENT_DEFINITION", String::class)
                  .initializer(CodeBlock.of("%S", fragmentDefinition))
                  .build()
              )
              .addFunctions(
                  this@interfaceTypeSpec.interfaceType.fragmentAccessors.map { accessor ->
                    FunSpec
                        .builder(accessor.name.escapeKotlinReservedWord())
                        .receiver(this.interfaceType.typeRef.asTypeName())
                        .returns(accessor.typeRef.asTypeName().copy(nullable = true))
                        .addStatement("return this as? %T", accessor.typeRef.asTypeName())
                        .build()

                  }
              )
              .build()
      )
      .build()
}

internal fun CodeGenerationAst.FragmentType.implementationTypeSpec(
    generateFragmentsAsInterfaces: Boolean,
): TypeSpec {
  val dataTypeName = this.implementationType.nestedObjects.single().typeRef.asTypeName()
  return this.implementationType
      .typeSpec(generateFragmentsAsInterfaces)
      .toBuilder()
      .apply {
        val dataTypeSpec = typeSpecs.single().addSuperinterface(Fragment.Data::class)
        typeSpecs[0] = dataTypeSpec
      }
      .addSuperinterface(Fragment::class.java.asClassName().parameterizedBy(dataTypeName))
      .makeDataClass(variables.map { it.toParameterSpec() })
      .apply {
        val buffered = implementationType.kind is CodeGenerationAst.ObjectType.Kind.ObjectWithFragments && !generateFragmentsAsInterfaces
        addFunction(
            adapterFunSpec(implementationType.typeRef.asAdapterTypeName(), buffered)
        )
      }
      .addFunction(
          FunSpec.builder(
              "responseFields",
          )
              .addModifiers(KModifier.OVERRIDE)
              .returns(
                  List::class.asClassName().parameterizedBy(
                      ResponseField.FieldSet::class.asClassName(),
                  )
              )
              .addCode("return %L", responseFieldsCode())
              .build()
      )
      .addFunction(serializeVariablesFunSpec(
          funName = "serializeVariables",
          packageName = this.implementationType.typeRef.packageName,
          name = this.implementationType.typeRef.name,
      ))
      .build()
}

private fun TypeSpec.addSuperinterface(superinterface: KClass<*>): TypeSpec {
  return toBuilder().addSuperinterface(superinterface).build()
}

private fun CodeGenerationAst.FragmentType.responseFieldsCode(): CodeBlock {
  val builder = CodeBlock.builder()

  builder.add("listOf(\n")
  builder.indent()
  val dataObject = implementationType.nestedObjects.first()
  when (val kind = dataObject.kind) {
    is CodeGenerationAst.ObjectType.Kind.Object -> {
      builder.add("%T(null, %T.RESPONSE_FIELDS)\n", ResponseField.FieldSet::class, dataObject.typeRef.asAdapterTypeName())
    }
    is CodeGenerationAst.ObjectType.Kind.ObjectWithFragments -> {
      kind.possibleImplementations.forEach { (possibleTypes, typeRef) ->
        possibleTypes.forEach { possibleType ->
          builder.add("%T(%S, %T.RESPONSE_FIELDS),\n", ResponseField.FieldSet::class, possibleType, typeRef.asAdapterTypeName())
        }
      }
      if (kind.defaultImplementation != null) {
        builder.add("%T(null, %T.RESPONSE_FIELDS),\n", ResponseField.FieldSet::class, kind.defaultImplementation.asAdapterTypeName())
      }
    }
  }
  builder.unindent()
  builder.add(")")

  return builder.build()
}
