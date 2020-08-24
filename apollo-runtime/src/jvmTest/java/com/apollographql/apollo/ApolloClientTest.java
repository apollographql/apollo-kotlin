package com.apollographql.apollo;

import com.apollographql.apollo.interceptor.ApolloInterceptor;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

public class ApolloClientTest {

  private final ApolloClient apolloClient = ApolloClient.builder().serverUrl("https://example.com").build();

  @Test public void clonedInterceptorsListsAreIndependent() {
    ApolloInterceptor interceptor = mock(ApolloInterceptor.class);
    apolloClient.newBuilder()
        .addApplicationInterceptor(interceptor)
        .build();
    assertThat(apolloClient.getApplicationInterceptors().size()).isEqualTo(0);
  }

  /**
   * When copying the client, stateful things like the connection pool are shared across all
   * clients.
   */
  @Test public void cloneSharesInstances() {
    ApolloClient copy = apolloClient.newBuilder().build();

    assertThat(apolloClient.getApolloStore()).isSameAs(copy.getApolloStore());
    assertThat(apolloClient.getSubscriptionManager()).isSameAs(copy.getSubscriptionManager());
    assertThat(apolloClient.getHttpCache()).isSameAs(copy.getHttpCache());
  }
}
