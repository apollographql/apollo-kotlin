package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.compiler.*
import com.squareup.javapoet.*
import javax.lang.model.element.Modifier

data class Field(
    val responseName: String,
    val fieldName: String,
    val type: String,
    val args: List<Argument>? = null,
    val isConditional: Boolean = false,
    val fields: List<Field>? = null,
    val fragmentSpreads: List<String>? = null,
    val inlineFragments: List<InlineFragment>? = null,
    val description: String? = null,
    val isDeprecated: Boolean? = false,
    val deprecationReason: String? = null,
    val conditions: List<Condition>? = null
) : CodeGenerator {

  override fun toTypeSpec(context: CodeGenerationContext, abstract: Boolean): TypeSpec {
    val fields = if (isNonScalar()) fields!! else emptyList()
    return SchemaTypeSpecBuilder(
        typeName = formatClassName(),
        schemaType = type,
        fields = fields,
        fragmentSpreads = fragmentSpreads ?: emptyList(),
        inlineFragments = inlineFragments ?: emptyList(),
        context = context,
        abstract = abstract
    )
        .build(Modifier.PUBLIC, Modifier.STATIC)
        .let {
          if (context.generateModelBuilder) {
            it.withBuilder()
          } else {
            it
          }
        }
  }

  fun accessorMethodSpec(context: CodeGenerationContext): MethodSpec {
    val respName = responseName.escapeJavaReservedWord()
    val returnTypeName = toTypeName(methodResponseType(), context)
    val name = if (context.useJavaBeansSemanticNaming) {
      val isBooleanField = returnTypeName == TypeName.BOOLEAN || returnTypeName == TypeName.BOOLEAN.box()
      respName.toJavaBeansSemanticNaming(isBooleanField = isBooleanField)
    } else {
      respName
    }
    return MethodSpec.methodBuilder(name)
        .addModifiers(Modifier.PUBLIC)
        .returns(returnTypeName)
        .addStatement("return this.\$L", responseName.escapeJavaReservedWord())
        .let { if (description != null) it.addJavadoc("\$L\n", description) else it }
        .let {
          if (isDeprecated == true && !deprecationReason.isNullOrBlank()) {
            it.addJavadoc("@deprecated \$L\n", deprecationReason)
          } else {
            it
          }
        }
        .build()
  }

  fun fieldSpec(context: CodeGenerationContext): FieldSpec {
    return FieldSpec.builder(toTypeName(methodResponseType(), context), responseName.escapeJavaReservedWord())
        .addModifiers(Modifier.FINAL)
        .build()
  }

  fun argumentCodeBlock(): CodeBlock {
    if (args == null || args.isEmpty()) return CodeBlock.of("null")

    val mapBuilderClass = ClassNames.parameterizedUnmodifiableMapBuilderOf(String::class.java, Any::class.java)
    return args
        .map { (name, value, type) ->
          when (value) {
            is Number -> {
              val scalarType = ScalarType.forName(type.removeSuffix("!"))
              when (scalarType) {
                is ScalarType.INT -> CodeBlock.of(".put(\$S, \$L)\n", name, value.toInt())
                is ScalarType.FLOAT -> CodeBlock.of(".put(\$S, \$Lf)\n", name, value.toDouble())
                else -> CodeBlock.of(".put(\$S, \$L)\n", name, value)
              }
            }
            is Boolean -> CodeBlock.of(".put(\$S, \$L)\n", name, value)
            is Map<*, *> -> CodeBlock.of(".put(\$S, \$L)\n", name, jsonMapToCodeBlock(value as Map<String, Any?>))
            else -> CodeBlock.of(".put(\$S, \$S)\n", name, value)
          }
        }
        .fold(CodeBlock.builder().add("new \$T(\$L)\n", mapBuilderClass, args.size), CodeBlock.Builder::add)
        .add(".build()")
        .build()
  }

  fun formatClassName() = responseName.capitalize().let { if (isList()) it.singularize() else it }

  fun isOptional(): Boolean = isConditional || !methodResponseType().endsWith("!")
      || (inlineFragments?.isNotEmpty() ?: false)

  fun isNonScalar() = hasFragments() || (fields?.any() ?: false)

  private fun hasFragments() = (fragmentSpreads?.any() ?: false) || (inlineFragments?.any() ?: false)

  private fun isList(): Boolean = type.removeSuffix("!").let { it.startsWith('[') && it.endsWith(']') }

  private fun jsonMapToCodeBlock(map: Map<String, Any?>): CodeBlock {
    val mapBuilderClass = ClassNames.parameterizedUnmodifiableMapBuilderOf(String::class.java, Any::class.java)
    return map
        .map { (key, value) ->
          if (value is Map<*, *>) {
            CodeBlock.of(".put(\$S, \$L)\n", key, jsonMapToCodeBlock(value as Map<String, Any?>))
          } else {
            CodeBlock.of(".put(\$S, \$S)\n", key, value)
          }
        }
        .fold(CodeBlock.builder().add("new \$T(\$L)\n", mapBuilderClass, map.size).indent(), CodeBlock.Builder::add)
        .add(".build()")
        .unindent()
        .build()
  }

  private fun toTypeName(responseType: String, context: CodeGenerationContext): TypeName {
    val packageName = if (isNonScalar()) "" else context.typesPackage
    return JavaTypeResolver(context, packageName, isDeprecated ?: false).resolve(responseType, isOptional())
  }

  private fun methodResponseType(): String {
    if (isNonScalar() || hasFragments()) {
      // For non scalar fields, we use the responseName as the method return type.
      // However, we need to also encode any extra information from the `type` field
      // eg, [lists], nonNulls!, [[nestedLists]], [nonNullLists]!, etc
      val normalizedName = formatClassName()
      if (type.startsWith("[")) {
        // array type
        return if (type.endsWith("!")) "[$normalizedName]!" else "[$normalizedName]"
      } else if (type.endsWith("!")) {
        // non-null type
        return "$normalizedName!"
      } else {
        // nullable type
        return normalizedName
      }
    } else {
      return type
    }
  }

  companion object {
    val TYPE_NAME_FIELD = Field(responseName = "__typename", fieldName = "__typename", type = "String!")
  }
}
