package com.apollographql.android.impl;

import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.cache.normalized.Record;

import java.util.Collection;

final class ParseResponse<T> {
  final Response<T> response;
  final Collection<Record> records;

  public ParseResponse(Response<T> response, Collection<Record> records) {
    this.response = response;
    this.records = records;
  }
}
