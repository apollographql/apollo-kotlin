package com.apollographql.android.gradle;

import com.google.common.collect.Lists;

import com.moowork.gradle.node.task.NodeTask;

import java.io.File;
import java.util.List;

public class ApolloSchemaIntrospectionTask extends NodeTask{
  // URL for the GraphQL server, also supports a local query file
  private String url;
  // Output path for the GraphQL schema file relative to the project root
  private String output;
  // Additional header to send to the server
  private String header;
  // Allows "insecure" SSL connection to the server
  private boolean insecure;

  public ApolloSchemaIntrospectionTask() {
    dependsOn(ApolloCodeGenInstallTask.NAME);
  }

  @Override
  public void exec() {
    if (Utils.isNullOrEmpty(url) || Utils.isNullOrEmpty(output)) {
      throw new IllegalArgumentException("Schema URL and output path can't be empty");
    }

    setScript(new File(getProject().getTasks().getByPath(ApolloCodeGenInstallTask.NAME).getOutputs().getFiles()
        .getAsPath(), ApolloIRGenTask.APOLLO_CODEGEN_EXEC_FILE));

    List<String> args = Lists.newArrayList("introspect-schema", url, "--output", getProject().file(output)
        .getAbsolutePath());

    if (!Utils.isNullOrEmpty(header)) {
      args.add("--header");
      args.add(header);
    }

    if (insecure) {
      args.add("--insecure");
      args.add("true");
    }

    setArgs(args);
    super.exec();
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public void setOutput(String output) {
    this.output = output;
  }

  public void setHeader(String header) {
    this.header = header;
  }

  public void setInsecure(boolean inSecure) {
    this.insecure = inSecure;
  }
}
