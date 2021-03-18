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
        dataImplementation = dataField.toAstModels(emptyList(), false).first() as AstExtImplementation
    )
  }

  private fun IrNamedFragment.toAstFragmentInterfaces(): AstExtFragmentInterfaces {
    val interfaces = dataField.toAstModels()
    return AstExtFragmentInterfaces(
        name = name,
        description = description ?: "",
        deprecationReason = null,
        fields = dataField.
    )
  }


  private fun IrField.toAstField(): AstExtField {
    return AstExtField(
        name = name,
        type = type.toAst(),
        override =
    )
  }

  /**
   * @param forceInterfaces generate all models as interfaces. Used for fragments interfaces when we know there will be no data class
   *
   * @param neighbourModels a list of interfaces from other merged fields that we need to implement too to conform to the base interface
   */
  private inner class FieldScope(
      val field: IrField,
      val path: List<String>,
      val forceInterfaces: Boolean,
      val neighbourModels: List<AstExtInterface>,
  ) {
    private val models = mutableMapOf<TypeSet, AstExtModel>()

    /**
     * Hopefully not re-entrant
     */
    private fun getOrBuildSet(fieldSet: IrFieldSet): AstExtModel {
      return models.getOrPut(fieldSet.typeSet) {
        buildSet(fieldSet)
      }
    }

    private fun buildSet(fieldSet: IrFieldSet): AstExtModel {
      val interfaces = field.fieldSets.filter {
        it.possibleTypes.isEmpty() && fieldSet.typeSet.implements(it.typeSet)
      }.sortedByDescending {
        // put the most qualified interface first in the list
        it.typeSet.size
      }.map {
        getOrBuildSet(it)
      }

      val selfPath = path + modelName(fieldSet.typeSet, field.responseName)

      if (fieldSet.possibleTypes.isEmpty() || forceInterfaces) {
        AstExtInterface(
            path = selfPath,
            description = field.description ?: "",
            deprecationReason = field.deprecationReason,
            fields = fieldSet.fields.map { it.toAstField() },
            nestedModels = fieldSet.fields.flatMap { childField ->
              val neighbourModels = interfaces.mapNotNull { superInterface ->
                superInterface.nestedModels.firstOrNull { it.path.last() == childField.responseName }
              }
              childField.toAstModels(
                  selfPath,
                  forceInterfaces,
                  interfaces.flatMap {
                    it.fields.filter { it.name == }
                  }
              )
            },
            implements = interfaces
        )
      } else {
        AstExtImplementation(
            name = name,
            description = description ?: "",
            deprecationReason = deprecationReason,
            typeRef = CodeGenerationAst.TypeRef(
                packageName = packageName,
                name = name,
                enclosingType = enclosingTypeRef,
                isNamedFragmentDataRef = false
            ),
        )
      }
    }
  }

  private fun IrField.toAstModels(
      path: List<String>,
      forceInterfaces: Boolean,
  ): List<AstExtModel> {
    return fieldSets.map {

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

  companion object {
    private fun modelName(typeSet: TypeSet, responseName: String): String {
      return (typeSet.sorted() + responseName).map { it.capitalize() }.joinToString("")
    }
  }
}

