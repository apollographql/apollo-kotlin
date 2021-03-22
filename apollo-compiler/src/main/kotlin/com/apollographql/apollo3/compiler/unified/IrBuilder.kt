package com.apollographql.apollo3.compiler.unified

import com.apollographql.apollo3.compiler.PackageNameProvider
import com.apollographql.apollo3.compiler.frontend.GQLBooleanValue
import com.apollographql.apollo3.compiler.frontend.GQLDirective
import com.apollographql.apollo3.compiler.frontend.GQLEnumTypeDefinition
import com.apollographql.apollo3.compiler.frontend.GQLEnumValue
import com.apollographql.apollo3.compiler.frontend.GQLEnumValueDefinition
import com.apollographql.apollo3.compiler.frontend.GQLFloatValue
import com.apollographql.apollo3.compiler.frontend.GQLFragmentDefinition
import com.apollographql.apollo3.compiler.frontend.GQLInputObjectTypeDefinition
import com.apollographql.apollo3.compiler.frontend.GQLInputValueDefinition
import com.apollographql.apollo3.compiler.frontend.GQLIntValue
import com.apollographql.apollo3.compiler.frontend.GQLInterfaceTypeDefinition
import com.apollographql.apollo3.compiler.frontend.GQLListType
import com.apollographql.apollo3.compiler.frontend.GQLListValue
import com.apollographql.apollo3.compiler.frontend.GQLNamedType
import com.apollographql.apollo3.compiler.frontend.GQLNonNullType
import com.apollographql.apollo3.compiler.frontend.GQLNullValue
import com.apollographql.apollo3.compiler.frontend.GQLObjectTypeDefinition
import com.apollographql.apollo3.compiler.frontend.GQLObjectValue
import com.apollographql.apollo3.compiler.frontend.GQLOperationDefinition
import com.apollographql.apollo3.compiler.frontend.GQLScalarTypeDefinition
import com.apollographql.apollo3.compiler.frontend.GQLStringValue
import com.apollographql.apollo3.compiler.frontend.GQLType
import com.apollographql.apollo3.compiler.frontend.GQLUnionTypeDefinition
import com.apollographql.apollo3.compiler.frontend.GQLValue
import com.apollographql.apollo3.compiler.frontend.GQLVariableDefinition
import com.apollographql.apollo3.compiler.frontend.GQLVariableValue
import com.apollographql.apollo3.compiler.frontend.InputValueScope
import com.apollographql.apollo3.compiler.frontend.Schema
import com.apollographql.apollo3.compiler.frontend.coerce
import com.apollographql.apollo3.compiler.frontend.findDeprecationReason
import com.apollographql.apollo3.compiler.frontend.inferVariables
import com.apollographql.apollo3.compiler.frontend.rootTypeDefinition
import com.apollographql.apollo3.compiler.frontend.toUtf8
import com.apollographql.apollo3.compiler.frontend.toUtf8WithIndents

