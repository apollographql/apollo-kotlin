package com.example.app;

import com.apollographql.java.client.ApolloClient;
import com.apollographql.apollo3.api.Query;
import com.apollographql.java.rx2.Rx2Apollo;
import com.example.GetHelloQuery;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World! " + Query.class.getName());

        ApolloClient.Builder apolloClientBuilder = new ApolloClient.Builder()
                .serverUrl("https://example.com");

        ApolloClient apolloClient = apolloClientBuilder.build();

        Rx2Apollo.single(apolloClient.query(new GetHelloQuery())).subscribe(
            response -> {
              System.out.println(response.data.hello);
            },
            throwable -> {
              System.out.println(throwable.getMessage());
            }
        );

        apolloClient.close();
    }
}
