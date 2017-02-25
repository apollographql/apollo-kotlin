package com.apollographql.android.impl;

import com.apollographql.android.ApolloCall;
import com.apollographql.android.CallAdapter;

public class ApolloCallAdapter implements CallAdapter<ApolloCall> {
  @Override
  public ApolloCall adapt(ApolloCall call) {
    return call;
  }
}
