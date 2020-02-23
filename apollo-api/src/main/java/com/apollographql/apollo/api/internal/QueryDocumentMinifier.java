package com.apollographql.apollo.api.internal;

import org.jetbrains.annotations.NotNull;

public final class QueryDocumentMinifier {

  private QueryDocumentMinifier() {
  }

  public static String minify(@NotNull String queryDocument) {
    return queryDocument.replaceAll("\\s *", " ");
  }
}
