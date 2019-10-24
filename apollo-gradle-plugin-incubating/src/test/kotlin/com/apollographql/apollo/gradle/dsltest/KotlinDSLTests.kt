package com.apollographql.apollo.gradle.dsltest

import com.apollographql.apollo.gradle.util.TestUtils
import com.apollographql.apollo.gradle.util.generatedChild
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class KotlinDSLTests {
  @Test
  fun `parameters do not throw`() {
    val apolloConfiguration = """
      configure<ApolloExtension> {
        nullableValueType("annotated")
        useJavaBeansSemanticNaming(false)
        generateModelBuilder(false)
        useSemanticNaming(false)
        useJavaBeansSemanticNaming(false)
        suppressRawTypesWarning(false)
        generateVisitorForPolymorphicDatatypes(false)
        //schemaFilePath("")
        //outputPackageName("")
        customTypeMapping(mapOf("DateTime" to "java.util.Date"))
        generateKotlinModels(false)
        generateTransformedQueries(false)
        
        service("starwars") {
          sourceFolder("com/example")
          schemaPath("com/example/schema.json")
          rootPackageName("com.starwars")
          exclude(listOf("*.gql"))
        }
      }
    """.trimIndent()

    TestUtils.withProject(
        usesKotlinDsl = true,
        plugins = listOf(TestUtils.javaPlugin, TestUtils.apolloPlugin),
        apolloConfiguration = apolloConfiguration
    ) { dir ->
      val result = TestUtils.executeTask("generateApolloSources", dir)
      assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)
      Assert.assertTrue(dir.generatedChild("main/starwars/com/starwars/DroidDetails.java").isFile)
    }
  }

  @Test
  fun `deprecated syntax is working`() {
    val apolloConfiguration = """
      configure<ApolloExtension> {
        setNullableValueType("annotated")
        setUseJavaBeansSemanticNaming(false)
        setGenerateModelBuilder(false)
        setUseSemanticNaming(false)
        setUseJavaBeansSemanticNaming(false)
        setSuppressRawTypesWarning(false)
        setGenerateVisitorForPolymorphicDatatypes(false)
        //setSchemaFilePath("")
        //setOutputPackageName("")
        setCustomTypeMapping(mapOf("DateTime" to "java.util.Date"))
        setGenerateKotlinModels(false)
        setGenerateTransformedQueries(false)
        
        service("starwars") {
          sourceFolder("com/example")
          schemaPath("com/example/schema.json")
          rootPackageName("com.starwars")
          exclude(listOf("*.gql"))
        }
      }
    """.trimIndent()

    TestUtils.withProject(
        usesKotlinDsl = true,
        plugins = listOf(TestUtils.javaPlugin, TestUtils.apolloPlugin),
        apolloConfiguration = apolloConfiguration
    ) { dir ->
      val result = TestUtils.executeTask("generateApolloSources", dir)
      assertEquals(TaskOutcome.SUCCESS, result.task(":generateApolloSources")!!.outcome)
      Assert.assertTrue(dir.generatedChild("main/starwars/com/starwars/DroidDetails.java").isFile)
    }
  }


}