package com.apollographql.apollo.compiler.backend.ir

import com.apollographql.apollo.compiler.PackageNameProvider
import com.apollographql.apollo.compiler.backend.ir.BackendIrMergeUtils.mergeFields
import com.apollographql.apollo.compiler.backend.ir.FrontendIrMergeUtils.mergeInlineFragmentsWithSameTypeConditions
import com.apollographql.apollo.compiler.backend.ir.SelectionKeyUtils.addFieldSelectionKey
import com.apollographql.apollo.compiler.backend.ir.SelectionKeyUtils.addFieldSelectionKeys
import com.apollographql.apollo.compiler.frontend.gql.GQLDirective
import com.apollographql.apollo.compiler.frontend.gql.GQLField
import com.apollographql.apollo.compiler.frontend.gql.GQLFieldDefinition
import com.apollographql.apollo.compiler.frontend.gql.GQLFragmentDefinition
import com.apollographql.apollo.compiler.frontend.gql.GQLFragmentSpread
import com.apollographql.apollo.compiler.frontend.gql.GQLInlineFragment
import com.apollographql.apollo.compiler.frontend.gql.GQLNamedType
import com.apollographql.apollo.compiler.frontend.gql.GQLOperationDefinition
import com.apollographql.apollo.compiler.frontend.gql.GQLSelectionSet
import com.apollographql.apollo.compiler.frontend.gql.GQLType
import com.apollographql.apollo.compiler.frontend.gql.GQLTypeDefinition
import com.apollographql.apollo.compiler.frontend.gql.GQLVariableValue
import com.apollographql.apollo.compiler.frontend.gql.Schema
import com.apollographql.apollo.compiler.frontend.gql.SourceLocation
import com.apollographql.apollo.compiler.frontend.gql.definitionFromScope
import com.apollographql.apollo.compiler.frontend.gql.findDeprecationReason
import com.apollographql.apollo.compiler.frontend.gql.leafType
import com.apollographql.apollo.compiler.frontend.gql.possibleTypes
import com.apollographql.apollo.compiler.frontend.gql.responseName
import com.apollographql.apollo.compiler.frontend.gql.rootTypeDefinition
import com.apollographql.apollo.compiler.frontend.gql.schemaKind
import com.apollographql.apollo.compiler.frontend.gql.toKotlinValue
import com.apollographql.apollo.compiler.frontend.gql.toSchemaType
import com.apollographql.apollo.compiler.frontend.gql.toUtf8WithIndents
import com.apollographql.apollo.compiler.frontend.gql.usedFragmentNames
import com.apollographql.apollo.compiler.frontend.gql.validateAndCoerce
import com.apollographql.apollo.compiler.frontend.ir.Condition
import com.apollographql.apollo.compiler.frontend.ir.Field
import com.apollographql.apollo.compiler.frontend.ir.Fragment
import com.apollographql.apollo.compiler.frontend.ir.FragmentRef
import com.apollographql.apollo.compiler.frontend.ir.InlineFragment
import com.apollographql.apollo.compiler.introspection.IntrospectionSchema

