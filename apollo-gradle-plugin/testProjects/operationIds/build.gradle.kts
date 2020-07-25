import com.apollographql.apollo.gradle.api.ApolloExtension
import com.apollographql.apollo.gradle.api.ApolloGenerateOperationIdsTask
import com.apollographql.apollo.compiler.operationoutput.OperationOutput
import com.apollographql.apollo.compiler.operationoutput.OperationList

buildscript {
  apply(from = "../../../gradle/dependencies.gradle")

  repositories {
    maven {
      url = uri("../../../build/localMaven")
    }
    google()
    mavenCentral()
    jcenter()
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
  jcenter()
  mavenCentral()
}

abstract class GenerateCustomOperationIdsTask: ApolloGenerateOperationIdsTask() {
  override fun generateOperationOutput(operationList: OperationList): OperationOutput {
    return operationList.map {
      it.filePath to it
    }.toMap()
  }
}

configure<ApolloExtension> {
  onCompilationUnit {
    val task = tasks.register("generate${name.capitalize()}OperationIds", GenerateCustomOperationIdsTask::class.java)
    setGenerateOperationIdsTaskProvider(task)
  }
}