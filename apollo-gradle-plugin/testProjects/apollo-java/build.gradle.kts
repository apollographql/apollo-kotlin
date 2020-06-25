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
        classpath(groovy.util.Eval.x(project, "x.dep.apollo.plugin"))
    }
}

apply(plugin = "java")
apply(plugin = "com.apollographql.apollo-java")

repositories {
    maven {
        url = uri("../../../build/localMaven")
    }
    jcenter()
    mavenCentral()
}

