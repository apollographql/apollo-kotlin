package apollo.plugin

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.compiler.OperationOutputGenerator
import com.apollographql.apollo.compiler.ApolloCompilerPlugin
import com.apollographql.apollo.compiler.ApolloCompilerPluginEnvironment
import com.apollographql.apollo.compiler.ApolloCompilerPluginProvider
import com.apollographql.apollo.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo.compiler.operationoutput.OperationId

class MyPlugin: ApolloCompilerPlugin {
    override fun operationIds(descriptors: List<OperationDescriptor>): List<OperationId>? {
        return descriptors.map { OperationId("${it.name}CustomId", it.name) }
    }
}

@OptIn(ApolloExperimental::class)
class MyPluginProvider: ApolloCompilerPluginProvider {
    override fun create(environment: ApolloCompilerPluginEnvironment): ApolloCompilerPlugin {
        return MyPlugin()
    }

}