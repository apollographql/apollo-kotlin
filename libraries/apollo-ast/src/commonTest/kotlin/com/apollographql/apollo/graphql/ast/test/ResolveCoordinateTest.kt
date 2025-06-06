package com.apollographql.apollo.graphql.ast.test

import com.apollographql.apollo.ast.ResolvedDirective
import com.apollographql.apollo.ast.ResolvedDirectiveArgument
import com.apollographql.apollo.ast.ResolvedEnumValue
import com.apollographql.apollo.ast.ResolvedField
import com.apollographql.apollo.ast.ResolvedFieldArgument
import com.apollographql.apollo.ast.ResolvedInputField
import com.apollographql.apollo.ast.ResolvedType
import com.apollographql.apollo.ast.parseAsGQLDocument
import com.apollographql.apollo.ast.resolveSchemaCoordinate
import com.apollographql.apollo.ast.toSchema
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ResolveCoordinateTest {
  val schema = """
        type Query {
          searchBusiness(criteria: SearchCriteria!): [Business]
        }
        input SearchCriteria {
          name: String
          filter: SearchFilter
        }
        enum SearchFilter {
          OPEN_NOW
          DELIVERS_TAKEOUT
          VEGETARIAN_MENU
        }
        type Business {
          id: ID
          name: String
          email: String @private(scope: "loggedIn")
        }
        directive @private(scope: String!) on FIELD_DEFINITION
  """.trimIndent().parseAsGQLDocument().getOrThrow().toSchema()

  @Test
  fun typeBusiness() {
    resolveSchemaCoordinate(schema, "Business").apply {
      assertIs<ResolvedType>(this)
      assertEquals("Business", typeDefinition.name)
    }
  }

  @Test
  fun typeString() {
    resolveSchemaCoordinate(schema, "String").apply {
      assertIs<ResolvedType>(this)
      assertEquals("String", typeDefinition.name)
    }
  }

  @Test
  fun typeNotFound() {
    resolveSchemaCoordinate(schema, "notFound").apply {
      assertNull(this)
    }
  }

  @Test
  fun fieldBusinessName() {
    resolveSchemaCoordinate(schema, "Business.name").apply {
      assertIs<ResolvedField>(this)
      assertEquals("name", fieldDefinition.name)
    }
  }

  @Test
  fun fieldBusiness__typename() {
    resolveSchemaCoordinate(schema, "Business.__typename").apply {
      assertIs<ResolvedField>(this)
      assertEquals("__typename", fieldDefinition.name)
    }
  }

  @Test
  fun fieldBusinessNotFound() {
    resolveSchemaCoordinate(schema, "Business.notFound").apply {
      assertNull(this)
    }
  }

  @Test
  fun fieldNotFoundName() {
    expectException {
      resolveSchemaCoordinate(schema, "NotFound.name")
    }.apply {
      assertIs<IllegalArgumentException>(this)
      assertEquals("Unknow type 'NotFound'", message)
    }
  }

  @Test
  fun fieldStringName() {
    expectException {
      resolveSchemaCoordinate(schema, "String.name")
    }.apply {
      assertIs<IllegalArgumentException>(this)
      assertEquals("Expected 'String' to be an object, input object, interface or enum type", message)
    }
  }

  @Test
  fun inputFieldSearchCriteriaFilter() {
    resolveSchemaCoordinate(schema, "SearchCriteria.filter").apply {
      assertIs<ResolvedInputField>(this)
      assertEquals("filter", inputField.name)
    }
  }

  @Test
  fun enumValueSearchFilterOPEN_NOW() {
    resolveSchemaCoordinate(schema, "SearchFilter.OPEN_NOW").apply {
      assertIs<ResolvedEnumValue>(this)
      assertEquals("OPEN_NOW", enumValue.name)
    }
  }

  @Test
  fun fieldArgumentQuerySearchBusinessCriteria() {
    resolveSchemaCoordinate(schema, "Query.searchBusiness(criteria:)").apply {
      assertIs<ResolvedFieldArgument>(this)
      assertEquals("criteria", argument.name)
    }
  }

  @Test
  fun fieldArgumentQuerySearchBusinessNotFound() {
    resolveSchemaCoordinate(schema, "Query.searchBusiness(notFound:)").apply {
      assertNull(this)
    }
  }

  @Test
  fun fieldArgument_notFound_arg() {
    expectException {
      resolveSchemaCoordinate(schema, "NotFound.field(arg:)")
    }.apply {
      assertIs<IllegalArgumentException>(this)
      assertEquals("Unknow type 'NotFound'", message)
    }
  }

  @Test
  fun fieldArgument_Business_notFound_Arg() {
    expectException {
      resolveSchemaCoordinate(schema, "Business.notFound(arg:)")
    }.apply {
      assertIs<IllegalArgumentException>(this)
      assertEquals("Unknow field 'notFound' in type 'Business'", message)
    }
  }

  @Test
  fun fieldArgument_SearchCriteria_notFound_Arg() {
    expectException {
      resolveSchemaCoordinate(schema, "SearchCriteria.field(arg:)")
    }.apply {
      assertIs<IllegalArgumentException>(this)
      assertEquals("Expected 'SearchCriteria' to be an object or interface type", message)
    }
  }

  @Test
  fun directive_private() {
    resolveSchemaCoordinate(schema, "@private").apply {
      assertIs<ResolvedDirective>(this)
      assertEquals("private", directiveDefinition.name)
    }
  }

  @Test
  fun directive_deprecated() {
    resolveSchemaCoordinate(schema, "@deprecated").apply {
      assertIs<ResolvedDirective>(this)
      assertEquals("deprecated", directiveDefinition.name)
    }
  }

  @Test
  fun directive_unknown() {
    resolveSchemaCoordinate(schema, "@unknown").apply {
      assertNull(this)
    }
  }

  @Test
  fun directiveArgument_private_scope() {
    resolveSchemaCoordinate(schema, "@private(scope:)").apply {
      assertIs<ResolvedDirectiveArgument>(this)
      assertEquals("scope", argument.name)
    }
  }

  @Test
  fun directiveArgument_private_scope_private_unknown() {
    resolveSchemaCoordinate(schema, "@private(unknown:)").apply {
      assertNull(this)
    }
  }

  @Test
  fun directiveArgument_private_scope_unknown_arg() {
    expectException {
      resolveSchemaCoordinate(schema, "@unknown(arg:)")
    }.apply {
      assertIs<IllegalArgumentException>(this)
      assertEquals("Unknow directive '@unknown'", message)
    }
  }
}

private fun expectException(block: () -> Unit): Exception {
  try {
    block()
  } catch (e: Exception) {
    return e
  }
  error("Expected an error but the block completed successfully")
}