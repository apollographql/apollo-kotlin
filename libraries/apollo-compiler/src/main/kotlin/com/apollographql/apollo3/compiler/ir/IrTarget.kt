package com.apollographql.apollo3.compiler.ir

import com.squareup.kotlinpoet.ClassName

data class IrClassName(
    val packageName: String,
    val names: List<String>
) {
  fun asString(): String {
    return "$packageName.${names.joinToString(".")}"
  }
}

fun IrClassName.asKotlinPoet(): ClassName = ClassName(packageName, names)

class IrTargetField(
    val name: String,
    val targetName: String,
    val isFunction: Boolean,
    val type: IrType,
    val arguments: List<IrTargetArgument>
)

sealed interface IrTargetArgument
object IrExecutionContextTargetArgument: IrTargetArgument
class IrGraphqlTargetArgument(
    val name: String,
    val targetName: String,
    val type: IrType,
): IrTargetArgument

class IrTargetObject(
    val name: String,
    val targetClassName: IrClassName,
    val isSingleton: Boolean,
    val hasNoArgsConstructor: Boolean,
    val operationType: String?,
    val fields: List<IrTargetField>,
)