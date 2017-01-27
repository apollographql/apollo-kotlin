package com.apollographql.android.gradle;

public class ApolloExtension {
  static final String NAME = "apollo";
  private boolean generateClasses = false;

  public boolean isGenerateClasses() {
    return generateClasses;
  }

  public void setGenerateClasses(boolean generateClasses) {
    this.generateClasses = generateClasses;
  }
}
