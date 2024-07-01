package hooks

import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.Transform
import com.apollographql.apollo.compiler.ir.IrOperations

class TestPlugin : ApolloCompilerPlugin {
  override fun irOperationsTransform(): Transform<IrOperations> {
    return object : Transform<IrOperations> {
      override fun transform(input: IrOperations): IrOperations {
        return input.copy(
            operations = input.operations.map {
              it.copy(
                  dataModelGroup = it.dataModelGroup.maybeFlatten(3)
              )
            }
        )
      }
    }
  }
}
