package test;

import com.apollographql.apollo3.ApolloClient;
import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.api.CustomScalarAdapters;
import com.google.common.base.Optional;
import com.apollographql.apollo3.api.json.MapJsonWriter;
import com.apollographql.apollo3.mockserver.MockResponse;
import com.apollographql.apollo3.mockserver.MockServer;
import com.apollographql.apollo3.rx2.Rx2Apollo;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import optionals.guava.MyQuery;
import optionals.guava.type.MyInput;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static test.MapUtils.entry;
import static test.MapUtils.mapOf;

@SuppressWarnings("unchecked")
public class GuavaOptionalsTest {
  MockServer mockServer;
  ApolloClient apolloClient;

  @Before
  public void before() {
    mockServer = new MockServer();
    String url = (String) mockServer.url(new Continuation<String>() {
      @NotNull @Override public CoroutineContext getContext() {
        return EmptyCoroutineContext.INSTANCE;
      }

      @Override public void resumeWith(@NotNull Object o) {
      }
    });
    apolloClient = new ApolloClient.Builder().serverUrl(url).build();
  }

  @Test
  public void serializeVariablesPresent() throws Exception {
    MyQuery query = new MyQuery(
        /* nullableInt = */ Optional.of(Optional.of(0)),
        /* nonNullableInt = */ 1,
        /* nonNullableIntWithDefault = */ Optional.of(2),
        /* nullableInput = */ Optional.of(Optional.of(myInputPresent())),
        /* nonNullableInput = */ myInputPresent(),
        /* nonNullableInputWithDefault = */ Optional.of(myInputPresent())
    );
    MapJsonWriter mapJsonWriter = new MapJsonWriter();
    mapJsonWriter.beginObject();
    query.serializeVariables(mapJsonWriter, CustomScalarAdapters.Empty);
    mapJsonWriter.endObject();

    Map<String, Object> expectedJsonMap = mapOf(
        entry("nullableInt", 0),
        entry("nonNullableInt", 1),
        entry("nonNullableIntWithDefault", 2),
        entry("nullableInput", mapOf(
            entry("nullableInt", 3),
            entry("nonNullableInt", 4),
            entry("nonNullableIntWithDefault", 5)
        )),
        entry("nonNullableInput", mapOf(
            entry("nullableInt", 3),
            entry("nonNullableInt", 4),
            entry("nonNullableIntWithDefault", 5)
        )),
        entry("nonNullableInputWithDefault", mapOf(
            entry("nullableInt", 3),
            entry("nonNullableInt", 4),
            entry("nonNullableIntWithDefault", 5)
        ))
    );

    Map<String, Object> actualJsonMap = (Map<String, Object>) mapJsonWriter.root();
    Assert.assertEquals(expectedJsonMap, actualJsonMap);
  }

  @Test
  public void serializeVariablesAbsent() throws Exception {
    MyQuery query = new MyQuery(
        /* nullableInt = */ Optional.absent(),
        /* nonNullableInt = */ 1,
        /* nonNullableIntWithDefault = */ Optional.absent(),
        /* nullableInput = */ Optional.absent(),
        /* nonNullableInput = */ myInputOptionalAbsent(),
        /* nonNullableInputWithDefault = */ Optional.absent()
    );
    MapJsonWriter mapJsonWriter = new MapJsonWriter();
    mapJsonWriter.beginObject();
    query.serializeVariables(mapJsonWriter, CustomScalarAdapters.Empty);
    mapJsonWriter.endObject();

    Map<String, Object> expectedJsonMap = mapOf(
        entry("nonNullableInt", 1),
        entry("nonNullableInput", mapOf(
            entry("nonNullableInt", 4)
        ))
    );

    Map<String, Object> actualJsonMap = (Map<String, Object>) mapJsonWriter.root();
    Assert.assertEquals(expectedJsonMap, actualJsonMap);
  }

