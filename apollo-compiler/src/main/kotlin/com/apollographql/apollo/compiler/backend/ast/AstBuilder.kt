package com.apollographql.apollo.compiler.backend.ast

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.compiler.backend.ir.BackendIr
import com.apollographql.apollo.compiler.backend.ir.SelectionKey
import com.apollographql.apollo.compiler.introspection.IntrospectionSchema
import com.apollographql.apollo.compiler.introspection.resolveType
import com.apollographql.apollo.compiler.operationoutput.OperationOutput
import com.apollographql.apollo.compiler.operationoutput.findOperationId
import com.apollographql.apollo.compiler.singularize
import com.squareup.kotlinpoet.MemberName

internal class AstBuilder private constructor(
    private val backendIr: BackendIr,
    private val schema: IntrospectionSchema,
    private val customScalarsMapping: Map<String, String>,
    private val typesPackageName: String,
    private val fragmentsPackage: String,
    private val operationOutput: OperationOutput,
) {

  companion object {
    fun BackendIr.buildAst(
        schema: IntrospectionSchema,
        customScalarsMapping: Map<String, String>,
        typesPackageName: String,
        fragmentsPackage: String,
        operationOutput: OperationOutput,
    ): CodeGenerationAst {
      return AstBuilder(
          backendIr = this,
          schema = schema,
          customScalarsMapping = customScalarsMapping,
          typesPackageName = typesPackageName,
          fragmentsPackage = fragmentsPackage,
          operationOutput = operationOutput,
      ).build()
    }
  }

  private fun build(): CodeGenerationAst {
    val enums = buildEnumTypes()
    val customTypes = buildCustomTypes()
    val inputTypes = buildInputTypes(customTypes)
    val operations = buildOperationTypes(customTypes)
    val fragments = buildFragmentTypes(customTypes)
    return CodeGenerationAst(
        operationTypes = operations,
        fragmentTypes = fragments,
        inputTypes = inputTypes,
        enumTypes = enums,
        customScalarTypes = customTypes,
    )
  }

  private fun buildEnumTypes(): List<CodeGenerationAst.EnumType> {
    return schema.types.values
        .filter { type -> type.kind == IntrospectionSchema.Kind.ENUM }
        .map { type ->
          val enumSchemaType = type as IntrospectionSchema.Type.Enum
          val name = enumSchemaType.name.normalizeTypeName()
          CodeGenerationAst.EnumType(
              graphqlName = enumSchemaType.name,
              name = name,
              description = enumSchemaType.description ?: "",
              consts = enumSchemaType.enumValues.map { value ->
                val constName = value.name.toUpperCase()
                val deprecationReason = value.deprecationReason.takeIf { value.isDeprecated }
                CodeGenerationAst.EnumConst(
                    constName = constName,
                    value = value.name,
                    description = value.description ?: "",
                    deprecationReason = deprecationReason,
                )
              },
          )
        }
  }

  private fun buildCustomTypes(): CustomScalarTypes {
    return customScalarsMapping.mapValues { (schemaType, mappedType) ->
      CodeGenerationAst.CustomScalarType(
          name = schemaType.normalizeTypeName(),
          schemaType = schemaType,
          mappedType = mappedType,
      )
    }
  }

  private fun buildInputTypes(customScalarTypes: CustomScalarTypes): List<CodeGenerationAst.InputType> {
    fun CodeGenerationAst.FieldType.isCustomScalarField(): Boolean {
      return when (this) {
        is CodeGenerationAst.FieldType.Scalar.Custom -> true
        is CodeGenerationAst.FieldType.Array -> rawType.isCustomScalarField()
        else -> false
      }
    }

    return schema.types.values
        .filter { type -> type.kind == IntrospectionSchema.Kind.INPUT_OBJECT }
        .map { type ->
          val inputSchemaType = type as IntrospectionSchema.Type.InputObject
          val name = inputSchemaType.name.normalizeTypeName()
          CodeGenerationAst.InputType(
              graphqlName = inputSchemaType.name,
              name = name,
              description = inputSchemaType.description ?: "",
              deprecationReason = null,
              fields = inputSchemaType.inputFields.map { field ->
                val fieldSchemaTypeRef = inputSchemaType.resolveInputField(field.name).type
                val fieldName = field.name.normalizeFieldName()
                val fieldType = fieldSchemaTypeRef.resolveInputFieldType(
                    typesPackageName = typesPackageName,
                    customScalarTypes = customScalarTypes,
                )
                val deprecationReason = field.deprecationReason?.takeIf { field.isDeprecated }
                val defaultValue = field.defaultValue
                    .takeUnless { fieldType.isCustomScalarField() }
                    ?.normalizeDefaultValue(fieldSchemaTypeRef)
                CodeGenerationAst.InputField(
                    name = fieldName,
                    schemaName = field.name,
                    deprecationReason = deprecationReason,
                    type = fieldType,
                    description = field.description ?: "",
                    defaultValue = defaultValue,
                )
              }
          )
        }
  }

  private fun Any?.normalizeDefaultValue(type: IntrospectionSchema.TypeRef): Any? {
    if (this == null) return null

    return when (type.kind) {
      IntrospectionSchema.Kind.SCALAR -> {
        when (type.name!!.toUpperCase()) {
          "INT" -> toString().trim().takeIf { it != "null" }?.toInt()
          "BOOLEAN" -> toString().trim().takeIf { it != "null" }?.toBoolean()
          "FLOAT" -> toString().trim().takeIf { it != "null" }?.toDouble()
          else -> toString()
        }
      }

      IntrospectionSchema.Kind.NON_NULL -> normalizeDefaultValue(type.ofType!!)

      IntrospectionSchema.Kind.LIST -> {
        toString().removePrefix("[").removeSuffix("]").split(',').filter { it.isNotBlank() }.map { value ->
          value.trim().replace("\"", "").normalizeDefaultValue(type.ofType!!)
        }
      }

      else -> toString()
    }
  }

  private fun IntrospectionSchema.TypeRef.resolveInputFieldType(
      typesPackageName: String,
      customScalarTypes: CustomScalarTypes
  ): CodeGenerationAst.FieldType {
    return when (this.kind) {
      IntrospectionSchema.Kind.ENUM -> CodeGenerationAst.FieldType.Scalar.Enum(
          nullable = true,
          typeRef = CodeGenerationAst.TypeRef(
              name = this.name!!.normalizeTypeName(),
              packageName = typesPackageName,
          )
      )

      IntrospectionSchema.Kind.SCALAR -> {
        when (this.name!!.toUpperCase()) {
          "ID" -> CodeGenerationAst.FieldType.Scalar.String(nullable = true)
          "STRING" -> CodeGenerationAst.FieldType.Scalar.String(nullable = true)
          "INT" -> CodeGenerationAst.FieldType.Scalar.Int(nullable = true)
          "BOOLEAN" -> CodeGenerationAst.FieldType.Scalar.Boolean(nullable = true)
          "FLOAT" -> CodeGenerationAst.FieldType.Scalar.Float(nullable = true)
          else -> {
            val customType = checkNotNull(customScalarsMapping[this.name]) {
              "Failed to resolve custom scalar type `${this.name}`"
            }
            CodeGenerationAst.FieldType.Scalar.Custom(
                nullable = true,
                schemaType = this.name,
                type = customType,
                memberName = MemberName(typesPackageName, this.name.toUpperCase())
            )
          }
        }
      }

      IntrospectionSchema.Kind.NON_NULL -> this.ofType!!.resolveInputFieldType(
          typesPackageName = typesPackageName,
          customScalarTypes = customScalarTypes,
      ).nonNullable()

      IntrospectionSchema.Kind.LIST -> CodeGenerationAst.FieldType.Array(
          nullable = true,
          rawType = this.ofType!!.resolveInputFieldType(
              typesPackageName = typesPackageName,
              customScalarTypes = customScalarTypes,
          )
      )

      IntrospectionSchema.Kind.INPUT_OBJECT -> {
        CodeGenerationAst.FieldType.Object(
            nullable = true,
            typeRef = CodeGenerationAst.TypeRef(
                name = this.name!!.normalizeTypeName(),
                packageName = typesPackageName,
            )
        )
      }

      else -> throw IllegalArgumentException("Unsupported input field type `$this`")
    }
  }

  private fun IntrospectionSchema.Type.resolveInputField(name: String): IntrospectionSchema.InputField {
    return (this as? IntrospectionSchema.Type.InputObject)?.inputFields?.find { field -> field.name == name }
        ?: throw IllegalArgumentException("Failed to resolve input field `$name` on type `${this.name}`")
  }

  private fun buildOperationTypes(customScalarTypes: CustomScalarTypes): List<CodeGenerationAst.OperationType> {
    return backendIr.operations.map { operation ->
      operation.buildOperationType(
          customScalarTypes = customScalarTypes
      )
    }
  }

  private fun BackendIr.Operation.buildOperationType(customScalarTypes: CustomScalarTypes): CodeGenerationAst.OperationType {
    val operationType = when (this.operationSchemaType) {
      schema.resolveType(schema.queryType) -> CodeGenerationAst.OperationType.Type.QUERY
      schema.mutationType?.let { schema.resolveType(it) } -> CodeGenerationAst.OperationType.Type.MUTATION
      schema.subscriptionType?.let { schema.resolveType(it) } -> CodeGenerationAst.OperationType.Type.SUBSCRIPTION
      else -> throw IllegalArgumentException("Unsupported GraphQL operation type: `${this.operationSchemaType}`")
    }
    val operationId = operationOutput.findOperationId(
        name = this.operationName,
        packageName = this.targetPackageName,
    )
    val operationDataType = this.astOperationDataObjectType(
        targetPackageName = this.targetPackageName,
        customScalarTypes = customScalarTypes,
    ).run {
      copy(
          implements = implements + CodeGenerationAst.TypeRef(
              name = "Data",
              enclosingType = CodeGenerationAst.TypeRef(
                  name = Operation::class.java.simpleName,
                  packageName = Operation::class.java.`package`.name
              )
          )
      )
    }
    return CodeGenerationAst.OperationType(
        name = this.name.normalizeTypeName(),
        packageName = this.targetPackageName,
        type = operationType,
        operationName = this.operationName,
        description = this.comment,
        operationId = operationId,
        queryDocument = this.definition,
        variables = this.variables.map { variable ->
          val fieldType = variable.type.resolveInputFieldType(
              typesPackageName = typesPackageName,
              customScalarTypes = customScalarTypes,
          )
          CodeGenerationAst.InputField(
              name = variable.name.normalizeFieldName(),
              schemaName = variable.name,
              deprecationReason = null,
              type = fieldType,
              description = "",
              defaultValue = null,
          )
        },
        dataType = operationDataType,
    )
  }

  private fun BackendIr.Operation.astOperationDataObjectType(
      targetPackageName: String,
      customScalarTypes: CustomScalarTypes,
  ): CodeGenerationAst.ObjectType {
    return this.dataField.asAstObjectType(
        targetPackageName = targetPackageName,
        abstract = false,
        currentSelectionKey = this.dataField.selectionKeys.single(),
        customScalarTypes = customScalarTypes,
    )
  }

  private fun buildFragmentTypes(customScalarTypes: CustomScalarTypes): List<CodeGenerationAst.FragmentType> {
    return backendIr.fragments.map { fragment ->
      fragment.buildFragmentType(
          customScalarTypes = customScalarTypes
      )
    }
  }

  private fun BackendIr.NamedFragment.buildFragmentType(customScalarTypes: CustomScalarTypes): CodeGenerationAst.FragmentType {
    val schemaType = schema.resolveType(this.selectionSet.typeCondition)
    val interfaceType = buildObjectType(
        name = name,
        description = schemaType.description,
        schemaTypename = schemaType.name,
        fields = selectionSet.fields,
        fragments = selectionSet.fragments,
        targetPackageName = fragmentsPackage,
        abstract = true,
        customScalarTypes = customScalarTypes,
        currentSelectionKey = SelectionKey(
            root = name,
            keys = listOf(name),
            type = SelectionKey.Type.Fragment,
        ),
        alternativeSelectionKeys = selectionSet.selectionKeys,
    )
    val implementationType = buildObjectType(
        name = this.defaultImplementationSelectionKey.root,
        description = schemaType.description,
        schemaTypename = schemaType.name,
        fields = defaultImplementationSelectionSet.fields,
        fragments = defaultImplementationSelectionSet.fragments,
        targetPackageName = fragmentsPackage,
        abstract = false,
        customScalarTypes = customScalarTypes,
        currentSelectionKey = defaultImplementationSelectionKey,
        alternativeSelectionKeys = defaultImplementationSelectionSet.selectionKeys,
    )
    return CodeGenerationAst.FragmentType(
        name = this.name.normalizeTypeName(),
        graphqlName = this.name,
        description = this.comment,
        interfaceType = interfaceType,
        defaultImplementationType = implementationType.copy(fragmentAccessors = emptyList()),
        fragmentDefinition = this.source,
        typeRef = CodeGenerationAst.TypeRef(
            name = this.name.normalizeTypeName(),
            packageName = fragmentsPackage,
        )
    )
  }

  private fun BackendIr.Field.asAstObjectType(
      targetPackageName: String,
      abstract: Boolean,
      currentSelectionKey: SelectionKey,
      customScalarTypes: CustomScalarTypes,
  ): CodeGenerationAst.ObjectType {
    val schemaType = schema.resolveType(schema.resolveType(this.type.rawType.name!!))
    return buildObjectType(
        name = responseName,
        description = schemaType.description,
        schemaTypename = this.type.rawType.name,
        fields = fields,
        fragments = fragments,
        targetPackageName = targetPackageName,
        abstract = abstract,
        customScalarTypes = customScalarTypes,
        currentSelectionKey = currentSelectionKey,
        alternativeSelectionKeys = selectionKeys,
    )
  }

  private fun buildObjectType(
      name: String,
      description: String?,
      schemaTypename: String?,
      fields: List<BackendIr.Field>,
      fragments: BackendIr.Fragments,
      targetPackageName: String,
      abstract: Boolean,
      customScalarTypes: CustomScalarTypes,
      currentSelectionKey: SelectionKey,
      alternativeSelectionKeys: Set<SelectionKey>,
  ): CodeGenerationAst.ObjectType {
    return if (fragments.isEmpty()) {
      buildObjectTypeWithoutFragments(
          name = name,
          description = description,
          schemaTypename = schemaTypename,
          fields = fields,
          targetPackageName = targetPackageName,
          abstract = abstract,
          customScalarTypes = customScalarTypes,
          currentSelectionKey = currentSelectionKey,
          alternativeSelectionKeys = alternativeSelectionKeys,
      )
    } else {
      buildObjectTypeWithFragments(
          parentTypeName = name,
          description = description,
          schemaTypename = schemaTypename,
          fields = fields,
          fragments = fragments,
          targetPackageName = targetPackageName,
          abstract = abstract,
          customScalarTypes = customScalarTypes,
          selectionKey = currentSelectionKey,
          alternativeSelectionKeys = alternativeSelectionKeys,
      )
    }
  }

  private fun buildObjectTypeWithoutFragments(
      name: String,
      description: String?,
      schemaTypename: String?,
      fields: List<BackendIr.Field>,
      targetPackageName: String,
      abstract: Boolean,
      customScalarTypes: CustomScalarTypes,
      currentSelectionKey: SelectionKey,
      alternativeSelectionKeys: Set<SelectionKey>,
  ): CodeGenerationAst.ObjectType {
    val astFields = fields.map { field ->
      field.buildField(
          targetPackageName = targetPackageName,
          customScalarTypes = customScalarTypes,
          selectionKey = currentSelectionKey + field.responseName,
      )
    }
    val implements = alternativeSelectionKeys.mapNotNull { keys ->
      if (keys != currentSelectionKey) {
        keys.asTypeRef(targetPackageName)
      } else null
    }.toSet()
    val kind = CodeGenerationAst.ObjectType.Kind.Interface.takeIf { abstract } ?: CodeGenerationAst.ObjectType.Kind.Object
    val nestedObjects = fields
        .filter { field -> field.fields.isNotEmpty() || field.fragments.isNotEmpty() }
        .map { field ->
          field.asAstObjectType(
              targetPackageName = targetPackageName,
              abstract = abstract,
              currentSelectionKey = currentSelectionKey + field.responseName,
              customScalarTypes = customScalarTypes,
          )
        }
    return CodeGenerationAst.ObjectType(
        name = name.normalizeTypeName(),
        description = description ?: "",
        deprecationReason = null,
        fields = astFields,
        implements = implements,
        kind = kind,
        typeRef = currentSelectionKey.asTypeRef(targetPackageName),
        nestedObjects = nestedObjects,
        schemaTypename = schemaTypename,
        fragmentAccessors = emptyList(),
    )
  }

  private fun buildObjectTypeWithFragments(
      parentTypeName: String,
      description: String?,
      schemaTypename: String?,
      fields: List<BackendIr.Field>,
      fragments: BackendIr.Fragments,
      targetPackageName: String,
      abstract: Boolean,
      customScalarTypes: CustomScalarTypes,
      selectionKey: SelectionKey,
      alternativeSelectionKeys: Set<SelectionKey>,
  ): CodeGenerationAst.ObjectType {
    val possibleImplementations = if (abstract) emptyMap() else fragments
        .filter { fragment -> fragment.type == BackendIr.Fragment.Type.Implementation }
        .flatMap { fragment ->
          val typeRef = (selectionKey + fragment.name).asTypeRef(targetPackageName)
          fragment.possibleTypes.map { possibleType -> possibleType.name!! to typeRef }
        }
        .toMap()

    val kind = CodeGenerationAst.ObjectType.Kind.Fragment(
        defaultImplementation = (selectionKey + "Other${parentTypeName.normalizeTypeName()}").asTypeRef(targetPackageName),
        possibleImplementations = possibleImplementations,
    )
    val astFields = fields.map { field ->
      field.buildField(
          targetPackageName = targetPackageName,
          customScalarTypes = customScalarTypes,
          selectionKey = selectionKey + field.responseName,
      )
    }
    val implements = alternativeSelectionKeys.mapNotNull { keys ->
      keys
          .takeIf { keys != selectionKey }
          ?.asTypeRef(targetPackageName)
    }.toSet()
    val nestedObjects = buildFragmentNestedObjectTypes(
        fragments = fragments,
        abstract = abstract,
        fields = fields,
        targetPackageName = targetPackageName,
        customScalarTypes = customScalarTypes,
        selectionKey = selectionKey,
    )
    val objectTypeRef = selectionKey.asTypeRef(targetPackageName)
    val fragmentAccessors = fragments.accessors
        .map { (name, selectionKey) ->
          CodeGenerationAst.ObjectType.FragmentAccessor(
              name = name.normalizeFieldName(),
              typeRef = selectionKey.asTypeRef(targetPackageName),
          )
        }

    return CodeGenerationAst.ObjectType(
        name = parentTypeName.normalizeTypeName(),
        description = description ?: "",
        deprecationReason = null,
        fields = astFields,
        implements = implements,
        kind = kind,
        typeRef = objectTypeRef,
        nestedObjects = nestedObjects,
        schemaTypename = schemaTypename,
        fragmentAccessors = fragmentAccessors,
    )
  }

  private fun buildFragmentNestedObjectTypes(
      fragments: List<BackendIr.Fragment>,
      abstract: Boolean,
      fields: List<BackendIr.Field>,
      targetPackageName: String,
      selectionKey: SelectionKey,
      customScalarTypes: CustomScalarTypes,
  ): List<CodeGenerationAst.ObjectType> {
    val fragmentObjectTypes = fragments.map { fragment ->
      fragment.buildObjectType(
          targetPackageName = targetPackageName,
          abstract = abstract,
          selectionKey = selectionKey,
          customScalarTypes = customScalarTypes,
      )
    }
    val fieldNestedObjects = fields
        .filter { field -> field.fields.isNotEmpty() || field.fragments.isNotEmpty() }
        .map { field ->
          field.asAstObjectType(
              targetPackageName = targetPackageName,
              abstract = abstract || fragmentObjectTypes.isNotEmpty(),
              currentSelectionKey = selectionKey + field.responseName,
              customScalarTypes = customScalarTypes,
          )
        }
    return fieldNestedObjects.plus(fragmentObjectTypes)
  }

  private fun BackendIr.Fragment.buildObjectType(
      targetPackageName: String,
      abstract: Boolean,
      selectionKey: SelectionKey,
      customScalarTypes: CustomScalarTypes,
  ): CodeGenerationAst.ObjectType {
    return if (abstract || this.type == BackendIr.Fragment.Type.Interface) {
      val objectType = buildObjectTypeWithoutFragments(
          name = this.name,
          description = null,
          schemaTypename = null,
          fields = this.fields,
          targetPackageName = targetPackageName,
          abstract = true,
          customScalarTypes = customScalarTypes,
          currentSelectionKey = selectionKey + this.name,
          alternativeSelectionKeys = this.selectionKeys,
      )
      objectType.copy(
          nestedObjects = objectType.nestedObjects + (this.nestedFragments?.map { nestedFragment ->
            nestedFragment.buildObjectType(
                targetPackageName = targetPackageName,
                abstract = true,
                selectionKey = selectionKey + this.name,
                customScalarTypes = customScalarTypes,
            )
          } ?: emptyList())
      )
    } else {
      buildObjectType(
          name = this.name,
          description = null,
          schemaTypename = null,
          fields = this.fields,
          fragments = this.nestedFragments ?: BackendIr.Fragments(
              fragments = emptyList(),
              accessors = emptyMap(),
          ),
          targetPackageName = targetPackageName,
          abstract = abstract,
          customScalarTypes = customScalarTypes,
          currentSelectionKey = selectionKey + this.name,
          alternativeSelectionKeys = this.selectionKeys,
      )
    }
  }

  private fun BackendIr.Field.buildField(
      targetPackageName: String,
      customScalarTypes: CustomScalarTypes,
      selectionKey: SelectionKey,
  ): CodeGenerationAst.Field {
    return CodeGenerationAst.Field(
        name = this.responseName.normalizeFieldName(),
        schemaName = this.name,
        responseName = this.responseName,
        type = this.resolveFieldType(
            targetPackageName = targetPackageName,
            schemaTypeRef = this.type,
            customScalarTypes = customScalarTypes,
            selectionKey = selectionKey,
        ),
        description = this.description,
        deprecationReason = this.deprecationReason,
        arguments = this.args.associate { argument -> argument.name to argument.value },
        conditions = this.condition.toAst().toSet(),
        override = this.selectionKeys.any { key -> key != selectionKey }
    )
  }

  private fun BackendIr.Condition.toAst(): List<CodeGenerationAst.Field.Condition.Directive> {
    return when (this) {
      is BackendIr.Condition.True -> emptyList()
      is BackendIr.Condition.And -> conditions.filterIsInstance<BackendIr.Condition.Variable>().map {
        CodeGenerationAst.Field.Condition.Directive(
            variableName = it.name,
            inverted = it.inverted
        )
      }
      is BackendIr.Condition.Variable -> listOf(CodeGenerationAst.Field.Condition.Directive(
          variableName = name,
          inverted = inverted
      ))
      // FIXME
      else -> emptyList()
    }
  }

  private fun BackendIr.Field.resolveFieldType(
      targetPackageName: String,
      schemaTypeRef: IntrospectionSchema.TypeRef,
      customScalarTypes: CustomScalarTypes,
      selectionKey: SelectionKey,
  ): CodeGenerationAst.FieldType {
    return when (schemaTypeRef.kind) {
      IntrospectionSchema.Kind.ENUM -> CodeGenerationAst.FieldType.Scalar.Enum(
          nullable = true,
          typeRef = CodeGenerationAst.TypeRef(
              name = schemaTypeRef.name!!.normalizeTypeName(),
              packageName = typesPackageName
          )
      )

      IntrospectionSchema.Kind.INTERFACE,
      IntrospectionSchema.Kind.OBJECT,
      IntrospectionSchema.Kind.UNION -> {
        CodeGenerationAst.FieldType.Object(
            nullable = true,
            typeRef = (selectionKey).asTypeRef(targetPackageName)
        )
      }

      IntrospectionSchema.Kind.SCALAR -> {
        when (schemaTypeRef.name!!.toUpperCase()) {
          "ID" -> CodeGenerationAst.FieldType.Scalar.String(nullable = true)
          "STRING" -> CodeGenerationAst.FieldType.Scalar.String(nullable = true)
          "INT" -> CodeGenerationAst.FieldType.Scalar.Int(nullable = true)
          "BOOLEAN" -> CodeGenerationAst.FieldType.Scalar.Boolean(nullable = true)
          "FLOAT" -> CodeGenerationAst.FieldType.Scalar.Float(nullable = true)
          else -> {
            val customType = checkNotNull(customScalarsMapping[schemaTypeRef.name]) {
              "Failed to resolve custom scalar type `${schemaTypeRef.name}`"
            }
            CodeGenerationAst.FieldType.Scalar.Custom(
                nullable = true,
                schemaType = schemaTypeRef.name,
                type = customType,
                memberName = MemberName(typesPackageName, schemaTypeRef.name.toUpperCase())
            )
          }
        }
      }

      IntrospectionSchema.Kind.NON_NULL -> this.resolveFieldType(
          targetPackageName = targetPackageName,
          schemaTypeRef = schemaTypeRef.ofType!!,
          customScalarTypes = customScalarTypes,
          selectionKey = selectionKey,
      ).nonNullable()

      IntrospectionSchema.Kind.LIST -> CodeGenerationAst.FieldType.Array(
          nullable = true,
          rawType = this.resolveFieldType(
              targetPackageName = targetPackageName,
              schemaTypeRef = schemaTypeRef.ofType!!,
              customScalarTypes = customScalarTypes,
              selectionKey = selectionKey,
          )
      )

      else -> throw IllegalArgumentException("Unsupported selection field type `$schemaTypeRef`")
    }.let { type ->
      if (this.condition != BackendIr.Condition.True) type.nullable() else type
    }
  }

  private fun SelectionKey.asTypeRef(targetPackageName: String): CodeGenerationAst.TypeRef {
    check(this.keys.isNotEmpty()) {
      "Can't convert empty selection key to TypeRef"
    }
    val packageName = when (this.type) {
      SelectionKey.Type.Query -> targetPackageName
      SelectionKey.Type.Fragment -> fragmentsPackage
    }
    val rootTypeRef = CodeGenerationAst.TypeRef(
        name = this.keys.first().normalizeTypeName(),
        packageName = packageName,
        enclosingType = null
    )
    return this.keys.drop(1).fold(rootTypeRef) { enclosingType, key ->
      CodeGenerationAst.TypeRef(
          name = key.normalizeTypeName(),
          packageName = packageName,
          enclosingType = enclosingType
      )
    }
  }

  private fun String.normalizeFieldName(): String {
    val firstLetterIndex = this.indexOfFirst { it.isLetter() }
    return this.substring(0, firstLetterIndex) + this.substring(firstLetterIndex, this.length).decapitalize()
  }

  private fun String.normalizeTypeName(): String {
    val firstLetterIndex = this.indexOfFirst { it.isLetter() }
    return this.substring(0, firstLetterIndex) + this.substring(firstLetterIndex, this.length).singularize().capitalize()
  }
}
