package com.apollographql.apollo.compiler

import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ResponseFieldMapper
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller
import com.apollographql.apollo.api.internal.ResponseReader
import com.apollographql.apollo.api.internal.ResponseWriter
import com.apollographql.apollo.compiler.ir.CodeGenerationContext
import com.apollographql.apollo.compiler.ir.Field
import com.apollographql.apollo.compiler.ir.FragmentRef
import com.apollographql.apollo.compiler.ir.InlineFragment
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

class SchemaTypeSpecBuilder(
    typeName: String,
    private val description: String = "",
    private val schemaType: String = "",
    private val fields: List<Field>,
    private val fragments: List<FragmentRef>,
    private val inlineFragments: List<InlineFragment>,
    private val context: CodeGenerationContext,
    private val abstract: Boolean = false
) {
  private val uniqueTypeName = formatUniqueTypeName(typeName, context.reservedTypeNames)

  init {
    context.reservedTypeNames += uniqueTypeName
  }

  fun build(vararg modifiers: Modifier): TypeSpec {
    return if (abstract || inlineFragments.isNotEmpty()) {
      interfaceTypeSpec(modifiers)
    } else {
      classTypeSpec(modifiers)
    }
  }

  private fun interfaceTypeSpec(modifiers: Array<out Modifier>): TypeSpec {
    val nestedTypeSpecs = nestedTypeSpecs()
    val inlineFragmentTypeSpecs = inlineFragmentsTypeSpecs()
    val nameOverrideMap = nestedTypeSpecs.plus(inlineFragmentTypeSpecs).map { it.first to it.second.name }.toMap()
    val methods = fieldAccessorMethodSpecs(nameOverrideMap)
        .map { method ->
          MethodSpec.methodBuilder(method.name)
              .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
              .addJavadoc(method.javadoc)
              .addAnnotations(method.annotations)
              .returns(method.returnType)
              .build()
              .withWildCardReturnType(nestedTypeSpecs.map { it.second.name })
        }
    val marshallerAccessorMethodSpec = MethodSpec.methodBuilder(RESPONSE_MARSHALLER_PARAM_NAME)
        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
        .returns(ResponseFieldMarshaller::class.java)
        .build()
    val surrogateInlineFragmentType: TypeSpec? = if (inlineFragments.isNotEmpty()) {
      SchemaTypeSpecBuilder(
          typeName = formatUniqueTypeName("As${normalizeGraphQlType(schemaType).capitalize()}",
              context.reservedTypeNames),
          description = description,
          schemaType = schemaType,
          fields = fields,
          fragments = fragments,
          inlineFragments = emptyList(),
          context = context
      )
          .build(Modifier.PUBLIC, Modifier.STATIC)
          .let { if (context.generateModelBuilder) it.withBuilder() else it }
    } else {
      null
    }
    return TypeSpec.interfaceBuilder(uniqueTypeName)
        .applyIf(description.isNotBlank()) { addJavadoc("\$L\n", description) }
        .addModifiers(*modifiers)
        .addMethods(methods)
        .addMethod(marshallerAccessorMethodSpec)
        .addTypes(nestedTypeSpecs.map { it.second })
        .apply {
          if (inlineFragments.isNotEmpty()) {
            addType(inlineFragmentsResponseMapperSpec(nameOverrideMap, surrogateInlineFragmentType!!))
            if (context.generateVisitorForPolymorphicDatatypes) {
              addType(inlineFragmentsVisitorInterfaceSpec(nameOverrideMap, surrogateInlineFragmentType))
              addMethod(inlineFragmentsVisitorMethodSpec(nameOverrideMap, surrogateInlineFragmentType))
            }
          }
        }
        .build()
        .let { protocol ->
          protocol.toBuilder()
              .addTypes(inlineFragmentTypeSpecs.map { it.second }.map { it.conformToProtocol(protocol) })
              .apply {
                if (surrogateInlineFragmentType != null) {
                  addType(surrogateInlineFragmentType.conformToProtocol(protocol))
                }
              }
              .build()
        }
  }

  private fun classTypeSpec(modifiers: Array<out Modifier>): TypeSpec {
    val nestedTypeSpecs = nestedTypeSpecs()
    val inlineFragmentTypeSpecs = inlineFragmentsTypeSpecs()
    val nameOverrideMap = nestedTypeSpecs.plus(inlineFragmentTypeSpecs).map { it.first to it.second.name }.toMap()
    val responseFieldSpecs = responseFieldSpecs(nameOverrideMap)
    return TypeSpec.classBuilder(uniqueTypeName)
        .applyIf(description.isNotBlank()) { addJavadoc("\$L\n", description) }
        .addModifiers(*modifiers)
        .addTypes(nestedTypeSpecs.map { it.second })
        .addFields(fieldSpecs(nameOverrideMap))
        .addMethods(fieldAccessorMethodSpecs(nameOverrideMap))
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

  private fun fieldSpecs(nameOverrideMap: Map<String, String>): List<FieldSpec> {
    return fields
        .map { it.fieldSpec(context) }
        .map { it.overrideType(nameOverrideMap) }
  }

  private fun fieldAccessorMethodSpecs(nameOverrideMap: Map<String, String>): List<MethodSpec> {
    return fields
        .map { it.accessorMethodSpec(context) }
        .map { it.overrideReturnType(nameOverrideMap) }
  }

  private fun TypeSpec.Builder.addFragments(): TypeSpec.Builder {
    if (fragments.isNotEmpty()) {
      addType(fragmentsTypeSpec())
      addField(fragmentsFieldSpec())
      addMethod(fragmentsAccessorMethodSpec())
    }
    return this
  }

  private fun nestedTypeSpecs(): List<Pair<String, TypeSpec>> {
    return fields.filter(Field::isNonScalar).map {
      it.formatClassName() to it.toTypeSpec(
          context = context,
          abstract = abstract || inlineFragments.isNotEmpty()
      )
    }
  }

  private fun inlineFragmentsTypeSpecs(): List<Pair<String, TypeSpec>> =
      inlineFragments.map { it.formatClassName() to it.toTypeSpec(context = context, abstract = false) }

  private fun fragmentsAccessorMethodSpec(): MethodSpec {
    val fragmentsName = FRAGMENTS_FIELD.name
    val name = if (context.useJavaBeansSemanticNaming) {
      fragmentsName.toJavaBeansSemanticNaming(isBooleanField = false)
    } else {
      fragmentsName
    }
    return MethodSpec.methodBuilder(name)
        .returns(FRAGMENTS_FIELD.type)
        .addModifiers(Modifier.PUBLIC)
        .addModifiers(emptyList())
        .addStatement("return this.\$L", FRAGMENTS_FIELD.name)
        .build()
  }

  private fun fragmentsFieldSpec(): FieldSpec {
    return FRAGMENTS_FIELD.toBuilder()
        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
        .build()
  }

  /** Returns a generic `Fragments` interface with methods for each of the provided fragments */
  private fun fragmentsTypeSpec(): TypeSpec {

    fun FragmentRef.isOptional(): Boolean {
      return conditions.isNotEmpty() || context.ir.fragments
          .first { it.fragmentName == name }
          .let { it.typeCondition != normalizeGraphQlType(schemaType) }
    }

    fun responseFieldSpecs(): List<ResponseFieldSpec> {
      return fragments
          .mapNotNull { fragmentRef -> context.ir.fragments.find { it.fragmentName == fragmentRef.name }?.let { fragmentRef to it } }
          .map { (fragmentRef, fragment) ->
            val optional = fragmentRef.isOptional()
            val possibleTypes = fragment.takeIf { fragment.typeCondition != normalizeGraphQlType(schemaType) }?.possibleTypes ?: emptyList()
            val fieldSpec = FieldSpec.builder(
                JavaTypeResolver(context = context, packageName = fragment.packageName)
                    .resolve(typeName = fragment.fragmentName.capitalize(), isOptional = optional), fragment.fragmentName.decapitalize())
                .addModifiers(Modifier.FINAL)
                .build()
            val normalizedFieldSpec = FieldSpec.builder(fieldSpec.type.unwrapOptionalType().withoutAnnotations(), fieldSpec.name).build()
            ResponseFieldSpec(
                irField = Field.TYPE_NAME_FIELD.copy(conditions = fragmentRef.conditions),
                fieldSpec = fieldSpec,
                normalizedFieldSpec = normalizedFieldSpec,
                responseFieldType = ResponseField.Type.FRAGMENT,
                context = context,
                typeConditions = possibleTypes
            )
          }
    }

    fun FieldSpec.accessorMethod(): MethodSpec {
      val methodName = if (context.useJavaBeansSemanticNaming) {
        name.toJavaBeansSemanticNaming(isBooleanField = false)
      } else {
        name.decapitalize()
      }
      return MethodSpec.methodBuilder(methodName)
          .returns(type)
          .addModifiers(Modifier.PUBLIC)
          .addStatement("return this.\$L", name)
          .build()
    }

    fun responseMarshallerSpec(fieldSpecs: List<FieldSpec>): MethodSpec {
      val code = fieldSpecs
          .map { fieldSpec ->
            if (fieldSpec.type.isNullable()) {
              CodeBlock.builder()
                  .addStatement("final \$T \$L = \$L", fieldSpec.type.unwrapOptionalType().withoutAnnotations(),
                      "\$${fieldSpec.name}", fieldSpec.type.unwrapOptionalValue(fieldSpec.name))
                  .beginControlFlow("if (\$L != null)", "\$${fieldSpec.name}")
                  .addStatement("\$L.writeFragment(\$L.\$L())", RESPONSE_WRITER_PARAM.name, "\$${fieldSpec.name}",
                      RESPONSE_MARSHALLER_PARAM_NAME)
                  .endControlFlow()
                  .build()
            } else {
              CodeBlock.of("\$L.writeFragment(\$L.\$L());\n", RESPONSE_WRITER_PARAM.name, fieldSpec.name, RESPONSE_MARSHALLER_PARAM_NAME)
            }
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

    val fragmentFields = responseFieldSpecs()
    val fragmentFieldSpecs = fragmentFields.map { it.fieldSpec }
    val mapper = FragmentsResponseMapperBuilder(fragmentFields, context)
        .build()
        .toBuilder()
        .addField(fieldArray(fragmentFields))
        .build()
    return TypeSpec.classBuilder(FRAGMENTS_FIELD.name.capitalize())
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addFields(fragmentFieldSpecs)
        .addMethods(fragmentFieldSpecs.map { it.accessorMethod() })
        .addType(mapper)
        .addMethod(responseMarshallerSpec(fragmentFields.map { it.fieldSpec }))
        .build()
        .withValueInitConstructor(context.nullableValueType)
        .withToStringImplementation()
        .withEqualsImplementation()
        .withHashCodeImplementation()
        .let {
          if (context.generateModelBuilder) {
            it.withBuilder()
          } else {
            it
          }
        }
  }

  private fun formatUniqueTypeName(typeName: String, reservedTypeNames: Collection<String>): String {
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
        return ResponseField.Type.LIST
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
    val fragments = if (fragments.isNotEmpty()) {
      val normalizedFieldSpec = FieldSpec.builder(
          FRAGMENTS_FIELD.type.withoutAnnotations(),
          FRAGMENTS_FIELD.name
      ).build()
      listOf(ResponseFieldSpec(
          irField = Field.TYPE_NAME_FIELD,
          fieldSpec = FRAGMENTS_FIELD,
          normalizedFieldSpec = normalizedFieldSpec,
          responseFieldType = ResponseField.Type.FRAGMENTS,
          context = context
      ))
    } else {
      emptyList()
    }
    return responseFields + fragments
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
          .map {
            var rawType = if (it.isList()) it.listParamType() else it
            while (rawType.isList()) {
              rawType = rawType.listParamType()
            }
            rawType
          }
          .filter { !it.isScalar(context) }
          .map { it.unwrapOptionalType().withoutAnnotations() }
          .map { it as ClassName }
          .map {
            val mapperClassName = ClassName.get(it.packageName(), it.simpleName(),
                Util.RESPONSE_FIELD_MAPPER_TYPE_NAME)
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

  private fun inlineFragmentsVisitorInterfaceSpec(
      nameOverrideMap: Map<String, String>,
      surrogateInlineFragmentType: TypeSpec
  ): TypeSpec {
    val typeClassName = ClassName.get("", uniqueTypeName)
    val implementations = inlineFragments.map { inlineFragment ->
      val fieldSpec = inlineFragment.fieldSpec(context).overrideType(nameOverrideMap)
      val inlineFragmentResponseFieldType = fieldSpec.type.rawType() as ClassName
      ClassName.get("", inlineFragmentResponseFieldType.simpleName())
    } + ClassName.get("", surrogateInlineFragmentType.name)
    return VisitorInterfaceSpec(typeClassName, implementations).createVisitorInterface()
  }

  private fun inlineFragmentsVisitorMethodSpec(
      nameOverrideMap: Map<String, String>,
      surrogateInlineFragmentType: TypeSpec
  ): MethodSpec {
    val implementations = inlineFragments.map { inlineFragment ->
      val fieldSpec = inlineFragment.fieldSpec(context).overrideType(nameOverrideMap)
      val inlineFragmentResponseFieldType = fieldSpec.type.rawType() as ClassName
      ClassName.get("", inlineFragmentResponseFieldType.simpleName())
    } + ClassName.get("", surrogateInlineFragmentType.name)
    return VisitorMethodSpec(implementations).createVisitorMethod()
  }

  private fun inlineFragmentsResponseMapperSpec(
      nameOverrideMap: Map<String, String>,
      surrogateInlineFragmentType: TypeSpec
  ): TypeSpec {
    val inlineFragments = inlineFragments.map { inlineFragment ->
      val fieldSpec = inlineFragment.fieldSpec(context).overrideType(nameOverrideMap)
      val normalizedFieldSpec = FieldSpec.builder(
          fieldSpec.type.unwrapOptionalType().withoutAnnotations(),
          fieldSpec.name
      ).build()
      ResponseFieldSpec(
          irField = Field.TYPE_NAME_FIELD.copy(conditions = inlineFragment.conditions),
          fieldSpec = fieldSpec,
          normalizedFieldSpec = normalizedFieldSpec,
          responseFieldType = ResponseField.Type.FRAGMENT,
          typeConditions = if (inlineFragment.possibleTypes.isNotEmpty())
            inlineFragment.possibleTypes
          else
            listOf(inlineFragment.typeCondition),
          context = context
      )
    }
    val surrogateInlineFragmentFieldMapperType = ClassName.get("", surrogateInlineFragmentType.name,
        Util.RESPONSE_FIELD_MAPPER_TYPE_NAME)
    val surrogateInlineFragmentFieldMapper = FieldSpec.builder(surrogateInlineFragmentFieldMapperType,
        "${surrogateInlineFragmentType.name.decapitalize()}${Util.FIELD_MAPPER_SUFFIX}", Modifier.FINAL)
        .initializer(CodeBlock.of("new \$L()", surrogateInlineFragmentFieldMapperType))
        .build()

    val mapperFields = inlineFragments.map {
      val inlineFragmentResponseFieldType = it.fieldSpec.type.rawType() as ClassName
      val mapperClassName = ClassName.get("", inlineFragmentResponseFieldType.simpleName(),
          Util.RESPONSE_FIELD_MAPPER_TYPE_NAME)
      FieldSpec.builder(mapperClassName, inlineFragmentResponseFieldType.mapperFieldName(), Modifier.FINAL)
          .initializer(CodeBlock.of("new \$L()", mapperClassName))
          .build()
    } + surrogateInlineFragmentFieldMapper

    val code = inlineFragments
        .foldIndexed(CodeBlock.builder()) { index, builder, responseFieldSpec ->
          val readValueCode = responseFieldSpec.readValueCode(
              readerParam = CodeBlock.of("\$L", RESPONSE_READER_PARAM.name),
              fieldParam = CodeBlock.of("\$L[\$L]", RESPONSE_FIELDS_PARAM.name, index)
          )
          builder
              .add(readValueCode)
              .add(CodeBlock.builder()
                  .beginControlFlow("if (\$L != null)", responseFieldSpec.fieldSpec.name)
                  .addStatement("return \$L", responseFieldSpec.fieldSpec.name)
                  .endControlFlow()
                  .build()
              )
        }
        .addStatement("return \$L.map(reader)", surrogateInlineFragmentFieldMapper.name)
        .build()
    val typeClassName = ClassName.get("", uniqueTypeName)
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
        .addField(fieldArray(inlineFragments))
        .addFields(mapperFields)
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
        .addAnnotation(Annotations.SUPPRESS_RAW_VALUE_AND_UNCHECKED_WARNING)
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
