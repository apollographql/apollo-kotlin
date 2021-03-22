package com.apollographql.apollo3.compiler.unified

class FieldNode(
    val irField: IrField,
    val interfacesFieldSets: List<IrFieldSet>,
    val implementationFieldSets: List<IrFieldSet>
)
fun IrField.implementations() {

}
