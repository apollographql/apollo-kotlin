buildscript {
    apply(from = "../../../../gradle/dependencies.gradle")

    repositories {
        jcenter()
        maven {
            url = uri("../../../../build/localMaven")
        }
    }
    dependencies {
        classpath(groovy.util.Eval.x(project, "x.dep.apollo.plugin"))
        classpath(groovy.util.Eval.x(project, "x.dep.kotlin.plugin"))
    }
}

