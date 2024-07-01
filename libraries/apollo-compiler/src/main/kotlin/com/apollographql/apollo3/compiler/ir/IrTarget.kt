package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.annotations.ApolloInternal
import com.squareup.kotlinpoet.ClassName

@ApolloInternal
data class IrClassName(
    val packageName: String,
    val names: List<String>
) {
  fun asString(): String {
    return "$packageName.${names.joinToString(".")}"
  }
}

internal fun IrClassName.asKotlinPoet(): ClassName = ClassName(packageName, names)

@ApolloInternal
class IrTargetField(
    val name: String,
    val targetName: String,
    val isFunction: Boolean,
    val type: IrType,
    val arguments: List<IrTargetArgument>
)

@ApolloInternal
sealed interface IrTargetArgument

@ApolloInternal
object IrExecutionContextTargetArgument: IrTargetArgument

@ApolloInternal
class IrGraphqlTargetArgument(
    val name: String,
    val targetName: String,
    val type: IrType,
): IrTargetArgument

@ApolloInternal
class IrTargetObject(
    val name: String,
    val targetClassName: IrClassName,
    val isSingleton: Boolean,
    val hasNoArgsConstructor: Boolean,
    val operationType: String?,
    val fields: List<IrTargetField>,
)