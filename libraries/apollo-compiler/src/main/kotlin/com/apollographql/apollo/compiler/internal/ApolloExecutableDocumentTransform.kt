package com.apollographql.apollo.compiler.internal

import com.apollographql.apollo.ast.GQLDirective
import com.apollographql.apollo.ast.GQLDocument
import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLFragmentSpread
import com.apollographql.apollo.ast.GQLInlineFragment
import com.apollographql.apollo.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo.ast.GQLNamedType
import com.apollographql.apollo.ast.GQLOperationDefinition
import com.apollographql.apollo.ast.GQLSelection
import com.apollographql.apollo.ast.GQLUnionTypeDefinition
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.definitionFromScope
import com.apollographql.apollo.ast.isAbstract
import com.apollographql.apollo.ast.rawType
import com.apollographql.apollo.ast.responseName
import com.apollographql.apollo.ast.rootTypeDefinition
import com.apollographql.apollo.compiler.ADD_TYPENAME_ALWAYS
import com.apollographql.apollo.compiler.ADD_TYPENAME_IF_ABSTRACT
import com.apollographql.apollo.compiler.ADD_TYPENAME_IF_FRAGMENTS
import com.apollographql.apollo.compiler.ADD_TYPENAME_IF_POLYMORPHIC
import com.apollographql.apollo.compiler.ExecutableDocumentTransform
import kotlin.collections.plus

