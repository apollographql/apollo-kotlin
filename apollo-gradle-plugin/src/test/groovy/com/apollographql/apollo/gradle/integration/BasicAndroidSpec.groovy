package com.apollographql.apollo.gradle.integration


import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Shared
import spock.lang.Specification

import static com.apollographql.apollo.gradle.ApolloPluginTestHelper.*
import static org.apache.commons.io.FileUtils.copyFile

/**
 * The ordering of the tests in this file matters, cleanup only happens after all feature
 * methods run.
 */
class BasicAndroidSpec extends Specification {
  @Shared File testProjectDir

  def setupSpec() {
    testProjectDir = setupBasicAndroidProject()
  }

  def "builds successfully and generates expected outputs"() {
    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("build", "-Dapollographql.skipRuntimeDep=true")
        .forwardStdError(new OutputStreamWriter(System.err))
//        .forwardStdOutput(new OutputStreamWriter(System.out))
        .build()

    then:
    result.task(":build").outcome == TaskOutcome.SUCCESS

    // Java classes generated successfully
    assert new File(testProjectDir, "build/generated/source/apollo/classes/release/com/example/DroidDetailsQuery.java").isFile()
    assert new File(testProjectDir, "build/generated/source/apollo/classes/release/com/example/FilmsQuery.java").isFile()
    assert new File(testProjectDir, "build/generated/source/apollo/classes/release/fragment/SpeciesInformation.java").isFile()

    // verify that the custom type generated was Object.class because no customType mapping was specified
    assert new File(testProjectDir, "build/generated/source/apollo/classes/release/type/CustomType.java").isFile()
    assert new File(testProjectDir, "build/generated/source/apollo/classes/release/type/CustomType.java").getText('UTF-8')
        .contains("return Object.class;")

    // Optional is not added to the generated classes
    assert !new File(testProjectDir,
        "build/generated/source/apollo/classes/release/com/example/DroidDetailsQuery.java").getText('UTF-8').contains("Optional")
    assert new File(testProjectDir, "build/generated/source/apollo/classes/release/com/example/DroidDetailsQuery.java").getText(
        'UTF-8').contains("import org.jetbrains.annotations.Nullable;")
  }

  def "nothing changed, generate classes up to date"() {
    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("generateApolloClasses", "-Dapollographql.skipRuntimeDep=true")
        .forwardStdError(new OutputStreamWriter(System.err))
//        .forwardStdOutput(new OutputStreamWriter(System.out))
        .build()

    then:
    result.task(":generateApolloClasses").outcome == TaskOutcome.UP_TO_DATE
    // Java classes generated successfully
    assert new File(testProjectDir, "build/generated/source/apollo/classes/release/com/example/DroidDetailsQuery.java").isFile()
    assert new File(testProjectDir, "build/generated/source/apollo/classes/release/com/example/FilmsQuery.java").isFile()
    assert new File(testProjectDir, "build/generated/source/apollo/classes/release/fragment/SpeciesInformation.java").isFile()
  }

  // ApolloExtension tests
  def "generateApolloClasses is up to date if inputs and apollo extension don't change"() {
    setup: "a testProject with a previous build"

    when:
    def result = GradleRunner.create().withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("generateApolloClasses", "-Dapollographql.skipRuntimeDep=true")
        .forwardStdError(new OutputStreamWriter(System.err))
//        .forwardStdOutput(new OutputStreamWriter(System.out))
        .build()

    then:
    result.task(":generateApolloClasses").outcome == TaskOutcome.UP_TO_DATE
  }

  def "exclude graphql files, builds successfully and generates expected outputs"() {
    setup: "a testProject with a excluded files"
    replaceTextInFile(new File("$testProjectDir/build.gradle")) {
      it.replace("apollo {",
          "apollo {\n sourceSet { \n exclude = [\"**/DroidDetails.graphql\", \"**/DroidDetailsSpeciesInfo.graphql\"]\n } \n")
    }

    when:
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("clean", "generateApolloClasses", "-Dapollographql.skipRuntimeDep=true")
        .forwardStdError(new OutputStreamWriter(System.err))
        .forwardStdOutput(new OutputStreamWriter(System.out))
        .build()

    then:
    result.task(":generateApolloClasses").outcome == TaskOutcome.SUCCESS

    // Java classes generated successfully
    assert new File(testProjectDir, "build/generated/source/apollo/classes/release/com/example/FilmsQuery.java").exists()
    assert !new File(testProjectDir, "build/generated/source/apollo/classes/release/com/example/DroidDetailsQuery.java").exists()
    assert !new File(testProjectDir, "build/generated/source/apollo/classes/release/com/example/DroidDetailsSpeciesInfoQuery.java").exists()
    assert !new File(testProjectDir, "build/generated/source/apollo/classes/release/fragment/SpeciesInformation.java").exists()

    cleanup:
    replaceTextInFile(new File("$testProjectDir/build.gradle")) {
      it.replace("sourceSet { \n exclude = [\"**/DroidDetails.graphql\", \"**/DroidDetailsSpeciesInfo.graphql\"]\n }",
          "")
    }
  }

