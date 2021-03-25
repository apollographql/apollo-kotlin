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
    private val packageNameProvider: PackageNameProvider,
) {
  private val allGQLFragmentDefinitions = (metadataFragmentDefinitions + fragmentDefinitions).associateBy { it.name }
  private val enumCache = mutableMapOf<String, IrEnum>()
  private val inputObjectCache = mutableMapOf<String, IrInputObject>()
  private val customScalarCache = mutableMapOf<String, IrCustomScalar>()
  private val inputObjectsToGenerate = mutableListOf<String>()

  private val fieldSetBuilder = IrFieldSetBuilder(
      schema = schema,
      allGQLFragmentDefinitions = allGQLFragmentDefinitions,
      packageNameProvider = packageNameProvider,
      registerType = { gqlType ->
        gqlType.toIr()
      }
  )

  private fun shouldAlwaysGenerate(name: String) = alwaysGenerateTypesMatching.map { Regex(it) }.any { it.matches(name) }

  fun build(): IntermediateRepresentation {
    val operations = operationDefinitions.map { it.toIr() }
    val fragments = allGQLFragmentDefinitions.values.map { it.toIr() }

    // fill in the input object cache before we return
    while (inputObjectsToGenerate.isNotEmpty()) {
      val name = inputObjectsToGenerate.removeAt(0)
      inputObjectCache[name] = (schema.typeDefinition(name) as GQLInputObjectTypeDefinition).toIr()
    }

    return IntermediateRepresentation(
        operations = operations,
        // TODO: multi-module
        fragments = fragments,
        inputObjects = inputObjectCache.values.toList(),
        enums = enumCache.values.toList(),
        customScalars = IrCustomScalars(
            packageName = packageNameProvider.customScalarsPackageName(),
            customScalars = customScalarCache.values.toList()
        )
    )
  }

  private fun GQLScalarTypeDefinition.toIr(): IrCustomScalar {
    return IrCustomScalar(
        name = name,
        kotlinName = customScalarToKotlinName[name],
        packageName = packageNameProvider.customScalarsPackageName(),
        description = description,
        deprecationReason = directives.findDeprecationReason()
    )
  }

  private fun GQLInputObjectTypeDefinition.toIr(): IrInputObject {
    return IrInputObject(
        packageName = packageNameProvider.inputObjectPackageName(name),
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
    val coercedDefaultValue = defaultValue?.coerce(type, schema)?.coercedValue

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
        packageName = packageNameProvider.enumPackageName(name),
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

    check(name != null) {
      "Apollo doesn't support anonymous operation."
    }
    val dataField = fieldSetBuilder.buildOperation(
        selections = selectionSet.selections,
        fieldType = typeDefinition.name,
        name = name,
    )

    return IrOperation(
        name = name,
        description = description,
        operationType = IrOperationType.valueOf(operationType.capitalize()),
        typeCondition = typeDefinition.name,
        variables = variableDefinitions.map { it.toIr() },
        dataField = dataField,
        sourceWithFragments = sourceWithFragments,
        packageName = packageName,
        // TODO: operation Id
        operationId = name
    )
  }

  private fun GQLFragmentDefinition.toIr(): IrNamedFragment {
    val typeDefinition = schema.typeDefinition(typeCondition.name)

    val variableDefinitions = inferVariables(schema, allGQLFragmentDefinitions)

    val packageName = packageNameProvider.fragmentPackageName(sourceLocation.filePath!!)
    return IrNamedFragment(
        name = name,
        description = description,
        filePath = sourceLocation.filePath,
        typeCondition = typeDefinition.name,
        variables = variableDefinitions.map { it.toIr() },
        dataField = fieldSetBuilder.buildFragment(
            selections = selectionSet.selections,
            fieldType = typeDefinition.name,
            name,
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
  private fun GQLType.toIr(): IrType {
    return when (this) {
      is GQLNonNullType -> IrNonNullType(ofType = type.toIr())
      is GQLListType -> IrListType(ofType = type.toIr())
      is GQLNamedType -> when (val typeDefinition = schema.typeDefinition(name)) {
        is GQLScalarTypeDefinition -> {
          when (name) {
            "String" -> IrStringType
            "Boolean" -> IrBooleanType
            "Int" -> IrIntType
            "Float" -> IrFloatType
            "ID" -> IrIdType
            else -> {
              val customScalar = customScalarCache.getOrPut(name) { typeDefinition.toIr() }
              if (customScalar.kotlinName != null) {
                IrCustomScalarType(customScalar = customScalar)
              } else {
                IrAnyType
              }
            }
          }
        }
        is GQLEnumTypeDefinition -> {
          val irEnum = enumCache.getOrPut(name) { typeDefinition.toIr() }
          IrEnumType(enum = irEnum)
        }
        is GQLInputObjectTypeDefinition -> {
          if (inputObjectCache[name] == null) {
            inputObjectsToGenerate.add(name)
          }
          // Compute the input object lazily to break circular references
          IrInputObjectType {
            inputObjectCache[name]!!
          }
        }
        is GQLObjectTypeDefinition,
        is GQLInterfaceTypeDefinition,
        is GQLUnionTypeDefinition -> IrCompoundType
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
