import com.apollographql.apollo3.gradle.api.ApolloExtension
import com.apollographql.apollo3.compiler.operationoutput.OperationOutput
import com.apollographql.apollo3.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo3.compiler.OperationOutputGenerator

buildscript {
  apply(from = "../../testProjects/buildscript.gradle.kts")
}

apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "com.apollographql.apollo3")

configure<ApolloExtension> {
  val customOperationOutputGenerator = object: OperationOutputGenerator {
    override fun generate(operationDescriptorList: Collection<OperationDescriptor>): OperationOutput {
      return operationDescriptorList.map {
        "${it.name}CustomId" to it
      }.toMap()
    }

    override val version = "OperationOutputGenerator-v1"
  }

  packageNamesFromFilePaths()
  operationOutputGenerator.set(customOperationOutputGenerator)
}