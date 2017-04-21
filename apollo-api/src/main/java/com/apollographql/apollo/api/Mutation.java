package com.apollographql.apollo.api;

/**
 * Represents an abstraction for GraphQl mutation operation that will be sent to the server.
 * @param <D>
 * @param <T>
 * @param <V>
 */
public interface Mutation<D extends Operation.Data, T, V extends Operation.Variables> extends Operation<D, T, V> {
}
