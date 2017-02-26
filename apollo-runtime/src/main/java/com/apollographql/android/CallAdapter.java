package com.apollographql.android;

import com.apollographql.android.api.graphql.Operation;

public interface CallAdapter<R> {

    R adapt(ApolloCall<? extends Operation.Data> call);
}
