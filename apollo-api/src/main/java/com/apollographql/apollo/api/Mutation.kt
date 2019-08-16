package com.apollographql.apollo.api;

/**
 * Represents a GraphQL mutation operation that will be sent to the server.
 */
public interface Mutation<D extends Operation.Data, T, V extends Operation.Variables> extends Operation<D, T, V> {
}
