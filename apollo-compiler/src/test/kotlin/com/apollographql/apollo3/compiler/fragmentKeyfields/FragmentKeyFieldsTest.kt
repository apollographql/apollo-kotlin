package com.apollographql.apollo3.compiler.fragmentKeyfields

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
class FragmentKeyFieldsTest {
  @Test
  fun test() {
    val schema = File("src/test/kotlin/com/apollographql/apollo3/compiler/fragmentKeyfields/schema.graphqls").toSchema()
    val definitions = File("src/test/kotlin/com/apollographql/apollo3/compiler/fragmentKeyfields/operations.graphql")
        .source()
        .buffer()
        .parseAsGQLDocument()
        .valueAssertNoErrors()
        .definitions
    val operation = definitions.filterIsInstance<GQLOperationDefinition>()
        .first()
        .let {
          addRequiredFields(it, schema)
        }

    val fragments = definitions.filterIsInstance<GQLFragmentDefinition>().map {
      addRequiredFields(it, schema)
    }

    try {
      checkKeyFields(operation, schema, fragments.associateBy { it.name })
    } catch (e: Exception) {
      e.message
      fail("no exception was expected")
    }

    try {
      checkKeyFields(fragments.first { it.name == "AuthorFragment" }, schema, fragments.associateBy { it.name })
      fail("an exception was expected")
    } catch (e: Exception) {
      e.message
      assert(e.message?.contains("are not queried") == true)
    }
  }
}