  def "adding a custom type to the build script re-generates the CustomType class"() {
    setup: "a testProject with a previous build and an apollo extension appended"
    replaceTextInFile(new File("$testProjectDir/build.gradle")) {
      it.replace("apollo {", "apollo {\n customTypeMapping=[\"DateTime\" : \"java.util.Date\"]\n")
    }

    when:
    def result = GradleRunner.create().withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("generateApolloClasses", "-Dapollographql.skipRuntimeDep=true", "--info")
        .forwardStdError(new OutputStreamWriter(System.err))
//        .forwardStdOutput(new OutputStreamWriter(System.out))
        .build()

    then:
    // modifying the customTypeMapping should cause the task to be out of date
    // and the task should run again
    result.task(":generateApolloClasses").outcome == TaskOutcome.SUCCESS

    assert new File(testProjectDir, "build/generated/source/apollo/classes/release/type/CustomType.java").isFile()
    assert new File(testProjectDir, "build/generated/source/apollo/classes/release/type/CustomType.java").getText('UTF-8').contains(
        "return Date.class;")
  }

  def "changing the class for a customTypeMapping key regenerates with the new class"() {
    setup: "a testProject with a previous build and a modified apollo extension"
    replaceTextInFile(new File("$testProjectDir/build.gradle")) {
      it.replace("java.util.Date", "java.util.Currency")
    }

    when:
    def result = GradleRunner.create().withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("generateApolloClasses", "-Dapollographql.skipRuntimeDep=true")
        .forwardStdError(new OutputStreamWriter(System.err))
//        .forwardStdOutput(new OutputStreamWriter(System.out))
        .build()

    then:
    result.task(":generateApolloClasses").outcome == TaskOutcome.SUCCESS

    assert new File(testProjectDir, "build/generated/source/apollo/classes/release/type/CustomType.java").isFile()
    assert new File(testProjectDir, "build/generated/source/apollo/classes/release/type/CustomType.java").getText('UTF-8').contains(
        "return Currency.class;")
  }

  def "adding nullableValueType = `annotated` in Apollo Extension generates classes annotated with JSR"() {
    setup: "a testProject with a previous build and a modified build script"
    replaceTextInFile(new File("$testProjectDir/build.gradle")) {
      it.replace("apollo {", "apollo {\n nullableValueType = 'annotated' \n")
    }

    when:
    def result = GradleRunner.create().withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("clean", "generateApolloClasses", "-Dapollographql.skipRuntimeDep=true")
        .forwardStdError(new OutputStreamWriter(System.err))
//        .forwardStdOutput(new OutputStreamWriter(System.out))
        .build()

    then:
    result.task(":generateApolloClasses").outcome == TaskOutcome.SUCCESS
    assert new File(testProjectDir, "build/generated/source/apollo/classes/release/com/example/DroidDetailsQuery.java").isFile()
    assert !new File(testProjectDir, "build/generated/source/apollo/classes/release/com/example/DroidDetailsQuery.java").getText(
        'UTF-8').contains("Optional")
    assert new File(testProjectDir, "build/generated/source/apollo/classes/release/com/example/DroidDetailsQuery.java").getText(
        'UTF-8').contains("import org.jetbrains.annotations.Nullable;")
  }

  def "adding nullableValueType = `apolloOptional` in Apollo Extension generates classes with Apollo Optional"() {
    setup: "a testProject with a previous build and a modified build script"
    replaceTextInFile(new File("$testProjectDir/build.gradle")) {
      it.replace("annotated", "apolloOptional")
    }

    when:
    def result = GradleRunner.create().withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("clean", "generateApolloClasses", "-Dapollographql.skipRuntimeDep=true")
        .forwardStdError(new OutputStreamWriter(System.err))
//        .forwardStdOutput(new OutputStreamWriter(System.out))
        .build()

    then:
    result.task(":generateApolloClasses").outcome == TaskOutcome.SUCCESS
    assert new File(testProjectDir, "build/generated/source/apollo/classes/release/com/example/DroidDetailsQuery.java").isFile()
    assert new File(testProjectDir, "build/generated/source/apollo/classes/release/com/example/DroidDetailsQuery.java").getText(
        'UTF-8').contains("import com.apollographql.apollo.api.internal.Optional;")
  }

  def "adding nullableValueType = `guavaOptional` in Apollo Extension generates classes with Guava Optional"() {
    setup: "a testProject with a previous build and a modified build script"
    replaceTextInFile(new File("$testProjectDir/build.gradle")) {
      it.replace("apolloOptional", "guavaOptional")
    }

    when:
    def result = GradleRunner.create().withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("clean", "generateApolloClasses", "-Dapollographql.skipRuntimeDep=true")
        .forwardStdError(new OutputStreamWriter(System.err))
//        .forwardStdOutput(new OutputStreamWriter(System.out))
        .build()

    then:
    result.task(":generateApolloClasses").outcome == TaskOutcome.SUCCESS
    assert new File(testProjectDir, "build/generated/source/apollo/classes/release/com/example/DroidDetailsQuery.java").isFile()
    assert new File(testProjectDir, "build/generated/source/apollo/classes/release/com/example/DroidDetailsQuery.java").getText(
        'UTF-8').contains("import com.google.common.base.Optional;")
  }

