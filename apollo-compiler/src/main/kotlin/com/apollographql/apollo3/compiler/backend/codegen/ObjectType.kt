package com.apollographql.apollo3.compiler.backend.codegen

import com.apollographql.apollo3.compiler.applyIf
import com.apollographql.apollo3.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo3.compiler.escapeKotlinReservedWord
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

internal fun CodeGenerationAst.ObjectType.typeSpec(generateFragmentsAsInterfaces: Boolean): TypeSpec {
  return if (kind is CodeGenerationAst.ObjectType.Kind.FragmentDelegate) {
    fragmentDelegateTypeSpec()
  } else {
    objectTypeSpec(generateFragmentsAsInterfaces)
  }
}

private fun CodeGenerationAst.ObjectType.objectTypeSpec(generateFragmentsAsInterfaces: Boolean): TypeSpec {
  val builder = if (abstract) {
    TypeSpec.interfaceBuilder(name.escapeKotlinReservedWord())
  } else {
    TypeSpec.classBuilder(name.escapeKotlinReservedWord())
  }

  val nestedTypes = this.nestedObjects.map { nestedObject ->
    nestedObject.objectTypeSpec(generateFragmentsAsInterfaces)
  }

  val companionObject = if (this.fragmentAccessors.isNotEmpty() && generateFragmentsAsInterfaces) {
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
  } else null

  return builder
      .addSuperinterfaces(implements.map { type -> type.asTypeName() })
      .applyIf(!abstract && this.fields.isNotEmpty()) { addModifiers(KModifier.DATA) }
      .apply { if (description.isNotBlank()) addKdoc("%L\n", description) }
      .applyIf(!abstract) { primaryConstructor(primaryConstructorSpec(generateFragmentsAsInterfaces)) }
      .addProperties(propertySpecs(generateFragmentsAsInterfaces))
      .addTypes(nestedTypes)
      .apply { if (companionObject != null) addType(companionObject) }
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

private fun CodeGenerationAst.ObjectType.primaryConstructorSpec(fragmentAsInterfaces: Boolean): FunSpec {
  val fieldParams = fields.map { field -> field.asParameterSpec(schemaTypename) }
  val fragmentParams = if (this.kind is CodeGenerationAst.ObjectType.Kind.ObjectWithFragments && !fragmentAsInterfaces) {
    this.kind.possibleImplementations.map { fragmentImplementation ->
      val propertyName = fragmentImplementation.typeRef.fragmentVariableName()
      ParameterSpec.builder(
          name = propertyName,
          type = fragmentImplementation.typeRef.asTypeName().copy(nullable = true)
      ).build()
    }
  } else emptyList()
  return FunSpec.constructorBuilder()
      .addParameters(fieldParams + fragmentParams)
      .build()
}

private fun CodeGenerationAst.Field.asParameterSpec(schemaTypenameDefaultValue: String?): ParameterSpec {
  val defaultValue = schemaTypenameDefaultValue?.takeIf {
    this.responseName == "__typename" && this.type is CodeGenerationAst.FieldType.Scalar.String
  }
  return ParameterSpec
      .builder(
          name = this.name.escapeKotlinReservedWord(),
          type = this.type.asTypeName()
      )
      .apply {
        if (defaultValue != null) {
          defaultValue("%S", defaultValue)
        }
      }
      .build()
}

private fun CodeGenerationAst.ObjectType.propertySpecs(fragmentAsInterfaces: Boolean): List<PropertySpec> {
  val fieldProps = fields
      .filter {
        it.type is CodeGenerationAst.FieldType.Object ||
            (it.type is CodeGenerationAst.FieldType.Array && (it.type.leafType is CodeGenerationAst.FieldType.Object)) ||
            !it.override ||
            !abstract
      }.map { field ->
        field.asPropertySpec(
            initializer = CodeBlock.of(field.name.escapeKotlinReservedWord()).takeUnless { abstract }
        )
      }
  val fragmentProps = if (this.kind is CodeGenerationAst.ObjectType.Kind.ObjectWithFragments && !fragmentAsInterfaces) {
    this.kind.possibleImplementations.map { fragmentImplementation ->
      PropertySpec
          .builder(
              name = fragmentImplementation.typeRef.fragmentVariableName(),
              type = fragmentImplementation.typeRef.asTypeName().copy(nullable = true)
          )
          .initializer(fragmentImplementation.typeRef.fragmentVariableName())
          .build()
    }
  } else emptyList()
  return fieldProps + fragmentProps
}
