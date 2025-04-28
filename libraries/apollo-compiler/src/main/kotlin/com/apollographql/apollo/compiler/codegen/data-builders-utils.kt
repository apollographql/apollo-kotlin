package com.apollographql.apollo.compiler.codegen


internal fun dataBuilderName(name: String, isOther: Boolean,layout: SchemaLayout): String {
  return buildString {
    append(dataName(name, isOther, layout))
    append("Builder")
  }
}
internal fun dataName(name: String, isOther: Boolean, layout: SchemaLayout): String {
  return buildString {
    if (isOther) {
      append("Other")
    }
    append(layout.schemaTypeName(name))
  }
}
internal fun dataMapName(name: String, isOther: Boolean, layout: SchemaLayout): String {
  return buildString {
    append(dataName(name, isOther, layout))
    append("Map")
  }
}