package com.apollographql.apollo.compiler.backend.ir

import com.apollographql.apollo.compiler.PackageNameProvider
import com.apollographql.apollo.compiler.backend.ir.BackendIrMergeUtils.mergeFields
import com.apollographql.apollo.compiler.backend.ir.SelectionKeyUtils.addFieldSelectionKey
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
  private lateinit var allFragmentDefinitions: Map<String, FrontendIr.NamedFragmentDefinition>

  fun buildBackendIR(frontendIr: FrontendIr): BackendIr {
    allFragmentDefinitions = frontendIr.allFragmentDefinitions

    return BackendIr(
        operations = frontendIr.operations.map { operation ->
          operation.buildBackendIrOperation()
        },
        fragments = frontendIr.fragmentDefinitions.map { fragment ->
          fragment.buildBackendIrNamedFragment()
        }
    )
  }

  private fun FrontendIr.Operation.buildBackendIrOperation(): BackendIr.Operation {
    val normalizedName = this.normalizeOperationName()

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
        description = description,
    )
        .buildBackendIrField(selectionKey)
        .buildFragmentImplementations(selectionKey)

    val variables = this.variables.map { variable ->
      BackendIr.Variable(
          name = variable.name,
          type = variable.type.toSchemaType()
      )
    }

    return BackendIr.Operation(
        name = normalizedName,
        operationName = name,
        targetPackageName = packageNameProvider?.operationPackageName(filePath = gqlOperationDefinition.sourceLocation.filePath ?: "")
            ?: "",
        operationType = IntrospectionSchema.TypeRef(
            kind = IntrospectionSchema.Kind.OBJECT,
            name = this.typeDefinition.name,
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
    return if (useSemanticNaming && !name.endsWith(operationType.name)) {
      name + operationType.name
    } else {
      name
    }
  }

  private fun FrontendIr.Selection.Field.buildBackendIrField(selectionKey: SelectionKey): BackendIr.Field {
    val selectionSet = selections.filterIsInstance<FrontendIr.Selection.Field>()
        .buildBackendIrFields(selectionKey)

    val possibleTypes = this.type.leafTypeDefinition.possibleTypes(schema.typeDefinitions)

    val genericFragments = buildGenericFragments(
        inlineFragments = selections.filterIsInstance<FrontendIr.Selection.InlineFragment>(),
        namedFragments = selections.filterIsInstance<FrontendIr.Selection.FragmentSpread>(),
        parentSelectionKey = selectionKey,
        parentSelectionSet = selectionSet,
    )

    val fragmentInterfaces = genericFragments.buildBackendIrFragmentInterfaces(
        fieldPossibleTypes = possibleTypes,
    )

    val arguments = this.arguments.map { argument ->
      BackendIr.Argument(
          name = argument.name,
          value = argument.value.toKotlinValue(false),
          type = argument.type.toSchemaType()
      )
    }

    return BackendIr.Field(
        name = this.name,
        alias = this.alias,
        type = type.toSchemaType(),
        args = arguments,
        fields = selectionSet,
        fragments = createFragments(
            selectionKey = selectionKey,
            fragments = fragmentInterfaces,
        ),
        deprecationReason = this.deprecationReason,
        description = this.description ?: "",
        condition = this.condition.toBackendIrCondition(),
        selectionKeys = setOf(selectionKey),
    )
  }

  private fun FrontendIr.Condition.toBackendIrCondition(): BackendIr.Condition {
    return when (this) {
      is FrontendIr.Condition.True -> BackendIr.Condition.True
      is FrontendIr.Condition.False -> BackendIr.Condition.False
      is FrontendIr.Condition.Or -> BackendIr.Condition.Or(conditions = conditions.map { it.toBackendIrCondition() }.toSet())
      is FrontendIr.Condition.And -> BackendIr.Condition.And(conditions = conditions.map { it.toBackendIrCondition() }.toSet())
      is FrontendIr.Condition.Variable -> BackendIr.Condition.Variable(name = name, inverted = inverted)
    }
  }

  private fun List<FrontendIr.Selection.Field>.buildBackendIrFields(selectionKey: SelectionKey): List<BackendIr.Field> {
    return this.map { field ->
      field.buildBackendIrField(
          selectionKey = selectionKey + field.responseName,
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
    )

    val implementationSelectionRootKey = SelectionKey(
        root = "${this@buildBackendIrNamedFragment.name.capitalize()}Impl",
        keys = listOf("${this@buildBackendIrNamedFragment.name.capitalize()}Impl"),
        type = SelectionKey.Type.Fragment,
    )

    val implementationSelectionSet = selectionSet
        .patchWithDefaultImplementationSelectionKey(
            fragmentNameToPatch = this.name,
            fragmentImplementationName = implementationSelectionRootKey.root,
        )
        .buildImplementationSelectionSet(
            implementationSelectionRootKey = implementationSelectionRootKey
        )

    return BackendIr.NamedFragment(
        name = this.name,
        defaultSelectionSetRootKey = implementationSelectionRootKey,
        source = this.source,
        comment = this.description ?: "",
        selectionSet = selectionSet,
        implementationSelectionSet = implementationSelectionSet,
    )
  }

  private fun FrontendIr.NamedFragmentDefinition.buildSelectionSet(rootSelectionKey: SelectionKey): BackendIr.NamedFragment.SelectionSet {
    val selectionSet = this.selections.filterIsInstance<FrontendIr.Selection.Field>()
        .buildBackendIrFields(rootSelectionKey)

    val possibleTypes = schema.typeDefinition(typeCondition.name).possibleTypes(schema.typeDefinitions)

    val fragments = buildGenericFragments(
        inlineFragments = this.selections.filterIsInstance<FrontendIr.Selection.InlineFragment>(),
        namedFragments = this.selections.filterIsInstance<FrontendIr.Selection.FragmentSpread>(),
        parentSelectionKey = rootSelectionKey,
        parentSelectionSet = selectionSet,
    )

    val fragmentInterfaces = fragments.buildBackendIrFragmentInterfaces(
        fieldPossibleTypes = possibleTypes
    )

    return BackendIr.NamedFragment.SelectionSet(
        fields = selectionSet,
        fragments = createFragments(
            selectionKey = rootSelectionKey,
            fragments = fragmentInterfaces
        ),
        typeCondition = FrontendIr.Type.Named(typeCondition).toSchemaType(),
        possibleTypes = possibleTypes.map { GQLNamedType(sourceLocation = SourceLocation.UNKNOWN, it).toSchemaType(schema) },
        selectionKeys = setOf(rootSelectionKey)
    )
  }

  private fun BackendIr.NamedFragment.SelectionSet.buildImplementationSelectionSet(
      implementationSelectionRootKey: SelectionKey,
  ): BackendIr.NamedFragment.SelectionSet {
    return if (this.fragments.isEmpty()) {
      this.copy(
          fields = this.fields.map { field ->
            field.buildFragmentImplementations(
                selectionKey = implementationSelectionRootKey + field.responseName
            )
          },
      )
    } else {
      val rootFragmentPossibleTypes = schema.typeDefinition(this.typeCondition.rawType.name!!)
          .possibleTypes(schema.typeDefinitions)
          .map { GQLNamedType(sourceLocation = SourceLocation.UNKNOWN, it).toSchemaType(schema) }
          .toSet()

      val fragmentInterfaces = this.fragments.filter { it.kind == BackendIr.Fragment.Kind.Interface }
      val groupedFragmentInterfaces = fragmentInterfaces.groupFragmentsByPossibleTypes()
      val fragmentImplementations = groupedFragmentInterfaces.map { (fragments, fragmentsPossibleTypes) ->
        fragments.buildFragmentImplementation(
            parentName = implementationSelectionRootKey.root,
            parentSelectionSet = this.fields,
            possibleTypes = rootFragmentPossibleTypes.intersect(fragmentsPossibleTypes),
            selectionKey = implementationSelectionRootKey,
        )
      }

      val fields = this.fields.map { field ->
        field.buildFragmentImplementations(
            selectionKey = implementationSelectionRootKey + field.responseName,
        )
      }

      val fallbackImplementation = BackendIr.Fragment(
          name = "Other${implementationSelectionRootKey.root}",
          fields = fields,
          possibleTypes = emptySet(),
          selectionKeys = this.selectionKeys + implementationSelectionRootKey,
          description = null,
          kind = BackendIr.Fragment.Kind.Fallback,
      )

      this.copy(
          fields = fields,
          fragments = BackendIr.Fragments(
              fragments = fragmentInterfaces + fragmentImplementations + fallbackImplementation,
              accessors = fragmentInterfaces
                  .flatMap { fragmentInterface ->
                    fragmentInterface.selectionKeys
                        .filter { selectionKey -> selectionKey.type == SelectionKey.Type.Fragment }
                        .takeUnless { it.isEmpty() }
                        ?: fragmentInterface.selectionKeys
                  }
                  .filter { key -> key != implementationSelectionRootKey }
                  .map { key ->
                    when {
                      key.type == SelectionKey.Type.Fragment && key.keys.size == 1 -> key.root.decapitalize() to key
                      else -> "as${key.keys.last().capitalize()}" to key
                    }
                  }
                  .toMap()
          ),
      )
    }
  }

  // do deep recursive traversal of all fields and nested fragments
  // find any selection key that belongs to the named fragment we generate default selection set for (`root == fragmentNameToPatch`)
  // copy it with `defaultImplementationName` as a root and add to the original selection set
  private fun BackendIr.NamedFragment.SelectionSet.patchWithDefaultImplementationSelectionKey(
      fragmentNameToPatch: String,
      fragmentImplementationName: String,
  ): BackendIr.NamedFragment.SelectionSet {
    return this.copy(
        fields = this.fields.map { field ->
          field.patchWithDefaultImplementationSelectionKey(
              fragmentNameToPatch = fragmentNameToPatch,
              defaultImplementationName = fragmentImplementationName,
          )
        },
        fragments = fragments.copy(
            fragments = fragments.map { fragment ->
              fragment.patchWithDefaultImplementationSelectionKey(
                  fragmentNameToPatch = fragmentNameToPatch,
                  defaultImplementationName = fragmentImplementationName,
              )
            }
        ),
        selectionKeys = this.selectionKeys + this.selectionKeys
            .filter { selectionKey ->
              selectionKey.type == SelectionKey.Type.Fragment && selectionKey.root == fragmentNameToPatch
            }
            .map { key ->
              key.copy(root = fragmentImplementationName, keys = listOf(fragmentImplementationName) + key.keys.drop(1))
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
        fragments = fragments.copy(
            fragments = fragments.fragments.map { fragment ->
              fragment.patchWithDefaultImplementationSelectionKey(
                  fragmentNameToPatch = fragmentNameToPatch,
                  defaultImplementationName = defaultImplementationName,
              )
            }
        )
    )
  }

  // do deep recursive traversal of all fields and nested fragments
  // find any selection key that belongs to the named fragment we generate default selection set for (`root == fragmentNameToPatch`)
  // copy it with `defaultImplementationName` as a root and add to the original selection set
  private fun BackendIr.Fragment.patchWithDefaultImplementationSelectionKey(
      fragmentNameToPatch: String,
      defaultImplementationName: String,
  ): BackendIr.Fragment {
    val selectionKeys = this.selectionKeys + this.selectionKeys
        .filter { selectionKey ->
          selectionKey.type == SelectionKey.Type.Fragment && selectionKey.root == fragmentNameToPatch
        }
        .map { key ->
          key.copy(root = defaultImplementationName, keys = listOf(defaultImplementationName) + key.keys.drop(1))
        }
    val fields = this.fields.map { field ->
      field.patchWithDefaultImplementationSelectionKey(
          fragmentNameToPatch = fragmentNameToPatch,
          defaultImplementationName = defaultImplementationName,
      )
    }
    return this.copy(
        selectionKeys = selectionKeys,
        fields = fields,
    )
  }

  private fun List<GenericFragment>.buildBackendIrFragmentInterfaces(
      fieldPossibleTypes: Set<String>,
  ): List<BackendIr.Fragment> {
    // we might get fragments defined with the same type condition - group them
    val groupedFragments = this.groupBy { fragment -> fragment.name }

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
      BackendIr.Fragment(
          name = fragments.first().name,
          fields = selectionSet,
          selectionKeys = selectionsKeys,
          possibleTypes = possibleTypes.map { GQLNamedType(sourceLocation = SourceLocation.UNKNOWN, it).toSchemaType(schema) }.toSet(),
          description = fragments.first().description,
          kind = BackendIr.Fragment.Kind.Interface,
      )
    }
  }

  private fun BackendIr.Field.buildFragmentImplementations(
      selectionKey: SelectionKey,
  ): BackendIr.Field {
    return if (this.fragments.isEmpty()) {
      this.copy(
          fields = this.fields.map { field ->
            field.buildFragmentImplementations(
                selectionKey = selectionKey + field.responseName,
            )
          }
      )
    } else {
      val fragmentInterfaces = this.fragments.filter { it.kind == BackendIr.Fragment.Kind.Interface }
      val groupedFragmentInterfaces = fragmentInterfaces.groupFragmentsByPossibleTypes()
      val fieldPossibleTypes = schema.typeDefinition(this.type.rawType.name!!)
          .possibleTypes(schema.typeDefinitions)
          .map { GQLNamedType(sourceLocation = SourceLocation.UNKNOWN, it).toSchemaType(schema) }
          .toSet()
      val fragmentImplementations = groupedFragmentInterfaces.map { (fragments, fragmentsPossibleTypes) ->
        fragments.buildFragmentImplementation(
            parentName = this.responseName,
            parentSelectionSet = this.fields,
            possibleTypes = fieldPossibleTypes.intersect(fragmentsPossibleTypes),
            selectionKey = selectionKey,
        )
      }
      val fields = this.fields.map { field ->
        field.buildFragmentImplementations(
            selectionKey = selectionKey + field.responseName,
        )
      }

      val fallbackImplementationFields = this.fields
          .addFieldSelectionKey(selectionKey + "Other${this.responseName.capitalize()}")
          .map { field ->
            field.buildFragmentImplementations(
                selectionKey = selectionKey + "Other${this.responseName.capitalize()}" + field.responseName,
            )
          }

      val fallbackImplementation = BackendIr.Fragment(
          name = "Other${this.responseName.capitalize()}",
          fields = fallbackImplementationFields,
          possibleTypes = emptySet(),
          selectionKeys = this.selectionKeys + selectionKey,
          description = null,
          kind = BackendIr.Fragment.Kind.Fallback,
      )

      this.copy(
          fields = fields,
          fragments = createFragments(
              selectionKey = selectionKey,
              fragments = fragmentInterfaces + fragmentImplementations + fallbackImplementation,
          ),
          selectionKeys = this.selectionKeys + selectionKey,
      )
    }
  }

  private fun List<BackendIr.Fragment>.buildFragmentImplementation(
      parentName: String,
      parentSelectionSet: List<BackendIr.Field>,
      possibleTypes: Set<IntrospectionSchema.TypeRef>,
      selectionKey: SelectionKey,
  ): BackendIr.Fragment {
    val fragmentName = this.distinctBy { fragment -> fragment.name }
        .joinToString(separator = "", postfix = parentName.capitalize()) { fragment -> fragment.name.capitalize() }

    val selectionSet = this.fold(parentSelectionSet) { acc, fragment ->
      acc.mergeFields(
          fragment.fields.addFieldSelectionKey(selectionKey + fragmentName)
      )
    }.map { field ->
      field.buildFragmentImplementations(
          selectionKey = selectionKey + fragmentName + field.responseName,
      )
    }

    val selectionsKeys = this.fold(emptySet<SelectionKey>()) { acc, fragment ->
      acc.plus(fragment.selectionKeys)
    }.plus(selectionKey)

    return BackendIr.Fragment(
        name = fragmentName,
        fields = selectionSet,
        possibleTypes = possibleTypes.toSet(),
        selectionKeys = selectionsKeys,
        description = null,
        kind = BackendIr.Fragment.Kind.Implementation,
    )
  }

  private fun buildGenericFragments(
      inlineFragments: List<FrontendIr.Selection.InlineFragment>,
      namedFragments: List<FrontendIr.Selection.FragmentSpread>,
      parentSelectionKey: SelectionKey,
      parentSelectionSet: List<BackendIr.Field>,
      parentNamedFragmentSelectionKey: SelectionKey? = null,
  ): List<GenericFragment> {
    val genericInlineFragments = inlineFragments
        .map { inlineFragment ->
          val fragmentName = inlineFragment.fragmentDefinition.typeCondition.name.capitalize()
          buildGenericFragment(
              fragmentName = fragmentName,
              fragmentTypeCondition = inlineFragment.fragmentDefinition.typeCondition.name,
              fragmentSelectionSet = inlineFragment.fragmentDefinition.selections,
              fragmentDescription = inlineFragment.fragmentDefinition.typeCondition.description ?: "",
              fragmentCondition = inlineFragment.condition.toBackendIrCondition(),
              namedFragmentSelectionKey = parentNamedFragmentSelectionKey?.plus(fragmentName),
              parentSelectionKey = parentSelectionKey,
              parentSelectionSet = parentSelectionSet.addFieldSelectionKey(
                  parentNamedFragmentSelectionKey?.plus(fragmentName)
              ),
          )
        }

    val genericNamedFragments = namedFragments
        .mapNotNull { fragmentSpread -> allFragmentDefinitions[fragmentSpread.name] }
        .flatMap { namedFragment ->
          val namedFragmentSelectionKey = SelectionKey(
              root = namedFragment.name.capitalize(),
              keys = listOf(namedFragment.name.capitalize()),
              type = SelectionKey.Type.Fragment,
          )

          val genericFragment = buildGenericFragment(
              fragmentName = namedFragment.typeCondition.name.capitalize(),
              fragmentTypeCondition = namedFragment.typeCondition.name,
              fragmentSelectionSet = namedFragment.selections.filterIsInstance<FrontendIr.Selection.Field>(),
              fragmentDescription = namedFragment.description ?: "",
              fragmentCondition = BackendIr.Condition.True,
              namedFragmentSelectionKey = parentNamedFragmentSelectionKey?.plus(namedFragment.typeCondition.name.capitalize())
                  ?: namedFragmentSelectionKey,
              parentSelectionKey = parentSelectionKey,
              parentSelectionSet = parentSelectionSet.addFieldSelectionKey(
                  parentNamedFragmentSelectionKey?.plus(namedFragment.typeCondition.name.capitalize())
              ),
          )

          val nestedGenericFragments = buildGenericFragments(
              inlineFragments = namedFragment.selections.filterIsInstance<FrontendIr.Selection.InlineFragment>(),
              namedFragments = namedFragment.selections.filterIsInstance<FrontendIr.Selection.FragmentSpread>(),
              parentSelectionKey = parentSelectionKey,
              parentSelectionSet = genericFragment.selectionSet,
              parentNamedFragmentSelectionKey = namedFragmentSelectionKey,
          )

          listOf(genericFragment) + nestedGenericFragments
        }

    return genericInlineFragments + genericNamedFragments
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
      fragmentName: String,
      fragmentTypeCondition: String,
      fragmentSelectionSet: List<FrontendIr.Selection>,
      fragmentDescription: String,
      fragmentCondition: BackendIr.Condition,
      namedFragmentSelectionKey: SelectionKey?,
      parentSelectionKey: SelectionKey,
      parentSelectionSet: List<BackendIr.Field>,
  ): GenericFragment {
    val selectionSet = parentSelectionSet
        .addFieldSelectionKey(parentSelectionKey + fragmentName)
        .mergeFields(
            fragmentSelectionSet
                .filterIsInstance<FrontendIr.Selection.Field>()
                .buildBackendIrFields(parentSelectionKey + fragmentName)
                .addFieldSelectionKey(namedFragmentSelectionKey)
        )

    val selectionKeys = listOfNotNull(
        parentSelectionKey,
        parentSelectionKey + fragmentName,
        namedFragmentSelectionKey
    ).toSet()

    return GenericFragment(
        name = fragmentName,
        typeCondition = GQLNamedType(sourceLocation = SourceLocation.UNKNOWN, fragmentTypeCondition).toSchemaType(schema),
        possibleTypes = schema.typeDefinition(fragmentTypeCondition).possibleTypes(schema.typeDefinitions).toList(),
        description = fragmentDescription,
        selectionSet = selectionSet,
        condition = fragmentCondition,
        selectionKeys = selectionKeys,
    )
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
  private fun List<BackendIr.Fragment>.groupFragmentsByPossibleTypes()
      : Map<List<BackendIr.Fragment>, List<IntrospectionSchema.TypeRef>> {
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

  private fun createFragments(
      selectionKey: SelectionKey,
      fragments: List<BackendIr.Fragment>,
  ): BackendIr.Fragments {
    return BackendIr.Fragments(
        fragments = fragments,
        accessors = fragments
            .filter { fragment -> fragment.kind == BackendIr.Fragment.Kind.Interface }
            .flatMap { fragmentInterface -> fragmentInterface.selectionKeys }
            .filter { key -> key != selectionKey }
            .map { key ->
              when {
                key.type == SelectionKey.Type.Fragment && key.keys.size == 1 -> key.root.decapitalize() to key
                else -> "as${key.keys.last().capitalize()}" to key
              }
            }
            .toMap()
    )
  }

  private data class GenericFragment(
      val name: String,
      val typeCondition: IntrospectionSchema.TypeRef,
      val possibleTypes: List<String>,
      val description: String,
      val selectionSet: List<BackendIr.Field>,
      val condition: BackendIr.Condition,
      val selectionKeys: Set<SelectionKey>,
  )
}

