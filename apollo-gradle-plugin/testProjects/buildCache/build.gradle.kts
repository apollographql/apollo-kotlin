buildscript {
    apply(from = "../../../../gradle/dependencies.gradle")

    repositories {
        maven {
            url = uri("../../../../build/localMaven")
        }
        mavenCentral()
    }
    dependencies {
        classpath(groovy.util.Eval.x(project, "x.dep.apollo.plugin"))
        classpath(groovy.util.Eval.x(project, "x.dep.kotlin.plugin"))
    }
}

