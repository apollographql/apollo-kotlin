import com.apollographql.apollo.compiler.operationoutput.OperationOutput
import com.apollographql.apollo.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo.compiler.OperationOutputGenerator

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.apollo)
}

apollo {
  service("service") {
    val customOperationOutputGenerator = object : OperationOutputGenerator {
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
}