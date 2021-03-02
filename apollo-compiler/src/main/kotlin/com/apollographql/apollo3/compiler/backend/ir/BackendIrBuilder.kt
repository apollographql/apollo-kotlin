package com.apollographql.apollo3.compiler.backend.ir

import com.apollographql.apollo3.compiler.PackageNameProvider
import com.apollographql.apollo3.compiler.backend.ir.BackendIrMergeUtils.mergeFields
import com.apollographql.apollo3.compiler.backend.ir.SelectionKeyUtils.addFieldSelectionKey
import com.apollographql.apollo3.compiler.backend.ir.SelectionKeyUtils.attachToNewSelectionRoot
import com.apollographql.apollo3.compiler.backend.ir.SelectionKeyUtils.isBelongToNamedFragment
import com.apollographql.apollo3.compiler.backend.ir.SelectionKeyUtils.removeFragmentSelectionKeys
import com.apollographql.apollo3.compiler.frontend.GQLNamedType
import com.apollographql.apollo3.compiler.frontend.Schema
import com.apollographql.apollo3.compiler.frontend.SourceLocation
import com.apollographql.apollo3.compiler.frontend.ir.FrontendIr
import com.apollographql.apollo3.compiler.frontend.possibleTypes
import com.apollographql.apollo3.compiler.frontend.schemaKind
import com.apollographql.apollo3.compiler.frontend.toKotlinValue
import com.apollographql.apollo3.compiler.frontend.toSchemaType
import com.apollographql.apollo3.compiler.introspection.IntrospectionSchema

