package com.apollographql.apollo.compiler

import com.apollographql.apollo.api.*
import com.apollographql.apollo.compiler.ir.CodeGenerationContext
import com.apollographql.apollo.compiler.ir.Field
import com.apollographql.apollo.compiler.ir.InlineFragment
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

class SchemaTypeSpecBuilder(
    val typeName: String,
    val schemaType: String = "",
    val fields: List<Field>,
    val fragmentSpreads: List<String>,
    val inlineFragments: List<InlineFragment>,
    val context: CodeGenerationContext
) {
  private val uniqueTypeName = formatUniqueTypeName(typeName, context.reservedTypeNames)

  fun build(vararg modifiers: Modifier): TypeSpec {
    context.reservedTypeNames += uniqueTypeName
    val nestedTypeSpecs = nestedTypeSpecs()
    val nameOverrideMap = nestedTypeSpecs.map { it.first to it.second.name }.toMap()
    val responseFieldSpecs = responseFieldSpecs(nameOverrideMap)
    return TypeSpec.classBuilder(uniqueTypeName)
        .addModifiers(*modifiers)
        .addTypes(nestedTypeSpecs.map { it.second })
        .addFields(nameOverrideMap)
        .addInlineFragments(nameOverrideMap)
        .addFragments()
        .addType(responseMapperSpec(responseFieldSpecs))
        .addField(fieldArray(responseFieldSpecs))
        .addMethod(responseMarshallerSpec(responseFieldSpecs))
        .build()
        .withValueInitConstructor(context.nullableValueType)
        .withToStringImplementation()
        .withEqualsImplementation()
        .withHashCodeImplementation()
  }

  private fun TypeSpec.Builder.addFields(nameOverrideMap: Map<String, String>): TypeSpec.Builder {
    addFields(fields
        .map { it.fieldSpec(context, !context.generateAccessors) }
        .map { it.overrideType(nameOverrideMap) })
    if (context.generateAccessors) {
      addMethods(fields
          .map { it.accessorMethodSpec(context) }
          .map { it.overrideReturnType(nameOverrideMap) })
    }
    return this
  }

  private fun TypeSpec.Builder.addFragments(): TypeSpec.Builder {
    if (fragmentSpreads.isNotEmpty()) {
      addType(fragmentsTypeSpec())
      addField(fragmentsFieldSpec())
      if (context.generateAccessors) {
        addMethod(fragmentsAccessorMethodSpec())
      }
    }
    return this
  }

  private fun nestedTypeSpecs(): List<Pair<String, TypeSpec>> {
    return fields.filter(Field::isNonScalar)
        .map { it.formatClassName() to it.toTypeSpec(context) }
        .plus(inlineFragments.map { it.formatClassName() to it.toTypeSpec(context) })
  }

  private fun TypeSpec.Builder.addInlineFragments(nameOverrideMap: Map<String, String>): TypeSpec.Builder {
    addFields(inlineFragments
        .map { it.fieldSpec(context, !context.generateAccessors) }
        .map { it.overrideType(nameOverrideMap) })

    if (context.generateAccessors) {
      addMethods(inlineFragments
          .map { it.accessorMethodSpec(context) }
          .map { it.overrideReturnType(nameOverrideMap) })
    }

    return this
  }

  private fun fragmentsAccessorMethodSpec(): MethodSpec {
    return MethodSpec.methodBuilder(FRAGMENTS_FIELD.name)
        .returns(FRAGMENTS_FIELD.type)
        .addModifiers(Modifier.PUBLIC)
        .addModifiers(emptyList())
        .addStatement("return this.\$L", FRAGMENTS_FIELD.name)
        .build()
  }

  private fun fragmentsFieldSpec(): FieldSpec {
    return FRAGMENTS_FIELD.toBuilder()
        .addModifiers(if (context.generateAccessors) Modifier.PRIVATE else Modifier.PUBLIC, Modifier.FINAL)
        .build()
  }

  /** Returns a generic `Fragments` interface with methods for each of the provided fragments */
  private fun fragmentsTypeSpec(): TypeSpec {

    fun isOptional(fragmentName: String): Boolean {
      return context.ir.fragments
          .find { it.fragmentName == fragmentName }
          ?.let { it.typeCondition != normalizeGraphQlType(schemaType) } ?: true
    }

    fun fragmentFields(): List<FieldSpec> {
      return fragmentSpreads.map { fragmentName ->
        val optional = isOptional(fragmentName)
        FieldSpec.builder(
            JavaTypeResolver(context = context, packageName = context.fragmentsPackage)
                .resolve(typeName = fragmentName.capitalize(), isOptional = optional), fragmentName.decapitalize())
            .let { if (!context.generateAccessors) it.addModifiers(Modifier.PUBLIC) else it }
            .addModifiers(Modifier.FINAL)
            .build()
      }
    }

    fun TypeSpec.Builder.addFragmentAccessorMethods(): TypeSpec.Builder {
      if (context.generateAccessors) {
        addMethods(fragmentSpreads.map { fragmentName ->
          val optional = isOptional(fragmentName)
          MethodSpec.methodBuilder(fragmentName.decapitalize())
              .returns(JavaTypeResolver(context = context, packageName = context.fragmentsPackage)
                  .resolve(typeName = fragmentName.capitalize(), isOptional = optional))
              .addModifiers(Modifier.PUBLIC)
              .addStatement("return this.\$L", fragmentName.decapitalize())
              .build()
        })
      }
      return this
    }

    fun responseMarshallerSpec(fieldSpecs: List<FieldSpec>): MethodSpec {
      val code = fieldSpecs
          .map { fieldSpec ->
            CodeBlock.builder()
                .addStatement("final \$T \$L = \$L", fieldSpec.type.unwrapOptionalType().withoutAnnotations(),
                    "\$${fieldSpec.name}", fieldSpec.type.unwrapOptionalValue(fieldSpec.name))
                .beginControlFlow("if (\$L != null)", "\$${fieldSpec.name}")
                .addStatement("\$L.\$L().marshal(\$L)", "\$${fieldSpec.name}", RESPONSE_MARSHALLER_PARAM_NAME,
                    RESPONSE_WRITER_PARAM.name)
                .endControlFlow()
                .build()
          }
          .fold(CodeBlock.builder(), CodeBlock.Builder::add)
          .build()
      val methodSpec = MethodSpec.methodBuilder("marshal")
          .addModifiers(Modifier.PUBLIC)
          .addAnnotation(Override::class.java)
          .addParameter(RESPONSE_WRITER_PARAM)
          .addCode(code)
          .build()
      val marshallerType = TypeSpec.anonymousClassBuilder("")
          .addSuperinterface(ResponseFieldMarshaller::class.java)
          .addMethod(methodSpec)
          .build()
      return MethodSpec.methodBuilder(RESPONSE_MARSHALLER_PARAM_NAME)
          .addModifiers(Modifier.PUBLIC)
          .returns(ResponseFieldMarshaller::class.java)
          .addStatement("return \$L", marshallerType)
          .build()
    }

    val fragmentFields = fragmentFields()
    val mapper = FragmentsResponseMapperBuilder(fragmentFields, context).build()
    return TypeSpec.classBuilder(FRAGMENTS_FIELD.name.capitalize())
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addFields(fragmentFields)
        .addFragmentAccessorMethods()
        .addType(mapper)
        .addMethod(responseMarshallerSpec(fragmentFields))
        .build()
        .withValueInitConstructor(context.nullableValueType)
        .withToStringImplementation()
        .withEqualsImplementation()
        .withHashCodeImplementation()
  }

  private fun formatUniqueTypeName(typeName: String, reservedTypeNames: List<String>): String {
    var index = 1
    var name = typeName
    while (reservedTypeNames.contains(name)) {
      name = "$typeName$index"
      index++
    }
    return name
  }

  private fun responseFieldSpecs(nameOverrideMap: Map<String, String>): List<ResponseFieldSpec> {
    fun responseFieldType(schemaField: Field, fieldSpec: FieldSpec): ResponseField.Type {
      if (fieldSpec.type.isList()) {
        val rawFieldType = fieldSpec.type.let { if (it.isList()) it.listParamType() else it }
        if (schemaField.type.isCustomScalarType(context)) {
          return ResponseField.Type.CUSTOM_LIST
        } else if (rawFieldType.isScalar(context)) {
          return ResponseField.Type.SCALAR_LIST
        } else {
          return ResponseField.Type.OBJECT_LIST
        }
      }

      if (schemaField.type.isCustomScalarType(context)) {
        return ResponseField.Type.CUSTOM
      }

      if (fieldSpec.type.isScalar(context)) {
        return when (fieldSpec.type) {
          TypeName.INT, TypeName.INT.box() -> ResponseField.Type.INT
          TypeName.LONG, TypeName.LONG.box() -> ResponseField.Type.LONG
          TypeName.DOUBLE, TypeName.DOUBLE.box() -> ResponseField.Type.DOUBLE
          TypeName.BOOLEAN, TypeName.BOOLEAN.box() -> ResponseField.Type.BOOLEAN
          else -> ResponseField.Type.STRING
        }
      }

      return ResponseField.Type.OBJECT
    }

    val responseFields = fields.map { irField ->
      val fieldSpec = irField.fieldSpec(context).overrideType(nameOverrideMap)
      val normalizedFieldSpec = FieldSpec.builder(
          fieldSpec.type.unwrapOptionalType().withoutAnnotations(),
          fieldSpec.name
      ).build()
      ResponseFieldSpec(
          irField = irField,
          fieldSpec = fieldSpec,
          normalizedFieldSpec = normalizedFieldSpec,
          responseFieldType = if (normalizedFieldSpec.type.isEnum(context))
            ResponseField.Type.ENUM
          else
            responseFieldType(irField, normalizedFieldSpec),
          context = context
      )
    }
    val inlineFragments = inlineFragments.map { inlineFragment ->
      val fieldSpec = inlineFragment.fieldSpec(context).overrideType(nameOverrideMap)
      val normalizedFieldSpec = FieldSpec.builder(
          fieldSpec.type.unwrapOptionalType().withoutAnnotations(),
          fieldSpec.name
      ).build()
      ResponseFieldSpec(
          irField = Field.TYPE_NAME_FIELD,
          fieldSpec = fieldSpec,
          normalizedFieldSpec = normalizedFieldSpec,
          responseFieldType = ResponseField.Type.INLINE_FRAGMENT,
          typeConditions = if (inlineFragment.possibleTypes != null && !inlineFragment.possibleTypes.isEmpty())
            inlineFragment.possibleTypes
          else
            listOf(inlineFragment.typeCondition),
          context = context
      )
    }
    val fragments = if (fragmentSpreads.isNotEmpty()) {
      val normalizedFieldSpec = FieldSpec.builder(
          FRAGMENTS_FIELD.type.withoutAnnotations(),
          FRAGMENTS_FIELD.name
      ).build()
      listOf(ResponseFieldSpec(
          irField = Field.TYPE_NAME_FIELD,
          fieldSpec = FRAGMENTS_FIELD,
          normalizedFieldSpec = normalizedFieldSpec,
          responseFieldType = ResponseField.Type.FRAGMENT,
          typeConditions = context.ir.fragments
              .filter { it.fragmentName in fragmentSpreads }
              .flatMap { it.possibleTypes },
          context = context
      ))
    } else {
      emptyList()
    }

    return responseFields + inlineFragments + fragments
  }

  private fun fieldArray(responseFieldSpecs: List<ResponseFieldSpec>): FieldSpec {
    return FieldSpec
        .builder(RESPONSE_FIELDS_PARAM.type, RESPONSE_FIELDS_PARAM.name)
        .addModifiers(Modifier.STATIC, Modifier.FINAL)
        .initializer(CodeBlock
            .builder()
            .add("{\n")
            .indent()
            .add(responseFieldSpecs
                .map { it.factoryCode() }
                .foldIndexed(CodeBlock.builder()) { i, builder, code ->
                  builder.add(if (i > 0) ",\n" else "").add(code)
                }
                .build()
            )
            .unindent()
            .add("\n}")
            .build()
        )
        .build()
  }

  private fun responseMapperSpec(responseFieldSpecs: List<ResponseFieldSpec>): TypeSpec {
    fun mapperFields(): List<FieldSpec> {
      return responseFieldSpecs
          .filter { !it.irField.type.isCustomScalarType(context) }
          .map { it.normalizedFieldSpec.type }
          .map { it.let { if (it.isList()) it.listParamType() else it } }
          .filter { !it.isScalar(context) }
          .map { it.unwrapOptionalType().withoutAnnotations() }
          .map { it as ClassName }
          .map {
            val mapperClassName = ClassName.get(it.packageName(), it.simpleName(), Util.RESPONSE_FIELD_MAPPER_TYPE_NAME)
            FieldSpec.builder(mapperClassName, it.mapperFieldName(), Modifier.FINAL)
                .initializer(CodeBlock.of("new \$L()", mapperClassName))
                .build()
          }
    }

    val typeClassName = ClassName.get("", uniqueTypeName)
    val code = CodeBlock.builder()
        .add(responseFieldSpecs
            .mapIndexed { i, field ->
              field.readValueCode(
                  readerParam = CodeBlock.of("\$L", RESPONSE_READER_PARAM.name),
                  fieldParam = CodeBlock.of("\$L[\$L]", RESPONSE_FIELDS_PARAM.name, i))
            }
            .fold(CodeBlock.builder(), CodeBlock.Builder::add)
            .build())
        .add("return new \$T(", typeClassName)
        .add(responseFieldSpecs
            .mapIndexed { i, field -> CodeBlock.of("\$L\$L", if (i > 0) ", " else "", field.fieldSpec.name) }
            .fold(CodeBlock.builder(), CodeBlock.Builder::add)
            .build())
        .add(");\n")
        .build()
    val methodSpec = MethodSpec.methodBuilder("map")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override::class.java)
        .addParameter(RESPONSE_READER_PARAM)
        .returns(typeClassName)
        .addCode(code)
        .build()

    return TypeSpec.classBuilder(Util.RESPONSE_FIELD_MAPPER_TYPE_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .addSuperinterface(ParameterizedTypeName.get(ClassName.get(ResponseFieldMapper::class.java), typeClassName))
        .addFields(mapperFields())
        .addMethod(methodSpec)
        .build()
  }

  private fun responseMarshallerSpec(responseFieldSpecs: List<ResponseFieldSpec>): MethodSpec {
    val writeCode = responseFieldSpecs
        .mapIndexed { i, field ->
          field.writeValueCode(
              writerParam = CodeBlock.of("\$L", RESPONSE_WRITER_PARAM.name),
              fieldParam = CodeBlock.of("\$L[\$L]", RESPONSE_FIELDS_PARAM.name, i),
              marshaller = CodeBlock.of("$RESPONSE_MARSHALLER_PARAM_NAME()")
          )
        }
        .fold(CodeBlock.builder(), CodeBlock.Builder::add)
        .build()
    val methodSpec = MethodSpec.methodBuilder("marshal")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override::class.java)
        .addParameter(RESPONSE_WRITER_PARAM)
        .addCode(writeCode)
        .build()
    val marshallerType = TypeSpec.anonymousClassBuilder("")
        .addSuperinterface(ResponseFieldMarshaller::class.java)
        .addMethod(methodSpec)
        .build()
    return MethodSpec.methodBuilder(RESPONSE_MARSHALLER_PARAM_NAME)
        .addModifiers(Modifier.PUBLIC)
        .returns(ResponseFieldMarshaller::class.java)
        .addStatement("return \$L", marshallerType)
        .build()
  }

  companion object {
    private val RESPONSE_FIELDS_PARAM =
        ParameterSpec.builder(Array<ResponseField>::class.java, "\$responseFields").build()
    private val RESPONSE_READER_PARAM =
        ParameterSpec.builder(ResponseReader::class.java, "reader").build()
    private val RESPONSE_WRITER_PARAM =
        ParameterSpec.builder(ResponseWriter::class.java, "writer").build()
    private const val RESPONSE_MARSHALLER_PARAM_NAME = "marshaller"
    val FRAGMENTS_FIELD: FieldSpec =
        FieldSpec.builder(ClassName.get("", "Fragments").annotated(Annotations.NONNULL), "fragments").build()
  }
}
