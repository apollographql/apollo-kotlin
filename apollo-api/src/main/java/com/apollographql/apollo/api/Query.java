package com.apollographql.apollo.api;

/**
 * Represents an abstraction for GraphQl query that will be sent to the server.
 */
public interface Query<D extends Operation.Data, T, V extends Operation.Variables> extends Operation<D, T, V> {
}
