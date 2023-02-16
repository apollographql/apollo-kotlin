package test;


import appsync.CommentsSubscription;
import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.runtime.java.ApolloCallback;
import com.apollographql.apollo3.runtime.java.ApolloClient;
import com.apollographql.apollo3.runtime.java.network.ws.protocol.AppSyncWsProtocol;
import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static test.Utils.sleep;

// Ignored because this test depends on a remote server.
@Ignore
public class AppSyncTest {
  @Test
  public void simple() {
    String apiKey = "CHANGEME";
    String host = "jsiypqtonfas3avgsqz7uargnm.appsync-api.eu-west-3.amazonaws.com";

    Map<String, Object> authorization = new HashMap<>();
    authorization.put("host", host);
    authorization.put("x-api-key", apiKey);

    String url = AppSyncWsProtocol.buildUrl(
        "https://jsiypqtonfas3avgsqz7uargnm.appsync-realtime-api.eu-west-3.amazonaws.com/graphql",
        authorization,
        new HashMap<>()
    );

    ApolloClient apolloClient = new ApolloClient.Builder()
        .serverUrl(url)
        .wsProtocolFactory(new AppSyncWsProtocol.Factory(authorization))
        .build();

    apolloClient.subscription(new CommentsSubscription()).enqueue(new ApolloCallback<CommentsSubscription.Data>() {
      @Override public void onResponse(@NotNull ApolloResponse<CommentsSubscription.Data> response) {
        System.out.println(response.data);
      }
    });

    sleep(100000);
  }
}
