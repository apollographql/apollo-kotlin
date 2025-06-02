package apollocompilerplugin

import com.apollographql.apollo.compiler.After
import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.ApolloCompilerPluginEnvironment
import com.apollographql.apollo.compiler.ApolloCompilerRegistry
import com.apollographql.apollo.compiler.codegen.kotlin.KotlinOutput


val executedTransforms = mutableListOf<String>()

class TestPlugin1 : ApolloCompilerPlugin {
  override fun beforeCompilationStep(
      environment: ApolloCompilerPluginEnvironment,
      registry: ApolloCompilerRegistry,
  ) {
    registry.registerKotlinOutputTransform("test1", After("test2")) { input ->
      executedTransforms.add("test1")
      input
    }
  }
}

class TestPlugin2: ApolloCompilerPlugin {

  override fun beforeCompilationStep(
      environment: ApolloCompilerPluginEnvironment,
      registry: ApolloCompilerRegistry,
  ) {
    registry.registerKotlinOutputTransform("test2") { input ->
      executedTransforms.add("test2")
      input
    }
  }
}

class TestPlugin3 : ApolloCompilerPlugin {

  override fun beforeCompilationStep(
      environment: ApolloCompilerPluginEnvironment,
      registry: ApolloCompilerRegistry,
  ) {
    registry.registerKotlinOutputTransform("test3", After("test1")) { input ->
      executedTransforms.add("test3")
      check(listOf("test2", "test1", "test3") == executedTransforms.takeLast(3))
      input
    }
  }
}

