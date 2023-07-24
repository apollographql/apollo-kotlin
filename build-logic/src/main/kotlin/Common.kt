
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

fun Project.commonSetup() {
  pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    tasks.register("ft") {
      if (this@commonSetup.name != "apollo-gradle-plugin") {
        dependsOn("test")
      }
    }
  }
  pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    val fastTest = tasks.register("ft")

    tasks.withType(KotlinJvmTest::class.java) {
      fastTest.configure {
        this.dependsOn(this@withType)
      }
    }
  }
}
