package hooks

import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.ApolloCompilerPluginEnvironment
import com.apollographql.apollo.compiler.ApolloCompilerRegistry


class TestPlugin : ApolloCompilerPlugin {
  override fun beforeCompilationStep(
      environment: ApolloCompilerPluginEnvironment,
      registry: ApolloCompilerRegistry,
  ) {
    registry.registerIrTransform("test") {
      it.copy(
          operations = it.operations.map {
            it.copy(
                dataModelGroup = it.dataModelGroup.maybeFlatten(3)
            )
          }
      )
    }
  }
}
