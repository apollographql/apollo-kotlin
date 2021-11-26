package com.apollographql.apollo3.compiler.keyfields

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.transformation.addRequiredFields
import com.apollographql.apollo3.ast.checkKeyFields
import com.apollographql.apollo3.ast.toGQLDocument
import com.apollographql.apollo3.ast.toSchema
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

@OptIn(ApolloExperimental::class)
class KeyFieldsTest {
  @Test
  fun test() {
    val schema = File("src/test/kotlin/com/apollographql/apollo3/compiler/keyfields/schema.graphqls").toSchema()

    val operation = File("src/test/kotlin/com/apollographql/apollo3/compiler/keyfields/operations.graphql")
        .toGQLDocument()
        .definitions
        .filterIsInstance<GQLOperationDefinition>()
        .first()
        .let {
          addRequiredFields(it, schema)
        }

    try {
      checkKeyFields(operation, schema, emptyMap())
      fail("an exception was expected")
    } catch (e: Exception) {
      assert(e.message?.contains("are not queried") == true)
    }
  }
}