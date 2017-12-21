package com.apollographql.apollo.gradle;

import java.io.File;
import java.io.IOException;

import okio.Okio;

import org.gradle.api.Task;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.OutputDirectory;

import com.apollographql.apollo.compiler.GraphQLCompiler;

import com.google.common.collect.Lists;

import com.moowork.gradle.node.npm.NpmTask;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;

public class ApolloCodeGenInstallTask extends NpmTask {
  static final String NAME = "installApolloCodegen";
  private static final String INSTALL_DIR =  "apollo-codegen/node_modules/apollo-codegen";

  @OutputDirectory private File installDir;
  File apolloPackageFile;
  
  public ApolloCodeGenInstallTask() {
    // TODO: set to const when ApolloPlugin is in java
    setGroup("apollo");
    setDescription("Runs npm install for apollo-codegen");
    installDir = getProject().file(getProject().getBuildDir() + File.separator + INSTALL_DIR);
    File workingDir = new File(getProject().getBuildDir(), "apollo-codegen");
    setWorkingDir(workingDir);
    apolloPackageFile = getProject().file(workingDir + File.separator + "package.json");

    final boolean isSameCodegenVersion = isSameApolloCodegenVersion(getApolloVersion());

    if (!isSameCodegenVersion) {
      Utils.deleteDirectory(installDir);
    }
    getOutputs().upToDateWhen(new Spec<Task>() {
      public boolean isSatisfiedBy(Task element) {
        return apolloPackageFile.isFile() && isSameCodegenVersion;
      }
    });
  }

  @Override
  public void exec() {
    if (!apolloPackageFile.isFile()) {
      writePackageFile(apolloPackageFile);
    }
    setArgs(Lists.newArrayList("install", "apollo-codegen@" + GraphQLCompiler.APOLLOCODEGEN_VERSION, "--save",
        "--save-exact"));
    getLogging().captureStandardOutput(LogLevel.INFO);
    super.exec();
  }
  private static class PackageJson {
    String version;
  }
  /**
   * Returns the locally install apollo-codegen version as found in the package.json file.
   *
   * @return null if build/apollo-codegen/node_modules/apollo-codegen/package.json wasn't found, version otherwise
   */
  private String getApolloVersion() {
    File packageFile = new File(getProject().getBuildDir(), INSTALL_DIR + "/package.json");
    if (!packageFile.isFile()) {
      return null;
    }

    Moshi moshi = new Moshi.Builder().build();
    JsonAdapter<PackageJson> adapter = moshi.adapter(PackageJson.class);
    try {
      PackageJson packageJson = adapter.fromJson(Okio.buffer(Okio.source(packageFile)));
      return packageJson.version;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }
  private boolean isSameApolloCodegenVersion(String packageVersion) {
    return packageVersion != null && packageVersion.equals(GraphQLCompiler.APOLLOCODEGEN_VERSION);
  }

  /**
   * Generates a dummy package.json file to silence npm warnings
   */
  private void writePackageFile(File apolloPackageFile) {
    try {
      JsonWriter writer = JsonWriter.of(Okio.buffer(Okio.sink(apolloPackageFile)));

      writer.beginObject();

      writer.name("name").value("apollo-android");
      writer.name("version").value("0.0.1");
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
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  public File getInstallDir() {
    return installDir;
  }
}
