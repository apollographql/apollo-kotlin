apollo {
  service("service") {
    packageName.set("com.example.rocketreserver")
    codegenModels.set("compat")
    codegenModels.set(com.apollographql.apollo.compiler.MODELS_COMPAT)
    codegenModels.set(MODELS_COMPAT)
  }
}
