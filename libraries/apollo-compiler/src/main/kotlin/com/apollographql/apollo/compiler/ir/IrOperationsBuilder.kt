package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.ast.Catch
import com.apollographql.apollo.ast.CatchTo
import com.apollographql.apollo.ast.GQLBooleanValue
import com.apollographql.apollo.ast.GQLDirective
import com.apollographql.apollo.ast.GQLEnumTypeDefinition
import com.apollographql.apollo.ast.GQLEnumValue
import com.apollographql.apollo.ast.GQLFloatValue
import com.apollographql.apollo.ast.GQLFragmentDefinition
import com.apollographql.apollo.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo.ast.GQLIntValue
import com.apollographql.apollo.ast.GQLInterfaceTypeDefinition
import com.apollographql.apollo.ast.GQLListType
import com.apollographql.apollo.ast.GQLListValue
import com.apollographql.apollo.ast.GQLNamedType
import com.apollographql.apollo.ast.GQLNode
import com.apollographql.apollo.ast.GQLNonNullType
import com.apollographql.apollo.ast.GQLNullValue
import com.apollographql.apollo.ast.GQLObjectTypeDefinition
import com.apollographql.apollo.ast.GQLObjectValue
import com.apollographql.apollo.ast.GQLOperationDefinition
import com.apollographql.apollo.ast.GQLScalarTypeDefinition
import com.apollographql.apollo.ast.GQLSelection
import com.apollographql.apollo.ast.GQLStringValue
import com.apollographql.apollo.ast.GQLType
import com.apollographql.apollo.ast.GQLUnionTypeDefinition
import com.apollographql.apollo.ast.GQLValue
import com.apollographql.apollo.ast.GQLVariableDefinition
import com.apollographql.apollo.ast.GQLVariableValue
import com.apollographql.apollo.ast.InferredVariable
import com.apollographql.apollo.ast.Schema
import com.apollographql.apollo.ast.TransformResult
import com.apollographql.apollo.ast.VariableUsage
import com.apollographql.apollo.ast.definitionFromScope
import com.apollographql.apollo.ast.fieldDefinitions
import com.apollographql.apollo.ast.findCatch
import com.apollographql.apollo.ast.findCatchByDefault
import com.apollographql.apollo.ast.findDeprecationReason
import com.apollographql.apollo.ast.findNonnull
import com.apollographql.apollo.ast.findOptInFeature
import com.apollographql.apollo.ast.findSemanticNonNulls
import com.apollographql.apollo.ast.isFieldNonNull
import com.apollographql.apollo.ast.optionalValue
import com.apollographql.apollo.ast.pretty
import com.apollographql.apollo.ast.rawType
import com.apollographql.apollo.ast.responseName
import com.apollographql.apollo.ast.rootTypeDefinition
import com.apollographql.apollo.ast.toUtf8
import com.apollographql.apollo.ast.transform
import com.apollographql.apollo.compiler.MODELS_OPERATION_BASED
import com.apollographql.apollo.compiler.MODELS_OPERATION_BASED_WITH_INTERFACES
import com.apollographql.apollo.compiler.MODELS_RESPONSE_BASED
import com.apollographql.apollo.compiler.UsedCoordinates

