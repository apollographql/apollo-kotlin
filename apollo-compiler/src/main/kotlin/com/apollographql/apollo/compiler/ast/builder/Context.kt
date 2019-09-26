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
      uniqueTypeRef = TypeRef(name = "$typeName${++index}", packageName = packageName)
    }
    return uniqueTypeRef
  }
}

internal fun Context.registerObjectType(
    type: String,
    schemaType: String,
    fragmentSpreads: List<String>,
    inlineFragments: List<InlineFragment>,
    fields: List<Field>
): TypeRef {
  val inlineFragmentField = inlineFragments.takeIf { it.isNotEmpty() }?.inlineFragmentField(
      type = type,
      schemaType = schemaType,
      context = this
  )

  val (fragmentsField, fragmentsObjectType) = fragmentSpreads
      .map { fragments[it] ?: throw IllegalArgumentException("Unable to find fragment definition: $it") }
      .astObjectFieldType(
          fragmentsPackage = fragmentsPackage,
          isOptional = { typeCondition != schemaType.removeSuffix("!") }
      )

  val normalizedClassName = type.removeSuffix("!").escapeKotlinReservedWord().let { originalClassName ->
    var className = originalClassName
    while (className.first() == '_') {
      className = className.removeRange(0, 1)
    }
    "_".repeat(originalClassName.length - className.length) + className.capitalize()
  }
  return addObjectType(normalizedClassName) { typeRef ->
    ObjectType.Object(
        className = typeRef.name,
        schemaName = type,
        fields = fields.map { it.ast(this) }
            .let { if (fragmentsField != null) it + fragmentsField else it }
            .let { if (inlineFragmentField != null) it + inlineFragmentField else it },
        fragmentsType = fragmentsObjectType
    )
  }
}
