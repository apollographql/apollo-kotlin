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
        operationTypes = operationTypes,
        inputTypes = inputObjectTypes,
        enumTypes = enumTypes,
        customScalarTypes = customScalarTypes,
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

  private fun IrEnumValue.toAst() : CodeGenerationAst.EnumConst {
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
        schemaName =  name,
        description = description ?: "",
        deprecationReason =  deprecationReason,
        type = type.toAst(null),
        // https://spec.graphql.org/draft/#sec-Input-Object-Required-Fields
        isRequired = type is NonNullIrType  && defaultValue == null,
    )
  }

  private fun IrOperation.toAst(): CodeGenerationAst.OperationType {
    val packageName = packageNameProvider.operationPackageName(filePath)

    return CodeGenerationAst.OperationType(
        name = name,
        packageName = packageName,
        type = when (operationType) {
          IrOperationType.Query -> CodeGenerationAst.OperationType.Type.QUERY
          IrOperationType.Mutation -> CodeGenerationAst.OperationType.Type.MUTATION
          IrOperationType.Subscription -> CodeGenerationAst.OperationType.Type.SUBSCRIPTION
        },
        operationName = name,
        description = description ?: "",
        operationId = operationOutput.findOperationId(name, packageName),
        queryDocument = sourceWithFragments,
        variables = variables.map { it.toAst() },
        dataType = dataField.toAst(packageName, null).first()
    )
  }

  private fun IrField.toAst(
      packageName: String,
      enclosingTypeRef: CodeGenerationAst.TypeRef?
  ): List<CodeGenerationAst.ObjectType> {

    return fieldSets.map {
      CodeGenerationAst.ObjectType(
          name = name,
          description = description ?: "",
          deprecationReason = deprecationReason,
          typeRef = CodeGenerationAst.TypeRef(
              packageName = packageName,
              name = name,
              enclosingType = enclosingTypeRef,
              isNamedFragmentDataRef = false
          ),
          schemaTypename = name,
          isShape = it.possibleTypes.isNotEmpty(),
          abstract =

      )
    }
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
      // We should certainly have a param to decide how to map the Id type
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
              isNamedFragmentDataRef =  false
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
      is InterfaceIrType -> TODO()
      is UnionIrType -> TODO()
    }
  }
}