internal class IrOperationsBuilder(
    private val schema: Schema,
    private val operationDefinitions: List<GQLOperationDefinition>,
    private val operationNameToNormalizedPath: Map<String, String>,
    private val fragmentDefinitions: List<GQLFragmentDefinition>,
    private val fragmentNameToNormalizedPath: Map<String, String>,
    private val allFragmentDefinitions: Map<String, GQLFragmentDefinition>,
    private val codegenModels: String,
    private val generateOptionalOperationVariables: Boolean,
    private val flattenModels: Boolean,
    private val decapitalizeFields: Boolean,
    private val alwaysGenerateTypesMatching: Set<String>,
    private val generateDataBuilders: Boolean,
    private val fragmentVariableUsages: Map<String, List<VariableUsage>>,
) : FieldMerger {
  private val usedCoordinates = UsedCoordinates()
  private val responseBasedBuilder = ResponseBasedModelGroupBuilder(
      schema,
      allFragmentDefinitions,
      this
  )

  private val builder = when (codegenModels) {
    MODELS_OPERATION_BASED -> OperationBasedModelGroupBuilder(
        schema = schema,
        allFragmentDefinitions = allFragmentDefinitions,
        fieldMerger = this,
    )

    MODELS_OPERATION_BASED_WITH_INTERFACES -> OperationBasedWithInterfacesModelGroupBuilder(
        schema = schema,
        allFragmentDefinitions = allFragmentDefinitions,
        fieldMerger = this,
    )

    MODELS_RESPONSE_BASED -> responseBasedBuilder
    else -> error("codegenModels='$codegenModels' is not supported")
  }

  fun build(): IrOperations {
    val operations = operationDefinitions.map { it.toIr() }
    val fragments = fragmentDefinitions.map { it.toIr() }

    addUserCoordinates(schema, alwaysGenerateTypesMatching, usedCoordinates)

    /**
     * Loop to add referenced types
     *
     * - Add the types that are needed by data builders
     * - Add the types that are referenced in CompiledGraphQL
     */
    val visitedTypes = mutableSetOf<String>()
    val typesToVisit = usedCoordinates.getTypes().toMutableList()
    while (typesToVisit.isNotEmpty()) {
      val name = typesToVisit.removeFirst()
      if (visitedTypes.contains(name)) {
        continue
      }
      visitedTypes.add(name)

      when (val typeDefinition = schema.typeDefinition(name)) {
        is GQLObjectTypeDefinition -> {
          if (generateDataBuilders) {
            /**
             * DataBuilder maps reference all their super types, including unions
             *
             * internal class DroidMap(
             *   __fields: Map<String, Any?>,
             * ) : CharacterMap, SearchResultMap, Map<String, Any?> by __fields
             */
            schema.typeDefinitions.values.filterIsInstance<GQLUnionTypeDefinition>().filter {
              it.memberTypes.any {
                it.name == typeDefinition.name
              }
            }.forEach {
              typesToVisit.add(it.name)
              usedCoordinates.putType(it.name)
            }
          }

          /**
           * Object classes reference their super interfaces, generate them:
           *
           * public class Droid {
           *   public companion object {
           *     public val type: ObjectType =
           *         ObjectType.Builder(name = "Droid").interfaces(listOf(Character.type)).build()
           *   }
           * }
           *
           * Also Make sure data builders generate the map interface
           */
          typeDefinition.implementsInterfaces.forEach {
            usedCoordinates.putType(it)
            typesToVisit.add(it)
          }
        }

        is GQLInterfaceTypeDefinition -> {
          if (generateDataBuilders) {
            /**
             * Add all possible types because the user might want to use any of them
             * GetHeroQuery.Data {
             *   hero = buildHuman {}  // or buildDroid {}
             * }
             */
            schema.typeDefinitions.values.filterIsInstance<GQLObjectTypeDefinition>()
                .filter {
                  it.implementsInterfaces.contains(typeDefinition.name)
                }
                .forEach {
                  /**
                   * Add all the fields of the interface to the implementing objects
                   *
                   * This is suboptimal. We should traverse from the bottom most types
                   * and collect used fields as we go up the type hierarchy
                   */
                  usedCoordinates.putAllFields(type = it.name, fields = usedCoordinates.getFields(name))
                  typesToVisit.add(it.name)
                }
          }
          /**
           * Interface classes reference their super interfaces, generate them:
           *
           * public class Character {
           *   public companion object {
           *     public val type: InterfaceType = InterfaceType.Builder(name = "Character").build()
           *   }
           * }
           * also Make sure data builders generate the map interface
           */
          typeDefinition.implementsInterfaces.forEach {
            usedCoordinates.putType(it)
            typesToVisit.add(it)
          }
        }

        is GQLUnionTypeDefinition -> {
          /**
           * Unions reference their members, generate them:
           *
           * ```
           * public class SearchResult {
           *   public companion object {
           *     public val type: UnionType = UnionType("SearchResult", Human.type, Droid.type, Starship.type)
           *   }
           * }
           * ```
           */
          typeDefinition.memberTypes.forEach {
            if (generateDataBuilders) {
              usedCoordinates.putAllFields(type = it.name, fields = usedCoordinates.getFields(name))
            } else {
              usedCoordinates.putType(it.name)
            }
            typesToVisit.add(it.name)
          }
        }

        is GQLInputObjectTypeDefinition -> {
          /**
           * Loop on the input types.
           * Recursively add all their input fields types.
           * Note that input types may contain cycles, so we have to keep track of visited types to
           * avoid looping endlessly (in addition to not computing them multiple times)
           */
          typeDefinition.inputFields.forEach {
            when (val fieldType = schema.typeDefinition(it.type.rawType().name)) {
              is GQLScalarTypeDefinition -> {
                typesToVisit.add(fieldType.name)
                usedCoordinates.putType(fieldType.name)
              }

              is GQLEnumTypeDefinition -> {
                typesToVisit.add(fieldType.name)
                usedCoordinates.putType(fieldType.name)
              }

              is GQLInputObjectTypeDefinition -> {
                typesToVisit.add(fieldType.name)
                usedCoordinates.putType(fieldType.name)
              }

              else -> error("output type '${fieldType.name}' used in input position")
            }
          }
        }

        else -> {}
      }
    }

    return IrOperations(
        operations = operations,
        fragments = fragments,
        usedCoordinates = usedCoordinates,
        flattenModels = flattenModels,
        decapitalizeFields = decapitalizeFields,
        fragmentDefinitions = fragmentDefinitions,
        codegenModels = codegenModels
    )
  }

  /**
   * Strip any custom Apollo directive and format
   */
  private fun GQLNode.formatToString(): String {
    return transform {
      if (it is GQLDirective && schema.shouldStrip(it.name)) {
        TransformResult.Delete
      } else {
        TransformResult.Continue
      }
    }!!.toUtf8()
  }

  private fun GQLOperationDefinition.toIr(): IrOperation {
    val typeDefinition = this.rootTypeDefinition(schema)
        ?: throw IllegalStateException("ApolloGraphql: cannot find root type for '$operationType'")

    check(name != null) {
      "Apollo doesn't support anonymous operation."
    }

    val usedFragments = usedFragments(
        schema = schema,
        allFragmentDefinitions = allFragmentDefinitions,
        selections = selections,
        rawTypename = typeDefinition.name,
    )

    val sourceWithFragments = (formatToString() + "\n" + usedFragments.joinToString(
        separator = "\n"
    ) { fragmentName ->
      allFragmentDefinitions[fragmentName]!!.formatToString()
    }).trimEnd('\n')

    val (dataProperty, dataModelGroup) = builder.buildOperationData(
        selections = selections,
        rawTypeName = typeDefinition.name,
        operationName = name!!,
        defaultCatchTo = findCatchByDefault(schema)
    )

    // Add the root type to use from the selections
    usedCoordinates.putType(typeDefinition.name)

    return IrOperation(
        name = name!!,
        description = description,
        operationType = operationType.toIrOperationType(schema.rootTypeNameFor(operationType)),
        typeCondition = typeDefinition.name,
        variables = variableDefinitions.map { it.toIr() },
        selectionSets = SelectionSetsBuilder(schema, allFragmentDefinitions).build(selections, typeDefinition.name),
        sourceWithFragments = sourceWithFragments,
        normalizedFilePath = operationNameToNormalizedPath.get(name!!) ?: "",
        dataProperty = dataProperty,
        dataModelGroup = dataModelGroup,
    )
  }

  private fun String.toIrOperationType(typeName: String) = when (this) {
    "query" -> IrOperationType.Query(typeName)
    "mutation" -> IrOperationType.Mutation(typeName)
    "subscription" -> IrOperationType.Subscription(typeName)
    else -> error("unknown operation $this")
  }

  private fun GQLFragmentDefinition.toIr(): IrFragmentDefinition {
    val typeDefinition = schema.typeDefinition(typeCondition.name)

    val inferredVariables = inferFragmentVariables(this)

    val interfaceModelGroup = builder.buildFragmentInterface(
        fragmentName = name
    )

    val (dataProperty, dataModelGroup) = builder.buildFragmentData(
        fragmentName = name,
        defaultCatchTo = findCatchByDefault(schema)
    )

    // Add the root type to use from the fragment selections
    usedCoordinates.putType(typeCondition.name)

    return IrFragmentDefinition(
        name = name,
        description = description,
        filePath = fragmentNameToNormalizedPath[name] ?: "",
        typeCondition = typeDefinition.name,
        variables = inferredVariables.map { it.toIr() },
        selectionSets = SelectionSetsBuilder(schema, allFragmentDefinitions).build(selections, typeCondition.name),
        interfaceModelGroup = interfaceModelGroup,
        dataProperty = dataProperty,
        dataModelGroup = dataModelGroup,
        source = formatToString(),
        isTypeConditionAbstract = typeDefinition !is GQLObjectTypeDefinition
    )
  }

  private fun List<GQLType>.findCompatibleType(): GQLType? {
    return drop(1).fold<GQLType, GQLType?>(first()) { acc, gqlType ->
      if (acc == null) {
        return@fold null
      }

      acc.mergeWith(gqlType)
    }
  }

  private fun GQLType.mergeWith(other: GQLType): GQLType? {
    return if (this is GQLNonNullType && other is GQLNonNullType) {
      type.mergeWith(other.type)?.let { GQLNonNullType(null, it) }
    } else if (this is GQLNonNullType && other !is GQLNonNullType) {
      type.mergeWith(other)?.let { GQLNonNullType(null, it) }
    } else if (this !is GQLNonNullType && other is GQLNonNullType) {
      this.mergeWith(other.type)?.let { GQLNonNullType(null, it) }
    } else if (this is GQLListType && other is GQLListType) {
      this.type.mergeWith(other.type)?.let { GQLListType(null, it) }
    } else if (this is GQLListType && other !is GQLListType) {
      null
    } else if (this !is GQLListType && other is GQLListType) {
      null
    } else if (this is GQLNamedType && other is GQLNamedType) {
      if (name != other.name) {
        null
      } else {
        this
      }
    } else {
      throw IllegalStateException()
    }
  }

  /**
   * Infer variables from a fragment definition. If a variable is used in both a nullable and non-nullable
   * position, the variable is inferred as non-nullable
   *
   * @throws IllegalStateException if some incompatibles types are found. This should never happen
   * because this should be caught during previous operation-wide validation.
   */
  private fun inferFragmentVariables(fragment: GQLFragmentDefinition): List<InferredVariable> {
    return fragmentVariableUsages.get(fragment.name).orEmpty().groupBy {
      it.variable.name
    }.entries.map {
      val types = it.value.map { it.locationType }
      val inferredType = types.findCompatibleType()
      if (inferredType == null) {
        error("Fragment ${fragment.name} uses different types for variable '${it.key}': ${types.joinToString()}")
      } else {
        InferredVariable(it.key, inferredType)
      }
    }
  }

  private fun InferredVariable.toIr(): IrVariable {
    var irType = type.toIr()
    if (type !is GQLNonNullType) {
      // If the type is nullable, the variable might be omitted, make it optional
      irType = irType.optional(true)
    }
    return IrVariable(
        name = name,
        type = irType,
        defaultValue = null
    )
  }

  private fun GQLVariableDefinition.toIr(): IrVariable {
    var irType = type.toIr()
    when {
      !irType.nullable && defaultValue == null -> {
        // The variable is non-nullable and has no defaultValue => it must always be sent
        // Leave irType as-is
      }

      defaultValue != null -> {
        // the variable has a defaultValue meaning that there is a use case for not providing it
        irType = irType.optional(true)
      }

      else -> {
        // The variable is nullable. By the GraphQL spec, it means it's also optional
        // In practice though, we often want it non-optional, because if the user added tha variable in
        // the first place, there is a high change they're going to use it.
        //
        // One counter example is bidirectional pagination where 'before' or 'after' could be
        // left Absent
        //
        // We default to add the [Optional] wrapper, but this can be overridden by the user globally or individually
        // with the @optional directive.

        val makeOptional = directives.optionalValue(schema) ?: generateOptionalOperationVariables
        if (makeOptional) {
          irType = irType.optional(true)
        }
      }
    }

    return IrVariable(
        name = name,
        type = irType,
        defaultValue = defaultValue?.toIrValue()
    )
  }

  /**
   * Maps to [IrType]
   */
  private fun GQLType.toIr(): IrType {
    usedCoordinates.putType(rawType().name)
    return toIr(schema)
  }

  /**
   * An intermediate class used during collection
   */
  private class CollectedField(
      /**
       * All fields with the same response name should have the same info here
       */
      val name: String,
      val alias: String?,

      val description: String?,
      val type: GQLType,
      val deprecationReason: String?,
      val optInFeature: String?,
      val semanticNonNulls: List<Int>,
      val catch: Catch?,
      val forceOptional: Boolean,

      /**
       * Merged field will merge their conditions and selectionSets
       */
      val condition: BooleanExpression<BVariable>,
      val selections: List<GQLSelection>,
      val parentType: String,
      val usedArguments: List<String>,
  ) {
    val responseName = alias ?: name
  }

  override fun merge(fields: List<FieldWithParent>, defaultCatchTo: CatchTo?): List<MergedField> {
    return fields.map { fieldWithParent ->
      val gqlField = fieldWithParent.gqlField
      val parentTypeDefinition = schema.typeDefinition(fieldWithParent.parentType)
      val fieldDefinition = gqlField.definitionFromScope(schema, parentTypeDefinition)

      check(fieldDefinition != null) {
        "cannot find field definition for field '${gqlField.responseName()}' of type '${parentTypeDefinition.name}'"
      }

      var semanticNonNulls = fieldDefinition.findSemanticNonNulls(schema)

      if (parentTypeDefinition.isFieldNonNull(gqlField.name, schema)) {
        check(semanticNonNulls.isEmpty()) {
          "${gqlField.sourceLocation}: bad '@nonnull' directive: field '${gqlField.responseName()}' already has nullability annotations (@nonnull, @semanticNonNull) in the schema."
        }
        semanticNonNulls = listOf(0)
      }

      if (gqlField.directives.findNonnull(schema)) {
        check(semanticNonNulls.isEmpty()) {
          "${gqlField.sourceLocation}: bad '@nonnull' directive: field '${gqlField.responseName()}' already has nullability annotations (@nonnull, @semanticNonNull) in the schema."
        }
        semanticNonNulls = listOf(0)
      }

      CollectedField(
          name = gqlField.name,
          alias = gqlField.alias,
          condition = gqlField.directives.toIncludeBooleanExpression(),
          selections = gqlField.selections,
          type = fieldDefinition.type,
          description = fieldDefinition.description,
          deprecationReason = fieldDefinition.directives.findDeprecationReason(),
          optInFeature = fieldDefinition.directives.findOptInFeature(schema),
          semanticNonNulls = semanticNonNulls,
          forceOptional = gqlField.directives.optionalValue(schema) == true,
          parentType = fieldWithParent.parentType,
          catch = gqlField.findCatch(schema),
          usedArguments = gqlField.arguments.map { it.name },
      )
    }.groupBy {
      it.responseName
    }.values.map { fieldsWithSameResponseName ->
      /**
       * Sanity checks, might be removed as this should be done during validation
       */
      check(fieldsWithSameResponseName.map { it.alias }.distinct().size == 1)
      check(fieldsWithSameResponseName.map { it.name }.distinct().size == 1)
      // GQLTypes might differ because of their source location. Use pretty()
      // to canonicalize them
      check(fieldsWithSameResponseName.map { it.type }.distinctBy { it.pretty() }.size == 1)

      val first = fieldsWithSameResponseName.first()
      val childSelections = fieldsWithSameResponseName.flatMap { it.selections }

      val forceOptional = fieldsWithSameResponseName.any {
        it.forceOptional
      }

      if (generateDataBuilders) {
        /**
         * Keep track of used fields.
         *
         * Note1: we don't want to track this if generateDataBuilders is false:
         *
         * ```graphql
         * {
         *   animal {
         *     ... on Cat {
         *        meow # We don't need to track Cat.meow here as Cat is not used unless data builders are enabled
         *     }
         *   }
         * }
         * ```
         *
         * Note2: that this over-generates a bit in cases where fragments conditions are always true and parentType is an interface:
         *
         * ```graphql
         * {
         *   cat {
         *     ... on Animal {
         *       species
         *     }
         *   }
         * }
         * ```
         *
         * In the above case, there's no reason a data builder would need to create a fallback animal
         * yet `buildOtherAnimal` will be generated
         */
        usedCoordinates.putField(first.parentType, first.name)
      }

      /**
       * We track field usages, but we also need to track the type itself because it might be that there is only fragments
       * node {
       *   # no field here but Node is still used
       *   ... on Product {
       *     price
       *   }
       * }
       */
      usedCoordinates.putType(first.type.rawType().name)

      // Track argument usage
      fields.map { it.parentType }.distinct().forEach { parentType ->
        for (usedArgument in first.usedArguments) {
          usedCoordinates.putArgument(parentType, first.name, usedArgument)
        }
      }

      val irType = first
          .type
          .toIr(schema)
          .semanticNonNull(first.semanticNonNulls, 0)
          .let {
            /**
             * We map @optional fields to nullable fields. This probably needs to be revisited in light of @catch
             */
            if (forceOptional) {
              it.nullable(true)
            } else {
              it
            }
          }
          // Finally, transform into Result or Nullable depending on catch
          .catch(first.catch, 0, defaultCatchTo)

      /**
       * Depending on the parent object/interface in which the field is queried, the field definition might have different descriptions/deprecationReasons
       */
      val description = fieldsWithSameResponseName.associateBy { it.description }.values.let { descriptionCandidates ->
        if (descriptionCandidates.size == 1) {
          descriptionCandidates.single().description
        } else {
          val parents = descriptionCandidates.map { it.parentType }
          "Merged field with multiple descriptions. See parentTypes: '${parents.joinToString(", ")}' for more information"
        }
      }

      val deprecationReason = fieldsWithSameResponseName.associateBy { it.deprecationReason }.values.let { deprecationCandidates ->
        if (deprecationCandidates.size == 1) {
          deprecationCandidates.single().deprecationReason
        } else {
          val parents = deprecationCandidates.filter { it.deprecationReason != null }.map { it.parentType }
          "Deprecated in: '${parents.joinToString(", ")}'"
        }
      }
      val optInFeature = fieldsWithSameResponseName.associateBy { it.optInFeature }.values.let { experimentalCandidates ->
        if (experimentalCandidates.size == 1) {
          experimentalCandidates.single().optInFeature
        } else {
          val parents = experimentalCandidates.filter { it.optInFeature != null }.map { it.parentType }
          "Experimental in: '${parents.joinToString(", ")}'"
        }
      }

      val info = IrFieldInfo(
          responseName = first.alias ?: first.name,
          description = description,
          deprecationReason = deprecationReason,
          optInFeature = optInFeature,
          type = irType,
          gqlType = first.type,
      )

      MergedField(
          info = info,
          condition = BooleanExpression.Or(fieldsWithSameResponseName.map { it.condition }.toSet()).simplify(),
          selections = childSelections,
          rawTypeName = first.type.rawType().name,
      )
    }
  }

  companion object {
    private fun IrType.semanticNonNull(semanticNonNullLevels: List<Int>, level: Int): IrType {
      val isNonNull = semanticNonNullLevels.any { it == level }

      return when (this) {
        is IrNamedType -> this
        is IrListType -> copy(ofType = ofType.semanticNonNull(semanticNonNullLevels, level + 1))
      }.let {
        if (isNonNull) {
          it.nullable(false)
        } else {
          it
        }
      }
    }

    private fun IrType.catch(catch: Catch?, level: Int, defaultCatchTo: CatchTo?): IrType {
      var type = when (this) {
        is IrNamedType -> this
        is IrListType -> copy(ofType = ofType.catch(catch, level + 1, defaultCatchTo))
      }

      val catchTo = if (catch != null) {
        catch.to.takeIf { catch.levels == null || catch.levels!!.contains(level) }
      } else {
        if (maybeError) {
          defaultCatchTo
        } else {
          // Can never be null: leave untouched
          null
        }
      }

      if (catchTo != null) {
        type = type.catchTo(catchTo.toIr())
        if (catchTo == CatchTo.NULL) {
          type = type.nullable(true)
        }
      }

      return type
    }

    private fun CatchTo.toIr(): IrCatchTo {
      return when (this) {
        CatchTo.RESULT -> IrCatchTo.Result
        CatchTo.NULL -> IrCatchTo.Null
        CatchTo.THROW -> IrCatchTo.NoCatch
      }
    }

    fun addUserCoordinates(
        schema: Schema,
        alwaysGenerateTypesMatching: Set<String>,
        usedCoordinates: UsedCoordinates,
    ) {
      val regexes = alwaysGenerateTypesMatching.map { Regex(it) }

      /**
       * Add schema types that are requested by the user
       */
      val visitedTypes = mutableSetOf<String>()
      val typesToVisit = schema.typeDefinitions.values.map { it.name }.toMutableList()
      while (typesToVisit.isNotEmpty()) {
        val type = typesToVisit.removeFirst()
        if (visitedTypes.contains(type)) {
          continue
        }
        visitedTypes.add(type)

        if (regexes.any { it.matches(type) }) {
          usedCoordinates.putType(type)

          // Add all arguments of all fields
          val gqlTypeDefinition = schema.typeDefinition(type)
          gqlTypeDefinition.fieldDefinitions(schema).forEach { gqlFieldDefinition ->
            gqlFieldDefinition.arguments.forEach { gqlArgumentDefinition ->
              usedCoordinates.putArgument(gqlTypeDefinition.name, gqlFieldDefinition.name, gqlArgumentDefinition.name)
              // Recursively add type of this argument
              typesToVisit.add(gqlArgumentDefinition.type.rawType().name)
            }
          }
        }

        val gqlTypeDefinition = schema.typeDefinition(type)
        gqlTypeDefinition.fieldDefinitions(schema).forEach { gqlFieldDefinition ->
          if (regexes.any { it.matches("${gqlTypeDefinition.name}.${gqlFieldDefinition.name}") }) {
            usedCoordinates.putField(gqlTypeDefinition.name, gqlFieldDefinition.name)
            // Recursively add types used by this field
            typesToVisit.add(gqlFieldDefinition.type.rawType().name)
          }
        }
      }
    }
  }
}

