package com.apollographql.apollo.gradle.integration

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Shared
import spock.lang.Specification

class KotlinSpec extends Specification {
    @Shared File testProjectDir

    def setupSpec() {
        def integrationTestsDir = new File(System.getProperty("user.dir"), "build/integrationTests/")
        testProjectDir = new File(integrationTestsDir, "kotlin")
        FileUtils.deleteDirectory(testProjectDir)
        FileUtils.forceMkdir(testProjectDir)

        File readOnlyDir = new File(System.getProperty("user.dir"), "src/test/testProject/kotlin/")
        if (!readOnlyDir.isDirectory()) {
            throw new IllegalArgumentException("Couldn't find test project")
        }

        FileUtils.copyDirectory(readOnlyDir, testProjectDir)
    }

    // ApolloExtension tests
    def "compilation succeeeds"() {
        when:
        def result = GradleRunner.create().withProjectDir(testProjectDir)
                .withPluginClasspath()
                .withArguments("assemble", "-Dapollographql.skipRuntimeDep=true")
                .forwardStdError(new OutputStreamWriter(System.err))
                .forwardStdOutput(new OutputStreamWriter(System.out))
                .build()

        then:
        result.task(":assemble").outcome == TaskOutcome.SUCCESS
    }

    def cleanupSpec() {
        FileUtils.deleteDirectory(testProjectDir)
    }
}
