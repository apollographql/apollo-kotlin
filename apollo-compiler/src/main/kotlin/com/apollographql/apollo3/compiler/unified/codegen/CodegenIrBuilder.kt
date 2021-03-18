package com.apollographql.apollo3.compiler.unified.codegen

import com.apollographql.apollo3.compiler.PackageNameProvider
import com.apollographql.apollo3.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo3.compiler.operationoutput.OperationOutput
import com.apollographql.apollo3.compiler.operationoutput.findOperationId
import com.apollographql.apollo3.compiler.unified.AstModelBuilder
import com.apollographql.apollo3.compiler.unified.BooleanIrType
import com.apollographql.apollo3.compiler.unified.CustomScalarIrType
import com.apollographql.apollo3.compiler.unified.EnumIrType
import com.apollographql.apollo3.compiler.unified.FloatIrType
import com.apollographql.apollo3.compiler.unified.IdIrType
import com.apollographql.apollo3.compiler.unified.InputObjectIrType
import com.apollographql.apollo3.compiler.unified.IntIrType
import com.apollographql.apollo3.compiler.unified.InterfaceIrType
import com.apollographql.apollo3.compiler.unified.IntermediateRepresentation
import com.apollographql.apollo3.compiler.unified.IrCustomScalar
import com.apollographql.apollo3.compiler.unified.IrEnum
import com.apollographql.apollo3.compiler.unified.IrEnumValue
import com.apollographql.apollo3.compiler.unified.IrInputField
import com.apollographql.apollo3.compiler.unified.IrInputObject
import com.apollographql.apollo3.compiler.unified.IrNamedFragment
import com.apollographql.apollo3.compiler.unified.IrOperation
import com.apollographql.apollo3.compiler.unified.IrOperationType
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

      val variables = irOperation.variables.map { it.toCodegenIr() }
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

      val variables = irFragment.variables.map { it.toCodegenIr() }
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
      irInputObject.toCodegenIr()
    }

    val enumTypes = ir.enums.map { irEnum ->
      irEnum.toCodegenIr()
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

  private fun IrCustomScalar.toCodegenIr(): CodeGenerationAst.CustomScalarType {
    return CodeGenerationAst.CustomScalarType(
        name = name,
        schemaType = name,
        mappedType = customScalarsMapping[name]!!,
    )
  }

  private fun IrInputObject.toCodegenIr(): CGInputObject {
    return CGInputObject(
        name = name,
        description = description,
        deprecationReason = deprecationReason,
        inputFields = fields.map { irInputField ->
          irInputField.toCodegenIr()
        }
    )
  }

  private fun IrEnum.toCodegenIr(): CGEnum {
    return CGEnum(
        name = name,
        description = description,
        values = values.map {
          it.toCodegenIr()
        }
    )
  }

  private fun IrEnumValue.toCodegenIr(): CGEnumValue {
    return CGEnumValue(
        name = name,
        description = description,
        deprecationReason = deprecationReason
    )
  }

  private fun IrInputField.toCodegenIr(): CGProperty {
    return CGProperty(
        name = name,
        description = description,
        deprecationReason = deprecationReason,
        type = type.toCodegenIr(null),
        // https://spec.graphql.org/draft/#sec-Input-Object-Required-Fields
        isRequired = type is NonNullIrType && defaultValue == null,
    )
  }

  private fun IrOperation.toCodegenIr(): CGOperation {
    val packageName = packageNameProvider.operationPackageName(filePath)
    return CGOperation(
        name = name,
        filePath = filePath,
        type = when (operationType) {
          IrOperationType.Query -> CGOperation.Type.QUERY
          IrOperationType.Mutation -> CGOperation.Type.MUTATION
          IrOperationType.Subscription -> CGOperation.Type.SUBSCRIPTION
        },
        description = description,
        operationId = operationOutput.findOperationId(name, packageName),
        operationDocument = sourceWithFragments,
        variables = variables.map { it.toCodegenIr() },
        dataImplementation = AstModelBuilder(dataField).build().first() as CGImplementation
    )
  }

  private fun IrNamedFragment.toCodegenIr(): Pair<CGFragmentInterface, CGFragmentImplementation> {
    val models = AstModelBuilder(dataField).build()
    return CGInterface(
        interfaces = models.filterIsInstance<CGInterface>()
    )
  }

  private fun IrVariable.toCodegenIr(): CGVariable {
    return CGVariable(
        name = name,
        type = type.toCodegenIr(),
        isRequired = type is NonNullIrType && defaultValue == null
    )
  }

  private fun IrType.toCodegenIr(filePath: String? = null, modelPath: ModelPath? = null): CGType {
    return when (this) {
      is NonNullIrType -> ofType.toCodegenIr(enclosingTypeRef).nonNullable()
      is ListIrType -> CodeGenerationAst.FieldType.Array(nullable = true, rawType = ofType.toCodegenIr(enclosingTypeRef))
      is StringIrType -> CodeGenerationAst.FieldType.Scalar.String(nullable = true)
      is FloatIrType -> CodeGenerationAst.FieldType.Scalar.Float(nullable = true)
      is IntIrType -> CodeGenerationAst.FieldType.Scalar.Int(nullable = true)
      is BooleanIrType -> CodeGenerationAst.FieldType.Scalar.Boolean(nullable = true)
      is IdIrType -> CodeGenerationAst.FieldType.Scalar.String(nullable = true)
      is CustomScalarIrType -> CodeGenerationAst.FieldType.Scalar.Custom(
          nullable = true,
          schemaTypeName = name,
          type = customScalarsMapping[name]!!,
          typeRef = CodeGenerationAst.TypeRef(
              packageName = typesPackageName,
              enclosingType = null,
              name = name,
              isNamedFragmentDataRef = false
          )
      )
      is EnumIrType -> CodeGenerationAst.FieldType.Scalar.Enum(
          nullable = true,
          schemaTypeName = name,
          typeRef = CodeGenerationAst.TypeRef(
              packageName = typesPackageName,
              name = name,
              isNamedFragmentDataRef = false
          )
      )
      is ObjectIrType -> CodeGenerationAst.FieldType.Object(
          nullable = true,
          schemaTypeName = name,
          typeRef = CodeGenerationAst.TypeRef(
              packageName = fragmentsPackage,
              name = name,
              enclosingType = enclosingTypeRef,
              isNamedFragmentDataRef = false
          )
      )
      is InterfaceIrType -> CodeGenerationAst.FieldType.Object(
          nullable = true,
          schemaTypeName = name,
          typeRef = CodeGenerationAst.TypeRef(
              packageName = fragmentsPackage,
              name = name,
              enclosingType = enclosingTypeRef,
              isNamedFragmentDataRef = false
          )
      )
      is UnionIrType -> CodeGenerationAst.FieldType.Object(
          nullable = true,
          schemaTypeName = name,
          typeRef = CodeGenerationAst.TypeRef(
              packageName = fragmentsPackage,
              name = name,
              enclosingType = enclosingTypeRef,
              isNamedFragmentDataRef = false
          )
      )
      is InputObjectIrType -> CodeGenerationAst.FieldType.InputObject(
          nullable = true,
          schemaTypeName = name,
          typeRef = CodeGenerationAst.TypeRef(
              packageName = typesPackageName,
              name = name,
              isNamedFragmentDataRef = false
          )
      )
    }
  }
}

