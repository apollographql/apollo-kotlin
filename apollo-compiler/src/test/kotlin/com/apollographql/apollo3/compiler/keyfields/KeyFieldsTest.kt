package com.apollographql.apollo3.compiler.keyfields

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.ast.GQLFragmentDefinition
import com.apollographql.apollo3.ast.GQLOperationDefinition
import com.apollographql.apollo3.ast.checkKeyFields
import com.apollographql.apollo3.ast.parseAsGQLDocument
import com.apollographql.apollo3.ast.transformation.addRequiredFields
import com.apollographql.apollo3.compiler.introspection.toSchema
import okio.buffer
import okio.source
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

@OptIn(ApolloExperimental::class)
class KeyFieldsTest {
  @Test
  fun test() {
    val schema = File("src/test/kotlin/com/apollographql/apollo3/compiler/keyfields/schema.graphqls").toSchema()

    val definitions = File("src/test/kotlin/com/apollographql/apollo3/compiler/keyfields/operations.graphql")
        .source()
        .buffer()
        .parseAsGQLDocument()
        .valueAssertNoErrors()
        .definitions

    val fragments = definitions.filterIsInstance<GQLFragmentDefinition>().associateBy { it.name }
    val operation = definitions
        .filterIsInstance<GQLOperationDefinition>()
        .first()
        .let {
          addRequiredFields(it, schema, fragments)
        }

    try {
      checkKeyFields(operation, schema, emptyMap())
      fail("an exception was expected")
    } catch (e: Exception) {
      assert(e.message?.contains("are not queried") == true)
    }
  }
}