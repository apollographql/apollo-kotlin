package com.apollographql.apollo.test.espresso;


import android.support.test.espresso.IdlingResource;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.IdleResourceCallback;

import org.jetbrains.annotations.NotNull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * An Espresso {@link IdlingResource} for {@link com.apollographql.apollo.ApolloClient}.
 */
public final class ApolloIdlingResource implements IdlingResource {

  private final String name;
  private final ApolloClient apolloClient;
  ResourceCallback callback;

  /**
   * Creates a new {@link IdlingResource} from {@link ApolloClient} with a given name. Register this instance using
   * Espresso class's registerIdlingResource in your test suite's setup method.
   *
   * @param name name of this idlingResource instance.
   * @param apolloClient the apolloClient for which idlingResource needs to be created.
   * @return a new ApolloIdlingResource.
   * @throws NullPointerException if name == null or apolloClient == null
   */
  public static ApolloIdlingResource create(@NotNull String name, @NotNull ApolloClient apolloClient) {
    checkNotNull(name, "name == null");
    checkNotNull(apolloClient, "apolloClient == null");
    return new ApolloIdlingResource(name, apolloClient);
  }

  private ApolloIdlingResource(String name, ApolloClient apolloClient) {
    this.apolloClient = apolloClient;
    this.name = name;
    apolloClient.idleCallback(new IdleResourceCallback() {
      @Override public void onIdle() {
        ResourceCallback callback = ApolloIdlingResource.this.callback;
        if (callback != null) {
          callback.onTransitionToIdle();
        }
      }
    });
  }

  @Override public String getName() {
    return name;
  }

  @Override public boolean isIdleNow() {
    return apolloClient.activeCallsCount() == 0;
  }

  @Override public void registerIdleTransitionCallback(ResourceCallback callback) {
    this.callback = callback;
  }
}
