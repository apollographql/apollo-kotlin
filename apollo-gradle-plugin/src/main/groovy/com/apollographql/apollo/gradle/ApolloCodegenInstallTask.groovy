package com.apollographql.apollo.gradle

import com.google.common.collect.Lists
import com.moowork.gradle.node.npm.NpmTask
import com.squareup.moshi.JsonWriter
import okio.Okio
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile

import static com.apollographql.apollo.compiler.GraphQLCompiler.APOLLOCODEGEN_VERSION

@CacheableTask
class ApolloCodegenInstallTask extends NpmTask {
  static final String NAME = "installApolloCodegen"
  static final String INSTALLATION_PATH = "apollo-codegen" + File.separator + "node_modules"
  static final String PACKAGE_FILE_PATH = "apollo-codegen" + File.separator + "package.json"

  @OutputDirectory final DirectoryProperty installDir = project.layout.directoryProperty()
  @OutputFile final RegularFileProperty apolloPackageFile = project.layout.fileProperty()

  ApolloCodegenInstallTask() {
    setGroup("apollo")
    setDescription("Runs npm install for apollo-codegen")

    installDir.set(project.file(new File(project.buildDir, INSTALLATION_PATH)))
    apolloPackageFile.set(project.file(new File(project.buildDir, PACKAGE_FILE_PATH)));

    setWorkingDir(new File(project.buildDir, "apollo-codegen"))
  }

  @Override
  void exec() {
    Utils.deleteDirectory(installDir.get().getAsFile());

    writePackageFile(apolloPackageFile.get().getAsFile());

    setArgs(Lists.newArrayList("install", "apollo-codegen@" + APOLLOCODEGEN_VERSION, "--save", "--save-exact"));
    getLogging().captureStandardOutput(LogLevel.INFO);

    super.exec();
  }

  /**
   * Generates a dummy package.json file to silence npm warnings
   */
  private static void writePackageFile(File apolloPackageFile) {
    try {
      JsonWriter writer = JsonWriter.of(Okio.buffer(Okio.sink(apolloPackageFile)));

      writer.beginObject();

      writer.name("name").value("apollo-android");
      writer.name("version").value(APOLLOCODEGEN_VERSION);
      writer.name("description").value("Generates Java code based on a GraphQL schema and query documents. Uses " +
          "apollo-codegen under the hood.");
      writer.name("name").value("apollo-android");
      writer.name("repository");
      writer.beginObject();
      writer.name("type").value("git");
      writer.name("url").value("git+https://github.com/apollostack/apollo-android.git");
      writer.endObject();
      writer.name("author").value("Apollo");
      writer.name("license").value("MIT");
      writer.endObject();
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
