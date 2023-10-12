package com.apollographql.apollo3.ksp

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.ast.GQLEnumTypeDefinition
import com.apollographql.apollo3.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo3.ast.GQLListType
import com.apollographql.apollo3.ast.GQLNamedType
import com.apollographql.apollo3.ast.GQLNonNullType
import com.apollographql.apollo3.ast.GQLObjectTypeDefinition
import com.apollographql.apollo3.ast.GQLScalarTypeDefinition
import com.apollographql.apollo3.ast.GQLType
import com.apollographql.apollo3.ast.GQLUnionTypeDefinition
import com.apollographql.apollo3.ast.Schema
import com.apollographql.apollo3.compiler.CodegenMetadata
import com.apollographql.apollo3.compiler.ScalarInfo
import com.apollographql.apollo3.compiler.codegen.ResolverClassName
import com.apollographql.apollo3.compiler.ir.IrClassName
import com.apollographql.apollo3.compiler.ir.IrInputObjectType
import com.apollographql.apollo3.compiler.ir.IrListType
import com.apollographql.apollo3.compiler.ir.IrNonNullType
import com.apollographql.apollo3.compiler.ir.IrObjectType
import com.apollographql.apollo3.compiler.ir.IrOptionalType
import com.apollographql.apollo3.compiler.ir.IrScalarType
import com.apollographql.apollo3.compiler.ir.IrType
import com.apollographql.apollo3.compiler.resolveSchemaType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Nullability

class IncompatibleType(message: String) : Exception(message)

@OptIn(ApolloInternal::class)
internal class ValidationScope(
    private val objectMapping: Map<String, ObjectInfo>,
    private val scalarMapping: Map<String, ScalarInfo>,
    private val schema: Schema,
    private val codegenMetadata: CodegenMetadata
) {
  fun validateAndCoerce(ksTypeReference: KSTypeReference, expectedType: GQLType, allowCovariant: Boolean): IrType {
    val ksType = ksTypeReference.resolve()
    val className = ksType.declaration.acClassName()

    var expectedNullableType = expectedType
    if (expectedType is GQLNonNullType) {
      expectedNullableType = expectedType.type
    }

    val irType = when (expectedNullableType) {
      is GQLListType -> {
        if (className != listClassName) {
          throw IncompatibleType("Expected list type at ${ksTypeReference.location}")
        }
        IrListType(
            validateAndCoerce(
                ksTypeReference.element!!.typeArguments.single().type!!,
                expectedNullableType.type,
                allowCovariant,
            )
        )
      }

      is GQLNamedType -> {
        when (val typeDefinition = schema.typeDefinition(expectedNullableType.name)) {
          is GQLScalarTypeDefinition -> {
            val scalarInfo = scalarMapping.get(typeDefinition.name)
            if (scalarInfo == null) {
              throw IncompatibleType("Expected scalar type '${typeDefinition.name}' but no adapter found. Did you forget a @ApolloAdapter? at ${ksTypeReference.location}")
            }
            if (scalarInfo.targetName != className.asString()) {
              throw IncompatibleType("Scalar type '${typeDefinition.name}' is mapped to '${scalarInfo.targetName} but '${className.asString()} was found at ${ksTypeReference.location}")
            }
            IrScalarType(typeDefinition.name)
          }

          is GQLInputObjectTypeDefinition -> {
            val expectedFQDN = codegenMetadata.resolveSchemaType(typeDefinition.name)?.toIrClassName() ?: error("Cannot resolve input ${typeDefinition.name}")
            if (className != expectedFQDN) {
              throw IncompatibleType("Input object type '${typeDefinition.name}' is mapped to '${expectedFQDN} but '${className.asString()} was found at ${ksTypeReference.location}")
            }

            IrInputObjectType(typeDefinition.name)
          }

          is GQLEnumTypeDefinition -> {
            val expectedFQDN = codegenMetadata.resolveSchemaType(typeDefinition.name)?.toIrClassName() ?: error("Cannot resolve enum ${typeDefinition.name}")
            if (className != expectedFQDN) {
              throw IncompatibleType("Enum type '${typeDefinition.name}' is mapped to '${expectedFQDN} but '${className.asString()} was found at ${ksTypeReference.location}")
            }

            IrInputObjectType(typeDefinition.name)
          }

          is GQLObjectTypeDefinition, is GQLUnionTypeDefinition, is GQLInterfaceTypeDefinition -> {
            /**
             * Because of interfaces we do the lookup the other way around. Contrary to scalars, there cannot be multiple objects mapped to the same target
             */
            val objectInfoEntry =
                objectMapping.entries.firstOrNull { it.value.className.asString() == className.asString() }

            if (objectInfoEntry == null) {
              throw IncompatibleType("Expected a composite type '${typeDefinition.name}' but no object found. Did you forget a @ApolloObject? at ${ksTypeReference.location}")
            }
            if (!schema.possibleTypes(typeDefinition.name).contains(objectInfoEntry.key)) {
              throw IncompatibleType("Expected type '${typeDefinition.name}' but '${objectInfoEntry.key}' is not a subtype at ${ksTypeReference.location}")
            }

            IrObjectType(typeDefinition.name)
          }
        }
      }

      is GQLNonNullType -> error("")
    }

    if (expectedType is GQLNonNullType) {
      if (ksType.nullability != Nullability.NOT_NULL) {
        throw IncompatibleType("Expected non-nullable type at ${ksTypeReference.location}, got nullable")
      }
    } else {
      if (!allowCovariant) {
        if (ksType.nullability == Nullability.NOT_NULL) {
          throw IncompatibleType("Expected nullable type at ${ksTypeReference.location}, got non nullable")
        }
      }
    }

    return if (ksType.nullability == Nullability.NOT_NULL) {
      IrNonNullType(irType)
    } else {
      irType
    }
  }
}

private fun ResolverClassName.toIrClassName(): IrClassName {
  return IrClassName(packageName, simpleNames)
}


internal fun ValidationScope.validateAndCoerceArgumentType(
    targetName: String,
    typeReference: KSTypeReference,
    gqlType: GQLType,
    hasDefault: Boolean
): IrType {
  val type = typeReference.resolve()
  val className = type.declaration.acClassName()
  val gqlOptional = gqlType !is GQLNonNullType && !hasDefault
  if (className == optionalClassName != gqlOptional) {
    if (gqlOptional) {
      throw IncompatibleType("The '$targetName' argument can be absent in GraphQL and must be of Optional<> type in Kotlin at ${typeReference.location}")
    } else {
      throw IncompatibleType("The '$targetName' argument is always present in GraphQL and must not be of Optional<> type in Kotlin at ${typeReference.location}")
    }
  }

  return if (className == optionalClassName) {
    IrOptionalType(validateAndCoerce(typeReference.element!!.typeArguments.first().type!!, gqlType, false))
  } else {
    validateAndCoerce(typeReference, gqlType, false)
  }
}