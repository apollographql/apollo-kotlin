package hooks

import com.apollographql.apollo3.compiler.Plugin
import com.apollographql.apollo3.compiler.Transform
import com.apollographql.apollo3.compiler.ir.IrOperations

class TestPlugin : Plugin {
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