internal class BackendIrBuilder private constructor(
    private val schema: Schema,
    private val fragmentDefinitions: Map<String, GQLFragmentDefinition>,
    private val useSemanticNaming: Boolean,
    val packageNameProvider: PackageNameProvider?,
    val typesPackageName: String,
    val fragmentsPackageName: String
) {
  data class BackendIrBuilderInput(
      /**
       * All the operations
       */
      val operations: List<GQLOperationDefinition>,
      /**
       * All the fragments
       */
      val fragments: List<GQLFragmentDefinition>,
      /**
       * All the types
       */
      val typeDeclarations: List<GQLTypeDefinition>,

      /**
       * The scalar types to generate
       * - For root compilation units, this will be all the scalar types
       * - For child compilation units, this will be empty
       * - For standalone compilation units, this will contain only the scalar types that are used
       */
      val scalarsToGenerate: Set<String>,
      /**
       * The fragments to generate
       */
      val fragmentsToGenerate: Set<String>,
      /**
       * The enums to generate
       */
      val enumsToGenerate: Set<String>,
      /**
       * The enums to generate
       */
      val inputObjectsToGenerate: Set<String>,

      /**
       * The package name for input/enum types
       */
      val typesPackageName: String,
      /**
       * The package name for fragments
       */
      val fragmentsPackageName: String
  )

  companion object {

    fun BackendIrBuilderInput.buildBackendIr(
        schema: Schema,
        useSemanticNaming: Boolean,
        packageNameProvider: PackageNameProvider
    ): BackendIr {
      return BackendIrBuilder(
          schema = schema,
          fragmentDefinitions = this.fragments.associateBy { it.name },
          useSemanticNaming = useSemanticNaming,
          typesPackageName = typesPackageName,
          fragmentsPackageName = fragmentsPackageName,
          packageNameProvider = packageNameProvider
      ).buildBackendIR(this)
    }
  }

  private fun buildBackendIR(input: BackendIrBuilderInput): BackendIr {
    return BackendIr(
        operations = input.operations.map { operation ->
          operation.buildBackendIrOperation()
        },
        fragments = input.fragments
            .filter { fragment ->
              fragment.name in input.fragmentsToGenerate
            }
            .map { fragment ->
              fragment.buildBackendIrNamedFragment()
            },
        typeDeclarations = input.typeDeclarations.map {
          IntrospectionSchema.TypeRef(
              kind = it.schemaKind(),
              name = it.name
          )
        },
        typesPackageName = input.typesPackageName,
        fragmentsPackageName = input.fragmentsPackageName,
    )
  }

  private fun GQLOperationDefinition.buildBackendIrOperation(): BackendIr.Operation {
    val normalizedName = this.normalizeOperationName()
    val rootTypeDefinition = this.rootTypeDefinition(schema)!!
    val selectionKey = SelectionKey(
        root = normalizedName,
        keys = listOf(normalizedName, "data"),
        type = SelectionKey.Type.Query,
    )
    val dataFieldDefinition = GQLFieldDefinition(
        description = "",
        name = "data",
        arguments = emptyList(),
        directives = emptyList(),
        type = GQLNamedType(name = rootTypeDefinition.name)
    )
    val dataField = GQLField(
        name = "data",
        alias = null,
        sourceLocation = com.apollographql.apollo.compiler.frontend.gql.SourceLocation.UNKNOWN,
        arguments = null,
        directives = emptyList(),
        selectionSet = selectionSet
    ).buildBackendIrField(
        selectionKey = selectionKey,
        generateFragmentImplementations = true,
        dataFieldDefinition
    )
    val variables = this.variableDefinitions.map { variable ->
      BackendIr.Variable(
          name = variable.name,
          type = variable.type.toSchemaType(schema)
      )
    }
    val fragmentNames = usedFragmentNames(schema, fragmentDefinitions)
    return BackendIr.Operation(
        name = normalizedName,
        operationName = name!!,
        targetPackageName = packageNameProvider?.operationPackageName(filePath = sourceLocation.filePath ?: "") ?: "",
        operationType = IntrospectionSchema.TypeRef(
            IntrospectionSchema.Kind.OBJECT,
            rootTypeDefinition.name
        ),
        comment = this.description ?: "",
        variables = variables,
        definition = (toUtf8WithIndents() + "\n" + fragmentNames.map { fragmentDefinitions[it]!!.toUtf8WithIndents() }.joinToString("\n")).trimEnd('\n'),
        dataField = dataField,
    )
  }

  private fun GQLOperationDefinition.normalizeOperationName(): String {
    fun normalizeOperationName(
        useSemanticNaming: Boolean,
        operationNameSuffix: String,
    ): String {
      require(name != null) {
        "anonymous operations are not supported. This should have been caught during validation"
      }
      return if (useSemanticNaming && !name.endsWith(operationNameSuffix)) {
        name.capitalize() + operationNameSuffix
      } else {
        name.capitalize()
      }
    }
    return normalizeOperationName(useSemanticNaming, operationType.capitalize())
  }

  private fun GQLField.buildBackendIrField(
      selectionKey: SelectionKey,
      generateFragmentImplementations: Boolean,
      fieldDefinition: GQLFieldDefinition,
  ): BackendIr.Field {
    val selectionSet = (this.selectionSet?.selections ?: emptyList()).filterIsInstance<GQLField>().buildBackendIrFields(
        selectionKey = selectionKey,
        generateFragmentImplementations = generateFragmentImplementations,
        parentType = fieldDefinition.type
    )
    val fragments = buildBackendIrFragments(
        parentSelectionName = this.responseName(),
        inlineFragments = this.selectionSet?.selections?.filterIsInstance<GQLInlineFragment>() ?: emptyList(),
        namedFragments = this.selectionSet?.selections?.filterIsInstance<GQLFragmentSpread>() ?: emptyList(),
        schemaType = fieldDefinition.type,
        selectionKey = selectionKey,
        selectionSet = selectionSet,
        generateFragmentImplementations = generateFragmentImplementations,
    )

    val arguments = this.arguments?.arguments?.map { argument ->
      val argumentType = fieldDefinition.arguments.first { it.name == argument.name }.type
      BackendIr.Argument(
          name = argument.name,
          value = argument.value.validateAndCoerce(argumentType, schema, null)
              .orThrow()
              .toKotlinValue(false),
          type = argumentType.toSchemaType(schema)
      )
    } ?: emptyList()

    val conditions = this.directives.mapNotNull { it.toCondition() }

    return BackendIr.Field(
        name = this.name,
        alias = this.alias,
        type = fieldDefinition.type.toSchemaType(schema),
        args = arguments,
        fields = selectionSet,
        fragments = fragments,
        deprecationReason = fieldDefinition.directives.findDeprecationReason(),
        description = fieldDefinition.description ?: "",
        conditions = conditions,
        selectionKeys = setOf(selectionKey),
    )
  }

private fun GQLDirective.toCondition(): BackendIr.Condition? {
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
    "include" -> BackendIr.Condition(
        kind = "BooleanCondition",
        variableName = argument.value.name,
        inverted = name == "skip",
        type = BackendIr.Condition.Type.Boolean
    )
    else -> null // unrecognized directive, skip
  }
}

