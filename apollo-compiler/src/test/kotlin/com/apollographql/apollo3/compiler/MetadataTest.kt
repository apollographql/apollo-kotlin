package com.apollographql.apollo3.compiler

import com.apollographql.apollo3.compiler.frontend.SourceAwareException
import com.google.common.truth.Truth
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File

class MetadataTest {
  private val buildDir = File("build/metadata-test/")
  private val rootGraphqlDir = File(buildDir, "root/graphql")
  private val rootSchemaFile = File(buildDir, "root/graphql/schema.sdl")
  private val rootSourcesDir = File(buildDir, "root/sources")
  private val rootMetadataFile = File(buildDir, "root/metadata/metadata.json")
  private val leafGraphqlDir = File(buildDir, "leaf/graphql")
  private val leafSourcesDir = File(buildDir, "leaf/sources")
  private val leafMetadataFile = File(buildDir, "leaf/metadata/metadata.json")

  @Before
  fun before() {
    buildDir.deleteRecursively()
    buildDir.mkdirs()
  }

  private fun alwaysGenerateTypesMatchingTest(alwaysGenerateTypesMatching: Set<String>) {
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
        metadataOutputFile = rootMetadataFile
    )
    GraphQLCompiler().write(rootArgs)


    val leafFolders = listOf(leafGraphqlDir)
    leafGraphqlDir.mkdirs()
    File(leafGraphqlDir, "queries.graphql").writeText(SchemaGenerator.generateMutation())
    val leafArgs = GraphQLCompiler.Arguments(
        rootFolders = leafFolders,
        graphqlFiles = leafFolders.graphqlFiles(),
        schemaFile = null,
        metadata = listOf(rootMetadataFile),
        outputDir = leafSourcesDir,
        generateKotlinModels = true,
        metadataOutputFile = leafMetadataFile,
    )
    GraphQLCompiler().write(leafArgs)

    KotlinCompiler.assertCompiles(listOf(rootSourcesDir, leafSourcesDir).kotlinFiles(), true)
  }

  @Test
  fun `empty alwaysGenerateTypesMatching does not generate types in root`() {
    alwaysGenerateTypesMatchingTest(emptySet())

    // Only scalar types are generated in the root
    rootSourcesDir.assertContents("CustomScalars.kt")

    // Leaf contains its referenced types but not the unused ones
    leafSourcesDir.assertContents(
        "Body0.kt",
        "Body0_InputAdapter.kt",
        "Encoding.kt",
        "MessageInput0.kt",
        "MessageInput0_InputAdapter.kt",
        "SendMessageMutation.kt",
        "SendMessageMutation_ResponseAdapter.kt",
        "SendMessageMutation_VariablesAdapter.kt",
        "User0.kt",
        "User0_InputAdapter.kt"
    )
  }

  @Test
  fun `alwaysGenerateTypesMatching can force generating a type not used downstream`() {
    alwaysGenerateTypesMatchingTest(setOf(".*1"))

    // types ending with "1" end up in root
    // but not Encoding
    rootSourcesDir.assertContents(
        "Body1.kt",
        "Body1_InputAdapter.kt",
        "CustomScalars.kt",
        "MessageInput1.kt",
        "MessageInput1_InputAdapter.kt",
        "User1.kt",
        "User1_InputAdapter.kt"
    )

    // Leaf contains Encoding and other used types (.*0) but not .*1
    leafSourcesDir.assertContents(
        "Body0.kt",
        "Body0_InputAdapter.kt",
        "Encoding.kt",
        "MessageInput0.kt",
        "MessageInput0_InputAdapter.kt",
        "SendMessageMutation.kt",
        "SendMessageMutation_ResponseAdapter.kt",
        "SendMessageMutation_VariablesAdapter.kt",
        "User0.kt",
        "User0_InputAdapter.kt"
    )
  }

  private fun fragmentTest(dirName: String) {
    val folder = File("src/test/metadata/$dirName/")
    val rootArgs = GraphQLCompiler.Arguments(
        rootFolders = listOf(folder),
        graphqlFiles = setOf(File(folder, "root.graphql")),
        schemaFile = File("src/test/metadata/schema.sdl"),
        alwaysGenerateTypesMatching = emptySet(),
        outputDir = rootSourcesDir,
        generateKotlinModels = true,
        metadataOutputFile = rootMetadataFile,
    )
    GraphQLCompiler().write(rootArgs)

    val leafArgs = GraphQLCompiler.Arguments(
        rootFolders = listOf(folder),
        graphqlFiles = setOf(File(folder, "leaf.graphql")),
        schemaFile = null,
        metadata = listOf(rootMetadataFile),
        outputDir = leafSourcesDir,
        generateKotlinModels = true,
        metadataOutputFile = leafMetadataFile,
    )
    GraphQLCompiler().write(leafArgs)

    KotlinCompiler.assertCompiles(listOf(rootSourcesDir, leafSourcesDir).kotlinFiles(), true)
  }

  @Test
  fun `fragments can be reused`() {
    fragmentTest("simple")

    // Root generates the fragment
    rootSourcesDir.assertContents(
        "Episode.kt",
        "CustomScalars.kt",
        "CharacterFragment.kt",
    )

    // Leaf contains the query but not the fragment
    leafSourcesDir.assertContents(
        "GetHeroQuery.kt",
        "GetHeroQuery_VariablesAdapter.kt",
        "GetHeroQuery_ResponseAdapter.kt",
    )
  }

  @Test
  fun `fragments validation error`() {
    try {
      fragmentTest("fragment-variable-error")
      fail("Parsing the fragment should have failed")
    } catch (e: SourceAwareException) {
      Truth.assertThat(e.message).contains("Variable `first` is not defined by operation `GetHero`")
    }

    // Nothing is generated
    leafSourcesDir.assertContents()
  }

  @Test
  fun `fragments nameclash error`() {
    try {
      fragmentTest("fragment-nameclash-error")
      fail("Parsing the fragment should have failed")
    } catch (e: SourceAwareException) {
      Truth.assertThat(e.message).contains("Fragment CharacterFragment is already defined")
    }

    // Nothing is generated
    leafSourcesDir.assertContents()
  }

  @Test
  fun `fragments in both parent and child`() {
    fragmentTest("fragment-multiple")

    rootSourcesDir.assertContents(
        "CustomScalars.kt",
        "CharacterFragment.kt",
    )

    leafSourcesDir.assertContents(
        "GetHeroQuery.kt",
        "GetHeroQuery_VariablesAdapter.kt",
        "Episode.kt",
        "HumanFragment.kt",
        "GetHeroQuery_ResponseAdapter.kt",
    )
  }

  companion object {
    private fun List<File>.graphqlFiles(): Set<File> {
      return flatMap {
        it.walk().filter { it.extension == "graphql" }.toList()
      }.toSet()
    }

    private fun List<File>.kotlinFiles(): Set<File> {
      return flatMap {
        it.walk().filter { it.extension == "kt" }.toList()
      }.toSet()
    }

    private fun File.assertContents(vararg files: String) {
      val expected = files.toSet()
      val actual = walk().filter { it.isFile }.map { it.name }.toSet()

      check(expected == actual) {
        "expected:\n${expected.prettify()}\nactual:\n${actual.prettify()}"
      }
    }

    // A method that makes it easy to copy/paste the results when updating the codegen
    private fun Set<String>.prettify() = sortedBy { it }.map { "\"$it\"" }.joinToString(",\n")
  }
}
