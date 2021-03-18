package com.apollographql.apollo3.compiler.unified

import com.apollographql.apollo3.compiler.PackageNameProvider
import com.apollographql.apollo3.compiler.backend.ast.CodeGenerationAst
import com.apollographql.apollo3.compiler.operationoutput.OperationOutput
import com.apollographql.apollo3.compiler.operationoutput.findOperationId

class AstBuilder(
    private val ir: IntermediateRepresentation,
    private val customScalarsMapping: Map<String, String>,
    private val typesPackageName: String,
    private val fragmentsPackage: String,
    private val packageNameProvider: PackageNameProvider,
    private val operationOutput: OperationOutput,
    private val generateFragmentsAsInterfaces: Boolean,
) {

  internal fun build(): CodeGenerationAst {

    val operationTypes = ir.operations.map { irOperation ->
      irOperation.toAst()
    }

    val fragmentInterfaces = ir.namedFragments.map { irFragment ->
      irFragment.toAstFragmentInterfaces()
    }

    val inputObjectTypes = ir.inputObjects.map { irInputObject ->
      irInputObject.toAst()
    }

    val enumTypes = ir.enums.map { irEnum ->
      irEnum.toAst()
    }

    val customScalarTypes = ir.customScalars.map { irCustomScalar ->
      irCustomScalar.toAst()
    }.associateBy { it.name }

    return CodeGenerationAst(
        operationTypes = emptyList(),
        fragmentTypes = emptyList(),
        inputTypes = inputObjectTypes,
        enumTypes = enumTypes,
        customScalarTypes = customScalarTypes,
        extOperations = operationTypes,
        extFragmentInterfaces = fragmentInterfaces
    )
  }

  private fun IrCustomScalar.toAst(): CodeGenerationAst.CustomScalarType {
    return CodeGenerationAst.CustomScalarType(
        name = name,
        schemaType = name,
        mappedType = customScalarsMapping[name]!!,
    )
  }

  private fun IrInputObject.toAst(): CodeGenerationAst.InputType {
    return CodeGenerationAst.InputType(
        name = name,
        graphqlName = name,
        description = description ?: "",
        deprecationReason = deprecationReason,
        fields = fields.map { irInputField ->
          irInputField.toAst()
        }
    )
  }

  private fun IrEnum.toAst(): CodeGenerationAst.EnumType {
    return CodeGenerationAst.EnumType(
        name = name,
        graphqlName = name,
        description = description ?: "",
        consts = values.map {
          it.toAst()
        }
    )
  }

  private fun IrEnumValue.toAst(): CodeGenerationAst.EnumConst {
    return CodeGenerationAst.EnumConst(
        constName = name,
        value = name,
        description = description ?: "",
        deprecationReason = deprecationReason
    )
  }

  private fun IrInputField.toAst(): CodeGenerationAst.InputField {
    return CodeGenerationAst.InputField(
        name = name,
        schemaName = name,
        description = description ?: "",
        deprecationReason = deprecationReason,
        type = type.toAst(null),
        // https://spec.graphql.org/draft/#sec-Input-Object-Required-Fields
        isRequired = type is NonNullIrType && defaultValue == null,
    )
  }

  private fun IrOperation.toAst(): AstExtOperation {
    val packageName = packageNameProvider.operationPackageName(filePath)
    return AstExtOperation(
        name = name,
        filePath = filePath,
        type = when (operationType) {
          IrOperationType.Query -> AstExtOperation.Type.QUERY
          IrOperationType.Mutation -> AstExtOperation.Type.MUTATION
          IrOperationType.Subscription -> AstExtOperation.Type.SUBSCRIPTION
        },
        description = description ?: "",
        operationId = operationOutput.findOperationId(name, packageName),
        operationDocument = sourceWithFragments,
        variables = variables.map { it.toAst() },
        dataImplementation = AstModelBuilder(dataField).build().first() as AstExtImplementation
    )
  }

  private fun IrNamedFragment.toAstFragmentInterfaces(): AstExtFragmentInterfaces {
    val models = AstModelBuilder(dataField).build()
    return AstExtFragmentInterfaces(
        interfaces = models.filterIsInstance<AstExtInterface>()
    )
  }

  private fun IrVariable.toAst(): CodeGenerationAst.InputField {
    return CodeGenerationAst.InputField(
        name = name,
        schemaName = name,
        description = "", // no description on variables
        deprecationReason = null, // no deprecation on variables
        type = type.toAst(),
        isRequired = type is NonNullIrType && defaultValue == null
    )
  }

  private fun IrType.toAst(enclosingTypeRef: CodeGenerationAst.TypeRef? = null): CodeGenerationAst.FieldType {
    return when (this) {
      is NonNullIrType -> ofType.toAst(enclosingTypeRef).nonNullable()
      is ListIrType -> CodeGenerationAst.FieldType.Array(nullable = true, rawType = ofType.toAst(enclosingTypeRef))
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
      is UnionIrType ->  CodeGenerationAst.FieldType.Object(
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

