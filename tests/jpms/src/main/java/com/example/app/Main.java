package com.example.app;

import com.apollographql.apollo3.ApolloClient;
import com.apollographql.apollo3.api.Query;
import com.apollographql.apollo3.cache.normalized.NormalizedCacheExtensions;
import com.apollographql.apollo3.cache.normalized.api.FieldPolicyCacheResolver;
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory;
import com.apollographql.apollo3.cache.normalized.api.TypePolicyCacheKeyGenerator;
import com.apollographql.apollo3.rx2.Rx2Apollo;
import com.example.GetHelloQuery;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World! " + Query.class.getName());

        ApolloClient.Builder apolloClientBuilder = ApolloClient.builder()
                .serverUrl("https://example.com");

      NormalizedCacheExtensions.normalizedCache(
          apolloClientBuilder,
          new MemoryCacheFactory(),
          TypePolicyCacheKeyGenerator.INSTANCE,
          FieldPolicyCacheResolver.INSTANCE,
          false
      );
        ApolloClient apolloClient = apolloClientBuilder.build();

        Rx2Apollo.rxSingle(apolloClient.query(new GetHelloQuery())).subscribe(
                response -> {
                    System.out.println(response.data.hello);
                },
                throwable -> {
                    System.out.println(throwable.getMessage());
                }
        );

        apolloClient.dispose();
    }
}
