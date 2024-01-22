package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.codegen.CodegenLayout
import com.apollographql.apollo3.compiler.codegen.ResolverClassName

internal class LayoutImpl(
    private val codegenSchema: CodegenSchema,
    private val rootPackageName: String,
    private val roots: Set<String>?,
    private val useSemanticNaming: Boolean
) : Layout {
  private val schemaTypeToClassName: Map<String, String> = mutableMapOf<String, String>().apply {
    val usedNames = mutableSetOf<String>()

    /**
     * Make it possible to support several types with different cases. Example:
     *
     * type URL @targetName(newName: "Url1")
     * type Url
     * type url
     *
     * Because we capitalize the first letter, we need to escape the name because else `Url` and `url` clash
     */
    val allTypes = codegenSchema.allTypes()
    // 1. Compute a unique name for types without a targetName
    for (type in allTypes.filter { it.targetName == null }) {
      val uniqueName = CodegenLayout.uniqueName(type.name, usedNames)
      val className = uniqueName.capitalizeFirstLetter()

      usedNames.add(className.lowercase())
      this[type.name] = className
    }

    // 2. Use targetName verbatim for types that define it
    for (type in allTypes.filter { it.targetName != null }) {
      this[type.name] = type.targetName!!
    }
  }

  private fun className(schemaTypeName: String): String = schemaTypeToClassName[schemaTypeName]
      ?: error("unknown schema type: $schemaTypeName")

  override fun className(kind: FileKind, name: String, filePath: String?): ResolverClassName {
    val filePackageName = if (roots != null && filePath != null) {
      filePackageName(roots, filePath)
    } else {
      ""
    }

    val packageName = when (kind) {
      FileKind.Type -> "$rootPackageName.$filePackageName.type"
      FileKind.Fragment -> "$rootPackageName.$filePackageName.fragment"
      FileKind.Schema -> "$rootPackageName.$filePackageName.schema"
      FileKind.Query, FileKind.Mutation, FileKind.Subscription, FileKind.Resolver -> "$rootPackageName.$filePackageName"
    }
    val simpleName = when (kind) {
      FileKind.Type -> className(name)
      FileKind.Query, FileKind.Mutation, FileKind.Subscription -> {
        val str = name.capitalizeFirstLetter()

        if (!useSemanticNaming) {
          str
        } else if (str.endsWith(kind.name)) {
          str
        } else {
          "$str${kind.name}"
        }
      }
      else -> name.capitalizeFirstLetter()
    }

    return ResolverClassName(packageName, listOf(simpleName))
  }
}