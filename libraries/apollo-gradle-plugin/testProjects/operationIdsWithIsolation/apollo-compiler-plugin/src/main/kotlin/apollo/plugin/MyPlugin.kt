package apollo.plugin

import com.apollographql.apollo3.compiler.OperationOutputGenerator
import com.apollographql.apollo3.compiler.Plugin
import com.apollographql.apollo3.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo3.compiler.operationoutput.OperationId

class MyPlugin: Plugin {
    override fun operationIds(descriptors: List<OperationDescriptor>): List<OperationId>? {
        return descriptors.map { OperationId("${it.name}CustomId", it.name) }
    }
}