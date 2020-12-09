package com.apollographql.apollo.compiler.backend.ir

import com.apollographql.apollo.compiler.PackageNameProvider
import com.apollographql.apollo.compiler.backend.ir.BackendIrMergeUtils.mergeFields
import com.apollographql.apollo.compiler.backend.ir.SelectionKeyUtils.addFieldSelectionKey
import com.apollographql.apollo.compiler.backend.ir.SelectionKeyUtils.addFieldSelectionKeys
import com.apollographql.apollo.compiler.frontend.GQLNamedType
import com.apollographql.apollo.compiler.frontend.Schema
import com.apollographql.apollo.compiler.frontend.SourceLocation
import com.apollographql.apollo.compiler.frontend.ir.FrontendIr
import com.apollographql.apollo.compiler.frontend.possibleTypes
import com.apollographql.apollo.compiler.frontend.schemaKind
import com.apollographql.apollo.compiler.frontend.toKotlinValue
import com.apollographql.apollo.compiler.frontend.toSchemaType
import com.apollographql.apollo.compiler.introspection.IntrospectionSchema

internal class BackendIrBuilder constructor(
    private val schema: Schema,
    private val useSemanticNaming: Boolean,
    val packageNameProvider: PackageNameProvider?
) {
  lateinit var allFragmentDefinitions: Map<String, FrontendIr.NamedFragmentDefinition>
  fun buildBackendIR(
      frontendIr: FrontendIr
  ): BackendIr {
    allFragmentDefinitions = frontendIr.allFragmentDefinitions
    return BackendIr(
        operations = frontendIr.operations.map { operation ->
          operation.buildBackendIrOperation()
        },
        fragments = frontendIr.fragmentDefinitions
            .map { fragmentDefinition ->
              fragmentDefinition.buildBackendIrNamedFragment()
            }
    )
  }

  private fun FrontendIr.Operation.buildBackendIrOperation(): BackendIr.Operation {
    val normalizedName = this.normalizeOperationName()
    val rootTypeDefinition = this.typeDefinition
    val selectionKey = SelectionKey(
        root = normalizedName,
        keys = listOf(normalizedName, "data"),
        type = SelectionKey.Type.Query,
    )

    val dataField = FrontendIr.Selection.Field(
                name = "data",
                alias = null,
                arguments = emptyList(),
                condition = FrontendIr.Condition.True,
                selections = selections,
                type = FrontendIr.Type.Named(typeDefinition),
                deprecationReason = null,
                description = description
    ).buildBackendIrField(
        selectionKey = selectionKey,
        generateFragmentImplementations = true,
    )
    val variables = this.variables.map { variable ->
      BackendIr.Variable(
          name = variable.name,
          type = variable.type.toSchemaType()
      )
    }
    return BackendIr.Operation(
        name = normalizedName,
        operationName = name,
        targetPackageName = packageNameProvider?.operationPackageName(filePath = gqlOperationDefinition.sourceLocation.filePath ?: "") ?: "",
        operationType = IntrospectionSchema.TypeRef(
            IntrospectionSchema.Kind.OBJECT,
            rootTypeDefinition.name
        ),
        comment = description ?: "",
        variables = variables,
        definition = sourceWithFragments,
        dataField = dataField,
    )
  }

  private fun FrontendIr.Type.toSchemaType(): IntrospectionSchema.TypeRef {
    return when (this) {
      is FrontendIr.Type.NonNull -> {
        IntrospectionSchema.TypeRef(
            kind = IntrospectionSchema.Kind.NON_NULL,
            name = "", // why "" and not null ?
            ofType = ofType.toSchemaType()
        )
      }
      is FrontendIr.Type.List -> {
        IntrospectionSchema.TypeRef(
            kind = IntrospectionSchema.Kind.LIST,
            name = "", // why "" and not null ?
            ofType = ofType.toSchemaType())
      }
      is FrontendIr.Type.Named -> {
        IntrospectionSchema.TypeRef(
            kind = typeDefinition.schemaKind(),
            name = typeDefinition.name,
            ofType = null
        )
      }
    }
  }


  private fun FrontendIr.Operation.normalizeOperationName(): String {
    fun normalizeOperationName(
        useSemanticNaming: Boolean,
        operationNameSuffix: String,
    ): String {
      return if (useSemanticNaming && !name.endsWith(operationNameSuffix)) {
        name.capitalize() + operationNameSuffix
      } else {
        name.capitalize()
      }
    }
    return normalizeOperationName(useSemanticNaming, operationType.name)
  }

  private fun FrontendIr.Selection.Field.buildBackendIrField(
      selectionKey: SelectionKey,
      generateFragmentImplementations: Boolean,
  ): BackendIr.Field {

    val selectionSet = selections.filterIsInstance<FrontendIr.Selection.Field>().buildBackendIrFields(
        selectionKey = selectionKey,
        generateFragmentImplementations = generateFragmentImplementations,
    )

    val fragments = buildBackendIrFragments(
        parentSelectionName = responseName,
        inlineFragments = selections.filterIsInstance<FrontendIr.Selection.InlineFragment>(),
        namedFragments = selections.filterIsInstance<FrontendIr.Selection.FragmentSpread>(),
        type = type,
        selectionKey = selectionKey,
        selectionSet = selectionSet,
        generateFragmentImplementations = generateFragmentImplementations,
    )

    val arguments = arguments.map { argument ->
      BackendIr.Argument(
          name = argument.name,
          value = argument.value.toKotlinValue(false),
          type = argument.type.toSchemaType()
      )
    }

    return BackendIr.Field(
        name = name,
        alias = alias,
        type = type.toSchemaType(),
        args = arguments,
        fields = selectionSet,
        fragments = fragments,
        deprecationReason = deprecationReason,
        description = description ?: "",
        condition = condition.toBackendIr(),
        selectionKeys = setOf(selectionKey),
    )
  }

  private fun FrontendIr.Condition.toBackendIr(): BackendIr.Condition {
    return when (this) {
      is FrontendIr.Condition.True -> BackendIr.Condition.True
      is FrontendIr.Condition.False -> BackendIr.Condition.False
      is FrontendIr.Condition.Or -> BackendIr.Condition.Or(conditions = conditions.map { it.toBackendIr() }.toSet())
      is FrontendIr.Condition.And -> BackendIr.Condition.And(conditions = conditions.map { it.toBackendIr() }.toSet())
      is FrontendIr.Condition.Variable -> BackendIr.Condition.Variable(name = name, inverted = inverted)
    }
  }

  private fun List<FrontendIr.Selection.Field>.buildBackendIrFields(
      selectionKey: SelectionKey,
      generateFragmentImplementations: Boolean,
  ): List<BackendIr.Field> {
    return this.map { field ->
      field.buildBackendIrField(
          selectionKey = selectionKey + field.responseName,
          generateFragmentImplementations = generateFragmentImplementations,
      )
    }
  }

  private fun FrontendIr.NamedFragmentDefinition.buildBackendIrNamedFragment(): BackendIr.NamedFragment {
    val selectionSet = buildSelectionSet(
        rootSelectionKey = SelectionKey(
            root = this.name,
            keys = listOf(this.name),
            type = SelectionKey.Type.Fragment,
        ),
        generateFragmentImplementations = false,
    )

    val defaultSelectionSet = buildSelectionSet(
        rootSelectionKey = SelectionKey(
            root = this.name,
            keys = listOf(this.name),
            type = SelectionKey.Type.Fragment,
        ),
        generateFragmentImplementations = true,
    )
        // as we generate default selection set for a fragment with `*Impl` suffix
        // we must patch all selection keys in this set by adding a new keys
        // these new keys are copies of existing ones with a different root
        // new root is `${this.fragmentName}Impl` instead of `${this.fragmentName}`
        // this is needed later when we generate models for default implementation of named fragments
        // to properly resolve inheritance to the original named fragment interfaces
        .patchWithDefaultImplementationSelectionKey(
            fragmentNameToPatch = this.name,
            defaultImplementationName = "${this.name}Impl",
        )
    return BackendIr.NamedFragment(
        name = this.name,
        defaultImplementationName = "${this.name}Impl",
        comment = this.description ?: "",
        selectionSet = selectionSet,
        defaultSelectionSet = defaultSelectionSet,
    )
  }

  private fun FrontendIr.NamedFragmentDefinition.buildSelectionSet(
      rootSelectionKey: SelectionKey,
      generateFragmentImplementations: Boolean,
  ): BackendIr.NamedFragment.SelectionSet {
    val selectionSet = this.selections.filterIsInstance<FrontendIr.Selection.Field>().buildBackendIrFields(
        selectionKey = rootSelectionKey,
        generateFragmentImplementations = generateFragmentImplementations,
    )

    // resolve all possible types
    val possibleTypes = schema.typeDefinition(typeCondition.name).possibleTypes(schema.typeDefinitions)

    // build interfaces for the fragments
    val fragmentInterfaces = buildBackendIrFragmentInterfaces(
        inlineFragments = this.selections.filterIsInstance<FrontendIr.Selection.InlineFragment>(),
        namedFragments = this.selections.filterIsInstance<FrontendIr.Selection.FragmentSpread>(),
        fieldPossibleTypes = possibleTypes,
        selectionKey = rootSelectionKey,
        fields = selectionSet,
    )

    // build implementations for the fragments if we allowed to do so
    val fragmentImplementations = if (generateFragmentImplementations) {
      buildFragmentImplementations(
          parentSelectionName = this.name,
          inlineFragments = this.selections.filterIsInstance<FrontendIr.Selection.InlineFragment>(),
          namedFragments = this.selections.filterIsInstance<FrontendIr.Selection.FragmentSpread>(),
          fieldPossibleTypes = possibleTypes,
          selectionKey = rootSelectionKey,
          fields = selectionSet,
      )
    } else emptyList()

    return BackendIr.NamedFragment.SelectionSet(
        fields = selectionSet,
        fragments = fragmentInterfaces + fragmentImplementations,
        typeCondition = FrontendIr.Type.Named(typeCondition).toSchemaType(),
        possibleTypes = possibleTypes.map { FrontendIr.Type.Named(schema.typeDefinition(it)).toSchemaType() },
        selectionKeys = setOf(rootSelectionKey)
    )
  }

  // do deep recursive traversal of all fields and nested fragments
  // find any selection key that belongs to the named fragment we generate default selection set for (`root == fragmentNameToPatch`)
  // copy it with `defaultImplementationName` as a root and add to the original selection set
  private fun BackendIr.NamedFragment.SelectionSet.patchWithDefaultImplementationSelectionKey(
      fragmentNameToPatch: String,
      defaultImplementationName: String,
  ): BackendIr.NamedFragment.SelectionSet {
    return this.copy(
        fields = this.fields.map { field ->
          field.patchWithDefaultImplementationSelectionKey(
              fragmentNameToPatch = fragmentNameToPatch,
              defaultImplementationName = defaultImplementationName,
          )
        },
        fragments = fragments.map { fragment ->
          when (fragment) {
            is BackendIr.Fragment.Interface -> fragment.patchWithDefaultImplementationSelectionKey(
                fragmentNameToPatch = fragmentNameToPatch,
                defaultImplementationName = defaultImplementationName,
            )
            is BackendIr.Fragment.Implementation -> fragment.patchWithDefaultImplementationSelectionKey(
                fragmentToPatch = fragmentNameToPatch,
                fragmentDefaultImplementation = defaultImplementationName,
            )
          }
        },
        selectionKeys = this.selectionKeys + this.selectionKeys
            .filter { selectionKey ->
              selectionKey.type == SelectionKey.Type.Fragment && selectionKey.root == fragmentNameToPatch
            }
            .map { key ->
              key.copy(root = defaultImplementationName, keys = listOf(defaultImplementationName) + key.keys.drop(1))
            },
    )
  }

  // do deep recursive traversal of all fields and nested fragments
  // find any selection key that belongs to the named fragment we generate default selection set for (`root == fragmentNameToPatch`)
  // copy it with `defaultImplementationName` as a root and add to the original selection set
  private fun BackendIr.Field.patchWithDefaultImplementationSelectionKey(
      fragmentNameToPatch: String,
      defaultImplementationName: String,
  ): BackendIr.Field {
    return this.copy(
        selectionKeys = this.selectionKeys + this.selectionKeys
            .filter { selectionKey ->
              selectionKey.type == SelectionKey.Type.Fragment && selectionKey.root == fragmentNameToPatch
            }
            .map { key ->
              key.copy(root = defaultImplementationName, keys = listOf(defaultImplementationName) + key.keys.drop(1))
            },
        fields = this.fields.map { field ->
          field.patchWithDefaultImplementationSelectionKey(
              fragmentNameToPatch = fragmentNameToPatch,
              defaultImplementationName = defaultImplementationName,
          )
        },
        fragments = fragments.map { fragment ->
          when (fragment) {
            is BackendIr.Fragment.Interface -> fragment.patchWithDefaultImplementationSelectionKey(
                fragmentNameToPatch = fragmentNameToPatch,
                defaultImplementationName = defaultImplementationName,
            )
            is BackendIr.Fragment.Implementation -> fragment.patchWithDefaultImplementationSelectionKey(
                fragmentToPatch = fragmentNameToPatch,
                fragmentDefaultImplementation = defaultImplementationName,
            )
          }
        }
    )
  }

  // do deep recursive traversal of all fields and nested fragments
  // find any selection key that belongs to the named fragment we generate default selection set for (`root == fragmentNameToPatch`)
  // copy it with `defaultImplementationName` as a root and add to the original selection set
  private fun BackendIr.Fragment.Interface.patchWithDefaultImplementationSelectionKey(
      fragmentNameToPatch: String,
      defaultImplementationName: String,
  ): BackendIr.Fragment {
    return this.copy(
        selectionKeys = this.selectionKeys + this.selectionKeys
            .filter { selectionKey ->
              selectionKey.type == SelectionKey.Type.Fragment && selectionKey.root == fragmentNameToPatch
            }
            .map { key ->
              key.copy(root = defaultImplementationName, keys = listOf(defaultImplementationName) + key.keys.drop(1))
            },
        fields = this.fields.map { field ->
          field.patchWithDefaultImplementationSelectionKey(
              fragmentNameToPatch = fragmentNameToPatch,
              defaultImplementationName = defaultImplementationName,
          )
        },
    )
  }

  // do deep recursive traversal of all fields and nested fragments
  // find any selection key that belongs to the named fragment we generate default selection set for (`root == fragmentNameToPatch`)
  // copy it with `defaultImplementationName` as a root and add to the original selection set
  private fun BackendIr.Fragment.Implementation.patchWithDefaultImplementationSelectionKey(
      fragmentToPatch: String,
      fragmentDefaultImplementation: String,
  ): BackendIr.Fragment {
    return this.copy(
        selectionKeys = this.selectionKeys + this.selectionKeys
            .filter { selectionKey ->
              selectionKey.type == SelectionKey.Type.Fragment && selectionKey.root == fragmentToPatch
            }
            .map { key ->
              key.copy(root = fragmentDefaultImplementation, keys = listOf(fragmentDefaultImplementation) + key.keys.drop(1))
            },
        fields = this.fields.map { field ->
          field.patchWithDefaultImplementationSelectionKey(
              fragmentNameToPatch = fragmentToPatch,
              defaultImplementationName = fragmentDefaultImplementation,
          )
        },
    )
  }

  // builds fragment interfaces and implementations for given field
  private fun buildBackendIrFragments(
      parentSelectionName: String,
      inlineFragments: List<FrontendIr.Selection.InlineFragment>,
      namedFragments: List<FrontendIr.Selection.FragmentSpread>,
      type: FrontendIr.Type,
      selectionKey: SelectionKey,
      selectionSet: List<BackendIr.Field>,
      generateFragmentImplementations: Boolean,
  ): List<BackendIr.Fragment> {
    // resolve all field's possible types
    val possibleTypes = type.leafTypeDefinition
        .possibleTypes(schema.typeDefinitions)

    // build interfaces for the fragments
    val fragmentInterfaces = buildBackendIrFragmentInterfaces(
        inlineFragments = inlineFragments,
        namedFragments = namedFragments,
        fieldPossibleTypes = possibleTypes,
        selectionKey = selectionKey,
        fields = selectionSet,
    )

    // build implementations for the fragments if we allowed to do so
    val fragmentImplementations = if (generateFragmentImplementations) {
      buildFragmentImplementations(
          parentSelectionName = parentSelectionName,
          inlineFragments = inlineFragments,
          namedFragments = namedFragments,
          fieldPossibleTypes = possibleTypes,
          selectionKey = selectionKey,
          fields = selectionSet,
      )
    } else emptyList()

    return fragmentInterfaces + fragmentImplementations
  }

  private fun buildBackendIrFragmentInterfaces(
      inlineFragments: List<FrontendIr.Selection.InlineFragment>,
      namedFragments: List<FrontendIr.Selection.FragmentSpread>,
      fieldPossibleTypes: Set<String>,
      selectionKey: SelectionKey,
      fields: List<BackendIr.Field>,
  ): List<BackendIr.Fragment.Interface> {
    // build all defined fragment interfaces including nested ones
    val fragments = buildGenericFragments(
        inlineFragments = inlineFragments,
        namedFragments = namedFragments,
        selectionKey = selectionKey,
        selectionSet = fields,
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
          possibleTypes = possibleTypes.map { GQLNamedType(sourceLocation = SourceLocation.UNKNOWN, it).toSchemaType(schema) }.toSet(),
          description = fragments.first().description,
          typeCondition = fragments.first().typeCondition,
      )
    }
  }

  private fun buildFragmentImplementations(
      parentSelectionName: String,
      inlineFragments: List<FrontendIr.Selection.InlineFragment>,
      namedFragments: List<FrontendIr.Selection.FragmentSpread>,
      fieldPossibleTypes: Set<String>,
      selectionKey: SelectionKey,
      fields: List<BackendIr.Field>,
  ): List<BackendIr.Fragment.Implementation> {
    // build all defined fragment implementations including nested ones
    val fragments = buildGenericFragments(
        inlineFragments = inlineFragments,
        namedFragments = namedFragments,
        selectionKey = selectionKey,
        selectionSet = fields,
        generateFragmentImplementations = true
    )
        // flatten fragments structure (remove nesting)
        .flatten()

    // we might get fragments that intersects by possible type - group them
    val groupedFragments = fragments.groupFragmentsByPossibleTypes()

    // merge fragments with possible types intersection into one implementation
    return groupedFragments.map { (fragments, fragmentsPossibleTypes) ->
      val fragmentName = fragments.formatFragmentImplementationName(
          postfix = parentSelectionName,
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
          possibleTypes = possibleTypes.map { GQLNamedType(sourceLocation = SourceLocation.UNKNOWN, it).toSchemaType(schema) }.toSet(),
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

  private fun buildGenericFragments(
      inlineFragments: List<FrontendIr.Selection.InlineFragment>,
      namedFragments: List<FrontendIr.Selection.FragmentSpread>,
      selectionKey: SelectionKey,
      selectionSet: List<BackendIr.Field>,
      generateFragmentImplementations: Boolean,
  ): List<GenericFragment> {
    // build generic fragments from inline fragments
    val genericInlineFragments = inlineFragments
        .map { inlineFragment ->
          inlineFragment.buildGenericFragment(
              parentSelectionKey = selectionKey,
              parentSelectionSet = selectionSet,
              parentNamedFragmentSelectionKeys = emptySet(),
              generateFragmentImplementations = generateFragmentImplementations
          )
        }
    // build generic fragments from named fragments
    val genericNamedFragments = namedFragments
        .map { fragmentSpread -> allFragmentDefinitions.get(fragmentSpread.name)!! }
        .map { namedFragment ->
          namedFragment.buildGenericFragment(
              parentSelectionKey = selectionKey,
              parentSelectionSet = selectionSet,
              parentNamedFragmentSelectionKeys = emptySet(),
              generateFragmentImplementations = generateFragmentImplementations
          )
        }
    return genericInlineFragments + genericNamedFragments
  }

  private fun FrontendIr.Selection.InlineFragment.buildGenericFragment(
      parentSelectionKey: SelectionKey,
      parentSelectionSet: List<BackendIr.Field>,
      parentNamedFragmentSelectionKeys: Set<SelectionKey>,
      generateFragmentImplementations: Boolean,
  ): GenericFragment {
    val typeDefinition = fragmentDefinition.typeCondition
    return buildGenericFragment(
        fragmentTypeCondition = typeDefinition.name,
        selections = fragmentDefinition.selections,
        fragmentDescription = typeDefinition.description ?: "",
        fragmentCondition = condition.toBackendIr(),
        namedFragmentSelectionKey = null,
        parentSelectionKey = parentSelectionKey,
        parentFields = parentSelectionSet,
        parentNamedFragmentSelectionKeys = parentNamedFragmentSelectionKeys,
        generateFragmentImplementations = generateFragmentImplementations
    )
  }

  private fun FrontendIr.NamedFragmentDefinition.buildGenericFragment(
      parentSelectionKey: SelectionKey,
      parentSelectionSet: List<BackendIr.Field>,
      parentNamedFragmentSelectionKeys: Set<SelectionKey>,
      generateFragmentImplementations: Boolean,
  ): GenericFragment {
    return buildGenericFragment(
        fragmentTypeCondition = typeCondition.name,
        selections = selections,
        fragmentDescription = this.description ?: "",
        fragmentCondition = BackendIr.Condition.True,
        namedFragmentSelectionKey = SelectionKey(
            root = this.name.capitalize(),
            keys = listOf(this.name.capitalize()),
            type = SelectionKey.Type.Fragment,
        ),
        parentSelectionKey = parentSelectionKey,
        parentFields = parentSelectionSet,
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
      fragmentTypeCondition: String,
      selections: List<FrontendIr.Selection>,
      fragmentDescription: String,
      fragmentCondition: BackendIr.Condition,
      namedFragmentSelectionKey: SelectionKey?,
      parentSelectionKey: SelectionKey,
      parentFields: List<BackendIr.Field>,
      parentNamedFragmentSelectionKeys: Set<SelectionKey>,
      generateFragmentImplementations: Boolean,
  ): GenericFragment {
    val fragmentName = fragmentTypeCondition.capitalize()
    val fragmentSelectionSet = selections.filterIsInstance<FrontendIr.Selection.Field>().buildBackendIrFields(
        selectionKey = parentSelectionKey + fragmentName,
        generateFragmentImplementations = generateFragmentImplementations,
    ).addFieldSelectionKey(namedFragmentSelectionKey)
    val parentSelectionSet = parentFields
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
    val childInlineFragments = selections.filterIsInstance<FrontendIr.Selection.InlineFragment>().map { inlineFragment ->
      val fragment = buildGenericFragment(
          fragmentTypeCondition = inlineFragment.fragmentDefinition.typeCondition.name,
          selections = inlineFragment.fragmentDefinition.selections,
          fragmentDescription = inlineFragment.fragmentDefinition.typeCondition.description ?: "",
          fragmentCondition = inlineFragment.condition.toBackendIr(),
          namedFragmentSelectionKey = namedFragmentSelectionKey?.let { selectionKey ->
            selectionKey.copy(
                keys = listOf(selectionKey.keys.first(), inlineFragment.fragmentDefinition.typeCondition.name.capitalize())
            )
          },
          parentSelectionKey = parentSelectionKey,
          parentFields = parentSelectionSet,
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
    val childNamedFragments = selections.filterIsInstance<FrontendIr.Selection.FragmentSpread>().map { namedFragment ->
      val fragmentDefinition = allFragmentDefinitions.get(namedFragment.name)!!
      buildGenericFragment(
          fragmentTypeCondition = fragmentDefinition.typeCondition.name,
          selections = fragmentDefinition.selections,
          fragmentDescription = fragmentDefinition.description ?: "",
          fragmentCondition = BackendIr.Condition.True,
          namedFragmentSelectionKey = SelectionKey(
              root = namedFragment.name.capitalize(),
              keys = listOf(namedFragment.name.capitalize()),
              type = SelectionKey.Type.Fragment,
          ),
          parentSelectionKey = parentSelectionKey,
          parentFields = parentSelectionSet,
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
        typeCondition = GQLNamedType(sourceLocation = SourceLocation.UNKNOWN, fragmentTypeCondition).toSchemaType(schema),
        possibleTypes = schema.typeDefinition(fragmentTypeCondition).possibleTypes(schema.typeDefinitions).toList(),
        description = fragmentDescription,
        selectionSet = parentSelectionSet,
        condition = fragmentCondition,
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
      : Map<List<GenericFragment>, List<String>> {
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

  private data class GenericFragment(
      val name: String,
      val typeCondition: IntrospectionSchema.TypeRef,
      val possibleTypes: List<String>,
      val description: String,
      val selectionSet: List<BackendIr.Field>,
      val condition: BackendIr.Condition,
      val nestedFragments: List<GenericFragment>,
      val selectionKeys: Set<SelectionKey>,
  )
}

