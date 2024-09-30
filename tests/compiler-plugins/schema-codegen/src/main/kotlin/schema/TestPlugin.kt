package schema

import com.apollographql.apollo.ast.ForeignSchema
import com.apollographql.apollo.ast.GQLDocument
import com.apollographql.apollo.ast.GQLIntValue
import com.apollographql.apollo.ast.GQLObjectTypeDefinition
import com.apollographql.apollo.ast.parseAsGQLDocument
import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.ApolloCompilerPluginEnvironment
import com.apollographql.apollo.compiler.ApolloCompilerPluginLogger
import com.apollographql.apollo.compiler.ApolloCompilerPluginProvider
import com.apollographql.apollo.compiler.CodegenSchema
import com.apollographql.apollo.compiler.SchemaDocumentListener
import com.apollographql.apollo.compiler.codegen.SchemaAndOperationsLayout
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.PropertySpec
import okio.buffer
import okio.source
import java.io.File
import kotlin.math.max

class TestPlugin(
    logger: ApolloCompilerPluginLogger,
) : ApolloCompilerPlugin {
  init {
    logger.info("TestPlugin")
  }

  override fun foreignSchemas(): List<ForeignSchema> {
    return listOf(
        ForeignSchema(
            "cache",
            "v0.1",
            javaClass.classLoader.getResourceAsStream("cache_v0.1.graphqls").source().buffer().use {
              it.parseAsGQLDocument().getOrThrow().definitions
            }
        )
    )
  }

  override fun schemaDocumentListener(): SchemaDocumentListener {
    return object : SchemaDocumentListener {
      override fun onSchemaDocument(schema: GQLDocument, outputDirectory: File) {
        val maxAge = schema.definitions.filterIsInstance<GQLObjectTypeDefinition>()
            .single {
              it.name == "Menu"
            }
            .directives
            .single {
              it.name == "cacheControl"
            }
            .arguments
            .single {
              it.name == "maxAge"
            }
            .value
            .let { it as GQLIntValue }
            .value

        FileSpec.builder("hooks.generated", "cache")
            .addProperty(
                PropertySpec.builder("menuMaxAge", ClassName("kotlin", "String"))
                    .initializer("%S", maxAge)
                    .build()
            )
            .build()
            .writeTo(outputDirectory)
      }
    }
  }
}

class TestPluginProvider : ApolloCompilerPluginProvider {
  override fun create(environment: ApolloCompilerPluginEnvironment): ApolloCompilerPlugin {
    return TestPlugin(environment.logger)
  }
}
