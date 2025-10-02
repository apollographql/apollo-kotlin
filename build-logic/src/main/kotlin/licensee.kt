import app.cash.licensee.LicenseeExtension
import app.cash.licensee.UnusedAction
import org.gradle.api.Project

fun Project.configureLicensee() {
  plugins.apply("app.cash.licensee")
  extensions.getByType(LicenseeExtension::class.java).apply {
    unusedAction(UnusedAction.IGNORE)

    allow("Apache-2.0")
    allow("MIT")
    allow("MIT-0")
    allow("CC0-1.0")

    // remove when https://github.com/apollographql/apollo-kotlin-execution/pull/64 is released
    allowDependency("com.apollographql.execution", "apollo-execution-runtime", "0.1.1")
    allowDependency("com.apollographql.execution", "apollo-execution-runtime-jvm", "0.1.1")

    allowUrl("https://asm.ow2.io/license.html")
    allowUrl("https://spdx.org/licenses/MIT.txt")
  }
}