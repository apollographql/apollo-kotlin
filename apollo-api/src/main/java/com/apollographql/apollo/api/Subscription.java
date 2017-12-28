package com.apollographql.apollo.api;

/**
 * Represents a GraphQL subscription.
 */
public interface Subscription<D extends Operation.Data, T, V extends Operation.Variables> extends Operation<D, T, V> {
}
