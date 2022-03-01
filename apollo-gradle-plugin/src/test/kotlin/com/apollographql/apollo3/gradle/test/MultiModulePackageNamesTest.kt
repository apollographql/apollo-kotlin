package com.apollographql.apollo3.gradle.test

import com.apollographql.apollo3.gradle.util.TestUtils
import com.apollographql.apollo3.gradle.util.TestUtils.withTestProject
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(TestParameterInjector::class)
class MultiModulePackageNamesTest {
  @Test
  fun test(
      @TestParameter packageNamesFromFilePaths: Boolean,
      @TestParameter useSchemaPackageNameForFragments: Boolean,
      @TestParameter useSchemaPackageNameForSchemaTypes: Boolean,
  ) {
    withTestProject("multi-modules-package-names") {
      val packageNameLine = if (packageNamesFromFilePaths) {
        "packageNamesFromFilePaths()"
      } else {
        "packageName.set(\"com.module2\")"
      }
      val apolloConfiguration = """
        apollo {
          useSchemaPackageNameForFragments.set($useSchemaPackageNameForFragments)
          $packageNameLine
        }
      """.trimIndent()
      it.resolve("module2/build.gradle.kts").appendText("\n" + apolloConfiguration)

      TestUtils.executeTaskAndAssertSuccess(":module2:generateApolloSources", it)

      if (useSchemaPackageNameForFragments) {
        checkModule2Class(it, "com.module1.fragment.QueryFragment")
      } else {
        if (packageNamesFromFilePaths) {
          checkModule2Class(it, "some.path.fragment.QueryFragment")
        } else {
          checkModule2Class(it, "com.module2.fragment.QueryFragment")
        }
      }

      if (useSchemaPackageNameForSchemaTypes) {
        checkModule2Class(it, "com.module1.type.FieldInput2")
      } else {
        if (packageNamesFromFilePaths) {
          checkModule2Class(it, "some.path.type.FieldInput2")
        } else {
          checkModule2Class(it, "com.module2.type.FieldInput2")
        }
      }

      if (packageNamesFromFilePaths) {
        checkModule2Class(it, "some.path.GetField2Query")
      } else {
        checkModule2Class(it, "com.module2.GetField2Query")
      }
    }
  }

  private fun checkModule2Class(dir: File, className: String) {
    val generatedFile = dir.resolve("module2/build/generated/source/apollo/service/${className.replace(".", "/")}.kt")
    assert(generatedFile.readText().contains("package " + className.substringBeforeLast(".")))
  }
}