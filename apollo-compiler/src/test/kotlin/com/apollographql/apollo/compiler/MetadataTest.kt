package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.frontend.SourceAwareException
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
    rootSourcesDir.assertContents("CustomScalarType.kt")

    // Leaf contains its referenced types but not the unused ones
    leafSourcesDir.assertContents(
        "MessageInput0.kt",
        "SendMessageMutation.kt",
        "Body0.kt",
        "User0.kt",
        "Encoding.kt",
        "SendMessageMutation_ResponseAdapter.kt"
    )
  }

  @Test
  fun `alwaysGenerateTypesMatching can force generating a type not used downstream`() {
    alwaysGenerateTypesMatchingTest(setOf(".*1"))

    // types ending with "1" end up in root
    // but not Encoding
    rootSourcesDir.assertContents(
        "MessageInput1.kt",
        "Body1.kt",
        "User1.kt",
        "CustomScalarType.kt"
    )

    // Leaf contains Encoding and other used types (.*0) but not .*1
    leafSourcesDir.assertContents(
        "MessageInput0.kt",
        "SendMessageMutation.kt",
        "Encoding.kt",
        "Body0.kt",
        "User0.kt",
        "SendMessageMutation_ResponseAdapter.kt",
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
        "CustomScalarType.kt",
        "CharacterFragment.kt",
        "CharacterFragmentImpl.kt",
        "CharacterFragmentImpl_ResponseAdapter.kt",
    )

    // Leaf contains the query but not the fragment
    leafSourcesDir.assertContents(
        "GetHeroQuery.kt",
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
        "CustomScalarType.kt",
        "CharacterFragment.kt",
        "CharacterFragmentImpl.kt",
        "CharacterFragmentImpl_ResponseAdapter.kt"
    )

    leafSourcesDir.assertContents(
        "GetHeroQuery.kt",
        "Episode.kt",
        "HumanFragment.kt",
        "HumanFragmentImpl.kt",
        "HumanFragmentImpl_ResponseAdapter.kt",
        "GetHeroQuery_ResponseAdapter.kt"
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
      Truth.assertThat(walk().filter { it.isFile }.map { it.name }.toSet()).isEqualTo(
          files.toSet()
      )
    }
  }
}