internal class BackendIrBuilder(
    private val schema: Schema,
    private val useSemanticNaming: Boolean,
    private val generateFragmentsAsInterfaces: Boolean,
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

    val packageName = packageNameProvider?.operationPackageName(filePath = gqlOperationDefinition.sourceLocation.filePath ?: "") ?: ""

    val variables = this.variables.map { variable ->
      BackendIr.Variable(
          name = variable.name,
          type = variable.type.toSchemaType(),
          hasDefaultValue = variable.defaultValue != null
      )
    }

    val operationSchemaType = IntrospectionSchema.TypeRef(
        kind = IntrospectionSchema.Kind.OBJECT,
        name = this.typeDefinition.name,
    )

    val dataFieldSelectionKey = SelectionKey(
        root = normalizedName,
        keys = listOf(normalizedName, "data"),
        type = SelectionKey.Type.Query,
    )

    val dataField = FrontendIr.Selection.Field(
        name = "data",
        alias = null,
        arguments = emptyList(),
        condition = FrontendIr.Condition.True,
        selections = this.selections,
        type = FrontendIr.Type.Named(typeDefinition),
        deprecationReason = null,
        description = description,
    )
        .buildBackendIrField(dataFieldSelectionKey)
        .buildFragmentImplementations(
            selectionKey = dataFieldSelectionKey,
            keepInterfaces = true,
        )

    return BackendIr.Operation(
        name = normalizedName,
        operationName = this.name,
        targetPackageName = packageName,
        operationSchemaType = operationSchemaType,
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
    val fields = selections.filterIsInstance<FrontendIr.Selection.Field>()
        .buildBackendIrFields(selectionKey)

    val fieldPossibleTypes = this.type.leafTypeDefinition.possibleTypes(schema.typeDefinitions)

    val genericFragments = buildGenericFragments(
        inlineFragments = selections.filterIsInstance<FrontendIr.Selection.InlineFragment>(),
        namedFragments = selections.filterIsInstance<FrontendIr.Selection.FragmentSpread>(),
        parentSelectionKey = selectionKey,
        parentFields = fields,
    )

    val fragments = if (generateFragmentsAsInterfaces) {
      val fragmentInterfaces = genericFragments.buildBackendIrFragmentInterfaces(
          parentSelectionKey = selectionKey,
          parentPossibleTypes = fieldPossibleTypes,
      )
      createFragments(
          selectionKey = selectionKey,
          fragments = fragmentInterfaces,
      )
    } else {
      val fragmentImplementations = genericFragments.buildFragmentImplementations(
          parentSelectionKey = selectionKey,
          parentPossibleTypes = fieldPossibleTypes,
      )
      BackendIr.Fragments(
          fragments = fragmentImplementations,
          accessors = fragmentImplementations.map { fragment ->
            val selectionKey = if (fragment.type is BackendIr.Fragment.Type.Delegate) {
              fragment.type.fragmentSelectionKey
            } else {
              selectionKey + fragment.name
            }
            fragment.name.decapitalize() to selectionKey
          }.toMap()
      )
    }

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
        schemaTypeRef = this.type.toSchemaType(),
        typeName = this.responseName,
        args = arguments,
        fields = fields,
        fragments = fragments,
        deprecationReason = this.deprecationReason,
        description = this.description ?: "",
        condition = this.condition.buildBackendIrCondition(),
        selectionKeys = setOf(selectionKey),
    )
  }

  private fun FrontendIr.Condition.buildBackendIrCondition(): BackendIr.Condition {
    return when (this) {
      is FrontendIr.Condition.True -> BackendIr.Condition.True
      is FrontendIr.Condition.False -> BackendIr.Condition.False
      is FrontendIr.Condition.Or -> BackendIr.Condition.Or(conditions = conditions.map { it.buildBackendIrCondition() }.toSet())
      is FrontendIr.Condition.And -> BackendIr.Condition.And(conditions = conditions.map { it.buildBackendIrCondition() }.toSet())
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
    val selectionSet = this.buildSelectionSet()
    val implementationSelectionSet = if (generateFragmentsAsInterfaces) {
      selectionSet.buildImplementationSelectionSet()
    } else {
      selectionSet.convertToImplementationSelectionSet()
    }
    return BackendIr.NamedFragment(
        source = this.source,
        comment = this.description ?: "",
        selectionSet = selectionSet,
        implementationSelectionSet = implementationSelectionSet,
        variables = this.variables.map { variable ->
          BackendIr.Variable(
              name = variable.name,
              type = variable.type.toSchemaType(),
              hasDefaultValue = false
          )
        }
    )
  }

  private fun FrontendIr.NamedFragmentDefinition.buildSelectionSet(): BackendIr.NamedFragment.SelectionSet {
    val defaultSelectionKey = SelectionKey(
        root = this.name,
        keys = listOf(this.name),
        type = SelectionKey.Type.Fragment,
    )

    val fields = this.selections.filterIsInstance<FrontendIr.Selection.Field>()
        .buildBackendIrFields(defaultSelectionKey)

    val fragmentPossibleTypes = schema.typeDefinition(typeCondition.name).possibleTypes(schema.typeDefinitions)
    val fragmentPossibleSchemaTypes = fragmentPossibleTypes.map { type ->
      GQLNamedType(sourceLocation = SourceLocation.UNKNOWN, type).toSchemaType(schema)
    }.toSet()

    val genericFragments = buildGenericFragments(
        inlineFragments = this.selections.filterIsInstance<FrontendIr.Selection.InlineFragment>(),
        namedFragments = this.selections.filterIsInstance<FrontendIr.Selection.FragmentSpread>(),
        parentSelectionKey = defaultSelectionKey,
        parentFields = fields,
    )

    val fragments = if (generateFragmentsAsInterfaces) {
      val fragmentInterfaces = genericFragments.buildBackendIrFragmentInterfaces(
          parentSelectionKey = defaultSelectionKey,
          parentPossibleTypes = fragmentPossibleTypes,
      )
      createFragments(
          selectionKey = defaultSelectionKey,
          fragments = fragmentInterfaces,
      )
    } else {
      val fragmentImplementations = genericFragments.buildFragmentImplementations(
          parentSelectionKey = defaultSelectionKey,
          parentPossibleTypes = fragmentPossibleTypes,
      )
      BackendIr.Fragments(
          fragments = fragmentImplementations,
          accessors = fragmentImplementations.map { fragment ->
            val selectionKey = if (fragment.type is BackendIr.Fragment.Type.Delegate) {
              fragment.type.fragmentSelectionKey
            } else {
              defaultSelectionKey + fragment.name
            }
            fragment.name.decapitalize() to selectionKey
          }.toMap()
      )
    }

    return BackendIr.NamedFragment.SelectionSet(
        name = this.name,
        fields = fields,
        fragments = fragments,
        typeCondition = FrontendIr.Type.Named(typeCondition).toSchemaType(),
        possibleTypes = fragmentPossibleSchemaTypes,
        defaultSelectionKey = defaultSelectionKey,
        selectionKeys = setOf(defaultSelectionKey)
    )
  }

  private fun BackendIr.NamedFragment.SelectionSet.convertToImplementationSelectionSet(): BackendIr.NamedFragment.SelectionSet {
    val selectionKey = SelectionKey(
        root = this.name.capitalize(),
        keys = listOf(this.name.capitalize(), "Data"),
        type = SelectionKey.Type.Fragment,
    )
    return this.copy(
        name = selectionKey.root,
        fields = this.fields,
        fragments = this.fragments,
        defaultSelectionKey = selectionKey,
        selectionKeys = setOf(selectionKey),
    )
  }

  private fun BackendIr.NamedFragment.SelectionSet.buildImplementationSelectionSet(): BackendIr.NamedFragment.SelectionSet {
    val defaultSelectionKey = SelectionKey(
        root = "${this.name.capitalize()}Impl",
        keys = listOf("${this.name.capitalize()}Impl", "Data"),
        type = SelectionKey.Type.Fragment,
    )

    if (this.fragments.isEmpty()) {
      return this.copy(
          name = defaultSelectionKey.root,
          fields = this.fields.map { field ->
            field.buildFragmentImplementations(
                selectionKey = defaultSelectionKey + field.typeName,
                keepInterfaces = false,
            )
          },
          defaultSelectionKey = defaultSelectionKey,
      )
    }

    val fragmentPossibleSchemaTypes = schema.typeDefinition(this.typeCondition.rawType.name!!)
        .possibleTypes(schema.typeDefinitions)
        .map { type -> GQLNamedType(sourceLocation = SourceLocation.UNKNOWN, type).toSchemaType(schema) }
        .toSet()

    val fragmentInterfaces = this.fragments.filter { it.type == BackendIr.Fragment.Type.Interface }

    val fragmentImplementations = fragmentInterfaces
        .flattenBackendFragments()
        .groupFragmentsByPossibleTypes()
        .map { (fragments, fragmentsPossibleTypes) ->
          fragments.buildFragmentImplementation(
              parentName = defaultSelectionKey.keys.last(),
              parentFields = this.fields,
              possibleSchemaTypes = fragmentPossibleSchemaTypes.intersect(fragmentsPossibleTypes),
              selectionKey = defaultSelectionKey,
          )
        }

    val fields = this.fields.map { field ->
      field.buildFragmentImplementations(
          selectionKey = defaultSelectionKey + field.typeName,
          keepInterfaces = true,
      )
    }

    val fallbackImplementation = BackendIr.Fragment(
        name = "Other${defaultSelectionKey.keys.last()}",
        fields = fields,
        nestedFragments = null,
        possibleTypes = emptySet(),
        selectionKeys = this.selectionKeys + defaultSelectionKey,
        description = null,
        type = BackendIr.Fragment.Type.Fallback,
    )

    val fragments = BackendIr.Fragments(
        fragments = fragmentImplementations + fallbackImplementation,
        accessors = emptyMap(),
    )

    return this.copy(
        name = defaultSelectionKey.root,
        fields = fields.filter { it.name == "__typename" },
        fragments = fragments,
        defaultSelectionKey = defaultSelectionKey,
    )
  }

  private fun List<GenericFragment>.buildBackendIrFragmentInterfaces(
      parentSelectionKey: SelectionKey,
      parentPossibleTypes: Set<String>,
  ): List<BackendIr.Fragment> {
    // we might get fragments defined with the same name
    val groupedFragments = this.groupBy { fragment -> fragment.name }

    // merge fragments with the same type condition into one interface
    return groupedFragments.map { (_, fragments) ->
      val fields = fragments.fold(emptyList<BackendIr.Field>()) { acc, fragment ->
        acc.mergeFields(fragment.fields)
      }
      val selectionsKeys = fragments.fold(emptySet<SelectionKey>()) { acc, fragment ->
        acc.plus(fragment.selectionKeys)
      }
      val nestedFragments = fragments.flatMap { it.fragments }.buildBackendIrFragmentInterfaces(
          parentSelectionKey = parentSelectionKey + fragments.first().name,
          parentPossibleTypes = fragments.first().possibleTypes.toSet(),
      )

      // as fragment can be defined on interface that has more possible implementations than field type where it used
      // build intersection of fragment's and field's possible types
      val possibleTypes = fragments.flatMap { it.possibleTypes }.intersect(parentPossibleTypes)

      val possibleSchemaTypes = possibleTypes.map { type ->
        GQLNamedType(sourceLocation = SourceLocation.UNKNOWN, type).toSchemaType(schema)
      }.toSet()

      BackendIr.Fragment(
          name = fragments.first().name,
          fields = fields,
          nestedFragments = createFragments(
              selectionKey = parentSelectionKey,
              fragments = nestedFragments,
          ),
          selectionKeys = selectionsKeys,
          possibleTypes = possibleSchemaTypes,
          description = fragments.joinToString { it.description },
          type = BackendIr.Fragment.Type.Interface,
      )
    }
  }

  private fun List<GenericFragment>.buildFragmentImplementations(
      parentSelectionKey: SelectionKey,
      parentPossibleTypes: Set<String>,
  ): List<BackendIr.Fragment> {
    // we might get fragments defined with the same name
    return this
        .groupBy { fragment -> "As${fragment.name.capitalize()}" }
        .map { (fragmentName, fragments) ->
          val fields = fragments.fold(emptyList<BackendIr.Field>()) { acc, fragment ->
            acc.mergeFields(fragment.fields)
          }
          val possibleTypes = fragments.flatMap { it.possibleTypes }
              .intersect(parentPossibleTypes)
              .map { type -> GQLNamedType(sourceLocation = SourceLocation.UNKNOWN, type).toSchemaType(schema) }
              .toSet()

          val namedFragmentSelectionKey = fragments.first().namedFragmentSelectionKey
          val type = if (namedFragmentSelectionKey == null) {
            BackendIr.Fragment.Type.Implementation
          } else {
            BackendIr.Fragment.Type.Delegate(namedFragmentSelectionKey + "Data")
          }

          BackendIr.Fragment(
              name = fragmentName,
              fields = fields.addFieldSelectionKey(parentSelectionKey + fragmentName),
              nestedFragments = null,
              selectionKeys = emptySet(),
              possibleTypes = possibleTypes,
              description = fragments.joinToString { it.description },
              type = type,
          )
        }
  }

  private fun BackendIr.Field.buildFragmentImplementations(
      selectionKey: SelectionKey,
      keepInterfaces: Boolean,
  ): BackendIr.Field {
    if (this.fragments.none { it.type == BackendIr.Fragment.Type.Interface }) {
      val fields = this.fields.map { field ->
        field.buildFragmentImplementations(
            selectionKey = selectionKey + field.typeName,
            keepInterfaces = keepInterfaces,
        )
      }
      return this.copy(
          fields = fields
      )
    }

    val fieldPossibleSchemaTypes = schema.typeDefinition(this.schemaTypeRef.rawType.name!!)
        .possibleTypes(schema.typeDefinitions)
        .map { GQLNamedType(sourceLocation = SourceLocation.UNKNOWN, it).toSchemaType(schema) }
        .toSet()

    val fragmentInterfaces = this.fragments
        .mergeInterfaceFragmentsWithTheSameName()

    val fragmentInterfaceRootSelectionKeys = fragmentInterfaces
        .map { selectionKey + it.name }

    val fragmentImplementations = fragmentInterfaces
        .flattenBackendFragments()
        .buildFragmentImplementations(
            parentName = this.typeName,
            // NOTE: required by new version that removes interfaces
            parentFields = this.fields.addFieldSelectionKey(selectionKey),
            parentPossibleSchemaTypes = fieldPossibleSchemaTypes,
            parentSelectionKeys = this.selectionKeys,
            selectionKey = selectionKey,
        )
        // NOTE: new version that removes interfaces
        .map { it.removeFragmentSelectionKeys(fragmentInterfaceRootSelectionKeys) }

    // NOTE: new version that removes interfaces
    return this.copy(
        fields = this.fields,
        fragments = BackendIr.Fragments(
            fragments = fragmentImplementations,
            accessors = fragmentImplementations
                .filterNot { it.type == BackendIr.Fragment.Type.Fallback }
                .map { "as${it.name.capitalize()}" to selectionKey + it.name }
                .plus(
                    // add accessors for named fragment interfaces
                    fragmentImplementations
                        .flatMap { fragment ->
                          fragment.selectionKeys.filter { fragmentSelectionKey ->
                            // filter only root named fragment keys that doesn't belong to current one
                            fragmentSelectionKey.type == SelectionKey.Type.Fragment &&
                                fragmentSelectionKey.keys.size == 1 &&
                                (selectionKey.type != SelectionKey.Type.Fragment || selectionKey.root != fragmentSelectionKey.root)
                          }
                        }
                        .map { fragmentSelectionKey -> "as${fragmentSelectionKey.root.capitalize()}" to fragmentSelectionKey }
                )
                .toMap()
        ),
        selectionKeys = this.selectionKeys + selectionKey,
    )

    // NOTE: this is old version that keeps interfaces
//    return this.copy(
//        fields = this.fields.takeIf { keepInterfaces } ?: this.fields.filter { it.name == "__typename" },
//        fragments = BackendIr.Fragments(
//            fragments = (fragmentInterfaces.takeIf { keepInterfaces } ?: emptyList()) + fragmentImplementations,
//            accessors = (this.fragments.accessors.takeIf { keepInterfaces } ?: emptyMap()),
//        ),
//        selectionKeys = this.selectionKeys + selectionKey,
//    )
  }

  private fun List<BackendIr.Fragment>.buildFragmentImplementations(
      parentName: String,
      parentFields: List<BackendIr.Field>,
      parentPossibleSchemaTypes: Set<IntrospectionSchema.TypeRef>,
      parentSelectionKeys: Set<SelectionKey>,
      selectionKey: SelectionKey,
  ): List<BackendIr.Fragment> {
    if (this.isEmpty()) return emptyList()

    val fragmentImplementations = this.groupFragmentsByPossibleTypes()
        .map { (fragments, fragmentsPossibleTypes) ->
          fragments.buildFragmentImplementation(
              parentName = parentName,
              parentFields = parentFields,
              possibleSchemaTypes = parentPossibleSchemaTypes.intersect(fragmentsPossibleTypes),
              selectionKey = selectionKey,
          )
        }

    val fallbackImplementationFields = parentFields.map { field ->
      field.buildFragmentImplementations(
          selectionKey = selectionKey + "Other${parentName.capitalize()}" + field.typeName,
          keepInterfaces = false,
      )
    }.addFieldSelectionKey(
        selectionKey + "Other${parentName.capitalize()}"
    )

    val fallbackImplementation = BackendIr.Fragment(
        name = "Other${parentName.capitalize()}",
        fields = fallbackImplementationFields,
        nestedFragments = null,
        possibleTypes = emptySet(),
        selectionKeys = parentSelectionKeys + selectionKey,
        description = null,
        type = BackendIr.Fragment.Type.Fallback,
    )

    return fragmentImplementations + fallbackImplementation
  }

  private fun List<BackendIr.Fragment>.flattenBackendFragments(): List<BackendIr.Fragment> {
    return this.flatMap { fragment ->
      listOf(fragment.copy(nestedFragments = null)) + (fragment.nestedFragments?.fragments?.flattenBackendFragments() ?: emptyList())
    }
  }

  private fun List<BackendIr.Fragment>.buildFragmentImplementation(
      parentName: String,
      parentFields: List<BackendIr.Field>,
      possibleSchemaTypes: Set<IntrospectionSchema.TypeRef>,
      selectionKey: SelectionKey,
  ): BackendIr.Fragment {
    val fragmentName = this.distinctBy { fragment -> fragment.name }
        .joinToString(separator = "", postfix = parentName.capitalize()) { fragment -> fragment.name.capitalize() }

    val fields = this.fold(parentFields) { acc, fragment ->
      acc.mergeFields(fragment.fields)
    }.run {
      this.map { field ->
        field.buildFragmentImplementations(
            selectionKey = selectionKey + fragmentName + field.typeName,
            keepInterfaces = false,
        )
      }
    }.addFieldSelectionKey(selectionKey + fragmentName)

    val selectionsKeys = this.fold(emptySet<SelectionKey>()) { acc, fragment ->
      acc + fragment.selectionKeys
    }.plus(selectionKey)

    return BackendIr.Fragment(
        name = fragmentName,
        fields = fields,
        nestedFragments = null,
        possibleTypes = possibleSchemaTypes.toSet(),
        selectionKeys = selectionsKeys,
        description = null,
        type = BackendIr.Fragment.Type.Implementation,
    )
  }

  private fun buildGenericFragments(
      inlineFragments: List<FrontendIr.Selection.InlineFragment>,
      namedFragments: List<FrontendIr.Selection.FragmentSpread>,
      parentSelectionKey: SelectionKey,
      parentFields: List<BackendIr.Field>,
  ): List<GenericFragment> {
    val genericInlineFragments = inlineFragments.map { inlineFragment ->
      val fragmentName = inlineFragment.fragmentDefinition.typeCondition.name.capitalize()

      buildGenericFragment(
          fragmentName = fragmentName,
          fragmentTypeCondition = inlineFragment.fragmentDefinition.typeCondition.name,
          fragmentFields = inlineFragment.fragmentDefinition.selections.filterIsInstance<FrontendIr.Selection.Field>(),
          nestedInlineFragments = inlineFragment.fragmentDefinition.selections.filterIsInstance<FrontendIr.Selection.InlineFragment>(),
          nestedNamedFragments = inlineFragment.fragmentDefinition.selections.filterIsInstance<FrontendIr.Selection.FragmentSpread>(),
          fragmentDescription = inlineFragment.fragmentDefinition.typeCondition.description ?: "",
          fragmentCondition = inlineFragment.condition.buildBackendIrCondition(),
          namedFragmentSelectionKey = null,
          parentSelectionKey = parentSelectionKey,
          parentFields = parentFields,
      )
    }

    val genericNamedFragments = namedFragments.map { fragmentSpread ->
      val namedFragment = checkNotNull(allFragmentDefinitions[fragmentSpread.name])

      val fragmentName = if (generateFragmentsAsInterfaces) {
        namedFragment.typeCondition.name.capitalize()
      } else {
        namedFragment.name.capitalize()
      }

      val namedFragmentSelectionKey = SelectionKey(
          root = namedFragment.name.capitalize(),
          keys = listOf(namedFragment.name.capitalize()),
          type = SelectionKey.Type.Fragment,
      )

      buildGenericFragment(
          fragmentName = fragmentName,
          fragmentTypeCondition = namedFragment.typeCondition.name,
          fragmentFields = namedFragment.selections.filterIsInstance<FrontendIr.Selection.Field>(),
          nestedInlineFragments = namedFragment.selections.filterIsInstance<FrontendIr.Selection.InlineFragment>(),
          nestedNamedFragments = namedFragment.selections.filterIsInstance<FrontendIr.Selection.FragmentSpread>(),
          fragmentDescription = namedFragment.description ?: "",
          fragmentCondition = BackendIr.Condition.True,
          namedFragmentSelectionKey = namedFragmentSelectionKey,
          parentFields = parentFields,
          parentSelectionKey = parentSelectionKey,
      )
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
  @Suppress("NAME_SHADOWING")
  private fun buildGenericFragment(
      fragmentName: String,
      fragmentTypeCondition: String,
      fragmentFields: List<FrontendIr.Selection.Field>,
      nestedInlineFragments: List<FrontendIr.Selection.InlineFragment>,
      nestedNamedFragments: List<FrontendIr.Selection.FragmentSpread>,
      fragmentDescription: String,
      fragmentCondition: BackendIr.Condition,
      namedFragmentSelectionKey: SelectionKey?,
      parentFields: List<BackendIr.Field>,
      parentSelectionKey: SelectionKey,
  ): GenericFragment {
    val fields = parentFields
        .attachToNewSelectionRoot(parentSelectionKey + fragmentName)
        .mergeFields(
            fragmentFields
                .buildBackendIrFields(parentSelectionKey + fragmentName)
                .addFieldSelectionKey(namedFragmentSelectionKey)
        )

    val selectionKey = parentSelectionKey + fragmentName

    val selectionKeys = listOfNotNull(
        parentSelectionKey,
        selectionKey,
        namedFragmentSelectionKey
    ).toSet()

    val nestedInlineFragments = nestedInlineFragments.map { inlineFragment ->
      val nestedFragmentName = inlineFragment.fragmentDefinition.typeCondition.name.capitalize()

      val parentFields = if (namedFragmentSelectionKey != null) {
        fields.map { field ->
          if (field.isBelongToNamedFragment(namedFragmentSelectionKey.root)) {
            field.addFieldSelectionKey(namedFragmentSelectionKey + nestedFragmentName + field.responseName)
          } else {
            field
          }
        }
      } else fields

      buildGenericFragment(
          fragmentName = nestedFragmentName,
          fragmentTypeCondition = inlineFragment.fragmentDefinition.typeCondition.name,
          fragmentFields = inlineFragment.fragmentDefinition.selections.filterIsInstance<FrontendIr.Selection.Field>(),
          nestedInlineFragments = inlineFragment.fragmentDefinition.selections.filterIsInstance<FrontendIr.Selection.InlineFragment>(),
          nestedNamedFragments = inlineFragment.fragmentDefinition.selections.filterIsInstance<FrontendIr.Selection.FragmentSpread>(),
          fragmentDescription = inlineFragment.fragmentDefinition.typeCondition.description ?: "",
          fragmentCondition = inlineFragment.condition.buildBackendIrCondition(),
          namedFragmentSelectionKey = namedFragmentSelectionKey?.plus(nestedFragmentName),
          parentFields = parentFields,
          parentSelectionKey = selectionKey,
      )
    }

    val nestedNamedFragments = nestedNamedFragments.map { fragmentSpread ->
      val namedFragment = checkNotNull(allFragmentDefinitions[fragmentSpread.name])

      val nestedFragmentName = if (generateFragmentsAsInterfaces) {
        namedFragment.typeCondition.name.capitalize()
      } else {
        namedFragment.name.capitalize()
      }

      val parentFields = if (namedFragmentSelectionKey != null) {
        fields.map { field ->
          if (field.isBelongToNamedFragment(namedFragmentSelectionKey.root)) {
            field.addFieldSelectionKey(namedFragmentSelectionKey + nestedFragmentName + field.responseName)
          } else {
            field
          }
        }
      } else fields

      buildGenericFragment(
          fragmentName = nestedFragmentName,
          fragmentTypeCondition = namedFragment.typeCondition.name,
          fragmentFields = namedFragment.selections.filterIsInstance<FrontendIr.Selection.Field>(),
          nestedInlineFragments = namedFragment.selections.filterIsInstance<FrontendIr.Selection.InlineFragment>(),
          nestedNamedFragments = namedFragment.selections.filterIsInstance<FrontendIr.Selection.FragmentSpread>(),
          fragmentDescription = namedFragment.typeCondition.description ?: "",
          fragmentCondition = fragmentSpread.condition.buildBackendIrCondition(),
          namedFragmentSelectionKey = namedFragmentSelectionKey.takeIf { generateFragmentsAsInterfaces }?.plus(nestedFragmentName)
              ?: SelectionKey(
                  root = namedFragment.name.capitalize(),
                  keys = listOf(namedFragment.name.capitalize()),
                  type = SelectionKey.Type.Fragment,
              ),
          parentFields = parentFields,
          parentSelectionKey = selectionKey,
      )
    }

    return GenericFragment(
        name = fragmentName,
        typeCondition = GQLNamedType(sourceLocation = SourceLocation.UNKNOWN, fragmentTypeCondition).toSchemaType(schema),
        possibleTypes = schema.typeDefinition(fragmentTypeCondition).possibleTypes(schema.typeDefinitions).toList(),
        description = fragmentDescription,
        fields = fields,
        fragments = nestedInlineFragments + nestedNamedFragments,
        condition = fragmentCondition,
        selectionKeys = selectionKeys,
        namedFragmentSelectionKey = namedFragmentSelectionKey
    )
  }

  private fun List<GenericFragment>.flattenGenericFragments(): List<GenericFragment> {
    return this + this.flatMap { fragment -> fragment.fragments.flattenGenericFragments() }
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
  private fun List<BackendIr.Fragment>.groupFragmentsByPossibleTypes(): Map<List<BackendIr.Fragment>, List<IntrospectionSchema.TypeRef>> {
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

  private fun List<BackendIr.Fragment>.mergeInterfaceFragmentsWithTheSameName(): List<BackendIr.Fragment> {
    this.forEach { fragment -> check(fragment.type == BackendIr.Fragment.Type.Interface) }

    return this
        .groupBy { fragment -> fragment.name }
        .map { (name, fragments) ->
          BackendIr.Fragment(
              name = name,
              fields = fragments.fold(emptyList()) { acc, fragment -> acc.mergeFields(fragment.fields) },
              nestedFragments = BackendIr.Fragments(
                  fragments = fragments.flatMap { it.nestedFragments ?: emptyList() },
                  accessors = fragments.fold(emptyMap()) { acc, fragment -> acc.plus(fragment.nestedFragments?.accessors ?: emptyMap()) }
              ).takeIf { it.isNotEmpty() },
              possibleTypes = fragments.fold(emptySet()) { acc, fragment -> acc + fragment.possibleTypes },
              selectionKeys = fragments.fold(emptySet()) { acc, fragment -> acc + fragment.selectionKeys },
              description = null,
              type = BackendIr.Fragment.Type.Interface,
          )
        }
  }

  private fun createFragments(
      selectionKey: SelectionKey,
      fragments: List<BackendIr.Fragment>,
  ): BackendIr.Fragments {
    return BackendIr.Fragments(
        fragments = fragments,
        accessors = fragments
            .flatMap { fragmentInterface -> fragmentInterface.selectionKeys }
            .filter { key -> key != selectionKey }
            .mapNotNull { key ->
              when {
                key.type == SelectionKey.Type.Fragment && key.keys.size == 1 -> key.root.decapitalize() to key
                else -> null
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
      val fields: List<BackendIr.Field>,
      val fragments: List<GenericFragment>,
      val condition: BackendIr.Condition,
      val selectionKeys: Set<SelectionKey>,
      val namedFragmentSelectionKey: SelectionKey?,
  )
}
