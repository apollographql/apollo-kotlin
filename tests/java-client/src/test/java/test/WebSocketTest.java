package test;

import com.apollographql.apollo.sample.server.DefaultApplication;
import com.apollographql.apollo3.api.ApolloRequest;
import com.apollographql.apollo3.api.CustomScalarAdapters;
import com.apollographql.apollo3.runtime.java.DefaultApolloDisposable;
import com.apollographql.apollo3.runtime.java.internal.ws.ApolloWsProtocol;
import com.apollographql.apollo3.runtime.java.internal.ws.WebSocketNetworkTransport;
import javatest.CountSubscription;
import okhttp3.OkHttpClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Collections;

public class WebSocketTest {
  private static ConfigurableApplicationContext context;

  @BeforeClass
  public static void beforeClass() {
    context = SpringApplication.run(DefaultApplication.class);
  }

  @AfterClass
  public static void afterClass() {
    context.close();
  }

//  @Test
//  public void testWebSocketConnection() {
//    Map<String, String> headers = mapOf(entry("Sec-WebSocket-Protocol", "graphql-transport-ws"));
//    WebSocketConnection webSocketConnection = new WebSocketConnection(new OkHttpClient(), "http://localhost:4000/graphql", headers);
//    boolean isOpen = webSocketConnection.open();
//    System.out.println("isOpen = " + isOpen);
//    webSocketConnection.send("{\"type\":\"connection_init\"}");
//    String rcv = webSocketConnection.receive();
//    System.out.println("rcv = " + rcv);
//
//    webSocketConnection.send("{\"type\":\"ping\"}");
//    rcv = webSocketConnection.receive();
//    System.out.println("rcv = " + rcv);
//
//    webSocketConnection.close();
//  }

  @Test
  public void testWebSocketNetworkTransport() {
    WebSocketNetworkTransport webSocketNetworkTransport = new WebSocketNetworkTransport(
        new OkHttpClient(),
        new ApolloWsProtocol.Factory(),
        "http://localhost:8080/subscriptions",
        Collections.emptyList()
    );

    ApolloRequest<CountSubscription.Data> request = new ApolloRequest.Builder<>(new CountSubscription(5, 0)).addExecutionContext(CustomScalarAdapters.Empty).build();


    webSocketNetworkTransport.execute(request, null, new DefaultApolloDisposable());
  }
}
