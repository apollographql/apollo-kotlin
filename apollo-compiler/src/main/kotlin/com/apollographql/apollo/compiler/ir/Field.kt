package com.apollographql.apollo.compiler.ir

import com.apollographql.apollo.compiler.singularize
import com.squareup.moshi.JsonClass

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
) {
  fun isOptional(): Boolean = isConditional || !methodResponseType().endsWith("!")

  fun isNonScalar() = hasFragments() || fields.any()

  private fun hasFragments() = fragmentRefs.any() || inlineFragments.any()

  private fun isList(): Boolean = type.removeSuffix("!").let { it.startsWith('[') && it.endsWith(']') }

  fun formatClassName() = responseName.let { if (isList()) it.singularize() else it }.let { originalClassName ->
    var className = originalClassName
    while (className.first() == '_') {
      className = className.removeRange(0, 1)
    }
    "_".repeat(originalClassName.length - className.length) + className.capitalize()
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
