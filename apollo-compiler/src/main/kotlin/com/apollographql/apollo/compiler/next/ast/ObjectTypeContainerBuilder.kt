package com.apollographql.apollo.compiler.next.ast

import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.apollographql.apollo.compiler.singularize

internal class ObjectTypeContainerBuilder(private val packageName: String) {

  private val usedTypeNames = HashSet<String>()
  private val container = LinkedHashMap<CodeGenerationAst.TypeRef, CodeGenerationAst.ObjectType>()

  val typeContainer: ObjectTypeContainer
    get() = container

  inline fun registerObjectType(
      typeName: String,
      enclosingType: CodeGenerationAst.TypeRef?,
      singularizeName: Boolean = false,
      builder: (typeRef: CodeGenerationAst.TypeRef) -> CodeGenerationAst.ObjectType
  ): CodeGenerationAst.TypeRef {
    val normalizedTypeName = normalizeTypeName(
        typeName = typeName,
        singularizeName = singularizeName
    )
    usedTypeNames.add(normalizedTypeName)

    val objectTypeRef = CodeGenerationAst.TypeRef(
        name = normalizedTypeName,
        packageName = packageName,
        enclosingType = enclosingType
    )
    container[objectTypeRef] = builder(objectTypeRef)

    return objectTypeRef
  }

  private fun normalizeTypeName(typeName: String, singularizeName: Boolean): String {
    val normalizedClassName = typeName.escapeKotlinReservedWord().let { originalClassName ->
      var className = originalClassName
      while (className.first() == '_') {
        className = className.removeRange(0, 1)
      }
      "_".repeat(originalClassName.length - className.length) + className.capitalize()
    }
    return usedTypeNames.generateUniqueTypeName(
        typeName = normalizedClassName.let { if (singularizeName) it.singularize() else it }
    )
  }

  private fun Set<String>.generateUniqueTypeName(typeName: String): String {
    var index = 0
    var uniqueTypeName = typeName
    while (find { it.toLowerCase() == uniqueTypeName.toLowerCase() } != null) {
      uniqueTypeName = "$typeName${++index}"
    }
    return uniqueTypeName
  }
}
