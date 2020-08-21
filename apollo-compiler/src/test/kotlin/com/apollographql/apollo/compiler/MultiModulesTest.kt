package com.apollographql.apollo.compiler

import com.apollographql.apollo.compiler.parser.error.DocumentParseException
import com.apollographql.apollo.compiler.parser.error.ParseException
import com.google.common.truth.Truth
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File

class MultiModulesTest {
  private val buildDir = File("build/multi-module-test/")
  private val rootGraphqlDir = File(buildDir, "root/graphql")
  private val rootSchemaFile = File(buildDir, "root/graphql/schema.sdl")
  private val rootSourcesDir = File(buildDir, "root/sources")
  private val rootMetadataDir = File(buildDir, "root/metadata")
  private val leafGraphqlDir = File(buildDir, "leaf/graphql")
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


    val leafFolders = listOf(leafGraphqlDir)
    leafGraphqlDir.mkdirs()
    File(leafGraphqlDir, "queries.graphql").writeText(SchemaGenerator.generateMutation())
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

    KotlinCompiler.assertCompiles(listOf(rootSourcesDir, leafSourcesDir).kotlinFiles(), true)
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

    // types ending with "1" end up in root
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

  fun fragmentTest(dirName: String) {
    val folder = File("src/test/multi-modules/$dirName/")
    val rootArgs = GraphQLCompiler.Arguments(
        rootFolders = listOf(folder),
        graphqlFiles = setOf(File(folder, "root.graphql")),
        schemaFile = File("src/test/multi-modules/schema.sdl"),
        alwaysGenerateTypesMatching = null,
        outputDir = rootSourcesDir,
        generateKotlinModels = true,
        metadataOutputDir = rootMetadataDir,
        rootProjectDir = folder
    )
    GraphQLCompiler().write(rootArgs)

    val leafArgs = GraphQLCompiler.Arguments(
        rootFolders = listOf(folder),
        graphqlFiles = setOf(File(folder, "leaf.graphql")),
        schemaFile = null,
        metadata = listOf(rootMetadataDir),
        outputDir = leafSourcesDir,
        generateKotlinModels = true,
        metadataOutputDir = null,
        rootProjectDir = folder
    )
    GraphQLCompiler().write(leafArgs)

    KotlinCompiler.assertCompiles(listOf(rootSourcesDir, leafSourcesDir).kotlinFiles(), true)
  }

  @Test
  fun `fragments can be reused`() {
    fragmentTest("simple")

    // Root generates the fragment
    rootSourcesDir.assertContents(
        "Hero_type.kt",
        "Episode.kt",
        "CustomType.kt",
        "LengthUnit.kt",
        "CharacterFragment.kt"
    )

    // Leaf contains the query but not the fragment
    leafSourcesDir.assertContents(
        "GetHeroQuery.kt"
    )
  }

  @Test
  fun `fragments validation error`() {
    try {
      fragmentTest("fragment-variable-error")
      fail("Parsing the fragment should have failed")
    } catch (e: DocumentParseException) {
      val actualMessage = e.message?.replace(File("src/test/multi-modules/fragment-variable-error/").absolutePath, "") ?: ""
      val expectedMessage = File("src/test/multi-modules/fragment-variable-error/error").readText()

      Truth.assertThat(actualMessage).isEqualTo(expectedMessage)
    }

    val apolloMetadata = ApolloMetadata.readFromDirectory(rootMetadataDir)
    // Make sure the metadata does not contain absolute paths
    apolloMetadata.fragments.forEach {
      Truth.assertThat(it.filePath).isEqualTo("root.graphql")
    }

    // Nothing is generated
    leafSourcesDir.assertContents()
  }

  @Test
  fun `fragments nameclash error`() {
    try {
      fragmentTest("fragment-nameclash-error")
      fail("Parsing the fragment should have failed")
    } catch (e: ParseException) {
      val actualMessage = e.message?.replace(File("src/test/multi-modules/fragment-nameclash-error/").absolutePath, "") ?: ""
      val expectedMessage = File("src/test/multi-modules/fragment-nameclash-error/error").readText()

      Truth.assertThat(actualMessage).isEqualTo(expectedMessage)
    }

    // Nothing is generated
    leafSourcesDir.assertContents()
  }

  @Test
  fun `fragments multiple`() {
    fragmentTest("fragment-multiple")

    rootSourcesDir.assertContents(
        "Hero_type.kt",
        "Episode.kt",
        "CustomType.kt",
        "LengthUnit.kt",
        "CharacterFragment.kt"
    )

    leafSourcesDir.assertContents(
        "GetHeroQuery.kt",
        "HumanFragment.kt"
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