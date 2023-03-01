package com.apollographql.apollo3.compiler.ir

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
import com.apollographql.apollo3.compiler.codegen.Identifier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed class IrType {
  open fun rawType() = this
}

@Serializable
@SerialName("nonnull")
internal data class IrNonNullType(val ofType: IrType) : IrType() {
  init {
    check(ofType !is IrNonNullType)
  }

  override fun rawType() = ofType.rawType()
}

@Serializable
@SerialName("optional")
internal data class IrOptionalType(val ofType: IrType) : IrType() {
  override fun rawType() = ofType.rawType()
}

@Serializable
@SerialName("list")
internal data class IrListType(val ofType: IrType) : IrType() {
  init {
    check(ofType !is IrOptionalType)
  }

  override fun rawType() = ofType.rawType()
}

@Serializable
internal sealed interface IrNamedType {
  val name: String
}

@Serializable
@SerialName("scalar")
internal data class IrScalarType(override val name: String) : IrType(), IrNamedType
@Serializable
@SerialName("input")
internal data class IrInputObjectType(override val name: String) : IrType(), IrNamedType
@Serializable
@SerialName("enum")
internal data class IrEnumType(override val name: String) : IrType(), IrNamedType


/**
 * @param path a unique path identifying a given model.
 *
 * - responseBased: Each dot is a dot in the Json response
 * operationData.$operationName.Data.DroidHero
 * fragmentData.$fragmentName.Data.Hero // interface
 * fragmentData.$fragmentName.Data.OtherHero
 * fragmentData.$fragmentName.Data.DroidHero
 * fragmentData.$fragmentName.Data.HumanHero
 * fragmentData.$fragmentName.Data.HumanHero.CharacterFriend
 * fragmentInterface.$fragmentName.Data.CharacterHero

 * - experimental_operationBasedWithInterfaces:
 * operationData.$operationName.Data.Hero // interface
 * operationData.$operationName.Data.DroidHero.OnDroid.HumanFriend.onHuman
 * operationData.$operationName.Data.OtherHero.Starship
 *
 */
@Serializable
@SerialName("model")
internal data class IrModelType(val path: String) : IrType()

internal const val MODEL_OPERATION_DATA = "operationData"
internal const val MODEL_FRAGMENT_DATA = "fragmentData"
internal const val MODEL_FRAGMENT_INTERFACE = "fragmentInterface"
internal const val MODEL_UNKNOWN = "?"

internal fun IrType.makeOptional(): IrType = IrNonNullType(IrOptionalType(this))
internal fun IrType.makeNullable(): IrType = if (this is IrNonNullType) {
  this.ofType.makeNullable()
} else {
  this
}

internal fun IrType.makeNonNull(): IrType = if (this is IrNonNullType) {
  this
} else {
  IrNonNullType(this)
}

internal fun IrType.isOptional() = (this is IrNonNullType) && (this.ofType is IrOptionalType)

internal fun IrType.makeNonOptional(): IrType {
  return ((this as? IrNonNullType)?.ofType as? IrOptionalType)?.ofType ?: error("${Identifier.type} is not an optional type")
}


internal fun IrType.replacePlaceholder(newPath: String): IrType {
  return when (this) {
    is IrNonNullType -> IrNonNullType(ofType = ofType.replacePlaceholder(newPath))
    is IrListType -> IrListType(ofType = ofType.replacePlaceholder(newPath))
    is IrModelType -> copy(path = newPath)
    else -> error("Not a compound type?")
  }
}

internal fun GQLType.toIr(schema: Schema): IrType {
  return when (this) {
    is GQLNonNullType -> IrNonNullType(ofType = type.toIr(schema))
    is GQLListType -> IrListType(ofType = type.toIr(schema))
    is GQLNamedType -> {
      when (schema.typeDefinition(name)) {
        is GQLScalarTypeDefinition -> {
          IrScalarType(name)
        }

        is GQLEnumTypeDefinition -> {
          IrEnumType(name = name)
        }

        is GQLInputObjectTypeDefinition -> {
          IrInputObjectType(name)
        }

        is GQLObjectTypeDefinition -> {
          IrModelType(MODEL_UNKNOWN)
        }

        is GQLInterfaceTypeDefinition -> {
          IrModelType(MODEL_UNKNOWN)
        }

        is GQLUnionTypeDefinition -> {
          IrModelType(MODEL_UNKNOWN)
        }
      }
    }
  }
}