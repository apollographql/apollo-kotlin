buildscript {
    /**
     * This doesn't use buildscript.gradle.kts as it's copied with different
     * directory layouts to ensure the build cache works as expected
     */
    apply(from = "../../../../gradle/dependencies.gradle")

    repositories {
        maven {
            url = uri("../../../../build/localMaven")
        }
        mavenCentral()
    }
    dependencies {
        classpath(groovy.util.Eval.x(project, "x.dep.apollo.plugin"))
        classpath(groovy.util.Eval.x(project, "x.dep.kotlinPlugin"))
    }
}