internal class ApolloExecutableDocumentTransform(private val addTypename: String, private val addKeyFields: Boolean) :
  ExecutableDocumentTransform {
  override fun transform(
      schema: Schema,
      document: GQLDocument,
      extraFragmentDefinitions: List<GQLFragmentDefinition>,
  ): GQLDocument {
    val userFragments = mutableListOf<GQLFragmentDefinition>()
    val userOperations = mutableListOf<GQLOperationDefinition>()

    document.definitions.forEach {
      when (it) {
        is GQLFragmentDefinition -> userFragments.add(it)
        is GQLOperationDefinition -> userOperations.add(it)
        else -> Unit
      }
    }
    val fragmentDefinitions = (userFragments + extraFragmentDefinitions).associateBy { it.name }
    val fragments = userFragments.map {
      addRequiredFields(it, schema, fragmentDefinitions)
    }

    val usesDisableErrorPropagation = schema.directiveDefinitions.containsKey(Schema.DISABLE_ERROR_PROPAGATION)
        && schema.schemaDefinition?.directives?.any { schema.originalDirectiveName(it.name) == Schema.CATCH_BY_DEFAULT } == true
    val operations = userOperations.map {
      var operation = addRequiredFields(it, schema, fragmentDefinitions)
      if (usesDisableErrorPropagation) {
        operation = operation.copy(
            directives = operation.directives + GQLDirective(null, Schema.DISABLE_ERROR_PROPAGATION, emptyList())
        )
      }
      operation
    }

    return document.copy(
        definitions = operations + fragments
    )
  }

  internal fun addRequiredFields(
      operation: GQLOperationDefinition,
      schema: Schema,
      fragments: Map<String, GQLFragmentDefinition>,
  ): GQLOperationDefinition {
    val parentType = operation.rootTypeDefinition(schema)!!.name
    return operation.copy(
        selections = operation.selections.addRequiredFields(
            schema = schema,
            fragments = fragments,
            parentType = parentType,
            parentFields = emptySet(),
            isRoot = false,
        )
    )
  }

  internal fun addRequiredFields(
      fragmentDefinition: GQLFragmentDefinition,
      schema: Schema,
      fragments: Map<String, GQLFragmentDefinition>,
  ): GQLFragmentDefinition {
    val newSelectionSet = fragmentDefinition.selections.addRequiredFields(
        schema = schema,
        fragments = fragments,
        parentType = fragmentDefinition.typeCondition.name,
        parentFields = emptySet(),
        isRoot = true,
    )

    return fragmentDefinition.copy(
        selections = newSelectionSet,
    )
  }

  private fun List<GQLSelection>.isPolymorphic(schema: Schema, fragments: Map<String, GQLFragmentDefinition>, rootType: String): Boolean {
    return any {
      when (it) {
        is GQLField -> false
        is GQLInlineFragment -> {
          val tc = it.typeCondition?.name ?: rootType
          !schema.isTypeASuperTypeOf(tc, rootType) || it.selections.isPolymorphic(schema, fragments, rootType)
        }

        is GQLFragmentSpread -> {
          val fragmentDefinition = fragments[it.name] ?: error("cannot find fragment ${it.name}")
          /**
           * If we were only looking at operationBased codegen, we wouldn't need to look inside fragment definitions but responseBased requires
           * the __typename at the root of the field to determine the shape
           */
          !schema.isTypeASuperTypeOf(fragmentDefinition.typeCondition.name, rootType) || fragmentDefinition.selections.isPolymorphic(schema, fragments, rootType)
        }
      }
    }
  }

  /**
   * @param isRoot: whether this selection set is considered a valid root for adding __typename
   * This is the case for field selection sets but also fragments since fragments can be executed from the cache
   */
  private fun List<GQLSelection>.addRequiredFields(
      schema: Schema,
      fragments: Map<String, GQLFragmentDefinition>,
      parentType: String,
      parentFields: Set<String>,
      isRoot: Boolean,
  ): List<GQLSelection> {
    if (isEmpty()) {
      return this
    }

    val selectionSet = this

    val requiresTypename = when (addTypename) {
      ADD_TYPENAME_IF_POLYMORPHIC -> isRoot && isPolymorphic(schema, fragments, parentType)
      ADD_TYPENAME_IF_FRAGMENTS -> {
        selectionSet.any { it is GQLFragmentSpread || it is GQLInlineFragment }
      }

      ADD_TYPENAME_IF_ABSTRACT -> isRoot && schema.typeDefinition(parentType).isAbstract()
      ADD_TYPENAME_ALWAYS -> isRoot
      else -> error("Unknown addTypename option: $addTypename")
    }
    val requiredFieldNames = if (addKeyFields) {
      schema.keyFields(parentType).toMutableSet()
    } else {
      mutableSetOf()
    }

    if (requiredFieldNames.isNotEmpty() || requiresTypename) {
      requiredFieldNames.add("__typename")
    }

    val fieldNames = parentFields + selectionSet.filterIsInstance<GQLField>().map { it.responseName() }.toSet()

    var newSelections = selectionSet.map {
      when (it) {
        is GQLInlineFragment -> {
          it.copy(
              selections = it.selections.addRequiredFields(
                  schema = schema,
                  fragments = fragments,
                  parentType = it.typeCondition?.name ?: parentType,
                  parentFields = fieldNames + requiredFieldNames,
                  isRoot = false
              )
          )
        }

        is GQLFragmentSpread -> it
        is GQLField -> it.addRequiredFields(schema, fragments, parentType)
      }
    }

    val fieldNamesToAdd = (requiredFieldNames - fieldNames).toMutableList()
    if (requiresTypename) {
      /**
       * For "ifFragments", we add the typename always, even if it's already in parentFields
       */
      fieldNamesToAdd.add("__typename")
    }

    newSelections.filterIsInstance<GQLField>().forEach {
      /**
       * Verify that the fields we add won't overwrite an existing alias
       * This is not 100% correct as this validation should be made more globally
       */
      check(!fieldNamesToAdd.contains(it.alias)) {
        "Field ${it.alias}: ${it.name} in $parentType conflicts with key fields"
      }
    }

    newSelections = newSelections + fieldNamesToAdd.map { buildField(it) }

    newSelections = if (requiresTypename) {
      // remove the __typename if it exists
      // and add it again at the top, so we're guaranteed to have it at the beginning of json parsing
      // also remove any @include/@skip directive on __typename
      listOf(buildField("__typename")) + newSelections.filter { (it as? GQLField)?.name != "__typename" }
    } else {
      newSelections
    }

    return newSelections
  }

  private fun GQLField.addRequiredFields(
      schema: Schema,
      fragments: Map<String, GQLFragmentDefinition>,
      parentType: String,
  ): GQLField {
    val typeDefinition = definitionFromScope(schema, parentType)
    if (typeDefinition == null) {
      // This will trigger a validation error later in the build
      return this
    }
    val newSelectionSet = selections.addRequiredFields(
        schema = schema,
        fragments = fragments,
        parentType = typeDefinition.type.rawType().name,
        parentFields = emptySet(),
        isRoot = true
    )

    return copy(
        selections = newSelectionSet,
    )
  }

  private fun buildField(name: String): GQLField {
    return GQLField(
        name = name,
        arguments = emptyList(),
        selections = emptyList(),
        sourceLocation = null,
        directives = emptyList(),
        alias = null,
    )
  }
}