class IrBuilder(
    private val schema: Schema,
    private val operationDefinitions: List<GQLOperationDefinition>,
    private val fragmentDefinitions: List<GQLFragmentDefinition>,
    metadataFragmentDefinitions: List<GQLFragmentDefinition>,
    private val alwaysGenerateTypesMatching: Set<String>,
    private val customScalarToKotlinName: Map<String, String>,
    private val packageNameProvider: PackageNameProvider
) {
  private val allGQLFragmentDefinitions = (metadataFragmentDefinitions + fragmentDefinitions).associateBy { it.name }
  private var usedEnums = mutableSetOf<String>()
  private var usedInputObjects = mutableSetOf<String>()
  private var usedCustomScalars = mutableSetOf<String>()

  private val fieldSetBuilder = IrFieldSetBuilder(
      schema = schema,
      allGQLFragmentDefinitions = allGQLFragmentDefinitions,
      registerType = { gqlType, modelPath ->
        gqlType.toIr(modelPath)
      }
  )

  private fun shouldAlwaysGenerate(name: String) = alwaysGenerateTypesMatching.map { Regex(it) }.any { it.matches(name) }

  fun build(): IntermediateRepresentation {
    val operations = operationDefinitions.map { it.toIr() }
    val allNamedFragments = allGQLFragmentDefinitions.values.map { it.toIr() }

    val enums = schema.typeDefinitions.values
        .filterIsInstance<GQLEnumTypeDefinition>()
        .filter { usedEnums.contains(it.name) || shouldAlwaysGenerate(it.name) }
        .map { it.toIr() }

    val inputObjects = schema.typeDefinitions.values
        .filterIsInstance<GQLInputObjectTypeDefinition>()
        .filter { usedInputObjects.contains(it.name) || shouldAlwaysGenerate(it.name) }
        .map { it.toIr() }

    val customScalars = schema.typeDefinitions.values
        .filterIsInstance<GQLScalarTypeDefinition>()
        .filter { !it.isBuiltIn() }
        .filter { usedCustomScalars.contains(it.name) || shouldAlwaysGenerate(it.name) }
        .map { it.toIr() }

    return IntermediateRepresentation(
        operations = operations,
        allNamedFragments = allNamedFragments,
        namedFragmentsToGenerate = fragmentDefinitions.map { it.name }.toSet(),
        inputObjects = inputObjects,
        enums = enums,
        customScalars = customScalars
    )
  }

  private fun GQLScalarTypeDefinition.toIr(): IrCustomScalar {
    return IrCustomScalar(name = name, customScalarToKotlinName[name] ?: "kotlin.Any")
  }

  private fun GQLInputObjectTypeDefinition.toIr(): IrInputObject {
    return IrInputObject(
        name = name,
        description = description,
        deprecationReason = directives.findDeprecationReason(),
        fields = inputFields.map { it.toIrInputField() }
    )
  }

  /**
   * This is not named `toIr` as [GQLInputValueDefinition] also maps to variables and arguments
   */
  private fun GQLInputValueDefinition.toIrInputField(): IrInputField {
    val coercedDefaultValue = defaultValue?.coerce(type, schema)?.orThrow()

    return IrInputField(
        name = name,
        description = description,
        deprecationReason = directives.findDeprecationReason(),
        type = type.toIr(),
        defaultValue = coercedDefaultValue?.toIr(),
        optional = type !is GQLNonNullType || coercedDefaultValue != null
    )
  }

  private fun GQLEnumTypeDefinition.toIr(): IrEnum {
    return IrEnum(
        name = name,
        description = description,
        values = enumValues.map { it.toIr() }
    )
  }

  private fun GQLEnumValueDefinition.toIr(): IrEnum.Value {
    return IrEnum.Value(
        name = name,
        description = description,
        deprecationReason = directives.findDeprecationReason()
    )
  }

  private fun GQLOperationDefinition.toIr(): IrOperation {
    val typeDefinition = this.rootTypeDefinition(schema)
        ?: throw IllegalStateException("ApolloGraphql: cannot find root type for '$operationType'")

    val usedNamedFragments = emptyList<String>()

    val sourceWithFragments = (toUtf8WithIndents() + "\n" + usedNamedFragments.joinToString(
        separator = "\n"
    ) { fragmentName ->
      allGQLFragmentDefinitions[fragmentName]!!.toUtf8WithIndents()
    }).trimEnd('\n')

    val packageName = packageNameProvider.operationPackageName(sourceLocation.filePath!!)
    return IrOperation(
        name = name ?: throw IllegalStateException("Apollo doesn't support anonymous operation."),
        description = description,
        operationType = IrOperationType.valueOf(operationType.capitalize()),
        typeCondition = typeDefinition.name,
        variables = variableDefinitions.map { it.toIr() },
        dataField = fieldSetBuilder.buildOperation(
            IrFieldSetBuilder.TypedSelectionSet(selectionSet.selections, typeDefinition.name),
            packageName = packageName
        ),
        sourceWithFragments = sourceWithFragments,
        packageName = packageName
    )
  }

  private fun GQLFragmentDefinition.toIr(): IrNamedFragment {
    val typeDefinition = schema.typeDefinition(typeCondition.name)

    val variableDefinitions = inferVariables(schema, allGQLFragmentDefinitions)

    val packageName = packageNameProvider.fragmentPackageName("unused")
    return IrNamedFragment(
        name = name,
        description = description,
        filePath = sourceLocation.filePath!!,
        typeCondition = typeDefinition.name,
        variables = variableDefinitions.map { it.toIr() },
        dataField =fieldSetBuilder.buildFragment(
            name,
            IrFieldSetBuilder.TypedSelectionSet(selectionSet.selections, typeDefinition.name),
            packageName = packageName
        ),
        packageName = packageName
    )
  }

  private fun InputValueScope.VariableReference.toIr(): IrVariable {
    return IrVariable(
        name = this.variable.name,
        defaultValue = null,
        type = expectedType.toIr(),
        optional = expectedType !is GQLNonNullType
    )
  }

  private fun GQLVariableDefinition.toIr(): IrVariable {
    val coercedDefaultValue = defaultValue?.coerce(type, schema)?.orThrow()

    return IrVariable(
        name = name,
        defaultValue = coercedDefaultValue?.toIr(),
        type = type.toIr(),
        optional = type !is GQLNonNullType || coercedDefaultValue != null
    )
  }

  /**
   * Maps to [IrType] and also keep tracks of what types are actually used so we only generate those
   */
  private fun GQLType.toIr(modelPath: ModelPath? = null): IrType {
    return when (this) {
      is GQLNonNullType -> IrNonNullType(ofType = type.toIr())
      is GQLListType -> IrListType(ofType = type.toIr())
      is GQLNamedType -> when (schema.typeDefinition(name)) {
        is GQLScalarTypeDefinition -> {
          when (name) {
            "String" -> IrStringType
            "Boolean" -> IrBooleanType
            "Int" -> IrIntType
            "Float" -> IrFloatType
            "ID" -> IrIdType
            else -> {
              val kotlinName = customScalarToKotlinName.get(name)
              if (kotlinName != null) {
                usedCustomScalars.add(name)
                IrCustomScalarType(
                    name,
                    kotlinName,
                    packageNameProvider.customScalarsPackageName()
                )
              } else {
                IrAnyType
              }
            }
          }
        }
        is GQLEnumTypeDefinition -> {
          usedEnums.add(name)
          IrEnumType(
              name,
              packageNameProvider.enumPackageName(name)
          )
        }
        is GQLObjectTypeDefinition,
        is GQLInterfaceTypeDefinition,
        is GQLUnionTypeDefinition -> IrCompoundType(name, modelPath ?: error("Compound object $name needs a modelPath"))
        is GQLInputObjectTypeDefinition -> {
          usedInputObjects.add(name)
          IrInputObjectType(
              name,
              packageNameProvider.inputObjectPackageName(name)
          )
        }
      }
    }
  }
}

