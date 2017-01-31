package com.apollographql.android.gradle;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.gradle.api.Task;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.OutputDirectory;

import com.google.common.collect.Lists;

import com.moowork.gradle.node.npm.NpmTask;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import okio.Okio;

public class ApolloCodeGenInstallTask extends NpmTask {
  static final String NAME = "installApolloCodegen";
  private static final String INSTALL_DIR = "node_modules/apollo-codegen";
  private static final String APOLLOCODEGEN_VERSION = "0.10.3";

  @OutputDirectory private File installDir;

  public ApolloCodeGenInstallTask() {
    // TODO: set to const when ApolloPlugin is in java
    setGroup("apollo");
    setDescription("Runs npm install for apollo-codegen");
    installDir = getProject().file(INSTALL_DIR);

    final File apolloPackageFile = getProject().file("package.json");
    final String apolloVersion = getApolloVersion();

    getOutputs().upToDateWhen(new Spec<Task>() {
      public boolean isSatisfiedBy(Task element) {
        return !(apolloPackageFile.isFile() || (apolloVersion != null && apolloVersion.equals(APOLLOCODEGEN_VERSION)));
      }
    });
    if (!apolloPackageFile.isFile()) {
      writePackageFile(apolloPackageFile);
    }
    setArgs(Lists.newArrayList("install", "apollo-codegen@" + APOLLOCODEGEN_VERSION, "--save", "--save-exact"));
  }

  static class PackageJson {
    String version;
  }
  /**
   * Returns the locally install apollo-codegen version as found in the package.json file.
   *
   * @return null if node_modules/apollo-codegen/package.json wasn't found, version otherwise
   */
  private String getApolloVersion() {
    File packageFile = getProject().file(INSTALL_DIR + "/package.json");
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
