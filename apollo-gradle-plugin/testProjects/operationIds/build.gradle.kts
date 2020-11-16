import com.apollographql.apollo.gradle.api.ApolloExtension
import com.apollographql.apollo.compiler.operationoutput.OperationOutput
import com.apollographql.apollo.compiler.operationoutput.OperationDescriptor
import com.apollographql.apollo.compiler.OperationOutputGenerator

buildscript {
  apply(from = "../../../gradle/dependencies.gradle")

  repositories {
    maven {
      url = uri("../../../build/localMaven")
    }
    mavenCentral()
  }
  dependencies {
    classpath(groovy.util.Eval.x(project, "x.dep.kotlin.plugin"))
    classpath(groovy.util.Eval.x(project, "x.dep.apollo.plugin"))
  }
}

apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "com.apollographql.apollo")

repositories {
  maven {
    url = uri("../../../build/localMaven")
  }
  mavenCentral()
}

configure<ApolloExtension> {
  val customOperationOutputGenerator = object: OperationOutputGenerator {
    override fun generate(operationDescriptorList: Collection<OperationDescriptor>): OperationOutput {
      return operationDescriptorList.map {
        "${it.name}CustomId" to it
      }.toMap()
    }

    override val version = "OperationOutputGenerator-v1"
  }

  operationOutputGenerator.set(customOperationOutputGenerator)
}