package com.apollographql.apollo.compiler

import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Test
import java.io.File

class MultiModulesTest {
  private val buildDir = File("build/multi-module/")
  private val rootSchemaFile = File(buildDir, "root/schema.sdl")
  private val rootSourcesDir = File(buildDir, "root/sources")
  private val rootMetadataDir = File(buildDir, "root/metadata")
  private val leafSourcesDir = File(buildDir, "leaf/sources")
  private val leafMetadataDir = File(buildDir, "leaf/metadata")

  @Before
  fun before() {
    buildDir.deleteRecursively()
    buildDir.mkdirs()
  }

  private fun alwaysGenerateTypesMatchingTest(alwaysGenerateTypesMatching: Set<String>?) {
    val schema = SchemaGenerator.generateSDLSchemaWithInputTypes(6)

    rootSchemaFile.parentFile.mkdirs()
    rootSchemaFile.writeText(schema)

    val rootArgs = GraphQLCompiler.Arguments(
        rootFolders = emptyList(),
        graphqlFiles = emptySet(),
        schemaFile = rootSchemaFile,
        alwaysGenerateTypesMatching = alwaysGenerateTypesMatching,
        outputDir = rootSourcesDir,
        generateKotlinModels = true,
        metadataOutputDir = rootMetadataDir
    )
    GraphQLCompiler().write(rootArgs)


    val leafFolders = listOf(File("src/test/multi-modules/simple/leaf"))
    val leafArgs = GraphQLCompiler.Arguments(
        rootFolders = leafFolders,
        graphqlFiles = leafFolders.graphqlFiles(),
        schemaFile = null,
        metadata = listOf(rootMetadataDir),
        outputDir = leafSourcesDir,
        generateKotlinModels = true,
        metadataOutputDir = null
    )
    GraphQLCompiler().write(leafArgs)
  }

  @Test
  fun `default alwaysGenerateTypesMatching generate all types in root`() {
    alwaysGenerateTypesMatchingTest(null)

    // All types are generated
    rootSourcesDir.assertContents(
            "MessageInput0.kt",
            "Body0.kt",
            "User0.kt",
            "MessageInput1.kt",
            "Body1.kt",
            "User1.kt",
            "CustomType.kt",
            "Encoding.kt"
        )

    // Only the mutation is generated in the leaf
    leafSourcesDir.assertContents("SendMessageMutation.kt")
  }

  @Test
  fun `empty alwaysGenerateTypesMatching does not generate types in root`() {
    alwaysGenerateTypesMatchingTest(emptySet())

    // Only scalar types are generated in the root
    rootSourcesDir.assertContents("CustomType.kt")

    // Leaf contains its referenced types but not the unused ones
    leafSourcesDir.assertContents(
        "MessageInput0.kt",
        "SendMessageMutation.kt",
        "Body0.kt",
        "User0.kt",
        "Encoding.kt"
    )
  }

  @Test
  fun `alwaysGenerateTypesMatching can force generating a type not used downstream`() {
    alwaysGenerateTypesMatchingTest(setOf(".*1"))

    // Only scalar types are generated in the root
    rootSourcesDir.assertContents(
        "MessageInput1.kt",
        "Body1.kt",
        "User1.kt",
        "CustomType.kt",
        "Encoding.kt"
    )

    // Leaf contains its referenced types but not the unused ones
    leafSourcesDir.assertContents(
        "MessageInput0.kt",
        "SendMessageMutation.kt",
        "Body0.kt",
        "User0.kt"
    )
  }

  companion object {
    private fun List<File>.graphqlFiles(): Set<File> {
      return flatMap {
        it.walk().filter { it.extension == "graphql" }.toList()
      }.toSet()
    }

    private fun File.assertContents(vararg files: String) {
      Truth.assertThat(walk().filter { it.isFile }.map { it.name }.toSet()).isEqualTo(
          files.toSet()
      )
    }
  }
}