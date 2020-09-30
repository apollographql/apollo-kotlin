package com.apollographql.apollo.compiler.ast

import com.apollographql.apollo.compiler.codegen.normalizeGraphQLType
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.apollographql.apollo.compiler.ir.Condition
import com.apollographql.apollo.compiler.ir.Field
import com.apollographql.apollo.compiler.ir.FragmentRef
import com.apollographql.apollo.compiler.ir.InlineFragment
import com.apollographql.apollo.compiler.parser.introspection.IntrospectionSchema
import com.apollographql.apollo.compiler.parser.introspection.resolveType
import com.apollographql.apollo.compiler.singularize

internal class ObjectTypeBuilder(
    private val schema: IntrospectionSchema,
    private val customTypes: CustomTypes,
    private val typesPackageName: String,
    private val fragmentsPackage: String,
    private val irFragments: Map<String, com.apollographql.apollo.compiler.ir.Fragment>,
    private val nestedTypeContainer: ObjectTypeContainerBuilder,
    private val enclosingType: CodeGenerationAst.TypeRef?
) {

  fun buildObjectType(
      typeRef: CodeGenerationAst.TypeRef,
      schemaType: IntrospectionSchema.Type,
      fields: List<Field>,
      abstract: Boolean,
      implements: Set<CodeGenerationAst.TypeRef> = emptySet()
  ): CodeGenerationAst.ObjectType {
    return CodeGenerationAst.ObjectType(
        name = typeRef.name,
        description = schemaType.description ?: "",
        deprecated = false,
        deprecationReason = "",
        fields = fields.map { field -> field.toAstField(abstract) },
        implements = implements,
        schemaType = schemaType.name,
        kind = CodeGenerationAst.ObjectType.Kind.Interface.takeIf { abstract } ?: CodeGenerationAst.ObjectType.Kind.Object
    )
  }

  private fun buildObjectType(
      name: String,
      schemaType: IntrospectionSchema.Type,
      fields: List<Field>,
      abstract: Boolean,
      implements: List<CodeGenerationAst.TypeRef>,
      singularizeName: Boolean = true
  ): CodeGenerationAst.TypeRef {
    return nestedTypeContainer.registerObjectType(
        typeName = name,
        enclosingType = enclosingType,
        singularizeName = singularizeName
    ) { typeRef ->
      CodeGenerationAst.ObjectType(
          name = typeRef.name,
          description = schemaType.description ?: "",
          deprecated = false,
          deprecationReason = "",
          fields = fields.map { field -> field.toAstField(abstract) },
          implements = implements.toSet(),
          schemaType = schemaType.name,
          kind = CodeGenerationAst.ObjectType.Kind.Interface.takeIf { abstract } ?: CodeGenerationAst.ObjectType.Kind.Object
      )
    }
  }

  private fun Field.toAstField(abstract: Boolean): CodeGenerationAst.Field {
    return CodeGenerationAst.Field(
        name = this.responseName.escapeKotlinReservedWord(),
        schemaName = this.fieldName,
        responseName = this.responseName,
        type = resolveFieldType(
            field = this,
            schemaTypeRef = schema.resolveType(this.type),
            abstract = abstract
        ),
        description = this.description,
        deprecated = this.isDeprecated,
        deprecationReason = this.deprecationReason,
        arguments = this.args.associate { it.name to it.value },
        conditions = this.normalizedConditions.toSet(),
        override = false
    )
  }

  fun resolveFieldType(
      field: Field,
      schemaTypeRef: IntrospectionSchema.TypeRef,
      abstract: Boolean,
      singularizeName: Boolean = false
  ): CodeGenerationAst.FieldType {
    return when (schemaTypeRef.kind) {
      IntrospectionSchema.Kind.ENUM -> CodeGenerationAst.FieldType.Scalar.Enum(
          nullable = true,
          typeRef = CodeGenerationAst.TypeRef(
              name = schemaTypeRef.name!!.capitalize().escapeKotlinReservedWord(),
              packageName = typesPackageName
          )
      )

      IntrospectionSchema.Kind.INTERFACE,
      IntrospectionSchema.Kind.OBJECT,
      IntrospectionSchema.Kind.UNION -> {
        if (field.inlineFragments.isEmpty() && field.fragmentRefs.isEmpty()) {
          val typeRef = buildObjectType(
              name = field.responseName,
              schemaType = schema.resolveType(schemaTypeRef),
              fields = field.fields,
              abstract = abstract,
              implements = emptyList(),
              singularizeName = singularizeName
          )
          CodeGenerationAst.FieldType.Object(
              nullable = true,
              typeRef = typeRef
          )
        } else {
          field.resolveFieldWithFragmentsType(
              singularizeName = singularizeName,
              abstract = abstract
          )
        }
      }

      IntrospectionSchema.Kind.SCALAR -> {
        when (schemaTypeRef.name!!.toUpperCase()) {
          "STRING" -> CodeGenerationAst.FieldType.Scalar.String(nullable = true)
          "INT" -> CodeGenerationAst.FieldType.Scalar.Int(nullable = true)
          "BOOLEAN" -> CodeGenerationAst.FieldType.Scalar.Boolean(nullable = true)
          "FLOAT" -> CodeGenerationAst.FieldType.Scalar.Float(nullable = true)
          else -> {
            val customType = checkNotNull(customTypes[schemaTypeRef.name])
            CodeGenerationAst.FieldType.Scalar.Custom(
                nullable = true,
                schemaType = schemaTypeRef.name,
                type = customType.mappedType,
                customEnumType = CodeGenerationAst.TypeRef(
                    name = customType.name,
                    packageName = typesPackageName,
                    enclosingType = CodeGenerationAst.customTypeRef(typesPackageName)
                )
            )
          }
        }
      }

      IntrospectionSchema.Kind.NON_NULL -> resolveFieldType(
          field = field,
          schemaTypeRef = schemaTypeRef.ofType!!,
          abstract = abstract,
          singularizeName = singularizeName
      ).nonNullable()

      IntrospectionSchema.Kind.LIST -> CodeGenerationAst.FieldType.Array(
          nullable = true,
          rawType = resolveFieldType(
              field = field,
              schemaTypeRef = schemaTypeRef.ofType!!,
              abstract = abstract,
              singularizeName = true
          )
      )

      else -> throw IllegalArgumentException("Unsupported selection field type `$schemaTypeRef`")
    }.let { type ->
      if (field.isConditional) type.nullable() else type
    }
  }

  private fun List<Fragment>.buildFragmentPossibleTypes(
      rootFragmentInterfaceType: CodeGenerationAst.TypeRef,
      rootFragmentInterfaceFields: List<Field>
  ): Map<String, CodeGenerationAst.TypeRef> {

    val fragmentPossibleTypes = this.flatMap { fragment -> fragment.possibleTypes }
        .toSet()
        // for each possible type find all possible fragments defined on it
        .map { possibleType -> possibleType to this.filter { fragment -> fragment.possibleTypes.contains(possibleType) } }
        // group possible types by the same set of fragments
        .fold(emptyMap<List<Fragment>, List<String>>()) { acc, (possibleType, fragments) ->
          acc + (fragments to (acc[fragments]?.plus(possibleType) ?: listOf(possibleType)))
        }

    // generate interface types only for fragments that intersect with other fragments
    val fragmentInterfaceTypes = fragmentPossibleTypes
        .filter { (fragments, _) -> fragments.size > 1 }
        .keys
        .flatten()
        .map { fragment ->
          fragment to (fragment.interfaceType ?: buildObjectType(
              name = fragment.typeCondition,
              schemaType = schema.resolveType(schema.resolveType(fragment.typeCondition)),
              fields = fragment.fields,
              abstract = true,
              implements = listOf(rootFragmentInterfaceType)
          ))
        }.toMap()

    return fragmentPossibleTypes
        .flatMap { (fragments, possibleTypes) ->
          val implementationType = when (fragments.size) {
            1 -> fragments.single().buildImplementationType(
                rootFragmentInterfaceType = rootFragmentInterfaceType,
                rootFragmentInterfaceFields = rootFragmentInterfaceFields,
                fragmentInterfaceTypes = fragmentInterfaceTypes
            )

            else -> fragments.buildImplementationType(
                rootFragmentInterfaceType = rootFragmentInterfaceType,
                rootFragmentInterfaceFields = rootFragmentInterfaceFields,
                fragmentInterfaceTypes = fragmentInterfaceTypes
            )
          }
          // associate each possible type with implementation type
          possibleTypes.map { it to implementationType }
        }
        .toMap()
  }

  private fun List<Fragment>.buildImplementationType(
      rootFragmentInterfaceType: CodeGenerationAst.TypeRef,
      rootFragmentInterfaceFields: List<Field>,
      fragmentInterfaceTypes: Map<Fragment, CodeGenerationAst.TypeRef>
  ): CodeGenerationAst.TypeRef {
    val typeName = joinToString(separator = "", postfix = "Impl") { fragment ->
      fragment.interfaceType?.name ?: fragment.typeCondition.capitalize().singularize()
    }
    return nestedTypeContainer.registerObjectType(
        typeName = typeName,
        enclosingType = enclosingType
    ) { typeRef ->

      // collect fields from all fragments
      val fields = flatMap { fragment -> fragment.fields }
          .plus(rootFragmentInterfaceFields)
          .distinctBy { field -> field.responseName }
          .associateBy { field -> field.responseName }
          .mapValues { (_, field) -> field.toAstField(abstract = false) }

      CodeGenerationAst.ObjectType(
          name = typeRef.name,
          description = "",
          deprecated = false,
          deprecationReason = "",
          fields = fields.values.toList(),
          implements = mapNotNull { fragment -> fragmentInterfaceTypes[fragment] }.plus(rootFragmentInterfaceType).toSet(),
          schemaType = null,
          kind = CodeGenerationAst.ObjectType.Kind.Object
      )
    }
  }

  private fun Fragment.buildImplementationType(
      rootFragmentInterfaceType: CodeGenerationAst.TypeRef,
      rootFragmentInterfaceFields: List<Field>,
      fragmentInterfaceTypes: Map<Fragment, CodeGenerationAst.TypeRef>
  ): CodeGenerationAst.TypeRef {
    val fragmentOnInterfaceTypesToImplement = fragmentInterfaceTypes
        .filter { (fragment, _) -> fragment.possibleTypes.contains(this.typeCondition) || fragment.typeCondition == this.typeCondition }
    val schemaType = schema.resolveType(schema.resolveType(this.typeCondition))
    val fieldsToMerge = rootFragmentInterfaceFields +
        fragmentOnInterfaceTypesToImplement.keys.filter { fragment -> fragment != this }.flatMap { it.fields }
    return if (
        fieldsToMerge.size == 1 &&
        fieldsToMerge.single() == Field.TYPE_NAME_FIELD &&
        this.interfaceType != null
    ) {
      // if named fragment used without mixing additional fields except `__typename` it's safe to skip fragment implementation generation
      // and use delegation to default implementation of this fragment instead
      nestedTypeContainer.registerObjectType(
          typeName = "${this.interfaceType.name}Impl",
          enclosingType = enclosingType,
          singularizeName = true
      ) { typeRef ->
        CodeGenerationAst.ObjectType(
            name = typeRef.name,
            description = schemaType.description ?: "",
            deprecated = false,
            deprecationReason = "",
            fields = emptyList(),
            implements = fragmentOnInterfaceTypesToImplement
                .map { (_, superInterfaceTypeRef) -> superInterfaceTypeRef }
                .plus(rootFragmentInterfaceType)
                .plus(listOfNotNull(this.interfaceType))
                .toSet(),
            schemaType = schemaType.name,
            kind = CodeGenerationAst.ObjectType.Kind.FragmentDelegate(this.interfaceType),
        )
      }
    } else {
      val typeName = when {
        this.interfaceType?.name != null -> "${this.interfaceType.name}Impl"
        schemaType.kind === IntrospectionSchema.Kind.OBJECT -> this.typeCondition
        else -> "${this.typeCondition}Impl"
      }
      buildObjectType(
          name = typeName,
          schemaType = schemaType,
          fields = this.fields.merge(fieldsToMerge),
          abstract = false,
          implements = fragmentOnInterfaceTypesToImplement
              .map { (_, superInterfaceTypeRef) -> superInterfaceTypeRef }
              .plus(rootFragmentInterfaceType)
              .plus(listOfNotNull(this.interfaceType))
      )
    }
  }

  private fun Field.resolveFieldWithFragmentsType(
      singularizeName: Boolean,
      abstract: Boolean
  ): CodeGenerationAst.FieldType.Object {
    val fieldSchemaTypeRef = schema.resolveType(this.type).rawType
    val fieldSchemaType = schema.resolveType(fieldSchemaTypeRef)
    val fragments = inlineFragments
        .toFragments()
        .plus(fragmentRefs.map { fragmentRef -> fragmentRef.toFragment() })
    return when {
      // when we generate just interfaces (in case of named fragments) skip fragment generation entirely
      abstract -> {
        val typeRef = buildObjectType(
            name = responseName,
            schemaType = fieldSchemaType,
            fields = fields,
            abstract = abstract,
            implements = emptyList(),
            singularizeName = singularizeName
        )
        CodeGenerationAst.FieldType.Object(
            nullable = true,
            typeRef = typeRef
        )
      }

      // when there is only one fragment and type condition aligns with field type
      // that means there is no need to generate fragment implementation but rather merge fields from this fragment
      fragments.size == 1 && fragments.first().typeCondition == this.type.normalizeGraphQLType() -> {
        val typeRef = buildObjectType(
            name = responseName,
            schemaType = fieldSchemaType,
            fields = fields.merge(fragments.first().fields),
            abstract = abstract,
            implements = listOfNotNull(fragments.first().interfaceType),
            singularizeName = singularizeName
        )
        CodeGenerationAst.FieldType.Object(
            nullable = true,
            typeRef = typeRef
        )
      }

      else -> {
        val fragmentRootInterfaceType = nestedTypeContainer.registerObjectType(
            typeName = responseName,
            enclosingType = enclosingType,
            singularizeName = singularizeName
        ) { fragmentRootInterfaceType ->
          val possibleImplementations = fragments.buildFragmentPossibleTypes(
              rootFragmentInterfaceType = fragmentRootInterfaceType,
              rootFragmentInterfaceFields = fields
          )
          val defaultImplementationType = buildObjectType(
              name = "${responseName.singularize()}Impl",
              schemaType = fieldSchemaType,
              fields = fields,
              abstract = false,
              implements = listOf(fragmentRootInterfaceType),
              singularizeName = singularizeName
          )
          CodeGenerationAst.ObjectType(
              name = fragmentRootInterfaceType.name,
              description = fieldSchemaType.description ?: "",
              deprecated = false,
              deprecationReason = "",
              fields = fields.map { field -> field.toAstField(abstract = true) },
              implements = emptySet(),
              schemaType = fieldSchemaType.name,
              kind = CodeGenerationAst.ObjectType.Kind.Fragment(
                  defaultImplementation = defaultImplementationType,
                  possibleImplementations = possibleImplementations
              )
          )
        }
        CodeGenerationAst.FieldType.Object(
            nullable = isOptional(),
            typeRef = fragmentRootInterfaceType
        )
      }
    }
  }

  private val Field.normalizedConditions: List<CodeGenerationAst.Field.Condition>
    get() {
      return if (isConditional) {
        conditions.filter { it.kind == Condition.Kind.BOOLEAN.rawValue }.map {
          CodeGenerationAst.Field.Condition.Directive(
              variableName = it.variableName,
              inverted = it.inverted
          )
        }
      } else {
        emptyList()
      }
    }

  private fun List<InlineFragment>.toFragments(): List<Fragment> {
    return if (isEmpty()) {
      emptyList()
    } else {
      flatMap { inlineFragment ->
        listOf(
            Fragment(
                typeCondition = inlineFragment.typeCondition,
                possibleTypes = inlineFragment.possibleTypes,
                description = inlineFragment.description,
                fields = inlineFragment.fields,
                fragments = inlineFragment.fragments,
                interfaceType = null
            )
        ) + inlineFragment.fragments.map { it.toFragment() }
      } + flatMap { fragment -> fragment.inlineFragments }.toFragments()
    }
  }

  private fun FragmentRef.toFragment(): Fragment {
    val fragment = requireNotNull(irFragments[name]) {
      "Unknown fragment `${name}` reference"
    }
    return Fragment(
        typeCondition = fragment.typeCondition,
        possibleTypes = fragment.possibleTypes,
        description = fragment.description,
        fields = fragment.fields,
        fragments = fragment.fragmentRefs,
        interfaceType = CodeGenerationAst.TypeRef(
            name = fragment.fragmentName.capitalize().escapeKotlinReservedWord(),
            packageName = fragmentsPackage
        )
    )
  }

  private data class Fragment(
      val typeCondition: String,
      val possibleTypes: List<String>,
      val description: String,
      val fields: List<Field>,
      val fragments: List<FragmentRef>,
      val interfaceType: CodeGenerationAst.TypeRef?
  )
}
