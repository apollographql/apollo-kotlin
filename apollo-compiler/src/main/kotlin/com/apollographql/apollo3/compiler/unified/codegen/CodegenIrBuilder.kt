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
      val result = ModelBuilder(irOperation.dataField).build()

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
          // TODO: remove the the package name from here
          operationId = operationOutput.findOperationId(irOperation.name, ""),
          operationDocument = irOperation.sourceWithFragments,
      )
    }

    val fragments = ir.namedFragments.map { irFragment ->
      val result = ModelBuilder(irFragment.dataField).build()

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
  return toCgUnsafe(null)
}

fun IrType.toInputCgType(): CGType {
  return toCgUnsafe(null)
}

fun IrType.toCgUnsafe(leafModelType: CGType?): CGType {
  if (this is NonNullIrType) {
    return ofType.toCgUnsafe(leafModelType)
  }

  return when (this) {
    is NonNullIrType -> error("") // make the compiler happy, this case is handled as a fast path
    is ListIrType -> CGListType(ofType = ofType.toCgUnsafe(leafModelType))
    is StringIrType -> CGStringType()
    is FloatIrType -> CGFloatType()
    is IntIrType -> CGIntType()
    is BooleanIrType -> CGBooleanType()
    is IdIrType -> CGStringType()
    is CustomScalarIrType -> CGCustomScalarType(name = name)
    is EnumIrType -> CGEnumType(name = name)
    is InputObjectIrType -> CGInputObjectType(name = name)
    is ObjectIrType -> leafModelType ?: error("A modelType is required to build this CGType")
    is InterfaceIrType ->  leafModelType ?: error("A modelType is required to build this CGType")
    is UnionIrType ->  leafModelType ?: error("A modelType is required to build this CGType")
  }.nullable(true)
}

