package schema

import com.apollographql.apollo.ast.ForeignSchema
import com.apollographql.apollo.ast.GQLIntValue
import com.apollographql.apollo.ast.GQLTypeDefinition
import com.apollographql.apollo.ast.parseAsGQLDocument
import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.ApolloCompilerPluginEnvironment
import com.apollographql.apollo.compiler.ApolloCompilerPluginLogger
import com.apollographql.apollo.compiler.ApolloCompilerPluginProvider
import com.apollographql.apollo.compiler.ApolloCompilerRegistry
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.PropertySpec
import okio.buffer
import okio.source

class TestPlugin(
    logger: ApolloCompilerPluginLogger,
) : ApolloCompilerPlugin {
  init {
    logger.info("TestPlugin")
  }

  override fun beforeCompilationStep(
      environment: ApolloCompilerPluginEnvironment,
      registry: ApolloCompilerRegistry,
  ) {
    registry.registerForeignSchemas(listOf(
        ForeignSchema(
            "cache",
            "v0.1",
            javaClass.classLoader.getResourceAsStream("cache_v0.1.graphqls").source().buffer().use {
              it.parseAsGQLDocument().getOrThrow().definitions
            }
        )
    ))

    registry.registerExtraCodeGenerator { schema ->
      val maxAge = schema.definitions.filterIsInstance<GQLTypeDefinition>()
          .first { it.name == "Menu" }
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

      if (environment.outputDirectory != null) {
        FileSpec.builder("hooks.generated", "cache")
            .addProperty(
                PropertySpec.builder("menuMaxAge", ClassName("kotlin", "String"))
                    .initializer("%S", maxAge)
                    .build()
            )
            .build()
            .writeTo(environment.outputDirectory!!)
      }
    }
  }
}

class TestPluginProvider : ApolloCompilerPluginProvider {
  override fun create(environment: ApolloCompilerPluginEnvironment): ApolloCompilerPlugin {
    return TestPlugin(environment.logger)
  }
}
