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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The IR representation of an input or output field
 *
 * **Allowed**
 *
 * * `T`
 * * `Nullable<T>`
 * * `Option<T>`
 * * `Option<Nullable<T>>`
 * * `Result<T>`
 * * `Result<Nullable<T>>`
 * * All `List` variations of the above
 *
 * **Disallowed**
 *
 * * `Nullable<Option<T>>`
 * * `Nullable<Result<T>>`
 * * `Nullable<Nullable<T>>`
 * * `Option<Option<T>>`
 * * `Option<Result<T>>`
 * * `Result<Option<T>>`
 * * `Result<Result<T>>`
 * * etc...
 *
 * We need both `Option` and `Nullable` to distinguish `?` vs `Option` in Kotlin (see this [Arrow article](https://arrow-kt.io/learn/typed-errors/nullable-and-option/)).
 * In java `Option` and `Nullable` might be represented using the same `Optional` depending on the settings.
 */
@Serializable
sealed interface IrType {
  val nullable: Boolean
  val optional: Boolean
  val catchTo: IrCatchTo

  fun copyWith(nullable: Boolean = this.nullable, optional: Boolean = this.optional, catchTo: IrCatchTo = this.catchTo): IrType
  fun rawType(): IrNamedType
}

@Serializable
enum class IrCatchTo {
  Null,
  Result,
  NoCatch
}

fun IrType.nullable(nullable: Boolean): IrType = copyWith(nullable = nullable)
fun IrType.optional(optional: Boolean): IrType = copyWith(optional = optional)
fun IrType.catchTo(catchTo: IrCatchTo): IrType = copyWith(catchTo = catchTo)

@Serializable
@SerialName("list")
data class IrListType(
    val ofType: IrType,
    override val nullable: Boolean = false,
    override val optional: Boolean = false,
    override val catchTo: IrCatchTo = IrCatchTo.NoCatch,
) : IrType {
  override fun copyWith(nullable: Boolean, optional: Boolean, catchTo: IrCatchTo): IrType = copy(nullable = nullable, optional = optional, catchTo = catchTo)

  override fun rawType() = ofType.rawType()
}

@Serializable
sealed interface IrNamedType : IrType {
  override fun rawType() = this
  val name: String
}

@Serializable
@SerialName("scalar")
data class IrScalarType(
    override val name: String,
    override val nullable: Boolean = false,
    override val optional: Boolean = false,
    override val catchTo: IrCatchTo = IrCatchTo.NoCatch,
) : IrNamedType {
  override fun copyWith(nullable: Boolean, optional: Boolean, catchTo: IrCatchTo): IrType = copy(nullable = nullable, optional = optional, catchTo = catchTo)
  override fun rawType() = this
}

@Serializable
@SerialName("input")
data class IrInputObjectType(
    override val name: String,
    override val nullable: Boolean = false,
    override val optional: Boolean = false,
    override val catchTo: IrCatchTo = IrCatchTo.NoCatch,
) : IrNamedType {
  override fun copyWith(nullable: Boolean, optional: Boolean, catchTo: IrCatchTo): IrType = copy(nullable = nullable, optional = optional, catchTo = catchTo)
  override fun rawType() = this
}

@Serializable
@SerialName("enum")
data class IrEnumType(
    override val name: String,
    override val nullable: Boolean = false,
    override val optional: Boolean = false,
    override val catchTo: IrCatchTo = IrCatchTo.NoCatch,
) : IrNamedType {
  override fun copyWith(nullable: Boolean, optional: Boolean, catchTo: IrCatchTo): IrType = copy(nullable = nullable, optional = optional, catchTo = catchTo)
  override fun rawType() = this
}

@Serializable
@SerialName("object")
data class IrObjectType(
    override val name: String,
    override val nullable: Boolean = false,
    override val optional: Boolean = false,
    override val catchTo: IrCatchTo = IrCatchTo.NoCatch,
) : IrNamedType {
  override fun copyWith(nullable: Boolean, optional: Boolean, catchTo: IrCatchTo): IrType = copy(nullable = nullable, optional = optional, catchTo = catchTo)
  override fun rawType() = this
}


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
internal data class IrModelType(
    val path: String,
    override val nullable: Boolean = false,
    override val optional: Boolean = false,
    override val catchTo: IrCatchTo = IrCatchTo.NoCatch,
) : IrNamedType {
  override val name: String
    get() = path

  override fun copyWith(nullable: Boolean, optional: Boolean, catchTo: IrCatchTo): IrType = copy(nullable = nullable, optional = optional, catchTo = catchTo)
  override fun rawType() = this
}

internal const val MODEL_OPERATION_DATA = "operationData"
internal const val MODEL_FRAGMENT_DATA = "fragmentData"
internal const val MODEL_FRAGMENT_INTERFACE = "fragmentInterface"
internal const val MODEL_UNKNOWN = "?"

internal fun IrType.replacePlaceholder(newPath: String): IrType {
  return when (this) {
    is IrListType -> copy(ofType = ofType.replacePlaceholder(newPath))
    is IrModelType -> copy(path = newPath)
    else -> error("Not a compound type?")
  }
}

internal fun GQLType.toIr(schema: Schema): IrType {
  return when (this) {
    is GQLNonNullType -> type.toIr(schema).copyWith(nullable = false)
    is GQLListType -> IrListType(ofType = type.toIr(schema), nullable = true)
    is GQLNamedType -> {
      when (schema.typeDefinition(name)) {
        is GQLScalarTypeDefinition -> {
          IrScalarType(name, nullable = true)
        }

        is GQLEnumTypeDefinition -> {
          IrEnumType(name, nullable = true)
        }

        is GQLInputObjectTypeDefinition -> {
          IrInputObjectType(name, nullable = true)
        }

        is GQLObjectTypeDefinition -> {
          IrModelType(MODEL_UNKNOWN, nullable = true)
        }

        is GQLInterfaceTypeDefinition -> {
          IrModelType(MODEL_UNKNOWN, nullable = true)
        }

        is GQLUnionTypeDefinition -> {
          IrModelType(MODEL_UNKNOWN, nullable = true)
        }
      }
    }
  }
}

internal fun IrNamedType.isComposite(): Boolean {
  return when (this) {
    is IrScalarType -> false
    is IrEnumType -> false
    is IrInputObjectType -> true
    is IrModelType -> true
    is IrObjectType -> true
  }
}
