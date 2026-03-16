package com.apollographql.apollo.graphql.ast.test

import com.apollographql.apollo.ast.GQLField
import com.apollographql.apollo.ast.GQLInputObjectTypeDefinition
import com.apollographql.apollo.ast.GQLInputValueDefinition
import com.apollographql.apollo.ast.GQLNamedType
import com.apollographql.apollo.ast.internal.FieldAndNode
import com.apollographql.apollo.ast.internal.Node
import com.apollographql.apollo.ast.internal.tarjanScc
import kotlin.test.Test

class TarjanTest {
  fun typeDefinition(name: String) = GQLInputObjectTypeDefinition(
      sourceLocation = null,
      description = "",
      name = name,
      directives = emptyList(),
      inputFields = emptyList()
  )
  val field = GQLInputValueDefinition(
      sourceLocation = null,
      name = "",
      directives = emptyList(),
      description = "",
      type = GQLNamedType(null, ""),
      defaultValue = null,
  )

  internal fun node(name: String) = Node(typeDefinition(name)).apply { isValid = false }

  @Test
  fun test1() {
    val a = node("a")
    val b = node("b")
    val c = node("c")

    a.sucessors.add(FieldAndNode(field, b))
    b.sucessors.add(FieldAndNode(field, c))
    c.sucessors.add(FieldAndNode(field, b))

    val sccs = tarjanScc(listOf(a, b, c))
    println(sccs)
  }

  @Test
  fun test2() {
    val a = node("a")
    val b = node("b")
    val c = node("c")
    val d = node("d")

    a.sucessors.add(FieldAndNode(field, b))
    a.sucessors.add(FieldAndNode(field, d))
    b.sucessors.add(FieldAndNode(field, c))
    c.sucessors.add(FieldAndNode(field, b))
    d.sucessors.add(FieldAndNode(field, a))

    val sccs = tarjanScc(listOf(a, b, c))
    println(sccs)
  }

}
