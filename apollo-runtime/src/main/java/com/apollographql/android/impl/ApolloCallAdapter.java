package com.apollographql.android.impl;

import com.apollographql.android.ApolloCall;
import com.apollographql.android.CallAdapter;
import com.apollographql.android.api.graphql.Operation;

public class ApolloCallAdapter implements CallAdapter<ApolloCall> {



  @Override public ApolloCall adapt(ApolloCall<? extends Operation.Data> call) {
    return null;
  }
}
