package com.apollographql.apollo.compiler.backend.ir

import com.apollographql.apollo.compiler.backend.ir.FieldMergeUtils.mergeFields
import com.apollographql.apollo.compiler.backend.ir.SelectionKeyUtils.addFieldSelectionKey
import com.apollographql.apollo.compiler.backend.ir.SelectionKeyUtils.addFieldSelectionKeys
import com.apollographql.apollo.compiler.frontend.ir.CodeGenerationIR
import com.apollographql.apollo.compiler.frontend.ir.Condition
import com.apollographql.apollo.compiler.frontend.ir.Field
import com.apollographql.apollo.compiler.frontend.ir.Fragment
import com.apollographql.apollo.compiler.frontend.ir.FragmentRef
import com.apollographql.apollo.compiler.frontend.ir.InlineFragment
import com.apollographql.apollo.compiler.frontend.ir.Operation
import com.apollographql.apollo.compiler.frontend.ir.SourceLocation
import com.apollographql.apollo.compiler.introspection.IntrospectionSchema
import com.apollographql.apollo.compiler.introspection.possibleTypes
import com.apollographql.apollo.compiler.introspection.resolveType

internal class BackendIrBuilder private constructor(
    private val schema: IntrospectionSchema,
    private val namedFragments: List<Fragment>,
    private val useSemanticNaming: Boolean,
) {
  companion object {

    fun CodeGenerationIR.buildBackendIr(
        schema: IntrospectionSchema,
        useSemanticNaming: Boolean,
    ): BackendIr {
      return BackendIrBuilder(
          schema = schema,
          namedFragments = this.fragments,
          useSemanticNaming = useSemanticNaming,
      ).buildBackendIR(this)
    }
  }

  private fun buildBackendIR(frontendIr: CodeGenerationIR): BackendIr {
    return BackendIr(
        operations = frontendIr.operations.map { operation ->
          operation.buildBackendIrOperation()
        },
        fragments = frontendIr.fragments
            .filter { fragment ->
              fragment.fragmentName in frontendIr.fragmentsToGenerate
            }
            .map { fragment ->
              fragment.buildBackendIrNamedFragment()
            },
        typeDeclarations = frontendIr.typeDeclarations
            .map { typeDeclaration -> schema.resolveType(typeDeclaration.name) }
            .distinct(),
        typesPackageName = frontendIr.typesPackageName,
        fragmentsPackageName = frontendIr.fragmentsPackageName,
    )
  }

  private fun Operation.buildBackendIrOperation(): BackendIr.Operation {
    val normalizedName = this.normalizeOperationName()
    val typeRef = this.operationTypeRef
    val selectionKey = SelectionKey(
        root = normalizedName,
        keys = listOf(normalizedName, "data"),
        type = SelectionKey.Type.Query,
    )
    val dataField = Field(
        fieldName = "data",
        responseName = "data",
        type = typeRef.name!!,
        fields = this.fields,
        inlineFragments = this.inlineFragments,
        fragmentRefs = this.fragments,
        typeDescription = "",
        sourceLocation = SourceLocation.UNKNOWN
    ).buildBackendIrField(
        selectionKey = selectionKey,
        generateFragmentImplementations = true,
    )
    val variables = this.variables.map { variable ->
      BackendIr.Variable(
          name = variable.name,
          type = schema.resolveType(variable.type)
      )
    }
    return BackendIr.Operation(
        name = normalizedName,
        operationName = this.operationName,
        targetPackageName = this.packageName,
        operationType = typeRef,
        comment = this.description,
        variables = variables,
        definition = this.sourceWithFragments,
        dataField = dataField,
    )
  }

  private val Operation.operationTypeRef: IntrospectionSchema.TypeRef
    get() {
      return when {
        this.isQuery() -> schema.resolveType(schema.queryType)
        this.isMutation() -> checkNotNull(schema.resolveType(schema.mutationType ?: "")) {
          "Can't resolve GraphQL mutation operation type"
        }
        this.isSubscription() -> checkNotNull(schema.resolveType(schema.subscriptionType ?: "")) {
          "Can't resolve GraphQL mutation operation type"
        }
        else -> throw IllegalStateException("Unsupported GraphQL operation type: $operationType")
      }
    }

  private fun Operation.normalizeOperationName(): String {
    fun normalizeOperationName(
        useSemanticNaming: Boolean,
        operationNameSuffix: String,
    ): String {
      return if (useSemanticNaming && !operationName.endsWith(operationNameSuffix)) {
        operationName.capitalize() + operationNameSuffix
      } else {
        operationName.capitalize()
      }
    }
    return when {
      this.isQuery() -> normalizeOperationName(useSemanticNaming, "Query")
      this.isMutation() -> normalizeOperationName(useSemanticNaming, "Mutation")
      this.isSubscription() -> normalizeOperationName(useSemanticNaming, "Subscription")
      else -> throw IllegalStateException("Unsupported GraphQL operation type: $operationType")
    }
  }

  private fun Field.buildBackendIrField(
      selectionKey: SelectionKey,
      generateFragmentImplementations: Boolean,
  ): BackendIr.Field {
    val schemaType = schema.resolveType(this.type)
    val selectionSet = this.fields.buildBackendIrFields(
        selectionKey = selectionKey,
        generateFragmentImplementations = generateFragmentImplementations,
    )
    val fragments = this.buildBackendIrFragments(
        schemaType = schemaType,
        selectionKey = selectionKey,
        selectionSet = selectionSet,
        generateFragmentImplementations = generateFragmentImplementations,
    )
    val arguments = this.args.map { argument ->
      BackendIr.Argument(
          name = argument.name,
          value = argument.value,
          type = schema.resolveType(this.type)
      )
    }
    val conditions = this.conditions.map { condition ->
      BackendIr.Condition(
          kind = condition.kind,
          variableName = condition.variableName,
          inverted = condition.inverted,
          type = BackendIr.Condition.Type.Boolean
      )
    }
    return BackendIr.Field(
        name = this.fieldName,
        alias = this.responseName.takeIf { this.fieldName != this.responseName },
        type = schemaType,
        args = arguments,
        fields = selectionSet,
        fragments = fragments,
        deprecationReason = this.deprecationReason.takeIf { this.isDeprecated },
        description = this.description,
        conditions = conditions,
        selectionKeys = setOf(selectionKey),
    )
  }

  private fun List<Field>.buildBackendIrFields(
      selectionKey: SelectionKey,
      generateFragmentImplementations: Boolean,
  ): List<BackendIr.Field> {
    return this.map { field ->
      field.buildBackendIrField(
          selectionKey = selectionKey + field.responseName,
          generateFragmentImplementations = generateFragmentImplementations
      )
    }
  }

  private fun Fragment.buildBackendIrNamedFragment(): BackendIr.NamedFragment {
    val selectionSet = buildSelectionSet(
        rootSelectionKey = SelectionKey(
            root = this.fragmentName,
            keys = listOf(this.fragmentName),
            type = SelectionKey.Type.Fragment,
        ),
        generateFragmentImplementations = false,
    )
    val defaultSelectionSet = buildSelectionSet(
        rootSelectionKey = SelectionKey(
            root = this.fragmentName,
            keys = listOf(this.fragmentName, "DefaultImpl"),
            type = SelectionKey.Type.Fragment,
        ),
        generateFragmentImplementations = true,
    )
    return BackendIr.NamedFragment(
        name = this.fragmentName,
        source = this.source,
        comment = this.description,
        selectionSet = selectionSet,
        defaultSelectionSet = defaultSelectionSet,
    )
  }

  private fun Fragment.buildSelectionSet(
      rootSelectionKey: SelectionKey,
      generateFragmentImplementations: Boolean,
  ): BackendIr.NamedFragment.SelectionSet {
    val dataField = Field(
        responseName = "",
        fieldName = "",
        type = this.typeCondition,
        typeDescription = "",
        fields = this.fields,
        fragmentRefs = this.fragmentRefs,
        inlineFragments = this.inlineFragments,
        sourceLocation = SourceLocation.UNKNOWN,
    ).buildBackendIrField(
        selectionKey = rootSelectionKey,
        generateFragmentImplementations = generateFragmentImplementations,
    )
    return BackendIr.NamedFragment.SelectionSet(
        fields = dataField.fields,
        fragments = dataField.fragments,
        typeCondition = schema.resolveType(this.typeCondition),
        possibleTypes = this.possibleTypes.map { possibleType -> schema.resolveType(possibleType) },
        selectionKeys = dataField.selectionKeys
    )
  }

  // builds fragment interfaces and implementations for given field
  private fun Field.buildBackendIrFragments(
      schemaType: IntrospectionSchema.TypeRef,
      selectionKey: SelectionKey,
      selectionSet: List<BackendIr.Field>,
      generateFragmentImplementations: Boolean,
  ): List<BackendIr.Fragment> {
    // resolve all field's possible types
    val possibleTypes = schema.resolveType(schemaType.rawType)
        .possibleTypes(schema)
        .map { type -> schema.resolveType(type) }
        .toSet()

    // build interfaces for the fragments
    val fragmentInterfaces = this.buildBackendIrFragmentInterfaces(
        fieldPossibleTypes = possibleTypes,
        selectionKey = selectionKey,
        selectionSet = selectionSet,
    )

    // build implementations for the fragments if we allowed to do so
    val fragmentImplementations = if (generateFragmentImplementations) {
      this.buildFragmentImplementations(
          fieldPossibleTypes = possibleTypes,
          selectionKey = selectionKey,
          selectionSet = selectionSet,
      )
    } else emptyList()

    return fragmentInterfaces + fragmentImplementations
  }

  private fun Field.buildBackendIrFragmentInterfaces(
      fieldPossibleTypes: Set<IntrospectionSchema.TypeRef>,
      selectionKey: SelectionKey,
      selectionSet: List<BackendIr.Field>,
  ): List<BackendIr.Fragment.Interface> {
    // build all defined fragment interfaces including nested ones
    val fragments = this.buildGenericFragments(
        selectionKey = selectionKey,
        selectionSet = selectionSet,
        generateFragmentImplementations = false
    )
        // flatten fragments structure (remove nesting)
        .flatten()

    // we might get fragments defined with the same type condition - group them
    val groupedFragments = fragments.groupBy { fragment -> fragment.name }

    // merge fragments with the same type condition into one interface
    return groupedFragments.map { (_, fragments) ->
      val selectionSet = fragments.fold(emptyList<BackendIr.Field>()) { acc, fragment ->
        acc.mergeFields(fragment.selectionSet)
      }
      val selectionsKeys = fragments.fold(emptySet<SelectionKey>()) { acc, fragment ->
        acc.plus(fragment.selectionKeys)
      }
      // as fragment can be defined on interface that has more possible implementations than field type where it used
      // build intersection of fragment's and field's possible types
      val possibleTypes = fragments.first().possibleTypes.intersect(fieldPossibleTypes)
      BackendIr.Fragment.Interface(
          name = fragments.first().name,
          fields = selectionSet,
          selectionKeys = selectionsKeys,
          possibleTypes = possibleTypes,
          description = fragments.first().description,
          typeCondition = fragments.first().typeCondition,
      )
    }
  }

  private fun Field.buildFragmentImplementations(
      fieldPossibleTypes: Set<IntrospectionSchema.TypeRef>,
      selectionKey: SelectionKey,
      selectionSet: List<BackendIr.Field>,
  ): List<BackendIr.Fragment.Implementation> {
    // build all defined fragment implementations including nested ones
    val fragments = this.buildGenericFragments(
        selectionKey = selectionKey,
        selectionSet = selectionSet,
        generateFragmentImplementations = true
    )
        // flatten fragments structure (remove nesting)
        .flatten()

    // we might get fragments that intersects by possible type - group them
    val groupedFragments = fragments.groupFragmentsByPossibleTypes()

    // merge fragments with possible types intersection into one implementation
    return groupedFragments.map { (fragments, fragmentsPossibleTypes) ->
      val fragmentName = fragments.formatFragmentImplementationName(
          postfix = this.responseName
      )
      val selectionSet = fragments.fold(emptyList<BackendIr.Field>()) { acc, fragment ->
        acc.mergeFields(fragment.selectionSet)
      }
      val selectionsKeys = fragments.fold(emptySet<SelectionKey>()) { acc, fragment ->
        acc.plus(fragment.selectionKeys)
      }
      val description = if (fragments.size == 1) {
        fragments.first().description
      } else null
      // as fragment can be defined on interface that has more possible implementation types than field's type where it used
      // build intersection of fragment's and field's possible types
      val possibleTypes = fragmentsPossibleTypes.intersect(fieldPossibleTypes)
      BackendIr.Fragment.Implementation(
          name = fragmentName,
          fields = selectionSet,
          possibleTypes = possibleTypes,
          selectionKeys = selectionsKeys,
          description = description,
      )
    }
  }

  private fun List<GenericFragment>.flatten(): List<GenericFragment> {
    return this.flatMap { fragment ->
      listOf(fragment.copy(nestedFragments = emptyList())) + fragment.nestedFragments.flatten()
    }
  }

  private fun Field.buildGenericFragments(
      selectionKey: SelectionKey,
      selectionSet: List<BackendIr.Field>,
      generateFragmentImplementations: Boolean,
  ): List<GenericFragment> {
    // build generic fragments from inline fragments
    val inlineFragments = this.inlineFragments.map { inlineFragment ->
      inlineFragment.buildGenericFragment(
          parentSelectionKey = selectionKey,
          parentSelectionSet = selectionSet,
          parentNamedFragmentSelectionKeys = emptySet(),
          generateFragmentImplementations = generateFragmentImplementations
      )
    }
    // build generic fragments from named fragments
    val namedFragments = this.fragmentRefs
        .map { fragmentRef -> lookupForNamedFragment(fragmentRef) }
        .map { namedFragment ->
          namedFragment.buildGenericFragment(
              parentSelectionKey = selectionKey,
              parentSelectionSet = selectionSet,
              parentNamedFragmentSelectionKeys = emptySet(),
              generateFragmentImplementations = generateFragmentImplementations
          )
        }
    return inlineFragments + namedFragments
  }

  private fun InlineFragment.buildGenericFragment(
      parentSelectionKey: SelectionKey,
      parentSelectionSet: List<BackendIr.Field>,
      parentNamedFragmentSelectionKeys: Set<SelectionKey>,
      generateFragmentImplementations: Boolean,
  ): GenericFragment {
    return buildGenericFragment(
        fragmentTypeCondition = schema.resolveType(this.typeCondition),
        fragmentFields = this.fields,
        fragmentPossibleTypes = this.possibleTypes.map { possibleType -> schema.resolveType(possibleType) },
        fragmentDescription = this.description,
        fragmentConditions = this.conditions,
        namedFragmentSelectionKey = null,
        nestedInlineFragments = this.inlineFragments,
        nestedNamedFragments = this.fragments.map { fragmentRef -> lookupForNamedFragment(fragmentRef) },
        parentSelectionKey = parentSelectionKey,
        parentSelectionSet = parentSelectionSet,
        parentNamedFragmentSelectionKeys = parentNamedFragmentSelectionKeys,
        generateFragmentImplementations = generateFragmentImplementations
    )
  }

  private fun Fragment.buildGenericFragment(
      parentSelectionKey: SelectionKey,
      parentSelectionSet: List<BackendIr.Field>,
      parentNamedFragmentSelectionKeys: Set<SelectionKey>,
      generateFragmentImplementations: Boolean,
  ): GenericFragment {
    return buildGenericFragment(
        fragmentTypeCondition = schema.resolveType(this.typeCondition),
        fragmentFields = this.fields,
        fragmentPossibleTypes = this.possibleTypes.map { possibleType -> schema.resolveType(possibleType) },
        fragmentDescription = this.description,
        fragmentConditions = emptyList(),
        namedFragmentSelectionKey = SelectionKey(
            root = this.fragmentName.capitalize(),
            keys = listOf(this.fragmentName.capitalize()),
            type = SelectionKey.Type.Fragment,
        ),
        nestedInlineFragments = this.inlineFragments,
        nestedNamedFragments = this.fragmentRefs.map { fragmentRef -> lookupForNamedFragment(fragmentRef) },
        parentSelectionKey = parentSelectionKey,
        parentSelectionSet = parentSelectionSet,
        parentNamedFragmentSelectionKeys = parentNamedFragmentSelectionKeys,
        generateFragmentImplementations = generateFragmentImplementations
    )
  }

  /**
   * Build generic fragment with merged parent fields and any nested fragments.
   *
   * case 1:
   * ```
   * query TestQuery {
   *  hero {
   *    name
   *    ... on Human { <--- imagine we are building this fragment
   *      height
   *    }
   *  }
   *}
   * ```
   * we must carry down field `name` into built fragment for `Human`
   *
   * case 2:
   * ```
   * fragment HeroDetails on Character {
   *  id
   *  friends {
   *    name
   *  }
   *  ... on Droid { <--- imagine we are building this fragment
   *    name
   *    friends {
   *      id
   *    }
   *  }
   * ```
   * we must carry down `id` and `friends` fields (including any nested fields `friends.name`)
   * from the parent `HeroDetails` fragment
   *
   *
   * case 3:
   * ```
   * fragment HeroDetails on Character {
   *  name
   *  ... on Droid { <--- imagine we are building this fragment
   *    id
   *    ...DroidDetails
   *  }
   *}
   *
   *fragment DroidDetails on Droid {
   *  friends {
   *    name
   *  }
   * }
   * ```
   */
  private fun buildGenericFragment(
      fragmentTypeCondition: IntrospectionSchema.TypeRef,
      fragmentFields: List<Field>,
      fragmentPossibleTypes: List<IntrospectionSchema.TypeRef>,
      fragmentDescription: String,
      fragmentConditions: List<Condition>,
      namedFragmentSelectionKey: SelectionKey?,
      nestedInlineFragments: List<InlineFragment>,
      nestedNamedFragments: List<Fragment>,
      parentSelectionKey: SelectionKey,
      parentSelectionSet: List<BackendIr.Field>,
      parentNamedFragmentSelectionKeys: Set<SelectionKey>,
      generateFragmentImplementations: Boolean,
  ): GenericFragment {
    val fragmentName = fragmentTypeCondition.name!!.capitalize()
    val fragmentSelectionSet = fragmentFields.buildBackendIrFields(
        selectionKey = parentSelectionKey + fragmentName,
        generateFragmentImplementations = generateFragmentImplementations,
    ).addFieldSelectionKey(namedFragmentSelectionKey)
    val selectionSet = parentSelectionSet
        .addFieldSelectionKey(parentSelectionKey + fragmentName)
        .map { field ->
          field.addFieldSelectionKeys(
              field.selectionKeys
                  .filter { selectionKey -> selectionKey.type == SelectionKey.Type.Fragment }
                  .map { selectionKey ->
                    selectionKey.copy(
                        keys = listOf(selectionKey.keys.first(), fragmentName, selectionKey.keys.last())
                    )
                  }
                  .toSet()
          )
        }
        .mergeFields(fragmentSelectionSet)
    val childInlineFragments = nestedInlineFragments.map { inlineFragment ->
      val fragment = buildGenericFragment(
          fragmentTypeCondition = schema.resolveType(inlineFragment.typeCondition),
          fragmentFields = inlineFragment.fields,
          fragmentPossibleTypes = inlineFragment.possibleTypes.map { possibleType -> schema.resolveType(possibleType) },
          fragmentDescription = inlineFragment.description,
          fragmentConditions = inlineFragment.conditions,
          namedFragmentSelectionKey = namedFragmentSelectionKey?.let { selectionKey ->
            selectionKey.copy(
                keys = listOf(selectionKey.keys.first(), inlineFragment.typeCondition.capitalize())
            )
          },
          nestedInlineFragments = inlineFragment.inlineFragments,
          nestedNamedFragments = inlineFragment.fragments.map { fragmentRef -> lookupForNamedFragment(fragmentRef) },
          parentSelectionKey = parentSelectionKey,
          parentSelectionSet = selectionSet,
          parentNamedFragmentSelectionKeys = parentNamedFragmentSelectionKeys.run {
            if (namedFragmentSelectionKey != null) {
              this + namedFragmentSelectionKey
            } else this
          },
          generateFragmentImplementations = generateFragmentImplementations
      )
      fragment.copy(
          selectionKeys = fragment.selectionKeys + (parentSelectionKey + fragmentName)
      )
    }
    val childNamedFragments = nestedNamedFragments.map { namedFragment ->
      buildGenericFragment(
          fragmentTypeCondition = schema.resolveType(namedFragment.typeCondition),
          fragmentFields = namedFragment.fields,
          fragmentPossibleTypes = namedFragment.possibleTypes.map { possibleType -> schema.resolveType(possibleType) },
          fragmentDescription = namedFragment.description,
          fragmentConditions = emptyList(),
          namedFragmentSelectionKey = SelectionKey(
              root = namedFragment.fragmentName.capitalize(),
              keys = listOf(namedFragment.fragmentName.capitalize()),
              type = SelectionKey.Type.Fragment,
          ),
          nestedInlineFragments = namedFragment.inlineFragments,
          nestedNamedFragments = namedFragment.fragmentRefs.map { fragmentRef -> lookupForNamedFragment(fragmentRef) },
          parentSelectionKey = parentSelectionKey,
          parentSelectionSet = selectionSet,
          parentNamedFragmentSelectionKeys = parentNamedFragmentSelectionKeys.run {
            if (namedFragmentSelectionKey != null) {
              this + namedFragmentSelectionKey
            } else this
          },
          generateFragmentImplementations = generateFragmentImplementations
      )
    }
    val selectionKeys = setOf(
        parentSelectionKey,
        parentSelectionKey + fragmentName,
    ).plus(
        parentNamedFragmentSelectionKeys.map { selectionKey ->
          //TODO figure out why this happens when we have nested fragments defined on the same type condition
          selectionKey
              .takeIf { selectionKey.keys.last() == fragmentName }
              ?: selectionKey + fragmentName
        }
    ).run {
      if (namedFragmentSelectionKey != null) {
        this + namedFragmentSelectionKey
      } else this
    }
    return GenericFragment(
        name = fragmentName,
        typeCondition = fragmentTypeCondition,
        possibleTypes = fragmentPossibleTypes,
        description = fragmentDescription,
        selectionSet = selectionSet,
        conditions = fragmentConditions,
        nestedFragments = childInlineFragments + childNamedFragments,
        selectionKeys = selectionKeys,
    )
  }

  /**
   * Formats fragment implementation name by joining type conditions:
   * ```
   *  query TestOperation {
   *   random {
   *    ... on Being {
   *    }
   *    ... on Human {
   *    }
   *  }
   *}
   * ```
   * generated name is going to be `BeingHumanRandom`.
   */
  private fun List<GenericFragment>.formatFragmentImplementationName(postfix: String): String {
    return this
        .distinctBy { fragment -> fragment.typeCondition }
        .joinToString(separator = "", postfix = postfix.capitalize()) { fragment ->
          fragment.typeCondition.name!!.capitalize()
        }
  }

  /**
   * Groups provided list of fragments by intersection of possible types.
   * ```
   * query TestOperation {
   *   random {
   *     ... on Being {
   *      ... on Human {
   *      }
   *      ... on Wookie {
   *      }
   *     }
   *  }
   *}
   * ```
   * as `Human` and `Wookie` are subtypes of `Being` grouped fragment map is going to be:
   * ```
   * [
   *  [Being, Human]: ["Human"],
   *  [Being, Wookie]: ["Wookie"]
   * ]
   * ```
   */
  private fun List<GenericFragment>.groupFragmentsByPossibleTypes()
      : Map<List<GenericFragment>, List<IntrospectionSchema.TypeRef>> {
    return this
        .flatMap { fragment -> fragment.possibleTypes }
        .toSet()
        .map { possibleType ->
          possibleType to this.filter { fragment ->
            fragment.possibleTypes.contains(possibleType)
          }
        }.fold(emptyMap()) { acc, (possibleType, fragments) ->
          acc + (fragments to (acc[fragments]?.plus(possibleType) ?: listOf(possibleType)))
        }
  }

  private fun lookupForNamedFragment(fragmentRef: FragmentRef): Fragment {
    return checkNotNull(namedFragments.find { fragment -> fragment.fragmentName == fragmentRef.name }) {
      "Failed to resolve fragment by name `${fragmentRef.name}`"
    }
  }

  private data class GenericFragment(
      val name: String,
      val typeCondition: IntrospectionSchema.TypeRef,
      val possibleTypes: List<IntrospectionSchema.TypeRef>,
      val description: String,
      val selectionSet: List<BackendIr.Field>,
      val conditions: List<Condition>,
      val nestedFragments: List<GenericFragment>,
      val selectionKeys: Set<SelectionKey>,
  )
}
