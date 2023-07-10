plugins {
  antlr
  id("org.jetbrains.kotlin.multiplatform")
  id("apollo.library")
  id("org.jetbrains.kotlin.plugin.serialization")
}

apolloLibrary {
  javaModuleName("com.apollographql.apollo3.ast")
  mpp {}
}

kotlin {
  jvm {
    withJava()
  }
  sourceSets {
    getByName("commonMain") {
      dependencies {
        api(golatac.lib("okio"))
        api(project(":apollo-annotations"))
        implementation(golatac.lib("kotlinx.serialization.json"))
      }
    }

    getByName("jsMain") {
      dependencies {
        implementation(golatac.lib("okio.nodefilesystem"))
      }
    }
    getByName("jvmMain") {
      dependencies {
        implementation(golatac.lib("antlr.runtime"))
      }
    }
  }
}

dependencies {
  antlr(golatac.lib("antlr"))
}

// Only expose the antlr runtime dependency
// See https://github.com/gradle/gradle/issues/820#issuecomment-288838412
configurations["jvmMainApi"].apply {
  setExtendsFrom(extendsFrom.filter { it.name != "antlr" })
}

/**
 * By default, antlr doesn't know about MPP, so we wire everything manually
 */
kotlin.sourceSets.getByName("jvmMain").kotlin.srcDir(file("build/generated-src/antlr/main"))
sourceSets.getByName("main").java.srcDir(file("build/generated-src/antlr/main"))

// See https://github.com/gradle/gradle/issues/19555
tasks.named("compileKotlinJvm") {
  dependsOn("generateGrammarSource")
}
// See https://github.com/gradle/gradle/issues/19555
tasks.named("compileJava") {
  dependsOn("generateGrammarSource")
}
tasks.named("compileKotlinJvm") {
  dependsOn("generateTestGrammarSource")
}
tasks.named("jvmSourcesJar") {
  dependsOn("generateGrammarSource")
}
