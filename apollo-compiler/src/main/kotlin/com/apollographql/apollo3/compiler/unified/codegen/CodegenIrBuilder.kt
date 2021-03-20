package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.compiler.PackageNameProvider
import com.apollographql.apollo3.compiler.operationoutput.OperationOutput
import com.apollographql.apollo3.compiler.operationoutput.findOperationId
import com.apollographql.apollo3.compiler.unified.IrBooleanType
import com.apollographql.apollo3.compiler.unified.IrCustomScalarType
import com.apollographql.apollo3.compiler.unified.IrEnumType
import com.apollographql.apollo3.compiler.unified.IrFloatType
import com.apollographql.apollo3.compiler.unified.IrIdType
import com.apollographql.apollo3.compiler.unified.IrInputObjectType
import com.apollographql.apollo3.compiler.unified.IrIntType
import com.apollographql.apollo3.compiler.unified.IrInterfaceType
import com.apollographql.apollo3.compiler.unified.IntermediateRepresentation
import com.apollographql.apollo3.compiler.unified.IrEnum
import com.apollographql.apollo3.compiler.unified.IrInputField
import com.apollographql.apollo3.compiler.unified.IrInputObject
import com.apollographql.apollo3.compiler.unified.IrType
import com.apollographql.apollo3.compiler.unified.IrVariable
import com.apollographql.apollo3.compiler.unified.IrListType
import com.apollographql.apollo3.compiler.unified.ImplementationBuilder
import com.apollographql.apollo3.compiler.unified.IrNonNullType
import com.apollographql.apollo3.compiler.unified.IrObjectType
import com.apollographql.apollo3.compiler.unified.IrStringType
import com.apollographql.apollo3.compiler.unified.IrUnionType

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
      val result = ImplementationBuilder(
          irOperation.dataField,
          ir.allNamedFragments,
          path
      ).build()

      val variables = irOperation.variables.map { it.toCg() }
      CgOperation(
          name = irOperation.name,
          description = irOperation.description,
          filePath = irOperation.filePath,
          dataImplementations = result.implementations,
          dataInterfaces = result.interfaces,
          variables = variables,
          dataAdapter = result.adapter,
          variablesAdapter = variables.toCgAdapter(irOperation.name),

          operationType = CgOperation.OperationType.valueOf(irOperation.operationType.name.toUpperCase()),
          // TODO: remove the package name from here
          operationId = operationOutput.findOperationId(irOperation.name, ""),
          operationDocument = irOperation.sourceWithFragments,
      )
    }

    val fragments = ir.allNamedFragments
        .filter { ir.namedFragmentsToGenerate.contains(it) }
        .map { irFragment ->
          val path = ModelPath(irFragment.filePath, ModelPath.Root.Fragment, emptyList())
          val result = ImplementationBuilder(irFragment.dataField, ir.allNamedFragments, path).build()

          val variables = irFragment.variables.map { it.toCg() }
          CgFragment(
              name = irFragment.name,
              description = irFragment.description,
              filePath = irFragment.filePath,
              dataImplementations = result.implementations,
              dataInterfaces = result.interfaces,
              variables = variables,
              dataAdapter = result.adapter,
              variablesAdapter = variables.toCgAdapter(irFragment.name)
          )
        }

    val inputObjectTypes = ir.inputObjects.map { irInputObject ->
      irInputObject.toCg()
    }

    val enumTypes = ir.enums.map { irEnum ->
      irEnum.toCg()
    }

    val customScalarTypes = CgCustomScalars(
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

  private fun IrInputObject.toCg(): CgInputObject {
    return CgInputObject(
        name = name,
        description = description,
        deprecationReason = deprecationReason,
        inputFields = fields.map { irInputField ->
          irInputField.toCg()
        }
    )
  }

  private fun IrEnum.toCg(): CgEnum {
    return CgEnum(
        name = name,
        description = description,
        values = values.map {
          it.toCg()
        }
    )
  }

  private fun IrEnum.Value.toCg(): CgEnum.Value {
    return CgEnum.Value(
        name = name,
        description = description,
        deprecationReason = deprecationReason
    )
  }

  private fun IrInputField.toCg(): CgProperty {
    return CgProperty(
        name = name,
        description = description,
        deprecationReason = deprecationReason,
        // https://spec.graphql.org/draft/#sec-Input-Object-Required-Fields
        type = type.toInputCgType().optional(type !is IrNonNullType || defaultValue != null),
        override = false
    )
  }

  private fun IrVariable.toCg(): CgVariable {
    return CgVariable(
        name = name,
        type = type.toVariableCgType().optional(type !is IrNonNullType || defaultValue != null),
    )
  }
}

fun IrType.toVariableCgType(): CgType {
  return toCg(null)
}

fun IrType.toInputCgType(): CgType {
  return toCg(null)
}

/**
 * If you expect to have a non-scalar output type somewhere, you **must** pass it in [compoundType]
 */
fun IrType.toCg(compoundType: CgType?): CgType {
  if (this is IrNonNullType) {
    return ofType.toCg(compoundType)
  }

  return when (this) {
    is IrNonNullType -> error("") // make the compiler happy, this case is handled as a fast path
    is IrListType -> CgListType(ofType = ofType.toCg(compoundType))
    is IrStringType -> CgStringType()
    is IrFloatType -> CgFloatType()
    is IrIntType -> CgIntType()
    is IrBooleanType -> CgBooleanType()
    is IrIdType -> CgStringType()
    is IrCustomScalarType -> CgCustomScalarType(name = name)
    is IrEnumType -> CgEnumType(name = name)
    is IrInputObjectType -> CgInputObjectType(name = name)
    is IrObjectType,
    is IrInterfaceType,
    is IrUnionType,
    -> compoundType ?: error("compoundType is required to build this CgType")
  }.nullable(true)
}