  def "adding useSemanticNaming = `false` in Apollo Extension generates classes without query suffix"() {
    setup: "a testProject with a previous build and a modified build script"
    replaceTextInFile(new File("$testProjectDir/build.gradle")) {
      it.replace("apollo {", "apollo {\n useSemanticNaming = false \n")
    }

    when:
    def result = GradleRunner.create().withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("clean", "generateApolloClasses", "-Dapollographql.skipRuntimeDep=true")
        .forwardStdError(new OutputStreamWriter(System.err))
//        .forwardStdOutput(new OutputStreamWriter(System.out))
        .build()

    then:
    result.task(":generateApolloClasses").outcome == TaskOutcome.SUCCESS
    assert new File(testProjectDir, "build/generated/source/apollo/classes/release/com/example/DroidDetails.java").isFile()
  }

  def "set explicit path path to schema.json but missing package name fails"() {
    setup: "a testProject with a previous build and a modified build script"
    replaceTextInFile(new File("$testProjectDir/build.gradle")) {
      it.replace("apollo {",
          "apollo {\n " +
              "schemaFilePath = \"/graphql/schema/my-schema.json\"\n "
      )
    }

    when:
    new File("$testProjectDir/src/release/graphql/schema.json").delete()
    String schemaFilesFixtures = "src/test/testProject/android/schemaFilesFixtures"
    copyFile(new File(schemaFilesFixtures + "/oldswapi.json"),
        new File("$testProjectDir/graphql/schema/my-schema.json"))

    def buildFailure = false
    try {
      GradleRunner.create()
          .withProjectDir(testProjectDir)
          .withPluginClasspath()
          .withArguments("clean", "generateApolloClasses", "-Dapollographql.skipRuntimeDep=true")
          .forwardStdError(new OutputStreamWriter(System.err))
//          .forwardStdOutput(new OutputStreamWriter(System.out))
          .build()
    } catch (UnexpectedBuildFailure e) {
      buildFailure = true
    }

    then:
    if (!buildFailure) {
      throw new RuntimeException("expected failure")
    }
    assert !new File(testProjectDir, "build/generated/source/apollo/classes/release/com/myexample/DroidDetails.java").isFile()
  }

  def "set explicit path path to schema.json and target package name generates classes successfully"() {
    setup: "a testProject with a previous build and a modified build script"
    replaceTextInFile(new File("$testProjectDir/build.gradle")) {
      it.replace("apollo {",
          "apollo {\n " +
              "outputPackageName = \"com.myexample\"\n"
      )
    }

    when:
    def result = GradleRunner.create().withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("clean", "generateApolloClasses", "-Dapollographql.skipRuntimeDep=true")
        .forwardStdError(new OutputStreamWriter(System.err))
//        .forwardStdOutput(new OutputStreamWriter(System.out))
        .build()

    then:
    result.task(":generateApolloClasses").outcome == TaskOutcome.SUCCESS
    assert new File(testProjectDir, "build/generated/source/apollo/classes/release/com/myexample/DroidDetails.java").isFile()
  }

  def "remove graphql files, builds successfully and generates expected outputs"() {
    when:
    assert new File("$testProjectDir/src/main/graphql/com/example/AllFilms.graphql").delete()
    assert new File("$testProjectDir/src/main/graphql/com/example/DroidDetailsSpeciesInfo.graphql").delete()
    def result = GradleRunner.create()
        .withProjectDir(testProjectDir)
        .withPluginClasspath()
        .withArguments("generateApolloClasses", "-Dapollographql.skipRuntimeDep=true")
        .forwardStdError(new OutputStreamWriter(System.err))
//        .forwardStdOutput(new OutputStreamWriter(System.out))
        .build()

    then:
    result.task(":generateApolloClasses").outcome == TaskOutcome.SUCCESS
    // Java classes generated successfully
    assert new File(testProjectDir, "build/generated/source/apollo/classes/release/com/myexample/DroidDetails.java").isFile()
    assert !new File(testProjectDir, "build/generated/source/apollo/classes/release/com/myexample/Films.java").exists()
    assert !new File(testProjectDir, "build/generated/source/apollo/classes/release/fragment/SpeciesInformation.java").exists()
  }

  def cleanupSpec() {
    FileUtils.deleteDirectory(testProjectDir)
  }

  private static File setupBasicAndroidProject() {
    def destDir = createTempTestDirectory("basic")
    prepareProjectTestDir(destDir, ProjectType.Android, "basic", "basic")
    String schemaFilesFixtures = "src/test/testProject/android/schemaFilesFixtures"
    copyFile(new File(schemaFilesFixtures + "/oldswapi.json"), new File("$destDir/src/main/graphql/schema.json"))
    return destDir
  }
}
