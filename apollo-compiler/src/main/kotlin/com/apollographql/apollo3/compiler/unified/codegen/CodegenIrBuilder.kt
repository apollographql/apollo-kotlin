package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.compiler.PackageNameProvider
import com.apollographql.apollo3.compiler.operationoutput.OperationOutput
import com.apollographql.apollo3.compiler.operationoutput.findOperationId
import com.apollographql.apollo3.compiler.unified.BooleanIrType
import com.apollographql.apollo3.compiler.unified.CustomScalarIrType
import com.apollographql.apollo3.compiler.unified.EnumIrType
import com.apollographql.apollo3.compiler.unified.FloatIrType
import com.apollographql.apollo3.compiler.unified.IdIrType
import com.apollographql.apollo3.compiler.unified.InputObjectIrType
import com.apollographql.apollo3.compiler.unified.IntIrType
import com.apollographql.apollo3.compiler.unified.InterfaceIrType
import com.apollographql.apollo3.compiler.unified.IntermediateRepresentation
import com.apollographql.apollo3.compiler.unified.IrEnum
import com.apollographql.apollo3.compiler.unified.IrEnumValue
import com.apollographql.apollo3.compiler.unified.IrInputField
import com.apollographql.apollo3.compiler.unified.IrInputObject
import com.apollographql.apollo3.compiler.unified.IrType
import com.apollographql.apollo3.compiler.unified.IrVariable
import com.apollographql.apollo3.compiler.unified.ListIrType
import com.apollographql.apollo3.compiler.unified.ModelBuilder
import com.apollographql.apollo3.compiler.unified.NonNullIrType
import com.apollographql.apollo3.compiler.unified.ObjectIrType
import com.apollographql.apollo3.compiler.unified.StringIrType
import com.apollographql.apollo3.compiler.unified.UnionIrType

class CodegenIrBuilder(
    private val ir: IntermediateRepresentation,
    private val customScalarsMapping: Map<String, String>,
    private val typesPackageName: String,
    private val fragmentsPackage: String,
    private val packageNameProvider: PackageNameProvider,
    private val operationOutput: OperationOutput,
    private val generateFragmentsAsInterfaces: Boolean,
) {

  internal fun build(): CodegenIr {

    val operations = ir.operations.map { irOperation ->
      val path = ModelPath(irOperation.filePath, ModelPath.Root.Operation, emptyList())
      val result = ModelBuilder(irOperation.filePath, irOperation.dataField, path).build()

      val variables = irOperation.variables.map { it.toCg() }
      CGOperation(
          name = irOperation.name,
          description = irOperation.description,
          filePath = irOperation.filePath,
          dataImplementations = result.implementations,
          dataInterfaces = result.interfaces,
          variables = variables,
          dataAdapter = result.adapter,
          variablesAdapter = variables.toCGAdapter(irOperation.name),

          operationType = CGOperation.OperationType.valueOf(irOperation.operationType.name.toUpperCase()),
          // TODO: remove the package name from here
          operationId = operationOutput.findOperationId(irOperation.name, ""),
          operationDocument = irOperation.sourceWithFragments,
      )
    }

    val fragments = ir.namedFragments.map { irFragment ->
      val path = ModelPath(irFragment.filePath, ModelPath.Root.Fragment, emptyList())
      val result = ModelBuilder(irFragment.filePath, irFragment.dataField, path).build()

      val variables = irFragment.variables.map { it.toCg() }
      CGFragment(
          name = irFragment.name,
          description = irFragment.description,
          filePath = irFragment.filePath,
          dataImplementations = result.implementations,
          dataInterfaces = result.interfaces,
          variables = variables,
          dataAdapter = result.adapter,
          variablesAdapter = variables.toCGAdapter(irFragment.name)
      )
    }

    val inputObjectTypes = ir.inputObjects.map { irInputObject ->
      irInputObject.toCg()
    }

    val enumTypes = ir.enums.map { irEnum ->
      irEnum.toCg()
    }

    val customScalarTypes = CGCustomScalars(
        names = ir.customScalars.map { it.name }
    )

    return CodegenIr(
        operations = operations,
        fragments = fragments,
        inputTypes = inputObjectTypes,
        enumTypes = enumTypes,
        customScalars = customScalarTypes,
    )
  }

  private fun IrInputObject.toCg(): CGInputObject {
    return CGInputObject(
        name = name,
        description = description,
        deprecationReason = deprecationReason,
        inputFields = fields.map { irInputField ->
          irInputField.toCg()
        }
    )
  }

  private fun IrEnum.toCg(): CGEnum {
    return CGEnum(
        name = name,
        description = description,
        values = values.map {
          it.toCg()
        }
    )
  }

  private fun IrEnumValue.toCg(): CGEnumValue {
    return CGEnumValue(
        name = name,
        description = description,
        deprecationReason = deprecationReason
    )
  }

  private fun IrInputField.toCg(): CGProperty {
    return CGProperty(
        name = name,
        description = description,
        deprecationReason = deprecationReason,
        // https://spec.graphql.org/draft/#sec-Input-Object-Required-Fields
        type = type.toInputCgType().optional(type !is NonNullIrType || defaultValue != null),
        override = false
    )
  }

  private fun IrVariable.toCg(): CGVariable {
    return CGVariable(
        name = name,
        type = type.toVariableCgType().optional(type !is NonNullIrType || defaultValue != null),
    )
  }
}

fun IrType.toVariableCgType(): CGType {
  return toCg(null)
}

fun IrType.toInputCgType(): CGType {
  return toCg(null)
}

/**
 * If you expect to have a non-scalar output type somewhere, you **must** pass it in [compoundType]
 */
fun IrType.toCg(compoundType: CGType?): CGType {
  if (this is NonNullIrType) {
    return ofType.toCg(compoundType)
  }

  return when (this) {
    is NonNullIrType -> error("") // make the compiler happy, this case is handled as a fast path
    is ListIrType -> CGListType(ofType = ofType.toCg(compoundType))
    is StringIrType -> CGStringType()
    is FloatIrType -> CGFloatType()
    is IntIrType -> CGIntType()
    is BooleanIrType -> CGBooleanType()
    is IdIrType -> CGStringType()
    is CustomScalarIrType -> CGCustomScalarType(name = name)
    is EnumIrType -> CGEnumType(name = name)
    is InputObjectIrType -> CGInputObjectType(name = name)
    is ObjectIrType,
    is InterfaceIrType,
    is UnionIrType ->  compoundType ?: error("compoundType is required to build this CGType")
  }.nullable(true)
}

