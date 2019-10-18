package com.apollographql.apollo.compiler.ast.builder

import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.ast.CustomTypes
import com.apollographql.apollo.compiler.ast.EnumType
import com.apollographql.apollo.compiler.ast.ObjectType
import com.apollographql.apollo.compiler.ast.TypeRef
import com.apollographql.apollo.compiler.escapeKotlinReservedWord
import com.apollographql.apollo.compiler.ir.Field
import com.apollographql.apollo.compiler.ir.Fragment
import com.apollographql.apollo.compiler.ir.InlineFragment
import com.apollographql.apollo.compiler.singularize

internal class Context(
    reservedObjectTypeRef: TypeRef?,
    val customTypeMap: CustomTypes,
    val enums: List<EnumType>,
    val typesPackageName: String,
    val fragmentsPackage: String,
    val fragments: Map<String, Fragment>,
    private val objectTypeContainer: MutableMap<TypeRef, ObjectType> = LinkedHashMap()
) : Map<TypeRef, ObjectType> by objectTypeContainer {

  private val reservedObjectTypeRefs = HashSet<TypeRef>().applyIf(reservedObjectTypeRef != null) {
    add(reservedObjectTypeRef!!)
  }

  fun registerObjectType(
      name: String,
      schemaTypeName: String,
      fragmentSpreads: List<String>,
      inlineFragments: List<InlineFragment>,
      fields: List<Field>,
      kind: ObjectType.Kind,
      singularize: Boolean = true
  ): TypeRef {
    val inlineFragmentField = inlineFragments.takeIf { it.isNotEmpty() }?.inlineFragmentField(
        type = name,
        schemaType = schemaTypeName,
        context = this
    )
    val (fragmentsField, fragmentsObjectType) = fragmentSpreads
        .map { fragments[it] ?: throw IllegalArgumentException("Unable to find fragment definition: $it") }
        .astFragmentsObjectFieldType(
            fragmentsPackage = fragmentsPackage,
            isOptional = { typeCondition != schemaTypeName }
        )

    val normalizedClassName = name.escapeKotlinReservedWord().let { originalClassName ->
      var className = originalClassName
      while (className.first() == '_') {
        className = className.removeRange(0, 1)
      }
      "_".repeat(originalClassName.length - className.length) + className.capitalize()
    }

    val uniqueTypeRef = (reservedObjectTypeRefs).generateUniqueTypeRef(
        typeName = normalizedClassName.let { if (singularize) it.singularize() else it }
    )
    reservedObjectTypeRefs.add(uniqueTypeRef)
    objectTypeContainer[uniqueTypeRef] = ObjectType(
        name = uniqueTypeRef.name,
        schemaTypeName = schemaTypeName,
        fields = fields.map { it.ast(this) }
            .let { if (fragmentsField != null) it + fragmentsField else it }
            .let { if (inlineFragmentField != null) it + inlineFragmentField else it },
        fragmentsType = fragmentsObjectType,
        kind = kind
    )
    return uniqueTypeRef
  }

  private fun Set<TypeRef>.generateUniqueTypeRef(typeName: String): TypeRef {
    var index = 0
    var uniqueTypeRef = TypeRef(name = typeName)
    while (find { it.name.toLowerCase() == uniqueTypeRef.name.toLowerCase() } != null) {
      uniqueTypeRef = TypeRef(name = "$typeName${++index}")
    }
    return uniqueTypeRef
  }
}