internal fun GQLValue.toIrValue(): IrValue {
  return when (this) {
    is GQLIntValue -> IrIntValue(value = value)
    is GQLFloatValue -> IrFloatValue(value = value)
    is GQLStringValue -> IrStringValue(value = value)
    is GQLBooleanValue -> IrBooleanValue(value = value)
    is GQLEnumValue -> IrEnumValue(value = value)
    is GQLNullValue -> IrNullValue
    is GQLVariableValue -> IrVariableValue(name = name)
    is GQLListValue -> IrListValue(values = values.map { it.toIrValue() })
    is GQLObjectValue -> IrObjectValue(
        fields = fields.map {
          IrObjectValue.Field(name = it.name, value = it.value.toIrValue())
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
 */
internal fun List<GQLDirective>.toIncludeBooleanExpression(): BooleanExpression<BVariable> {
  val conditions = mapNotNull {
    it.toIncludeBooleanExpression()
  }
  return if (conditions.isEmpty()) {
    BooleanExpression.True
  } else {
    check(conditions.toSet().size == conditions.size) {
      "Apollo: duplicate @skip/@include directives are not allowed"
    }
    // Having both @skip and @include is allowed
    // In that case, it's equivalent to an "And"
    // See https://spec.graphql.org/draft/#sec--include
    BooleanExpression.And(conditions.toSet()).simplify()
  }
}

internal fun GQLDirective.toIncludeBooleanExpression(): BooleanExpression<BVariable>? {
  if (setOf("skip", "include").contains(name).not()) {
    // not a condition directive
    return null
  }
  if (arguments.size != 1) {
    throw IllegalStateException("Apollo: wrong number of arguments for '$name' directive: ${arguments.size}")
  }

  val argument = arguments.first()

  return when (val value = argument.value) {
    is GQLBooleanValue -> {
      if (value.value) BooleanExpression.True else BooleanExpression.False
    }

    is GQLVariableValue -> BooleanExpression.Element(BVariable(name = value.name))
    else -> throw IllegalStateException("Apollo: cannot pass ${value.toUtf8()} to '$name' directive")
  }.let {
    if (name == "skip") not(it) else it
  }
}

/**
 * A combination of the result of [toIncludeBooleanExpression] and either `True` or a [BLabel].
 */
internal fun List<GQLDirective>.toBooleanExpression(): BooleanExpression<BTerm> {
  val deferBooleanConditions = mapNotNull {
    it.toDeferBooleanExpression()
  }
  val deferBooleanExpression = if (deferBooleanConditions.isEmpty()) {
    BooleanExpression.True
  } else {
    check(deferBooleanConditions.size == 1) {
      "Apollo: duplicate @defer directives are not allowed"
    }
    deferBooleanConditions.first()
  }
  return toIncludeBooleanExpression().and(deferBooleanExpression).simplify()
}

internal fun GQLDirective.toDeferBooleanExpression(): BooleanExpression<BTerm>? {
  if (name != "defer") return null
  val ifArgumentValue = arguments.firstOrNull { it.name == "if" }?.value ?: GQLBooleanValue(value = true)

  val labelArgumentValue = arguments.firstOrNull { it.name == "label" }?.value
  if (labelArgumentValue != null && labelArgumentValue !is GQLStringValue) throw IllegalStateException("Apollo: cannot pass ${labelArgumentValue.toUtf8()} to 'label' argument of 'defer' directive")
  @Suppress("USELESS_CAST", "KotlinRedundantDiagnosticSuppress")
  val label = (labelArgumentValue as GQLStringValue?)?.value
  return when (ifArgumentValue) {
    is GQLBooleanValue -> {
      if (!ifArgumentValue.value) {
        // @defer(if: false) means we should parse
        BooleanExpression.True
      } else {
        BooleanExpression.Element(BLabel(label))
      }
    }

    is GQLVariableValue -> {
      // @defer(label: $lbl1, if: $var1) can be translated to BLabel("lbl1") || !BVariable("var1")
      BooleanExpression.Element(BLabel(label)).or(not(BooleanExpression.Element(BVariable(ifArgumentValue.name))))
    }

    else -> throw IllegalStateException("Apollo: cannot pass ${ifArgumentValue.toUtf8()} to 'if' argument of 'defer' directive")
  }
}

internal fun IrFieldInfo.maybeNullable(makeNullable: Boolean): IrFieldInfo {
  if (!makeNullable) {
    return this
  }

  return copy(
      type = type.nullable(true)
  )
}
