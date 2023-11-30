plugins {
  id("java")
  id("apollo.test")
  id("com.apollographql.apollo3")
}

dependencies {
  implementation(golatac.lib("apollo.runtime.java"))
  implementation(golatac.lib("apollo.mockserver"))
  implementation(golatac.lib("apollo.rx3.java"))
  testImplementation(golatac.lib("junit"))
  testImplementation(golatac.lib("truth"))
  testImplementation(project(":sample-server"))
}

apollo {
  service("main") {
    srcDir("src/main/graphql/main")
    packageName.set("javatest")
    generateModelBuilders.set(true)
    mapScalarToJavaString("LanguageCode")
    mapScalarToJavaObject("Json")
    mapScalarToJavaLong("Long")
    mapScalar("GeoPoint", "scalar.GeoPoint")
  }

  service("graphql-ws") {
    srcDir("src/main/graphql/graphql-ws")
    packageName.set("graphqlws")
    generateModelBuilders.set(true)
  }

  service("appsync") {
    srcDir("src/main/graphql/appsync")
    packageName.set("appsync")
    generateModelBuilders.set(true)
  }

  service("batching") {
    srcDir("src/main/graphql/batching")
    packageName.set("batching")
    generateModelBuilders.set(true)
  }

}
