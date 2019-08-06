package com.apollographql.apollo.gradle

import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Property
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory

import static com.apollographql.apollo.compiler.GraphQLCompiler.APOLLOCODEGEN_VERSION

class ApolloSystemCodegenGenerationTask extends AbstractExecTask<ApolloSystemCodegenGenerationTask> {
  @Input final Property<String> variant = project.objects.property(String.class)
  @Input final Property<List<String>> sourceSetNames = project.objects.listProperty(String.class)
  @Input @Optional final Property<String> schemaFilePath = project.objects.property(String.class)
  @Input @Optional final Property<String> outputPackageName = project.objects.property(String.class)
  @OutputDirectory final DirectoryProperty outputDir = project.objects.directoryProperty()

  ApolloSystemCodegenGenerationTask() {
    super(ApolloSystemCodegenGenerationTask.class)
  }

  @Override
  void exec() {
    verifySystemApolloCodegenVersion(logger)
    setCommandLine("apollo-codegen")

    List<CodegenGenerationTaskCommandArgsBuilder.CommandArgs> args = new CodegenGenerationTaskCommandArgsBuilder(
        this, schemaFilePath.get(), outputPackageName.get(), outputDir.get().asFile, variant.get(),
        sourceSetNames.get().collect { it.toString() }).build()

    for (CodegenGenerationTaskCommandArgsBuilder.CommandArgs commandArgs : args) {
      setArgs(commandArgs.taskArguments)
      super.exec()
    }
  }

  private static verifySystemApolloCodegenVersion(Logger logger) {
    logger.info("Verifying system 'apollo-codegen' version (executing command 'apollo-codegen --version') ...")
    try {
      StringBuilder output = new StringBuilder()
      Process checkGlobalApolloCodegen = "apollo-codegen --version".execute()
      checkGlobalApolloCodegen.consumeProcessOutput(output, null)
      checkGlobalApolloCodegen.waitForOrKill(5000)

      def version = output.toString().trim()
      if (version == APOLLOCODEGEN_VERSION) {
        logger.info("Found required 'apollo-codegen@$APOLLOCODEGEN_VERSION' version.")
        logger.info("Skip apollo-codegen installation.")
      } else {
        throw new GradleException("Required 'apollo-codegen@$APOLLOCODEGEN_VERSION' version but found: $version. Consider disabling `apollographql.useGlobalApolloCodegen` in gradle.properties file.")
      }
    } catch (Exception exception) {
      throw new GradleException("Failed to verify system 'apollo-codegen' version. Consider disabling `apollographql.useGlobalApolloCodegen` in gradle.properties file.", exception)
    }
  }
}
