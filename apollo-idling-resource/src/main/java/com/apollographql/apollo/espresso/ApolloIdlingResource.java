package com.apollographql.apollo.espresso;


import android.support.test.espresso.IdlingResource;

import com.apollographql.apollo.ApolloClient;

import javax.annotation.Nonnull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * An Espresso {@link IdlingResource} for {@link com.apollographql.apollo.ApolloClient}.
 */
public class ApolloIdlingResource implements IdlingResource {

  private final String name;
  private final ApolloClient apolloClient;
  private ResourceCallback callback;

  /**
   * Creates a new {@link IdlingResource} from {@link ApolloClient} with a given name. Register this instance using
   * Espresso class's registerIdlingResource in your test suite's setup method.
   *
   * @param name         name of this idlingResource instance.
   * @param apolloClient the apolloClient for which idlingResource needs to be created.
   * @return a new ApolloIdlingResource.
   * @throws NullPointerException if name == null or apolloClient == null
   */
  public static ApolloIdlingResource create(@Nonnull String name, @Nonnull ApolloClient apolloClient) {
    checkNotNull(name, "name == null");
    checkNotNull(apolloClient, "apolloClient == null");
    return new ApolloIdlingResource(name, apolloClient);
  }

  private ApolloIdlingResource(String name, ApolloClient apolloClient) {
    this.apolloClient = apolloClient;
    this.name = name;
  }

  @Override public String getName() {
    return name;
  }

  @Override public boolean isIdleNow() {
    return false;
  }

  @Override public void registerIdleTransitionCallback(ResourceCallback callback) {
    this.callback = callback;
  }
}
