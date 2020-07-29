import com.android.build.gradle.BaseExtension
import com.apollographql.apollo.gradle.api.ApolloExtension

apply(plugin = "com.apollographql.apollo")
apply(plugin = "org.jetbrains.kotlin.jvm")

dependencies {
  add("implementation", groovy.util.Eval.x(project, "x.dep.kotlin.coroutines.core"))
  add("implementation", groovy.util.Eval.x(project, "x.dep.kotlin.stdLib"))

  add("implementation", "com.apollographql.apollo:apollo-runtime")
  add("implementation", "com.apollographql.apollo:apollo-rx2-support")
  add("implementation", "com.apollographql.apollo:apollo-rx3-support")
  add("implementation", "com.apollographql.apollo:apollo-coroutines-support")
  add("implementation", "com.apollographql.apollo:apollo-http-cache")
  add("implementation", "com.apollographql.apollo:apollo-compiler")

  add("testImplementation", groovy.util.Eval.x(project, "x.dep.junit"))
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.truth"))
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.okHttp.mockWebServer"))
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.mockito"))
  add("testImplementation", groovy.util.Eval.x(project, "x.dep.moshi.moshi"))
}

configure<ApolloExtension> {
  customTypeMapping.set(mapOf(
      "Date" to "java.util.Date",
      "Upload" to "com.apollographql.apollo.api.FileUpload"
  ))
  generateOperationOutput.set(true)
  service("httpcache") {
    sourceFolder.set("com/apollographql/apollo/integration/httpcache")
    rootPackageName.set("com.apollographql.apollo.integration.httpcache")
  }
  service("interceptor") {
    sourceFolder.set("com/apollographql/apollo/integration/interceptor")
    rootPackageName.set("com.apollographql.apollo.integration.interceptor")
  }
  service("normalizer") {
    sourceFolder.set("com/apollographql/apollo/integration/normalizer")
    rootPackageName.set("com.apollographql.apollo.integration.normalizer")
  }
  service("upload") {
    sourceFolder.set("com/apollographql/apollo/integration/upload")
    rootPackageName.set("com.apollographql.apollo.integration.upload")
  }
  service("subscription") {
    sourceFolder.set("com/apollographql/apollo/integration/subscription")
    rootPackageName.set("com.apollographql.apollo.integration.subscription")
  }
}