private fun List<GQLField>.buildBackendIrFields(
      selectionKey: SelectionKey,
      generateFragmentImplementations: Boolean,
      parentType: GQLType
  ): List<BackendIr.Field> {
    return this.map { field ->
      val fieldDefinition = field.definitionFromScope(schema, schema.typeDefinition(parentType.leafType().name))!!
      field.buildBackendIrField(
          selectionKey = selectionKey + field.responseName(),
          generateFragmentImplementations = generateFragmentImplementations,
          fieldDefinition
      )
    }
  }

  private fun GQLFragmentDefinition.buildBackendIrNamedFragment(): BackendIr.NamedFragment {
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
        source = this.toUtf8WithIndents(),
        comment = this.description ?: "",
        selectionSet = selectionSet,
        defaultSelectionSet = defaultSelectionSet,
    )
  }

  private fun GQLFragmentDefinition.buildSelectionSet(
      rootSelectionKey: SelectionKey,
      generateFragmentImplementations: Boolean,
  ): BackendIr.NamedFragment.SelectionSet {
    val selectionSet = this.selectionSet.selections.filterIsInstance<GQLField>().buildBackendIrFields(
        selectionKey = rootSelectionKey,
        generateFragmentImplementations = generateFragmentImplementations,
        parentType = typeCondition
    )

    // resolve all possible types
    val possibleTypes = schema.typeDefinition(typeCondition.name).possibleTypes(schema.typeDefinitions)

    // build interfaces for the fragments
    val fragmentInterfaces = buildBackendIrFragmentInterfaces(
        inlineFragments = this.selectionSet.selections.filterIsInstance<GQLInlineFragment>(),
        namedFragments = this.selectionSet.selections.filterIsInstance<GQLFragmentSpread>(),
        fieldPossibleTypes = possibleTypes,
        selectionKey = rootSelectionKey,
        selectionSet = selectionSet,
    )

    // build implementations for the fragments if we allowed to do so
    val fragmentImplementations = if (generateFragmentImplementations) {
      buildFragmentImplementations(
          parentSelectionName = this.name,
          inlineFragments = this.selectionSet.selections.filterIsInstance<GQLInlineFragment>(),
          namedFragments = this.selectionSet.selections.filterIsInstance<GQLFragmentSpread>(),
          fieldPossibleTypes = possibleTypes,
          selectionKey = rootSelectionKey,
          selectionSet = selectionSet,
      )
    } else emptyList()

    return BackendIr.NamedFragment.SelectionSet(
        fields = selectionSet,
        fragments = fragmentInterfaces + fragmentImplementations,
        typeCondition = this.typeCondition.toSchemaType(schema),
        possibleTypes = possibleTypes.map { GQLNamedType(sourceLocation = SourceLocation.UNKNOWN, it).toSchemaType(schema) },
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
      inlineFragments: List<GQLInlineFragment>,
      namedFragments: List<GQLFragmentSpread>,
      schemaType: GQLType,
      selectionKey: SelectionKey,
      selectionSet: List<BackendIr.Field>,
      generateFragmentImplementations: Boolean,
  ): List<BackendIr.Fragment> {
    // resolve all field's possible types
    val possibleTypes = schema.typeDefinition(schemaType.leafType().name)
        .possibleTypes(schema.typeDefinitions)

    // build interfaces for the fragments
    val fragmentInterfaces = buildBackendIrFragmentInterfaces(
        inlineFragments = inlineFragments,
        namedFragments = namedFragments,
        fieldPossibleTypes = possibleTypes,
        selectionKey = selectionKey,
        selectionSet = selectionSet,
    )

    // build implementations for the fragments if we allowed to do so
    val fragmentImplementations = if (generateFragmentImplementations) {
      buildFragmentImplementations(
          parentSelectionName = parentSelectionName,
          inlineFragments = inlineFragments,
          namedFragments = namedFragments,
          fieldPossibleTypes = possibleTypes,
          selectionKey = selectionKey,
          selectionSet = selectionSet,
      )
    } else emptyList()

    return fragmentInterfaces + fragmentImplementations
  }

  private fun buildBackendIrFragmentInterfaces(
      inlineFragments: List<GQLInlineFragment>,
      namedFragments: List<GQLFragmentSpread>,
      fieldPossibleTypes: Set<String>,
      selectionKey: SelectionKey,
      selectionSet: List<BackendIr.Field>,
  ): List<BackendIr.Fragment.Interface> {
    // build all defined fragment interfaces including nested ones
    val fragments = buildGenericFragments(
        inlineFragments = inlineFragments,
        namedFragments = namedFragments,
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
          possibleTypes = possibleTypes.map { GQLNamedType(sourceLocation = SourceLocation.UNKNOWN, it).toSchemaType(schema) }.toSet(),
          description = fragments.first().description,
          typeCondition = fragments.first().typeCondition,
      )
    }
  }

  private fun buildFragmentImplementations(
      parentSelectionName: String,
      inlineFragments: List<GQLInlineFragment>,
      namedFragments: List<GQLFragmentSpread>,
      fieldPossibleTypes: Set<String>,
      selectionKey: SelectionKey,
      selectionSet: List<BackendIr.Field>,
  ): List<BackendIr.Fragment.Implementation> {
    // build all defined fragment implementations including nested ones
    val fragments = buildGenericFragments(
        inlineFragments = inlineFragments,
        namedFragments = namedFragments,
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
      inlineFragments: List<GQLInlineFragment>,
      namedFragments: List<GQLFragmentSpread>,
      selectionKey: SelectionKey,
      selectionSet: List<BackendIr.Field>,
      generateFragmentImplementations: Boolean,
  ): List<GenericFragment> {
    // build generic fragments from inline fragments
    val genericInlineFragments = inlineFragments
        .mergeInlineFragmentsWithSameTypeConditions()
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
        .map { fragmentSpread -> fragmentDefinitions.get(fragmentSpread.name)!! }
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

  private fun GQLInlineFragment.buildGenericFragment(
      parentSelectionKey: SelectionKey,
      parentSelectionSet: List<BackendIr.Field>,
      parentNamedFragmentSelectionKeys: Set<SelectionKey>,
      generateFragmentImplementations: Boolean,
  ): GenericFragment {
    val typeDefinition = schema.typeDefinition(typeCondition.name)
    return buildGenericFragment(
        fragmentTypeCondition = typeCondition.name,
        selectionSet = selectionSet,
        fragmentDescription = typeDefinition.description ?: "",
        fragmentConditions = directives.mapNotNull { it.toCondition() },
        namedFragmentSelectionKey = null,
        parentSelectionKey = parentSelectionKey,
        parentSelectionSet = parentSelectionSet,
        parentNamedFragmentSelectionKeys = parentNamedFragmentSelectionKeys,
        generateFragmentImplementations = generateFragmentImplementations
    )
  }

  private fun GQLFragmentDefinition.buildGenericFragment(
      parentSelectionKey: SelectionKey,
      parentSelectionSet: List<BackendIr.Field>,
      parentNamedFragmentSelectionKeys: Set<SelectionKey>,
      generateFragmentImplementations: Boolean,
  ): GenericFragment {
    return buildGenericFragment(
        fragmentTypeCondition = typeCondition.name,
        selectionSet = selectionSet,
        fragmentDescription = this.description ?: "",
        fragmentConditions = emptyList(),
        namedFragmentSelectionKey = SelectionKey(
            root = this.name.capitalize(),
            keys = listOf(this.name.capitalize()),
            type = SelectionKey.Type.Fragment,
        ),
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
      fragmentTypeCondition: String,
      selectionSet: GQLSelectionSet,
      fragmentDescription: String,
      fragmentConditions: List<BackendIr.Condition>,
      namedFragmentSelectionKey: SelectionKey?,
      parentSelectionKey: SelectionKey,
      parentSelectionSet: List<BackendIr.Field>,
      parentNamedFragmentSelectionKeys: Set<SelectionKey>,
      generateFragmentImplementations: Boolean,
  ): GenericFragment {
    val fragmentName = fragmentTypeCondition.capitalize()
    val fragmentSelectionSet = selectionSet.selections.filterIsInstance<GQLField>().buildBackendIrFields(
        selectionKey = parentSelectionKey + fragmentName,
        generateFragmentImplementations = generateFragmentImplementations,
        parentType = GQLNamedType(sourceLocation = SourceLocation.UNKNOWN, fragmentTypeCondition)
    ).addFieldSelectionKey(namedFragmentSelectionKey)
    val parentSelectionSet = parentSelectionSet
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
    val childInlineFragments = selectionSet.selections.filterIsInstance<GQLInlineFragment>().map { inlineFragment ->
      val fragment = buildGenericFragment(
          fragmentTypeCondition = inlineFragment.typeCondition.name,
          selectionSet = inlineFragment.selectionSet,
          fragmentDescription = schema.typeDefinition(inlineFragment.typeCondition.name).description ?: "",
          fragmentConditions = inlineFragment.directives.mapNotNull { it.toCondition() },
          namedFragmentSelectionKey = namedFragmentSelectionKey?.let { selectionKey ->
            selectionKey.copy(
                keys = listOf(selectionKey.keys.first(), inlineFragment.typeCondition.name.capitalize())
            )
          },
          parentSelectionKey = parentSelectionKey,
          parentSelectionSet = parentSelectionSet,
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
    val childNamedFragments = selectionSet.selections.filterIsInstance<GQLFragmentSpread>().map { namedFragment ->
      val fragmentDefinition = fragmentDefinitions.get(namedFragment.name)!!
      buildGenericFragment(
          fragmentTypeCondition = fragmentDefinition.typeCondition.name,
          selectionSet = fragmentDefinition.selectionSet,
          fragmentDescription = fragmentDefinition.description ?: "",
          fragmentConditions = emptyList(),
          namedFragmentSelectionKey = SelectionKey(
              root = namedFragment.name.capitalize(),
              keys = listOf(namedFragment.name.capitalize()),
              type = SelectionKey.Type.Fragment,
          ),
          parentSelectionKey = parentSelectionKey,
          parentSelectionSet = parentSelectionSet,
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
        possibleTypes = schema.typeDefinition(fragmentTypeCondition)!!.possibleTypes(schema.typeDefinitions).toList(),
        description = fragmentDescription,
        selectionSet = parentSelectionSet,
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
      val conditions: List<BackendIr.Condition>,
      val nestedFragments: List<GenericFragment>,
      val selectionKeys: Set<SelectionKey>,
  )
}

