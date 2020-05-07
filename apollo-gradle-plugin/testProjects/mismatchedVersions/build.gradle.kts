
buildscript {
    apply(from = "../../../gradle/dependencies.gradle")

    repositories {
        jcenter()
        maven {
            url = uri("../../../build/localMaven")
        }
    }
    dependencies {
        classpath(groovy.util.Eval.x(project, "x.dep.apollo.plugin"))
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.72")
    }
}

subprojects {
    repositories {
        jcenter()
    }
}