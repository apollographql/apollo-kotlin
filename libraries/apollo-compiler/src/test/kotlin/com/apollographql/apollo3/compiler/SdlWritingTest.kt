package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.ast.toSchema
import com.apollographql.apollo3.ast.toUtf8
import com.apollographql.apollo3.ast.introspection.toGQLDocument
import com.apollographql.apollo3.ast.introspection.toIntrospectionSchema
import com.apollographql.apollo3.ast.introspection.toSchema
import org.junit.Assert
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

class SdlWritingTest {

  /**
   * A dirty diff that compares two documents while skipping the [SourceLocation] fields
   * because we can't carry whitespace information and this changes between two invocations
   */
  private fun diff(a: Any?, b: Any?, path: String = "root"): String? {
    when {
      a == null && b == null -> return null
      a == null && b != null -> return path
      a != null && b == null -> return path
    }

    check(a != null && b != null)

    if (a.javaClass != b.javaClass) {
      return path
    }
    val clazz = a.javaClass
    if (clazz.simpleName == "SourceLocation") {
      return null
    }

    if (a is List<*> && b is List<*>) {
      if (a.size != b.size) {
        return path
      }
      for (i in a.indices) {
        val d = diff(a[i], b[i], "$path.$i")
        if (d != null) {
          return d
        }
      }
    }

    when {
      clazz.isPrimitive -> return if (a != b) path else null
      a is String || a is Int || a is Boolean || a is Double ->return if (a != b) path else null
      !clazz.`package`.name.startsWith("com.apollographql.apollo3.compiler.frontend.gql") -> {
        // don't compare classes outside of our control
        // espectially since Int has circular references
        return null

      }
    }

    val fields = clazz.declaredFields
    fields.forEach {
      it.isAccessible = true
      if (it.name == "children") {
        return@forEach
      }

      val d = diff(it.get(a), it.get(b), "$path.${it.name}")
      if (d != null) {
        return d
      }
    }
    return null
  }

  @Test
  fun `writing a SDL schema does not lose information`() {
    /**
     * Things to watch out:
     * - leading/trailing spaces in descriptions
     * - defaultValue coercion
     */
    val schema1 = File("src/test/sdl/schema.sdl").toSchema().toGQLDocument()

    val scratchFile = File("build/sdl-test/schema.sdl")
    scratchFile.parentFile.mkdirs()
    schema1.toUtf8(scratchFile)

    val schema2 = scratchFile.toSchema().toGQLDocument()

    val path = diff(schema1,schema2)
    if (path != null) {
      Assert.fail("Schemas don't match at: $path")
    }
  }

  @Test
  fun `empty string default value introspection`() {
    val src = """
      type Query {
        field(arg: String! = ""): String
      }
    """.trimIndent()

    val jsonSchema = src.buffer().toSchema().toIntrospectionSchema().toJson()
    assertTrue(jsonSchema.contains("\"defaultValue\":\"\\\"\\\"\""))

    val sdlSchema = jsonSchema.toIntrospectionSchema().toGQLDocument().toUtf8()
    assertTrue(sdlSchema.contains("arg: String! = \"\""))
  }
}
