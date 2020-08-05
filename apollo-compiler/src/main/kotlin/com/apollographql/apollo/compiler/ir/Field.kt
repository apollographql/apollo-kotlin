package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.compiler.ClassNames
import com.apollographql.apollo.compiler.JavaTypeResolver
import com.apollographql.apollo.compiler.SchemaTypeSpecBuilder
import com.apollographql.apollo.compiler.escapeJavaReservedWord
import com.apollographql.apollo.compiler.singularize
import com.apollographql.apollo.compiler.toJavaBeansSemanticNaming
import com.apollographql.apollo.compiler.withBuilder
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.moshi.JsonClass
import javax.lang.model.element.Modifier

@JsonClass(generateAdapter = true)
data class Field(
    val responseName: String,
    val fieldName: String,
    val type: String,
    val typeDescription: String,
    val args: List<Argument> = emptyList(),
    val isConditional: Boolean = false,
    val fields: List<Field> = emptyList(),
    val fragmentRefs: List<FragmentRef>,
    val inlineFragments: List<InlineFragment> = emptyList(),
    val description: String = "",
    val isDeprecated: Boolean = false,
    val deprecationReason: String = "",
    val conditions: List<Condition> = emptyList(),
    val sourceLocation: SourceLocation
) : CodeGenerator {

  override fun toTypeSpec(context: CodeGenerationContext, abstract: Boolean): TypeSpec {
    val fields = if (isNonScalar()) fields else emptyList()
    return SchemaTypeSpecBuilder(
        typeName = formatClassName(),
        description = typeDescription,
        schemaType = type,
        fields = fields,
        fragments = fragmentRefs,
        inlineFragments = inlineFragments,
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
        .let { if (description.isNotEmpty()) it.addJavadoc("\$L\n", description) else it }
        .let {
          if (isDeprecated && deprecationReason.isNotEmpty()) {
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
    if (args.isEmpty()) return CodeBlock.of("null")

    val mapBuilderClass = ClassNames.parameterizedUnmodifiableMapBuilderOf(String::class.java, Any::class.java)
    return args
        .map { (name, value, type) ->
          when (value) {
            is Number -> {
              when (ScalarType.forName(type.removeSuffix("!"))) {
                is ScalarType.INT -> CodeBlock.of(".put(\$S, \$L)\n", name, value.toInt())
                is ScalarType.FLOAT -> CodeBlock.of(".put(\$S, \$Lf)\n", name, value.toDouble())
                else -> CodeBlock.of(".put(\$S, \$L)\n", name, value)
              }
            }
            is Boolean -> CodeBlock.of(".put(\$S, \$L)\n", name, value)
            is Map<*, *> -> {
              @Suppress("UNCHECKED_CAST")
              CodeBlock.of(".put(\$S, \$L)\n", name, jsonMapToCodeBlock(value as Map<String, Any?>))
            }
            else -> CodeBlock.of(".put(\$S, \$S)\n", name, value)
          }
        }
        .fold(CodeBlock.builder().add("new \$T(\$L)\n", mapBuilderClass, args.size), CodeBlock.Builder::add)
        .add(".build()")
        .build()
  }

  fun formatClassName() = responseName.let { if (isList()) it.singularize() else it }.let { originalClassName ->
    var className = originalClassName
    while (className.first() == '_') {
      className = className.removeRange(0, 1)
    }
    "_".repeat(originalClassName.length - className.length) + className.capitalize()
  }

  fun isOptional(): Boolean = isConditional || !methodResponseType().endsWith("!")

  fun isNonScalar() = hasFragments() || fields.any()

  private fun hasFragments() = fragmentRefs.any() || inlineFragments.any()

  private fun isList(): Boolean = type.removeSuffix("!").let { it.startsWith('[') && it.endsWith(']') }

  private fun jsonMapToCodeBlock(map: Map<String, Any?>): CodeBlock {
    val mapBuilderClass = ClassNames.parameterizedUnmodifiableMapBuilderOf(String::class.java, Any::class.java)
    return map
        .map { (key, value) ->
          if (value is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
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
    val packageName = if (isNonScalar()) "" else context.ir.typesPackageName
    return JavaTypeResolver(context, packageName, isDeprecated).resolve(responseType, isOptional())
  }

  private fun methodResponseType(): String {
    if (isNonScalar() || hasFragments()) {
      // For non scalar fields, we use the responseName as the method return type.
      // However, we need to also encode any extra information from the `type` field
      // eg, [lists], nonNulls!, [[nestedLists]], [nonNullLists]!, etc
      val normalizedName = formatClassName()
      return when {
        type.startsWith("[") -> {// array type
          type.count { it == '[' }.let {
            "[".repeat(it) + normalizedName + "]".repeat(it)
          }.let {
            if (type.endsWith("!")) "$it!" else it
          }
        }
        type.endsWith("!") -> {// non-null type
          "$normalizedName!"
        }
        else -> {// nullable type
          normalizedName
        }
      }
    } else {
      return type
    }
  }

  companion object {
    val TYPE_NAME_FIELD = Field(
        responseName = "__typename",
        fieldName = "__typename",
        type = "String!",
        typeDescription = "",
        fragmentRefs = emptyList(),
        sourceLocation = SourceLocation.UNKNOWN
    )
  }
}
