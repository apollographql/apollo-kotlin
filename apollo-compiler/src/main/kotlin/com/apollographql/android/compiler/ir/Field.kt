package com.apollographql.android.compiler.ir

import com.apollographql.android.compiler.ClassNames
import com.apollographql.android.compiler.JavaTypeResolver
import com.apollographql.android.compiler.SchemaTypeSpecBuilder
import com.cesarferreira.pluralize.singularize
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeName
import java.util.Arrays
import javax.lang.model.element.Modifier

data class Field(
    val responseName: String,
    val fieldName: String,
    val type: String,
    val args: List<Map<String, Any>>?,
    val isConditional: Boolean = false,
    val fields: List<Field>?,
    val fragmentSpreads: List<String>?,
    val inlineFragments: List<InlineFragment>?
) : CodeGenerator {
  override fun toTypeSpec(context: CodeGenerationContext): TypeSpec =
      SchemaTypeSpecBuilder(normalizedName(), fields ?: emptyList(), fragmentSpreads ?: emptyList(),
          inlineFragments ?: emptyList(), context).build(Modifier.PUBLIC, Modifier.STATIC)

  fun accessorMethodSpec(typesPackage: String = "", customScalarTypeMap: Map<String, String>): MethodSpec {
    return MethodSpec.methodBuilder(responseName)
        .addModifiers(Modifier.PUBLIC)
        .returns(toTypeName(methodResponseType(), typesPackage, customScalarTypeMap))
        .addStatement("return this.\$L", responseName)
        .build()
  }

  fun fieldSpec(customScalarTypeMap: Map<String, String>, typesPackage: String = ""): FieldSpec =
      FieldSpec.builder(toTypeName(methodResponseType(), typesPackage, customScalarTypeMap), responseName)
          .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
          .build()

  fun argumentCodeBlock(): CodeBlock {
    if (args == null || args.isEmpty()) {
      return CodeBlock.builder().add("null").build()
    }
    return args
        .mapIndexed { i, arg ->
          var jsonMapCodeBlock = jsonMapToCodeBlock(arg)
          if (i != args.size - 1) {
            jsonMapCodeBlock = jsonMapCodeBlock.toBuilder().add(",\n").build()
          }
          jsonMapCodeBlock
        }
        .fold(CodeBlock.builder().add("\$T.asList(", Arrays::class.java), CodeBlock.Builder::add)
        .add(")")
        .build()
  }

  private fun jsonMapToCodeBlock(jsonMap: Map<String, Any?>): CodeBlock {
    return jsonMap.entries.map { entry ->
      val codeBuilder = CodeBlock.builder()
      if (entry.value is Map<*, *>) {
        @Suppress("UNCHECKED_CAST")
        codeBuilder.add(".put(\$S, ", entry.key).add("\$L)\n", jsonMapToCodeBlock(entry.value as Map<String, Any?>))
      } else {
        codeBuilder.add(".put(\$S, \$S)\n", entry.key, entry.value).build()
      }
      codeBuilder.build()
    }.fold(CodeBlock.builder().add("new \$T(\$L)\n",
        ClassNames.parameterizedUnmodifiableMapBuilderOf(String::class.java, Any::class.java),
        jsonMap.size
    ).indent(), CodeBlock.Builder::add)
        .unindent()
        .add(".build()").build()
  }

  private fun toTypeName(responseType: String, typesPackage: String,
      customScalarTypeMap: Map<String, String>): TypeName {
    val packageName = if (isNonScalar()) "" else typesPackage
    return JavaTypeResolver(customScalarTypeMap, packageName).resolve(responseType, isOptional())
  }

  fun normalizedName() = responseName.capitalize().singularize()

  private fun methodResponseType(): String {
    if (isNonScalar() || hasFragments()) {
      // For non scalar fields, we use the responseName as the method return type.
      // However, we need to also encode any extra information from the `type` field
      // eg, [lists], nonNulls!, [[nestedLists]], [nonNullLists]!, etc
      val normalizedName = normalizedName()
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

  fun isNonScalar() = hasFragments() || (fields?.any() ?: false)

  fun hasFragments() = (fragmentSpreads?.any() ?: false) || (inlineFragments?.any() ?: false)

  fun isOptional(): Boolean = isConditional || !methodResponseType().endsWith("!")
}
