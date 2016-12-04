package com.apollostack.compiler

import com.apollostack.compiler.ir.Field
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import java.util.*
import javax.lang.model.element.Modifier

class CustomTypeCollector(val fields: List<Field>) {
  private val typeMap = HashMap<String, Field>()

  init {
    fields.filter { it.fields.any() }.forEach {
      val existingMapping = typeMap[it.type]
      if (existingMapping != null && it.fields != existingMapping.fields) {
        // If we already saw this type and it has a different set of fields, it means
        // we have the same type used multiple times with different fields. In this case,
        // we'll use Field.toTypeName() to disambiguate the name since the type cannot be
        // used more than once. We also need to use the explicit naming for the previously
        // seen type, so we need to also remove it and re-add with the explicit naming.
        typeMap.put(it.toTypeName(), it)
        typeMap.put(existingMapping.toTypeName(), existingMapping)
        typeMap.remove(it.type)
      } else {
        typeMap.put(it.type, it)
      }
    }
  }

  fun collectTypes() =
      typeMap.entries.map {
        // TODO: Also need to recurse into inner types
        TypeSpec.interfaceBuilder(it.key)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addMethods(it.value.fields.map { it.toMethodSpec() })
            .build()
      }

  fun collectMethods(): Iterable<MethodSpec> {
    return fields.map { field ->
      val mapping = typeMap.entries.firstOrNull { it.value == field }
      // If we can't find a mapping for this field, it probably means that we have
      // the same type being used more than once, but both are identical. In this case,
      // we don't need to disambiguate the type name and can use field.type directly
      mapping?.value?.toMethodSpec(mapping.key) ?: field.toMethodSpec(field.type)
    }
  }
}