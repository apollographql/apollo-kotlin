package com.apollostack.android;

import android.database.Observable;

import java.util.List;

import javax.annotation.Nullable;

/** A {@link retrofit2.Retrofit} service for executing GraphQL requests. */
public interface GraphqlService {
  // TODO: Replace the wildcard with an interface once apollo-api module is merged.
  // TODO: Also need to figure out the exact API for this method.
  Observable<?> execute(String query, @Nullable List<String> arguments);
}
