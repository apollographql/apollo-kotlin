package test;

import com.apollographql.apollo3.runtime.java.internal.ws.WebSocketConnection;
import okhttp3.OkHttpClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

import static test.MapUtils.entry;
import static test.MapUtils.mapOf;

public class WebSocketTest {
  private static ConfigurableApplicationContext context;

  @BeforeClass
  public static void beforeClass() {
//    context = SpringApplication.run(DefaultApplication.class);
  }

  @AfterClass
  public static void afterClass() {
//    context.close();
  }

  @Test
  public void test() {
    Map<String, String> headers = mapOf(entry("Sec-WebSocket-Protocol", "graphql-transport-ws"));
//    WebSocketConnection webSocketConnection = new WebSocketConnection(new OkHttpClient(), "http://localhost:8080/subscriptions", headers);
    WebSocketConnection webSocketConnection = new WebSocketConnection(new OkHttpClient(), "http://localhost:4000/graphql", headers);
    boolean isOpen = webSocketConnection.open();
    System.out.println("isOpen = " + isOpen);
    webSocketConnection.send("{\"type\":\"connection_init\"}");
    String rcv = webSocketConnection.receiveMessage();
    System.out.println("rcv = " + rcv);

    webSocketConnection.send("{\"type\":\"ping\"}");
    rcv = webSocketConnection.receiveMessage();
    System.out.println("rcv = " + rcv);
  }
}
