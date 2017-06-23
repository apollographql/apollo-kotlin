package com.apollographql.apollo.compiler

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.ResponseFieldMapper
import com.apollographql.apollo.api.ResponseReader
import com.apollographql.apollo.compiler.ir.CodeGenerationContext
import com.apollographql.apollo.compiler.ir.Field
import com.apollographql.apollo.compiler.ir.InlineFragment
import com.squareup.javapoet.*
import java.io.IOException
import java.util.*
import javax.lang.model.element.Modifier

class SchemaTypeSpecBuilder(
    val typeName: String,
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
    return MethodSpec.methodBuilder(FRAGMENTS_TYPE_NAME.decapitalize())
        .returns(JavaTypeResolver(context, "").resolve(FRAGMENTS_TYPE_NAME, false))
        .addModifiers(Modifier.PUBLIC)
        .addModifiers(emptyList())
        .addCode(CodeBlock.of("return this.${FRAGMENTS_TYPE_NAME.toLowerCase(Locale.ENGLISH)};\n"))
        .build()
  }

  private fun fragmentsFieldSpec(): FieldSpec = FieldSpec
      .builder(ClassName.get("", FRAGMENTS_TYPE_NAME.capitalize()).annotated(Annotations.NONNULL),
          FRAGMENTS_TYPE_NAME.decapitalize())
      .addModifiers(if (context.generateAccessors) Modifier.PRIVATE else Modifier.PUBLIC, Modifier.FINAL)
      .build()

  /** Returns a generic `Fragments` interface with methods for each of the provided fragments */
  private fun fragmentsTypeSpec(): TypeSpec {

    fun isOptional(fragmentName: String): Boolean {
      return context.ir.fragments
          .find { it.fragmentName == fragmentName }
          ?.let { it.typeCondition == typeName } ?: true
    }

    fun TypeSpec.Builder.addFragmentFields(): TypeSpec.Builder {
      return addFields(fragmentSpreads.map { fragmentName ->
        val optional = isOptional(fragmentName)
        FieldSpec.builder(
            JavaTypeResolver(context = context, packageName = context.fragmentsPackage)
                .resolve(typeName = fragmentName.capitalize(), isOptional = optional), fragmentName.decapitalize())
            .addModifiers(if (context.generateAccessors) Modifier.PRIVATE else Modifier.PUBLIC, Modifier.FINAL)
            .build()
      })
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

    val mapper = FragmentsResponseMapperBuilder(fragmentSpreads, context).build()
    return TypeSpec.classBuilder(formatUniqueTypeName(FRAGMENTS_TYPE_NAME, context.reservedTypeNames))
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addFragmentFields()
        .addFragmentAccessorMethods()
        .addType(mapper)
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
      val fieldSpec = irField.fieldSpec(context).let {
        FieldSpec.builder(
            it.type
                .overrideTypeName(nameOverrideMap)
                .unwrapOptionalType()
                .withoutAnnotations(),
            it.name
        ).build()
      }
      ResponseFieldSpec(
          irField = irField,
          fieldSpec = fieldSpec,
          responseFieldType = if (fieldSpec.type.isEnum(context))
            ResponseField.Type.ENUM
          else
            responseFieldType(irField, fieldSpec),
          context = context
      )
    }
    val inlineFragments = inlineFragments.map { inlineFragment ->
      val fieldSpec = inlineFragment.fieldSpec(context).let {
        FieldSpec.builder(
            it.type
                .overrideTypeName(nameOverrideMap)
                .unwrapOptionalType()
                .withoutAnnotations(),
            it.name
        ).build()
      }
      ResponseFieldSpec(
          irField = Field.TYPE_NAME_FIELD,
          fieldSpec = fieldSpec,
          responseFieldType = ResponseField.Type.INLINE_FRAGMENT,
          typeConditions = if (inlineFragment.possibleTypes != null && !inlineFragment.possibleTypes.isEmpty())
            inlineFragment.possibleTypes
          else
            listOf(inlineFragment.typeCondition),
          context = context
      )
    }
    val fragments = if (fragmentSpreads.isNotEmpty())
      listOf(ResponseFieldSpec(
          irField = Field.TYPE_NAME_FIELD,
          fieldSpec = FRAGMENTS_FIELD,
          responseFieldType = ResponseField.Type.FRAGMENT,
          typeConditions = context.ir.fragments
              .filter { it.fragmentName in fragmentSpreads }
              .flatMap { it.possibleTypes },
          context = context
      ))
    else
      emptyList()

    return responseFields + inlineFragments + fragments
  }

  private fun fieldArray(responseFieldSpecs: List<ResponseFieldSpec>): FieldSpec {
    return FieldSpec
        .builder(Array<ResponseField>::class.java, RESPONSE_FIELDS_VAR)
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
          .map { it.fieldSpec.type }
          .map { it.let { if (it.isList()) it.listParamType() else it } }
          .filter { !it.isScalar(context) }
          .map { it.unwrapOptionalType().withoutAnnotations() }
          .map { it as ClassName }
          .map {
            val mapperClassName = ClassName.get(it.packageName(), it.simpleName(), Util.MAPPER_TYPE_NAME)
            FieldSpec.builder(mapperClassName, it.mapperFieldName(), Modifier.FINAL)
                .initializer(CodeBlock.of("new \$L()", mapperClassName))
                .build()
          }
    }

    val typeClassName = ClassName.get("", uniqueTypeName)
    val code = CodeBlock.builder()
        .add(responseFieldSpecs
            .mapIndexed { i, field -> field.readValueCode(RESPONSE_FIELDS_VAR, i) }
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
        .addParameter(READER_PARAM)
        .addException(IOException::class.java)
        .returns(typeClassName)
        .addCode(code)
        .build()

    return TypeSpec.classBuilder(Util.MAPPER_TYPE_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
        .addSuperinterface(ParameterizedTypeName.get(RESPONSE_FIELD_MAPPER_TYPE, typeClassName))
        .addFields(mapperFields())
        .addMethod(methodSpec)
        .build()
  }

  companion object {
    const val RESPONSE_FIELDS_VAR: String = "\$responseFields"
    const val FRAGMENTS_TYPE_NAME: String = "Fragments"
    val FRAGMENTS_TYPE: TypeName = ClassName.get("", FRAGMENTS_TYPE_NAME).annotated(Annotations.NONNULL)
    private val FRAGMENTS_FIELD = FieldSpec.builder(ClassName.get("", FRAGMENTS_TYPE_NAME),
        SchemaTypeSpecBuilder.FRAGMENTS_TYPE_NAME.decapitalize()).build()
    private val RESPONSE_FIELD_MAPPER_TYPE = ClassName.get(ResponseFieldMapper::class.java)
    private val READER_VAR = "reader"
    private val READER_PARAM = ParameterSpec.builder(ResponseReader::class.java, READER_VAR).build()
  }
}