internal fun GQLValue.toIr(): IrValue {
  return when (this) {
    is GQLIntValue -> IrIntValue(value = value)
    is GQLStringValue -> IrStringValue(value = value)
    is GQLFloatValue -> IrFloatValue(value = value)
    is GQLBooleanValue -> IrBooleanValue(value = value)
    is GQLEnumValue -> IrEnumValue(value = value)
    is GQLNullValue -> IrNullValue
    is GQLVariableValue -> IrVariableValue(name = name)
    is GQLListValue -> IrListValue(values = values.map { it.toIr() })
    is GQLObjectValue -> IrObjectValue(
        fields = fields.map {
          IrObjectValue.Field(name = it.name, value = it.value.toIr())
        }
    )
  }
}

/**
 * This is guaranteed to return one of:
 * - True
 * - False
 * - (!)Variable
 * - (!)Variable & (!)Variable
 *
 */
internal fun List<GQLDirective>.toBooleanExpression(): BooleanExpression {
  val conditions = mapNotNull {
    it.toBooleanExpression()
  }
  return if (conditions.isEmpty()) {
    BooleanExpression.True
  } else {
    check(conditions.toSet().size == conditions.size) {
      "ApolloGraphQL: duplicate @skip/@include directives are not allowed"
    }
    // Having both @skip and @include is allowed
    // In that case, it's equivalent to a "And"
    // See https://spec.graphql.org/draft/#sec--include
    BooleanExpression.And(conditions.toSet()).simplify()
  }
}

internal fun GQLDirective.toBooleanExpression(): BooleanExpression? {
  if (setOf("skip", "include").contains(name).not()) {
    // not a condition directive
    return null
  }
  if (arguments?.arguments?.size != 1) {
    throw IllegalStateException("ApolloGraphQL: wrong number of arguments for '$name' directive: ${arguments?.arguments?.size}")
  }

  val argument = arguments.arguments.first()

  return when (val value = argument.value) {
    is GQLBooleanValue -> {
      if (value.value) BooleanExpression.True else BooleanExpression.False
    }
    is GQLVariableValue -> BooleanExpression.Variable(
        name = value.name,
    ).let {
      if (name == "skip") it.not() else it
    }
    else -> throw IllegalStateException("ApolloGraphQL: cannot pass ${value.toUtf8()} to '$name' directive")
  }
}