  @Test
  public void serializeVariablesMixed() throws Exception {
    MyQuery query = new MyQuery(
        /* nullableInt = */ Optional.of(Optional.absent()),
        /* nonNullableInt = */ 1,
        /* nonNullableIntWithDefault = */ Optional.of(2),
        /* nullableInput = */ Optional.of(Optional.absent()),
        /* nonNullableInput = */ myInputMixed(),
        /* nonNullableInputWithDefault = */ Optional.of(myInputMixed())
    );
    MapJsonWriter mapJsonWriter = new MapJsonWriter();
    mapJsonWriter.beginObject();
    query.serializeVariables(mapJsonWriter, CustomScalarAdapters.Empty);
    mapJsonWriter.endObject();

    Map<String, Object> expectedJsonMap = mapOf(
        entry("nullableInt", null),
        entry("nonNullableInt", 1),
        entry("nonNullableIntWithDefault", 2),
        entry("nullableInput", null),
        entry("nonNullableInput", mapOf(
            entry("nullableInt", null),
            entry("nonNullableInt", 4),
            entry("nonNullableIntWithDefault", 5)
        )),
        entry("nonNullableInputWithDefault", mapOf(
            entry("nullableInt", null),
            entry("nonNullableInt", 4),
            entry("nonNullableIntWithDefault", 5)
        ))
    );

    Map<String, Object> actualJsonMap = (Map<String, Object>) mapJsonWriter.root();
    Assert.assertEquals(expectedJsonMap, actualJsonMap);
  }

  @Test
  public void readResultAbsent() throws Exception {
    MyQuery query = new MyQuery(Optional.absent(), 0, Optional.absent(), Optional.absent(), myInputOptionalAbsent(), Optional.absent());
    mockServer.enqueue(new MockResponse.Builder().body("{\n" +
        "        \"data\": {\n" +
        "          \"nullableInt\": null,\n" +
        "          \"nonNullableInt\": 1,\n" +
        "          \"nullableMyType\": null,\n" +
        "          \"nonNullableMyType\": {\n" +
        "            \"nullableInt\": null,\n" +
        "            \"nonNullableInt\": 2\n" +
        "          }\n" +
        "        }\n" +
        "      }").build());
    ApolloResponse<MyQuery.Data> result = Rx2Apollo.single(apolloClient.query(query)).blockingGet();
    Assert.assertEquals(
        new MyQuery.Data(
            /* nullableInt = */ Optional.absent(),
            /* nonNullableInt = */ 1,
            /* nullableMyType = */ Optional.absent(),
            /* nonNullableMyType = */
            new MyQuery.NonNullableMyType(
                /* nullableInt = */ Optional.absent(),
                /* nonNullableInt = */ 2
            )
        ),
        result.dataAssertNoErrors()
    );

    mockServer.enqueue(new MockResponse.Builder().body("{\n" +
        "        \"data\": {\n" +
        "          \"nullableInt\": 0,\n" +
        "          \"nonNullableInt\": 1,\n" +
        "          \"nullableMyType\": {\n" +
        "            \"nullableInt\": 2,\n" +
        "            \"nonNullableInt\": 3\n" +
        "          },\n" +
        "          \"nonNullableMyType\": {\n" +
        "            \"nullableInt\": null,\n" +
        "            \"nonNullableInt\": 4\n" +
        "          }\n" +
        "        }\n" +
        "      }").build());
    result = Rx2Apollo.single(apolloClient.query(query)).blockingGet();
    Assert.assertEquals(
        new MyQuery.Data(
            /* nullableInt = */ Optional.of(0),
            /* nonNullableInt = */ 1,
            /* nullableMyType = */
            Optional.of(new MyQuery.NullableMyType(
                /* nullableInt = */ Optional.of(2),
                /* nonNullableInt = */ 3
            )),
            /* nonNullableMyType = */
            new MyQuery.NonNullableMyType(
                /* nullableInt = */ Optional.absent(),
                /* nonNullableInt = */ 4
            )
        ),
        result.dataAssertNoErrors()
    );

  }


  private static MyInput myInputPresent() {
    return new MyInput(
        /* nullableInt = */ Optional.of(Optional.of(3)),
        /* nonNullableInt = */ 4,
        /* nonNullableIntWithDefault = */ Optional.of(5)
    );
  }

  private static MyInput myInputOptionalAbsent() {
    return new MyInput(
        /* nullableInt = */ Optional.absent(),
        /* nonNullableInt = */ 4,
        /* nonNullableIntWithDefault = */ Optional.absent()
    );
  }

  private static MyInput myInputMixed() {
    return new MyInput(
        /* nullableInt = */ Optional.of(Optional.absent()),
        /* nonNullableInt = */ 4,
        /* nonNullableIntWithDefault = */ Optional.of(5)
    );
  }
}
