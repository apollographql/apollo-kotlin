package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.ast.GQLEnumTypeDefinition
import com.apollographql.apollo.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo.ast.GQLListType
import com.apollographql.apollo.ast.GQLNamedType
import com.apollographql.apollo.ast.GQLNonNullType
import com.apollographql.apollo.ast.GQLObjectTypeDefinition
import com.apollographql.apollo.ast.GQLScalarTypeDefinition
import com.apollographql.apollo.ast.GQLType
import com.apollographql.apollo.ast.GQLUnionTypeDefinition
import com.apollographql.apollo.ast.Schema
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
@ApolloInternal
sealed interface IrType {
  /**
   * This type is nullable in Kotlin
   */
  val nullable: Boolean

  /**
   * This type is optional in Kotlin
   */
  val optional: Boolean

  /**
   * reading this type must catch exceptions during parsing
   */
  val catchTo: IrCatchTo

  /**
   * This type may be an error
   * true if the type is nullable in the server schema.
   * Used to generate error aware adapters
   */
  val maybeError: Boolean

  fun copyWith(
      maybeError: Boolean = this.maybeError,
      nullable: Boolean = this.nullable,
      optional: Boolean = this.optional,
      catchTo: IrCatchTo = this.catchTo,
  ): IrType
  fun rawType(): IrNamedType
}

@Serializable
@ApolloInternal
enum class IrCatchTo {
  Null,
  Result,
  NoCatch
}

@ApolloInternal fun IrType.nullable(nullable: Boolean): IrType = copyWith(nullable = nullable)
@ApolloInternal fun IrType.optional(optional: Boolean): IrType = copyWith(optional = optional)
internal fun IrType.catchTo(catchTo: IrCatchTo): IrType = copyWith(catchTo = catchTo)
internal fun IrType.maybeError(maybeError: Boolean): IrType = copyWith(maybeError = maybeError)

@Serializable
@SerialName("list")
@ApolloInternal
data class IrListType(
    val ofType: IrType,
    override val nullable: Boolean = false,
    override val optional: Boolean = false,
    override val catchTo: IrCatchTo = IrCatchTo.NoCatch,
    override val maybeError: Boolean = false,
) : IrType {
  override fun copyWith(maybeError: Boolean, nullable: Boolean, optional: Boolean, catchTo: IrCatchTo): IrType = copy(nullable = nullable, optional = optional, catchTo = catchTo, maybeError = maybeError)

  override fun rawType() = ofType.rawType()
}

@Serializable
@ApolloInternal
sealed interface IrNamedType : IrType {
  override fun rawType() = this
  val name: String
}

@Serializable
@SerialName("scalar")
@ApolloInternal
data class IrScalarType(
    override val name: String,
    override val nullable: Boolean = false,
    override val optional: Boolean = false,
    override val catchTo: IrCatchTo = IrCatchTo.NoCatch,
    override val maybeError: Boolean = false,
) : IrNamedType {
  override fun copyWith(maybeError: Boolean, nullable: Boolean, optional: Boolean, catchTo: IrCatchTo): IrType = copy(nullable = nullable, optional = optional, catchTo = catchTo, maybeError = maybeError)
  override fun rawType() = this
}

@Serializable
@SerialName("input")
@ApolloInternal
data class IrInputObjectType(
    override val name: String,
    override val nullable: Boolean = false,
    override val optional: Boolean = false,
    override val catchTo: IrCatchTo = IrCatchTo.NoCatch,
    override val maybeError: Boolean = false,
) : IrNamedType {
  override fun copyWith(maybeError: Boolean, nullable: Boolean, optional: Boolean, catchTo: IrCatchTo): IrType = copy(nullable = nullable, optional = optional, catchTo = catchTo, maybeError = maybeError)
  override fun rawType() = this
}

@Serializable
@SerialName("enum")
@ApolloInternal
data class IrEnumType(
    override val name: String,
    override val nullable: Boolean = false,
    override val optional: Boolean = false,
    override val catchTo: IrCatchTo = IrCatchTo.NoCatch,
    override val maybeError: Boolean = false,
) : IrNamedType {
  override fun copyWith(maybeError: Boolean, nullable: Boolean, optional: Boolean, catchTo: IrCatchTo): IrType = copy(nullable = nullable, optional = optional, catchTo = catchTo, maybeError = maybeError)
  override fun rawType() = this
}

@Serializable
@SerialName("object")
@ApolloInternal
data class IrObjectType(
    override val name: String,
    override val nullable: Boolean = false,
    override val optional: Boolean = false,
    override val catchTo: IrCatchTo = IrCatchTo.NoCatch,
    override val maybeError: Boolean = false,
) : IrNamedType {
  override fun copyWith(maybeError: Boolean, nullable: Boolean, optional: Boolean, catchTo: IrCatchTo): IrType = copy(nullable = nullable, optional = optional, catchTo = catchTo, maybeError = maybeError)
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
@ApolloInternal
data class IrModelType(
    val path: String,
    override val nullable: Boolean = false,
    override val optional: Boolean = false,
    override val catchTo: IrCatchTo = IrCatchTo.NoCatch,
    override val maybeError: Boolean = false,
) : IrNamedType {
  override val name: String
    get() = path

  override fun copyWith(maybeError: Boolean, nullable: Boolean, optional: Boolean, catchTo: IrCatchTo): IrType = copy(nullable = nullable, optional = optional, catchTo = catchTo, maybeError = maybeError)
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
    is GQLNonNullType -> type.toIr(schema).copyWith(nullable = false, maybeError = false)
    is GQLListType -> IrListType(ofType = type.toIr(schema), nullable = true, maybeError = schema.errorAware)
    is GQLNamedType -> {
      when (schema.typeDefinition(name)) {
        is GQLScalarTypeDefinition -> {
          IrScalarType(name, nullable = true, maybeError = schema.errorAware)
        }

        is GQLEnumTypeDefinition -> {
          IrEnumType(name, nullable = true, maybeError = schema.errorAware)
        }

        is GQLInputObjectTypeDefinition -> {
          IrInputObjectType(name, nullable = true, maybeError = schema.errorAware)
        }

        is GQLObjectTypeDefinition -> {
          IrModelType(MODEL_UNKNOWN, nullable = true, maybeError = schema.errorAware)
        }

        is GQLInterfaceTypeDefinition -> {
          IrModelType(MODEL_UNKNOWN, nullable = true, maybeError = schema.errorAware)
        }

        is GQLUnionTypeDefinition -> {
          IrModelType(MODEL_UNKNOWN, nullable = true, maybeError = schema.errorAware)
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
