package com.apollographql.android;

public interface CallAdapter<R> {
    R adapt(ApolloCall call);
}
