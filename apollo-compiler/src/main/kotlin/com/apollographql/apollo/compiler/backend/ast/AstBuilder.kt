package com.apollographql.apollo.compiler.backend.ast

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.compiler.backend.ir.BackendIr
import com.apollographql.apollo.compiler.backend.ir.SelectionKey
import com.apollographql.apollo.compiler.ir.ScalarType
import com.apollographql.apollo.compiler.operationoutput.OperationOutput
import com.apollographql.apollo.compiler.operationoutput.findOperationId
import com.apollographql.apollo.compiler.parser.introspection.IntrospectionSchema
import com.apollographql.apollo.compiler.parser.introspection.resolveType
import com.apollographql.apollo.compiler.singularize

internal class AstBuilder private constructor(
    private val backendIr: BackendIr,
    private val schema: IntrospectionSchema,
    private val customTypeMap: Map<String, String>,
    private val typesPackageName: String,
    private val fragmentsPackage: String,
    private val operationOutput: OperationOutput,
) {

  companion object {
    fun BackendIr.buildAst(
        schema: IntrospectionSchema,
        customTypeMap: Map<String, String>,
        typesPackageName: String,
        fragmentsPackage: String,
        operationOutput: OperationOutput,
    ): CodeGenerationAst {
      return AstBuilder(
          backendIr = this,
          schema = schema,
          customTypeMap = customTypeMap,
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
        customTypes = customTypes,
    )
  }

  private fun buildEnumTypes(): List<CodeGenerationAst.EnumType> {
    return backendIr.typeDeclarations
        .filter { type -> type.kind == IntrospectionSchema.Kind.ENUM }
        .map { type ->
          val enumSchemaType = schema.resolveType(type) as IntrospectionSchema.Type.Enum
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

  private fun buildCustomTypes(): CustomTypes {
    return customTypeMap
        .filter { (schemaType, _) ->
          backendIr.typeDeclarations
              .find { typeRef -> typeRef.rawType.name == schemaType } != null
        }
        .mapValues { (schemaType, mappedType) ->
          CodeGenerationAst.CustomType(
              name = schemaType.normalizeTypeName(),
              schemaType = schemaType,
              mappedType = mappedType,
          )
        }
  }

  private fun buildInputTypes(customTypes: CustomTypes): List<CodeGenerationAst.InputType> {
    fun CodeGenerationAst.FieldType.isCustomScalarField(): Boolean {
      return when (this) {
        is CodeGenerationAst.FieldType.Scalar.Custom -> true
        is CodeGenerationAst.FieldType.Array -> rawType.isCustomScalarField()
        else -> false
      }
    }

    return backendIr
        .typeDeclarations
        .filter { type -> type.kind == IntrospectionSchema.Kind.INPUT_OBJECT }
        .map { type ->
          val inputSchemaType = schema.resolveType(type) as IntrospectionSchema.Type.InputObject
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
                    customTypes = customTypes,
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
        when (ScalarType.forName(type.name ?: "")) {
          ScalarType.INT -> toString().trim().takeIf { it != "null" }?.toInt()
          ScalarType.BOOLEAN -> toString().trim().takeIf { it != "null" }?.toBoolean()
          ScalarType.FLOAT -> toString().trim().takeIf { it != "null" }?.toDouble()
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
      customTypes: CustomTypes
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
          "STRING" -> CodeGenerationAst.FieldType.Scalar.String(nullable = true)
          "INT" -> CodeGenerationAst.FieldType.Scalar.Int(nullable = true)
          "BOOLEAN" -> CodeGenerationAst.FieldType.Scalar.Boolean(nullable = true)
          "FLOAT" -> CodeGenerationAst.FieldType.Scalar.Float(nullable = true)
          else -> {
            val customType = checkNotNull(customTypes[this.name])
            CodeGenerationAst.FieldType.Scalar.Custom(
                nullable = true,
                schemaType = this.name,
                type = customType.mappedType,
                customEnumType = CodeGenerationAst.TypeRef(
                    name = customType.name,
                    packageName = typesPackageName,
                    enclosingType = CodeGenerationAst.customTypeRef(typesPackageName),
                )
            )
          }
        }
      }

      IntrospectionSchema.Kind.NON_NULL -> this.ofType!!.resolveInputFieldType(
          typesPackageName = typesPackageName,
          customTypes = customTypes,
      ).nonNullable()

      IntrospectionSchema.Kind.LIST -> CodeGenerationAst.FieldType.Array(
          nullable = true,
          rawType = this.ofType!!.resolveInputFieldType(
              typesPackageName = typesPackageName,
              customTypes = customTypes,
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

  private fun buildOperationTypes(customTypes: CustomTypes): List<CodeGenerationAst.OperationType> {
    return backendIr.operations.map { operation ->
      operation.buildOperationType(
          customTypes = customTypes
      )
    }
  }

  private fun BackendIr.Operation.buildOperationType(customTypes: CustomTypes): CodeGenerationAst.OperationType {
    val operationType = when (this.operationType) {
      schema.resolveType(schema.queryType) -> CodeGenerationAst.OperationType.Type.QUERY
      schema.resolveType(schema.mutationType) -> CodeGenerationAst.OperationType.Type.MUTATION
      schema.resolveType(schema.subscriptionType) -> CodeGenerationAst.OperationType.Type.SUBSCRIPTION
      else -> throw IllegalArgumentException("Unsupported GraphQL operation type: `${this.operationType}`")
    }
    val operationId = operationOutput.findOperationId(
        name = this.operationName,
        packageName = this.targetPackageName,
    )
    val operationDataType = this.astOperationDataObjectType(
        targetPackageName = this.targetPackageName,
        customTypes = customTypes,
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
              customTypes = customTypes,
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
      customTypes: CustomTypes,
  ): CodeGenerationAst.ObjectType {
    return this.dataField.asAstObjectType(
        targetPackageName = targetPackageName,
        abstract = false,
        currentSelectionKey = this.dataField.selectionKeys.single(),
        customTypes = customTypes,
    )
  }

  private fun buildFragmentTypes(customTypes: CustomTypes): List<CodeGenerationAst.FragmentType> {
    return backendIr.fragments.map { fragment ->
      fragment.buildFragmentType(
          customTypes = customTypes
      )
    }
  }

  private fun BackendIr.NamedFragment.buildFragmentType(customTypes: CustomTypes): CodeGenerationAst.FragmentType {
    val interfaceType = buildObjectType(
        name = name,
        schemaTypeRef = selectionSet.typeCondition,
        fields = selectionSet.fields,
        inlineFragments = selectionSet.fragments,
        targetPackageName = fragmentsPackage,
        abstract = true,
        customTypes = customTypes,
        currentSelectionKey = SelectionKey(
            root = name,
            keys = listOf(name),
            type = SelectionKey.Type.Fragment,
        ),
        alternativeSelectionKeys = selectionSet.selectionKeys,
    )
    val implementationType = buildObjectType(
        name = "DefaultImpl",
        schemaTypeRef = defaultSelectionSet.typeCondition,
        fields = defaultSelectionSet.fields,
        inlineFragments = defaultSelectionSet.fragments,
        targetPackageName = fragmentsPackage,
        abstract = false,
        customTypes = customTypes,
        currentSelectionKey = SelectionKey(
            root = name,
            keys = listOf(name, "DefaultImpl"),
            type = SelectionKey.Type.Fragment,
        ),
        alternativeSelectionKeys = defaultSelectionSet.selectionKeys,
    )
    return CodeGenerationAst.FragmentType(
        name = this.name.normalizeTypeName(),
        graphqlName = this.name,
        description = this.comment,
        interfaceType = interfaceType,
        implementationType = implementationType,
        fragmentDefinition = this.source,
    )
  }

  private fun BackendIr.Field.asAstObjectType(
      targetPackageName: String,
      abstract: Boolean,
      currentSelectionKey: SelectionKey,
      customTypes: CustomTypes,
  ): CodeGenerationAst.ObjectType {
    return buildObjectType(
        name = responseName,
        schemaTypeRef = type,
        fields = fields,
        inlineFragments = fragments,
        targetPackageName = targetPackageName,
        abstract = abstract,
        customTypes = customTypes,
        currentSelectionKey = currentSelectionKey,
        alternativeSelectionKeys = selectionKeys,
    )
  }

  private fun buildObjectType(
      name: String,
      schemaTypeRef: IntrospectionSchema.TypeRef,
      fields: List<BackendIr.Field>,
      inlineFragments: List<BackendIr.InlineFragment>,
      targetPackageName: String,
      abstract: Boolean,
      customTypes: CustomTypes,
      currentSelectionKey: SelectionKey,
      alternativeSelectionKeys: Set<SelectionKey>,
  ): CodeGenerationAst.ObjectType {
    return if (inlineFragments.isEmpty()) {
      buildObjectTypeWithoutFragments(
          name = name,
          schemaTypeRef = schemaTypeRef,
          fields = fields,
          targetPackageName = targetPackageName,
          abstract = abstract,
          customTypes = customTypes,
          currentSelectionKey = currentSelectionKey,
          alternativeSelectionKeys = alternativeSelectionKeys,
      )
    } else {
      buildObjectTypeWithFragments(
          parentTypeName = name,
          schemaTypeRef = schemaTypeRef,
          fields = fields,
          inlineFragments = inlineFragments,
          targetPackageName = targetPackageName,
          abstract = abstract,
          customTypes = customTypes,
          selectionKey = currentSelectionKey,
          alternativeSelectionKeys = alternativeSelectionKeys,
      )
    }
  }

  private fun buildObjectTypeWithoutFragments(
      name: String,
      schemaTypeRef: IntrospectionSchema.TypeRef,
      fields: List<BackendIr.Field>,
      targetPackageName: String,
      abstract: Boolean,
      customTypes: CustomTypes,
      currentSelectionKey: SelectionKey,
      alternativeSelectionKeys: Set<SelectionKey>,
  ): CodeGenerationAst.ObjectType {
    val schemaType = schema.resolveType(schemaTypeRef.rawType)
    val astFields = fields.map { field ->
      field.buildField(
          targetPackageName = targetPackageName,
          customTypes = customTypes,
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
              customTypes = customTypes,
          )
        }
    return CodeGenerationAst.ObjectType(
        name = name.normalizeTypeName(),
        description = schemaType.description ?: "",
        deprecationReason = null,
        fields = astFields,
        implements = implements,
        kind = kind,
        typeRef = currentSelectionKey.asTypeRef(targetPackageName),
        nestedObjects = nestedObjects,
        introspectionSchemaType = schemaTypeRef.rawType,
    )
  }

  private fun buildObjectTypeWithFragments(
      parentTypeName: String,
      schemaTypeRef: IntrospectionSchema.TypeRef,
      fields: List<BackendIr.Field>,
      inlineFragments: List<BackendIr.InlineFragment>,
      targetPackageName: String,
      abstract: Boolean,
      customTypes: CustomTypes,
      selectionKey: SelectionKey,
      alternativeSelectionKeys: Set<SelectionKey>,
  ): CodeGenerationAst.ObjectType {
    val schemaType = schema.resolveType(schemaTypeRef.rawType)
    val kind = CodeGenerationAst.ObjectType.Kind.Interface.takeIf { abstract }
        ?: buildFragmentKind(
            parentTypeName = parentTypeName,
            inlineFragments = inlineFragments,
            targetPackageName = targetPackageName,
            selectionKey = selectionKey,
        )
    val astFields = fields.map { field ->
      field.buildField(
          targetPackageName = targetPackageName,
          customTypes = customTypes,
          selectionKey = selectionKey + field.responseName,
      )
    }
    val implements = alternativeSelectionKeys.mapNotNull { keys ->
      keys
          .takeIf { keys != selectionKey }
          ?.asTypeRef(targetPackageName)
    }.toSet()
    val nestedObjects = buildFieldWithFragmentNestedObjects(
        inlineFragments = inlineFragments,
        abstract = abstract,
        fields = fields,
        targetPackageName = targetPackageName,
        customTypes = customTypes,
        selectionKey = selectionKey,
        parentTypeName = parentTypeName,
        schemaTypeRef = schemaTypeRef,
        alternativeSelectionKeys = alternativeSelectionKeys,
    )
    return CodeGenerationAst.ObjectType(
        name = parentTypeName.normalizeTypeName(),
        description = schemaType.description ?: "",
        deprecationReason = null,
        fields = astFields,
        implements = implements,
        kind = kind,
        typeRef = selectionKey.asTypeRef(targetPackageName),
        nestedObjects = nestedObjects,
        introspectionSchemaType = schemaTypeRef.rawType,
    )
  }

  private fun buildFragmentKind(
      parentTypeName: String,
      inlineFragments: List<BackendIr.InlineFragment>,
      targetPackageName: String,
      selectionKey: SelectionKey,
  ): CodeGenerationAst.ObjectType.Kind.Fragment {
    val inlineFragmentInterfaces = inlineFragments.filterIsInstance<BackendIr.InlineFragment.Interface>()
    val namedFragmentTypeRefs = inlineFragmentInterfaces
        .flatMap { inlineFragment -> inlineFragment.selectionKeys }
        .filter { key -> key.type == SelectionKey.Type.Fragment && key.keys.size == 1 }
        .map { key -> key.asTypeRef(targetPackageName) }
    val inlineFragmentInterfacesTypeRefs = inlineFragmentInterfaces
        .map { inlineFragment -> selectionKey.plus(inlineFragment.name) }
        .map { key -> key.asTypeRef(targetPackageName) }
    val fragmentAccessors = inlineFragmentInterfacesTypeRefs
        .associateBy { typeRef -> "as${typeRef.name.capitalize()}" }
        .plus(
            namedFragmentTypeRefs.associateBy { typeRef ->
              "as${typeRef.name.capitalize()}"
            }
        )
    val possibleImplementations = inlineFragments.flatMap { fragment ->
      val typeRef = (selectionKey + fragment.name).asTypeRef(targetPackageName)
      fragment.possibleTypes.map { possibleType -> possibleType.name!! to typeRef }
    }.toMap()

    return CodeGenerationAst.ObjectType.Kind.Fragment(
        defaultImplementation = (selectionKey + "Other${parentTypeName.normalizeTypeName()}").asTypeRef(targetPackageName),
        possibleImplementations = possibleImplementations,
        fragmentAccessors = fragmentAccessors,
    )
  }

  private fun buildFieldWithFragmentNestedObjects(
      inlineFragments: List<BackendIr.InlineFragment>,
      abstract: Boolean,
      fields: List<BackendIr.Field>,
      targetPackageName: String,
      selectionKey: SelectionKey,
      customTypes: CustomTypes,
      parentTypeName: String,
      schemaTypeRef: IntrospectionSchema.TypeRef,
      alternativeSelectionKeys: Set<SelectionKey>,
  ): List<CodeGenerationAst.ObjectType> {
    val fragmentObjectTypes = inlineFragments.mapNotNull { fragment ->
      if (fragment is BackendIr.InlineFragment.Interface || !abstract) {
        fragment.buildObjectType(
            targetPackageName = targetPackageName,
            abstract = abstract,
            selectionKey = selectionKey,
            customTypes = customTypes,
        ).run {
          copy(
              implements = implements + selectionKey.asTypeRef(targetPackageName)
          )
        }
      } else null
    }
    val fieldNestedObjects = fields
        .filter { field -> field.fields.isNotEmpty() || field.fragments.isNotEmpty() }
        .map { field ->
          field.asAstObjectType(
              targetPackageName = targetPackageName,
              abstract = true,
              currentSelectionKey = selectionKey + field.responseName,
              customTypes = customTypes,
          )
        }
    val otherFragmentObjectType = if (abstract) null else buildObjectType(
        name = "Other${parentTypeName.normalizeTypeName()}",
        schemaTypeRef = schemaTypeRef.rawType,
        fields = fields,
        inlineFragments = emptyList(),
        targetPackageName = targetPackageName,
        abstract = abstract,
        customTypes = customTypes,
        currentSelectionKey = selectionKey + "Other${parentTypeName.normalizeTypeName()}",
        alternativeSelectionKeys = alternativeSelectionKeys
            .plus(setOf(selectionKey + "Other${parentTypeName.normalizeTypeName()}"))
            .toSet(),
    ).run {
      copy(
          implements = implements + selectionKey.asTypeRef(targetPackageName)
      )
    }
    return fieldNestedObjects
        .plus(fragmentObjectTypes)
        .run {
          if (otherFragmentObjectType != null) this.plus(otherFragmentObjectType) else this
        }
  }

  private fun BackendIr.InlineFragment.buildObjectType(
      targetPackageName: String,
      abstract: Boolean,
      selectionKey: SelectionKey,
      customTypes: CustomTypes,
  ): CodeGenerationAst.ObjectType {
    val name = this.name.normalizeTypeName()
    val fields = this.fields.map { field ->
      field.buildField(
          targetPackageName = targetPackageName,
          customTypes = customTypes,
          selectionKey = selectionKey + this.name + field.responseName,
      )
    }
    val implements = this.selectionKeys
        .mapNotNull { keys ->
          keys
              .takeIf { keys != (selectionKey + this.name) }
              ?.asTypeRef(targetPackageName)
        }
        .toSet()
    val kind = CodeGenerationAst.ObjectType.Kind.Interface.takeIf { this is BackendIr.InlineFragment.Interface }
        ?: CodeGenerationAst.ObjectType.Kind.Object
    val typeRef = (selectionKey + this.name).asTypeRef(targetPackageName)
    val nestedObjects = this.fields
        .filter { field -> field.fields.isNotEmpty() || field.fragments.isNotEmpty() }
        .map { field ->
          field.asAstObjectType(
              targetPackageName = targetPackageName,
              abstract = abstract || this is BackendIr.InlineFragment.Interface,
              currentSelectionKey = selectionKey + this.name + field.responseName,
              customTypes = customTypes,
          )
        }
    val introspectionSchemaType = this.possibleTypes.first().takeIf { this.possibleTypes.size == 1 }
    return CodeGenerationAst.ObjectType(
        name = name,
        description = this.description ?: "",
        deprecationReason = null,
        fields = fields,
        implements = implements,
        kind = kind,
        typeRef = typeRef,
        nestedObjects = nestedObjects,
        introspectionSchemaType = introspectionSchemaType,
    )
  }

  private fun BackendIr.Field.buildField(
      targetPackageName: String,
      customTypes: CustomTypes,
      selectionKey: SelectionKey,
  ): CodeGenerationAst.Field {
    return CodeGenerationAst.Field(
        name = this.responseName.normalizeFieldName(),
        schemaName = this.name,
        responseName = this.responseName,
        type = this.resolveFieldType(
            targetPackageName = targetPackageName,
            schemaTypeRef = this.type,
            customTypes = customTypes,
            selectionKey = selectionKey,
        ),
        description = this.description,
        deprecationReason = this.deprecationReason,
        arguments = this.args.associate { argument -> argument.name to argument.value },
        conditions = this.conditions.map { condition ->
          CodeGenerationAst.Field.Condition.Directive(
              variableName = condition.variableName,
              inverted = condition.inverted
          )
        }.toSet(),
        override = this.selectionKeys.any { key -> key != selectionKey }
    )
  }

  private fun BackendIr.Field.resolveFieldType(
      targetPackageName: String,
      schemaTypeRef: IntrospectionSchema.TypeRef,
      customTypes: CustomTypes,
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

      IntrospectionSchema.Kind.NON_NULL -> this.resolveFieldType(
          targetPackageName = targetPackageName,
          schemaTypeRef = schemaTypeRef.ofType!!,
          customTypes = customTypes,
          selectionKey = selectionKey,
      ).nonNullable()

      IntrospectionSchema.Kind.LIST -> CodeGenerationAst.FieldType.Array(
          nullable = true,
          rawType = this.resolveFieldType(
              targetPackageName = targetPackageName,
              schemaTypeRef = schemaTypeRef.ofType!!,
              customTypes = customTypes,
              selectionKey = selectionKey,
          )
      )

      else -> throw IllegalArgumentException("Unsupported selection field type `$schemaTypeRef`")
    }.let { type ->
      if (this.conditions.isNotEmpty()) type.nullable() else type
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
