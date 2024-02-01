package apollo.plugin

import com.apollographql.apollo3.compiler.OperationOutputGenerator
import com.apollographql.apollo3.compiler.Plugin
import com.apollographql.apollo3.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo3.compiler.operationoutput.OperationOutput

class MyPlugin: Plugin {
    override fun operationOutputGenerator(): OperationOutputGenerator {
        return object : OperationOutputGenerator {
            override fun generate(operationDescriptorList: Collection<OperationDescriptor>): OperationOutput {
                return operationDescriptorList.map {
                    "${it.name}CustomId" to it
                }.toMap()
            }

            override val version: String get() = error("")
        }
    }
}