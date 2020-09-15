package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.compiler.ApolloMetadata
import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.ir.TypeDeclaration.Companion.KIND_ENUM
import com.apollographql.apollo.compiler.ir.TypeDeclaration.Companion.KIND_INPUT_OBJECT_TYPE
import com.apollographql.apollo.compiler.ir.TypeDeclaration.Companion.KIND_SCALAR_TYPE
import com.apollographql.apollo.compiler.parser.error.DocumentParseException
import com.apollographql.apollo.compiler.parser.error.ParseException
import com.apollographql.apollo.compiler.parser.graphql.DocumentParseResult
import com.apollographql.apollo.compiler.parser.graphql.checkMultipleFragmentDefinitions
import com.apollographql.apollo.compiler.parser.graphql.checkMultipleOperationDefinitions
import com.apollographql.apollo.compiler.parser.graphql.validateArguments
import com.apollographql.apollo.compiler.parser.introspection.IntrospectionSchema
import com.apollographql.apollo.compiler.parser.introspection.asGraphQLType
import com.apollographql.apollo.compiler.parser.introspection.possibleTypes

class IRBuilder(private val schema: IntrospectionSchema,
                private val schemaPackageName: String,
                private val incomingMetadata: ApolloMetadata?,
                private val alwaysGenerateTypesMatching: Set<String>?,
                generateMetadata: Boolean
) {
  private val isRootCompilationUnit = incomingMetadata == null && generateMetadata

  private fun extraTypes(): Set<String> {
    val regexes = (alwaysGenerateTypesMatching ?: (listOf(".*").takeIf { isRootCompilationUnit } ?: emptyList()))
        .map { Regex(it) }

    return schema.types.values.filter { type ->
      (type.kind == IntrospectionSchema.Kind.ENUM
          || type.kind == IntrospectionSchema.Kind.INPUT_OBJECT)
          && regexes.indexOfFirst { it.matches(type.name) } >= 0
    }.map { it.name }
        .toSet()
  }

  fun build(documentParseResult: DocumentParseResult): CodeGenerationIR {
    val allFragments = (incomingMetadata?.fragments ?: emptyList()) + documentParseResult.fragments

    documentParseResult.operations.checkMultipleOperationDefinitions()
    allFragments.checkMultipleFragmentDefinitions()

    val fragmentsToGenerate = documentParseResult.fragments.map { it.fragmentName }

    val incomingTypes = incomingMetadata?.types ?: emptySet()
    val extraTypes = extraTypes()

    val typeDeclarations = (documentParseResult.usedTypes + extraTypes) .usedTypeDeclarations()

    val enumsToGenerate = typeDeclarations.filter { it.kind == KIND_ENUM }
        .map { it.name }
        .filter { !incomingTypes.contains(it) }

    val inputObjectsToGenerate = typeDeclarations.filter { it.kind == KIND_INPUT_OBJECT_TYPE }
        .map { it.name }
        .filter { !incomingTypes.contains(it) }

    // Always generate the extra "ID" scalar type
    // I'm not 100% sure why this is required but keep this for backward compatibility
    val scalarsToGenerate = when {
      isRootCompilationUnit -> schema.types.values.filter { it is IntrospectionSchema.Type.Scalar }.map { it.name } + ScalarType.ID.name
      incomingMetadata != null -> emptyList()
      else -> typeDeclarations.filter { it.kind == KIND_SCALAR_TYPE }.map { it.name } + ScalarType.ID.name
    }.filter {
      // remove any built-in scalar
      ScalarType.forName(it) == null
    }

    return CodeGenerationIR(
        operations = documentParseResult.operations.map { operation ->
          val referencedFragmentNames = operation.fields.referencedFragmentNames(fragments = allFragments, filePath = operation.filePath)
          val referencedFragments = referencedFragmentNames.mapNotNull { fragmentName -> allFragments.find { it.fragmentName == fragmentName } }
          referencedFragments.forEach { it.validateArguments(operation = operation, schema = schema) }

          val fragmentSource = referencedFragments.joinToString(separator = "\n") { it.source }
          operation.copy(
              sourceWithFragments = operation.source + if (fragmentSource.isNotBlank()) "\n$fragmentSource" else "",
              fragmentsReferenced = referencedFragmentNames.toList()
          )
        },
        fragments = allFragments,
        typeDeclarations = typeDeclarations,
        fragmentsToGenerate = fragmentsToGenerate.toSet(),
        enumsToGenerate = enumsToGenerate.toSet(),
        inputObjectsToGenerate = inputObjectsToGenerate.toSet(),
        scalarsToGenerate = scalarsToGenerate.toSet(),
        typesPackageName = "$schemaPackageName.type".removePrefix("."),
        fragmentsPackageName = "$schemaPackageName.fragment".removePrefix(".")
    )
  }

  private fun Set<String>.usedTypeDeclarations(): List<TypeDeclaration> {
    return usedSchemaTypes().map { type ->
      TypeDeclaration(
          kind = when (type.kind) {
            IntrospectionSchema.Kind.SCALAR -> "ScalarType"
            IntrospectionSchema.Kind.ENUM -> "EnumType"
            IntrospectionSchema.Kind.INPUT_OBJECT -> "InputObjectType"
            else -> null
          }!!,
          name = type.name,
          description = type.description?.trim() ?: "",
          values = (type as? IntrospectionSchema.Type.Enum)?.enumValues?.map { value ->
            TypeDeclarationValue(
                name = value.name,
                description = value.description?.trim() ?: "",
                isDeprecated = value.isDeprecated || !value.deprecationReason.isNullOrBlank(),
                deprecationReason = value.deprecationReason ?: ""
            )
          } ?: emptyList(),
          fields = (type as? IntrospectionSchema.Type.InputObject)?.inputFields?.map { field ->
            TypeDeclarationField(
                name = field.name,
                description = field.description?.trim() ?: "",
                type = field.type.asGraphQLType(),
                defaultValue = field.defaultValue.normalizeValue(field.type)
            )
          } ?: emptyList()
      )
    }
  }

  private fun Any?.normalizeValue(type: IntrospectionSchema.TypeRef): Any? {
    if (this == null) {
      return null
    }
    return when (type.kind) {
      IntrospectionSchema.Kind.SCALAR -> {
        when (ScalarType.forName(type.name ?: "")) {
          ScalarType.INT -> toString().trim().takeIf { it != "null" }?.toInt()
          ScalarType.BOOLEAN -> toString().trim().takeIf { it != "null" }?.toBoolean()
          ScalarType.FLOAT -> toString().trim().takeIf { it != "null" }?.toDouble()
          else -> toString()
        }
      }
      IntrospectionSchema.Kind.NON_NULL -> normalizeValue(type.ofType!!)
      IntrospectionSchema.Kind.LIST -> {
        toString().removePrefix("[").removeSuffix("]").split(',').filter { it.isNotBlank() }.map { value ->
          value.trim().replace("\"", "").normalizeValue(type.ofType!!)
        }
      }
      else -> toString()
    }
  }

  private fun Set<String>.usedSchemaTypes(): Set<IntrospectionSchema.Type> {
    if (isEmpty()) {
      return emptySet()
    }

    val (scalarTypes, inputObjectTypes) = filter { ScalarType.forName(it) == null }
        .map { schema[it] ?: throw ParseException(message = "Undefined schema type `$it`") }
        .filter { type -> type.kind == IntrospectionSchema.Kind.SCALAR || type.kind == IntrospectionSchema.Kind.ENUM || type.kind == IntrospectionSchema.Kind.INPUT_OBJECT }
        .partition { type -> type.kind == IntrospectionSchema.Kind.SCALAR || type.kind == IntrospectionSchema.Kind.ENUM }
        .let { (scalarTypes, inputObjectTypes) ->
          @Suppress("UNCHECKED_CAST")
          scalarTypes to (inputObjectTypes as List<IntrospectionSchema.Type.InputObject>)
        }

    val usedTypes = (scalarTypes + inputObjectTypes).toMutableSet()
    val visitedTypeNames = scalarTypes.map { it.name }.toMutableSet()

    val inputTypesToVisit = inputObjectTypes.toMutableList()
    while (inputTypesToVisit.isNotEmpty()) {
      val inputType = inputTypesToVisit.removeAt(inputTypesToVisit.lastIndex).also {
        usedTypes.add(it)
        visitedTypeNames.add(it.name)
      }
      val (nestedScalarTypes, nestedInputTypes) = inputType
          .inputFields
          .asSequence()
          .map { field -> field.type.rawType.name!! }
          .filterNot { type -> visitedTypeNames.contains(type) }
          .map { schema[it] ?: throw ParseException(message = "Undefined schema type `$it`") }
          .filter { type -> type.kind == IntrospectionSchema.Kind.SCALAR || type.kind == IntrospectionSchema.Kind.ENUM || type.kind == IntrospectionSchema.Kind.INPUT_OBJECT }
          .partition { type -> type.kind == IntrospectionSchema.Kind.SCALAR || type.kind == IntrospectionSchema.Kind.ENUM }
          .let { (scalarTypes, inputTypes) ->
            @Suppress("UNCHECKED_CAST")
            scalarTypes.filter { ScalarType.forName(it.name) == null } to (inputTypes as List<IntrospectionSchema.Type.InputObject>)
          }

      usedTypes.addAll(nestedScalarTypes)
      visitedTypeNames.addAll(nestedScalarTypes.map { it.name })

      inputTypesToVisit.addAll(nestedInputTypes)
    }
    return usedTypes
  }

  private fun List<Field>.referencedFragmentNames(fragments: List<Fragment>, filePath: String): Set<String> {
    return flatMap { it.referencedFragmentNames(fragments = fragments, filePath = filePath) }
        .union(flatMap { it.fields.referencedFragmentNames(fragments = fragments, filePath = filePath) })
        .union(flatMap { it.inlineFragments.flatMap { it.referencedFragments(fragments = fragments, filePath = filePath) } })
  }

  private fun Field.referencedFragmentNames(fragments: List<Fragment>, filePath: String): Set<String> {
    val rawFieldType = type.replace("!", "").replace("[", "").replace("]", "")
    val referencedFragments = fragmentRefs.findFragments(
        typeCondition = rawFieldType,
        fragments = fragments,
        filePath = filePath
    )
    return fragmentRefs.map { it.name }
        .union(referencedFragments.flatMap { it.referencedFragments(fragments) })
  }

  private fun InlineFragment.referencedFragments(fragments: List<Fragment>, filePath: String): Set<String> {
    val referencedFragments = this.fragments.findFragments(
        typeCondition = typeCondition,
        fragments = fragments,
        filePath = filePath
    )
    return this.fragments.map { it.name }
        .union(fields.referencedFragmentNames(fragments = fragments, filePath = filePath))
        .union(referencedFragments.flatMap { it.referencedFragments(fragments) })
  }

  private fun Fragment.referencedFragments(fragments: List<Fragment>): Set<String> {
    val referencedFragments = fragmentRefs.findFragments(
        typeCondition = typeCondition,
        fragments = fragments,
        filePath = filePath
    )
    return fragmentRefs.map { it.name }
        .union(fields.referencedFragmentNames(fragments = fragments, filePath = filePath))
        .union(inlineFragments.flatMap { it.referencedFragments(fragments = fragments, filePath = filePath) })
        .union(referencedFragments.flatMap { it.referencedFragments(fragments) })
  }

  private fun List<FragmentRef>.findFragments(typeCondition: String, fragments: List<Fragment>, filePath: String): List<Fragment> {
    return map { ref ->
      val fragment = fragments.find { fragment -> fragment.fragmentName == ref.name }
          ?: throw DocumentParseException(
              message = "Unknown fragment `${ref.name}`",
              sourceLocation = ref.sourceLocation,
              filePath = filePath
          )

      when (val schemaType = schema[typeCondition]) {
        is IntrospectionSchema.Type.Object -> schemaType.possibleTypes(schema)
        is IntrospectionSchema.Type.Interface -> schemaType.possibleTypes(schema)
        is IntrospectionSchema.Type.Union -> schemaType.possibleTypes(schema)
        else -> emptySet()
      }.also { possibleTypes ->
        if (fragment.possibleTypes.intersect(possibleTypes).isEmpty()) {
          throw DocumentParseException(
              message = "Fragment `${ref.name}` can't be spread here as result can never be of type `${fragment.typeCondition}`",
              sourceLocation = ref.sourceLocation,
              filePath = filePath
          )
        }
      }

      fragment
    }
  }
}