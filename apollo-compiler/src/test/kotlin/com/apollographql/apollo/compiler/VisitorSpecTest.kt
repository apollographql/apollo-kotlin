package com.apollographql.apollo.compiler

import com.squareup.javapoet.ClassName
import org.junit.Assert
import org.junit.Test

class VisitorSpecTest {

  private val schemaType = ClassName.get("", "Hero")
  private val implementations = listOf(
    ClassName.get("", "AsHuman"),
    ClassName.get("", "AsDroid"),
    ClassName.get("", "AsCharacter")
  )

  @Test
  fun createVisitorInterface() {
    val expected = """
      public interface Visitor<T> {
        T visitDefault(@org.jetbrains.annotations.NotNull Hero hero);
      
        T visit(@org.jetbrains.annotations.NotNull AsHuman asHuman);
      
        T visit(@org.jetbrains.annotations.NotNull AsDroid asDroid);
      
        T visit(@org.jetbrains.annotations.NotNull AsCharacter asCharacter);
      }
      
    """.trimIndent()
    val actual = VisitorInterfaceSpec(schemaType, implementations).createVisitorInterface().toString()

    Assert.assertEquals(expected, actual)
  }

  @Test
  fun createVisitorMethod() {
    val expected = """
      public default <T> T visit(Visitor<T> visitor) {
        if (this instanceof AsHuman) {
          return visitor.visit((AsHuman) this);
        } else if (this instanceof AsDroid) {
          return visitor.visit((AsDroid) this);
        } else if (this instanceof AsCharacter) {
          return visitor.visit((AsCharacter) this);
        }
        return visitor.visitDefault(this);
      }

    """.trimIndent()
    val actual = VisitorMethodSpec(implementations).createVisitorMethod().toString()

    Assert.assertEquals(expected, actual)
  }

}