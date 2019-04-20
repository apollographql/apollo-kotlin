package com.apollographql.apollo.compiler.ast.builder

import com.apollographql.apollo.compiler.applyIf
import com.apollographql.apollo.compiler.ast.*
import com.apollographql.apollo.compiler.ir.Fragment
import com.apollographql.apollo.compiler.ir.InlineFragment
import com.apollographql.apollo.compiler.singularize

internal class Context(
    val reservedObjectTypeRef: TypeRef?,
    val customTypeMap: CustomTypes,
    val enums: List<EnumType>,
    val typesPackageName: String,
    val fragmentsPackage: String,
    val fragments: Map<String, Fragment>
) {
  private val reservedObjectTypeRefs = HashSet<TypeRef>().applyIf(reservedObjectTypeRef != null) {
    add(reservedObjectTypeRef!!)
  }
  private val objectTypeContainer: MutableMap<TypeRef, ObjectType> = LinkedHashMap()
  val objectTypes: Map<TypeRef, ObjectType> = objectTypeContainer

  fun addObjectType(typeName: String, packageName: String = "",
      provideObjectType: (TypeRef) -> ObjectType): TypeRef {
    val uniqueTypeRef = (reservedObjectTypeRefs).generateUniqueTypeRef(
        typeName = typeName.let { if (it != "Data") it.singularize() else it },
        packageName = packageName
    )
    reservedObjectTypeRefs.add(uniqueTypeRef)
    objectTypeContainer[uniqueTypeRef] = provideObjectType(uniqueTypeRef)
    return uniqueTypeRef
  }

  private fun Set<TypeRef>.generateUniqueTypeRef(typeName: String, packageName: String): TypeRef {
    var index = 0
    var uniqueTypeRef = TypeRef(name = typeName, packageName = packageName)
    while (find { it.name.toLowerCase() == uniqueTypeRef.name.toLowerCase() } != null) {
      uniqueTypeRef = TypeRef(name = "${uniqueTypeRef.name}${++index}",
          packageName = packageName)
    }
    return uniqueTypeRef
  }
}

internal fun Context.registerInlineFragmentType(inlineFragment: InlineFragment): TypeRef {
  return registerObjectType(
      type = "As${inlineFragment.typeCondition}",
      schemaType = inlineFragment.typeCondition,
      fragmentSpreads = inlineFragment.fragmentSpreads ?: emptyList(),
      inlineFragments = emptyList(),
      fields = inlineFragment.fields
  )
}