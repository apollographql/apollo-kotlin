package com.apollographql.apollo.compiler.frontend.gql

import com.apollographql.apollo.compiler.PackageNameProvider
import com.apollographql.apollo.compiler.frontend.ir.Argument
import com.apollographql.apollo.compiler.frontend.ir.Condition
import com.apollographql.apollo.compiler.frontend.ir.Field
import com.apollographql.apollo.compiler.frontend.ir.Field.Companion.TYPE_NAME_FIELD
import com.apollographql.apollo.compiler.frontend.ir.Fragment
import com.apollographql.apollo.compiler.frontend.ir.FragmentRef
import com.apollographql.apollo.compiler.frontend.ir.InlineFragment
import com.apollographql.apollo.compiler.frontend.ir.Operation
import com.apollographql.apollo.compiler.frontend.ir.TypeDeclaration
import com.apollographql.apollo.compiler.frontend.ir.TypeDeclarationField
import com.apollographql.apollo.compiler.frontend.ir.TypeDeclarationValue
import com.apollographql.apollo.compiler.frontend.ir.Variable
import com.apollographql.apollo.compiler.frontend.ir.SourceLocation as IRSourceLocation

private class SchemaHolder(
    val schema: Schema,
    val fragmentDefinitions: Map<String, GQLFragmentDefinition>,
    val packageNameProvider: PackageNameProvider?
) {

  fun operationToIR(operation: GQLOperationDefinition) = operation.toIR()

  private fun GQLOperationDefinition.toIR(): Operation {
    val fragmentNames = usedFragmentNames(schema, fragmentDefinitions)
    return Operation(
        operationName = name!!,
        packageName = packageNameProvider?.operationPackageName(filePath = sourceLocation.filePath ?: "") ?: "",
        operationType = operationType,
        description = description ?: "",
        variables = variableDefinitions.map { it.toIR() },
        source = toUtf8WithIndents(),
        sourceWithFragments = (toUtf8WithIndents() + "\n" + fragmentNames.map { fragmentDefinitions[it]!!.toUtf8WithIndents() }.joinToString("\n")).trimEnd('\n'),
        fields = selectionSet.selections.filterIsInstance<GQLField>().map { it.toIR(rootTypeDefinition(schema)!!) },
        fragments = selectionSet.selections.filterIsInstance<GQLFragmentSpread>().map { it.toIR() },
        filePath = sourceLocation.filePath ?: "(unknown)",
        fragmentsReferenced = fragmentNames.toList(),
        inlineFragments = selectionSet.selections.filterIsInstance<GQLInlineFragment>().map { it.toIR() }
    )
  }

  private fun GQLVariableDefinition.toIR(): Variable {
    return Variable(
        name = name,
        type = type.pretty(),
        sourceLocation = sourceLocation.toIR()
    )
  }

  private fun SourceLocation.toIR(): IRSourceLocation {
    return IRSourceLocation(
        line = line,
        position = position
    )
  }

  private fun GQLInlineFragment.toIR(): InlineFragment {
    val typeDefinitionInScope = schema.typeDefinition(typeCondition.name)
    val (fields, inlineFragments, namedFragments) = selectionSet.toIR(typeDefinitionInScope)
    return InlineFragment(
        typeCondition = typeCondition.name,
        possibleTypes = schema.typeDefinitions[typeCondition.name]!!.possibleTypes(schema.typeDefinitions).toList(),
        description = "",
        fields = fields,
        inlineFragments = inlineFragments,
        fragments = namedFragments,
        sourceLocation = sourceLocation.toIR(),
        conditions = directives.mapNotNull { it.toIRCondition() }
    )
  }

  private fun GQLSelectionSet.toIR(typeDefinitionInScope: GQLTypeDefinition): Triple<List<Field>, List<InlineFragment>, List<FragmentRef>> {
    val fields = mutableListOf<Field>()
    val inlineFragments = mutableListOf<InlineFragment>()
    val fragments = mutableListOf<FragmentRef>()

    selections.forEach {
      when (it) {
        is GQLField -> fields.add(it.toIR(typeDefinitionInScope))
        is GQLInlineFragment -> inlineFragments.add(it.toIR())
        is GQLFragmentSpread -> fragments.add(it.toIR())
      }
    }

    return Triple(fields, inlineFragments, fragments)
  }

  private fun GQLDirective.toIRCondition(): Condition? {
    if (arguments?.arguments?.size != 1) {
      // skip and include both have only one argument
      return null
    }

    val argument = arguments.arguments.first()

    if (argument.value !is GQLVariableValue) {
      // FIXME: support literal values
      return null
    }

    return when (name) {
      "skip",
      "include" -> Condition(
          kind = "BooleanCondition",
          variableName = argument.value.name,

          inverted = name == "skip",
          sourceLocation = sourceLocation.toIR()
      )
      else -> null // unrecognized directive, skip
    }
  }

  private fun GQLField.toIR(typeDefinitionInScope: GQLTypeDefinition): Field {
    if (name == "__typename") {
      // The codegen compares by reference
      return TYPE_NAME_FIELD
    }
    val fieldDefinition = definitionFromScope(schema, typeDefinitionInScope)!!
    val typeDefinition = schema.typeDefinition(fieldDefinition.type.leafType().name)

    val (fields, inlineFragments, fragments) = selectionSet?.toIR(typeDefinition) ?: Triple(emptyList(), emptyList(), emptyList())
    val deprecationReason = fieldDefinition.directives.findDeprecationReason()
    val conditions = directives.mapNotNull { it.toIRCondition() }
    return Field(
        responseName = responseName(),
        fieldName = name,
        type = fieldDefinition.type.pretty(),
        typeDescription = typeDefinition.description ?: "",
        description = fieldDefinition.description ?: "",
        args = arguments?.arguments?.map { it.toIR(fieldDefinition) } ?: emptyList(),
        isConditional = conditions.isNotEmpty(),
        fields = fields,
        fragmentRefs = fragments,
        inlineFragments = inlineFragments,
        isDeprecated = deprecationReason != null,
        deprecationReason = deprecationReason ?: "",
        conditions = conditions,
        sourceLocation = sourceLocation.toIR()
    )
  }

  private fun GQLArgument.toIR(fieldDefinition: GQLFieldDefinition): Argument {
    val type = fieldDefinition.arguments.first { it.name == name }.type
    return Argument(
        name = name,
        sourceLocation = sourceLocation.toIR(),
        value = value.validateAndCoerce(type, schema, null)
            .orThrow()
            .toKotlinValue(false),
        type = type.pretty()
    )
  }

  private fun GQLFragmentSpread.toIR(): FragmentRef {
    return FragmentRef(
        name = name,
        conditions = directives.mapNotNull { it.toIRCondition() },
        sourceLocation = sourceLocation.toIR()
    )
  }

  fun fragmentToIR(fragmentDefinition: GQLFragmentDefinition): Fragment {
    return fragmentDefinition.toIR()
  }

  fun typeDefinitionToIR(typeDefinition: GQLTypeDefinition): TypeDeclaration {
    return typeDefinition.toIR()
  }

  fun GQLFragmentDefinition.toIR(): Fragment {
    val typeDefinitionInScope = schema.typeDefinition(typeCondition.name)
    val (fields, inlineFragments, fragmentRefs) = selectionSet.toIR(typeDefinitionInScope)
    return Fragment(
        fragmentName = name,
        source = toUtf8WithIndents(),
        description = description ?: "",
        typeCondition = typeCondition.name,
        possibleTypes = schema.typeDefinition(typeCondition.name).possibleTypes(schema.typeDefinitions).toList(),
        fields = fields,
        fragmentRefs = fragmentRefs,
        inlineFragments = inlineFragments,
        filePath = sourceLocation.filePath ?: "(unknown)",
        sourceLocation = sourceLocation.toIR()
    )
  }


  fun GQLInputObjectTypeDefinition.toIR(): TypeDeclaration {
    return TypeDeclaration(
        kind = TypeDeclaration.KIND_INPUT_OBJECT_TYPE,
        name = name,
        description = description ?: "",
        values = emptyList(),
        fields = inputFields.map { it.toIR() }
    )
  }

  private fun GQLInputValueDefinition.toIR(): TypeDeclarationField {
    return TypeDeclarationField(
        name = name,
        description = description ?: "",
        defaultValue = defaultValue?.validateAndCoerce(type, schema, null)
            ?.orThrow()
            ?.toKotlinValue(true),
        type = type.pretty()
    )
  }

  fun GQLTypeDefinition.toIR(): TypeDeclaration {
    return when (this) {
      is GQLInputObjectTypeDefinition -> toIR()
      is GQLEnumTypeDefinition -> toIR()
      else -> throw ConversionException("cannot convert $name to IR")
    }
  }

  fun GQLEnumTypeDefinition.toIR(): TypeDeclaration {
    return TypeDeclaration(
        kind = TypeDeclaration.KIND_ENUM,
        name = name,
        description = description ?: "",
        values = enumValues.map { it.toIR() },
        fields = emptyList()
    )
  }

  private fun GQLEnumValueDefinition.toIR(): TypeDeclarationValue {
    val deprecationReason = directives.findDeprecationReason()
    return TypeDeclarationValue(
        name = name,
        description = description ?: "",
        isDeprecated = deprecationReason != null,
        deprecationReason = deprecationReason ?: ""
    )
  }
}

fun GQLOperationDefinition.toIR(schema: Schema, fragmentDefinitions: Map<String, GQLFragmentDefinition>, packageNameProvider: PackageNameProvider) = SchemaHolder(
    schema,
    fragmentDefinitions,
    packageNameProvider
).operationToIR(this)


fun GQLFragmentDefinition.toIR(schema: Schema, fragmentDefinitions: Map<String, GQLFragmentDefinition>) = SchemaHolder(
    schema,
    fragmentDefinitions,
    null
).fragmentToIR(this)

fun GQLTypeDefinition.toIR(schema: Schema) = SchemaHolder(
    schema,
    emptyMap(),
    null
).typeDefinitionToIR(this)
