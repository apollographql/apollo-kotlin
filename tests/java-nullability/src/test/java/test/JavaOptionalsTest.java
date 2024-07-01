package test;

import com.apollographql.apollo.api.CustomScalarAdapters;
import com.apollographql.apollo.api.json.BufferedSourceJsonReader;
import com.apollographql.apollo.api.json.JsonReader;
import com.apollographql.apollo.api.json.MapJsonWriter;
import okio.Buffer;
import optionals.java.MyQuery;
import optionals.java.type.FindUserInput;
import optionals.java.type.MyInput;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;

import static test.MapUtils.entry;
import static test.MapUtils.mapOf;

@SuppressWarnings("unchecked")
public class JavaOptionalsTest {
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
    query.serializeVariables(mapJsonWriter, CustomScalarAdapters.Empty, false);
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
        /* nullableInt = */ Optional.empty(),
        /* nonNullableInt = */ 1,
        /* nonNullableIntWithDefault = */ Optional.empty(),
        /* nullableInput = */ Optional.empty(),
        /* nonNullableInput = */ myInputOptionalAbsent(),
        /* nonNullableInputWithDefault = */ Optional.empty()
    );
    MapJsonWriter mapJsonWriter = new MapJsonWriter();
    mapJsonWriter.beginObject();
    query.serializeVariables(mapJsonWriter, CustomScalarAdapters.Empty, false);
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
        /* nullableInt = */ Optional.of(Optional.empty()),
        /* nonNullableInt = */ 1,
        /* nonNullableIntWithDefault = */ Optional.of(2),
        /* nullableInput = */ Optional.of(Optional.empty()),
        /* nonNullableInput = */ myInputMixed(),
        /* nonNullableInputWithDefault = */ Optional.of(myInputMixed())
    );
    MapJsonWriter mapJsonWriter = new MapJsonWriter();
    mapJsonWriter.beginObject();
    query.serializeVariables(mapJsonWriter, CustomScalarAdapters.Empty, false);
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
  public void dataAdapter() throws Exception {
    MyQuery query = new MyQuery(Optional.empty(), 0, Optional.empty(), Optional.empty(), myInputOptionalAbsent(), Optional.empty());
    Buffer buffer = new Buffer();
    buffer.writeUtf8("{\n" +
        "          \"nullableInt\": null,\n" +
        "          \"nonNullableInt\": 1,\n" +
        "          \"nullableMyType\": null,\n" +
        "          \"nonNullableMyType\": {\n" +
        "            \"nullableInt\": null,\n" +
        "            \"nonNullableInt\": 2\n" +
        "          },\n" +
        "          \"nullableListOfNullableString\":  null,\n" +
        "          \"nullableListOfNonNullableString\": null,\n" +
        "          \"myUnion\": null,\n" +
        "          \"myInterface\": null\n" +
        "      }");
    JsonReader jsonReader = new BufferedSourceJsonReader(buffer);
    MyQuery.Data actualData = query.adapter().fromJson(jsonReader, CustomScalarAdapters.Empty);
    Assert.assertEquals(
        new MyQuery.Data(
            /* nullableInt = */ Optional.empty(),
            /* nonNullableInt = */ 1,
            /* nullableMyType = */ Optional.empty(),
            /* nonNullableMyType = */
            new MyQuery.NonNullableMyType(
                /* nullableInt = */ Optional.empty(),
                /* nonNullableInt = */ 2
            ),
            /* nullableListOfNullableString = */ Optional.empty(),
            /* nullableListOfNonNullableString = */ Optional.empty(),
            /* myUnion = */ Optional.empty(),
            /* myEnum = */ Optional.empty()
        ),
        actualData
    );

    buffer = new Buffer();
    buffer.writeUtf8("{\n" +
        "          \"nullableInt\": 0,\n" +
        "          \"nonNullableInt\": 1,\n" +
        "          \"nullableMyType\": {\n" +
        "            \"nullableInt\": 2,\n" +
        "            \"nonNullableInt\": 3\n" +
        "          },\n" +
        "          \"nonNullableMyType\": {\n" +
        "            \"nullableInt\": null,\n" +
        "            \"nonNullableInt\": 4\n" +
        "          },\n" +
        "          \"nullableListOfNullableString\":  null,\n" +
        "          \"nullableListOfNonNullableString\": null,\n" +
        "          \"myUnion\": null,\n" +
        "          \"myInterface\": null\n" +
        "      }");
    jsonReader = new BufferedSourceJsonReader(buffer);
    actualData = query.adapter().fromJson(jsonReader, CustomScalarAdapters.Empty);
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
                /* nullableInt = */ Optional.empty(),
                /* nonNullableInt = */ 4
            ),
            /* nullableListOfNullableString = */ Optional.empty(),
            /* nullableListOfNonNullableString = */ Optional.empty(),
            /* myUnion = */ Optional.empty(),
            /* myEnum = */ Optional.empty()
        ),
        actualData
    );

    buffer.writeUtf8("{\n" +
        "          \"nullableInt\": null,\n" +
        "          \"nonNullableInt\": 1,\n" +
        "          \"nullableMyType\": null,\n" +
        "          \"nonNullableMyType\": {\n" +
        "            \"nullableInt\": null,\n" +
        "            \"nonNullableInt\": 2\n" +
        "          },\n" +
        "          \"nullableListOfNullableString\":  null,\n" +
        "          \"nullableListOfNonNullableString\": null,\n" +
        "          \"myUnion\": {\n" +
        "            \"__typename\": \"A\",\n" +
        "            \"a\": \"a\"\n" +
        "          },\n" +
        "          \"myInterface\": {\n" +
        "            \"__typename\": \"C\",\n" +
        "            \"x\": 3\n" +
        "          }\n" +
        "      }");
    jsonReader = new BufferedSourceJsonReader(buffer);
    actualData = query.adapter().fromJson(jsonReader, CustomScalarAdapters.Empty);
    Assert.assertEquals(
        new MyQuery.Data(
            /* nullableInt = */ Optional.empty(),
            /* nonNullableInt = */ 1,
            /* nullableMyType = */ Optional.empty(),
            /* nonNullableMyType = */
            new MyQuery.NonNullableMyType(
                /* nullableInt = */ Optional.empty(),
                /* nonNullableInt = */ 2
            ),
            /* nullableListOfNullableString = */ Optional.empty(),
            /* nullableListOfNonNullableString = */ Optional.empty(),
            /* myUnion = */ Optional.of(new MyQuery.MyUnion("A", Optional.of(new MyQuery.OnA(Optional.of("a"))), Optional.empty())),
            /* myInterface = */ Optional.of(new MyQuery.MyInterface("C", Optional.of(new MyQuery.OnC(Optional.of(3)))))
        ),
        actualData
    );
  }

  @Test
  public void oneOfWithConstructor() {
    FindUserInput findUserInput = new FindUserInput(
        /* email = */ Optional.of(Optional.of("test@example.com")),
        /* name = */ Optional.empty(),
        /* identity = */ Optional.empty(),
        /* friends = */ Optional.empty()
    );

    try {
      new FindUserInput(
          /* email = */ Optional.of(Optional.of("test@example.com")),
          /* name = */ Optional.of(Optional.of("Test User")),
          /* identity = */ Optional.empty(),
          /* friends = */ Optional.empty()
      );
      Assert.fail();
    } catch (IllegalArgumentException e) {
      Assert.assertEquals("@oneOf input must have one field set (got 2)", e.getMessage());
    }

    try {
      new FindUserInput(
          /* email = */ Optional.empty(),
          /* name = */ Optional.empty(),
          /* identity = */ Optional.empty(),
          /* friends = */ Optional.empty()
      );
      Assert.fail();
    } catch (IllegalArgumentException e) {
      Assert.assertEquals("@oneOf input must have one field set (got 0)", e.getMessage());
    }

    try {
      new FindUserInput(
          /* email = */ Optional.of(Optional.empty()),
          /* name = */ Optional.empty(),
          /* identity = */ Optional.empty(),
          /* friends = */ Optional.empty()
      );
      Assert.fail();
    } catch (IllegalArgumentException e) {
      Assert.assertEquals("The value set on @oneOf input field must be non-null", e.getMessage());
    }

    try {
      new FindUserInput(
          /* email = */ Optional.of(Optional.empty()),
          /* name = */ Optional.of(Optional.of("Test User")),
          /* identity = */ Optional.empty(),
          /* friends = */ Optional.empty()
      );
      Assert.fail();
    } catch (IllegalArgumentException e) {
      Assert.assertEquals("@oneOf input must have one field set (got 2)", e.getMessage());
    }
  }

  @Test
  public void oneOfWithBuilder() {
    FindUserInput.builder()
        .email(Optional.of("test@example.com"))
        .build();

    try {
      FindUserInput.builder()
          .email(Optional.of("test@example.com"))
          .name(Optional.of("Test User"))
          .build();
      Assert.fail();
    } catch (IllegalArgumentException e) {
      Assert.assertEquals("@oneOf input must have one field set (got 2)", e.getMessage());
    }

    try {
      FindUserInput.builder()
          .build();
      Assert.fail();
    } catch (IllegalArgumentException e) {
      Assert.assertEquals("@oneOf input must have one field set (got 0)", e.getMessage());
    }

    try {
      FindUserInput.builder()
          .email(Optional.empty())
          .build();
      Assert.fail();
    } catch (IllegalArgumentException e) {
      Assert.assertEquals("The value set on @oneOf input field must be non-null", e.getMessage());
    }

    try {
      FindUserInput.builder()
          .email(Optional.empty())
          .name(Optional.of("Test User"))
          .build();
      Assert.fail();
    } catch (IllegalArgumentException e) {
      Assert.assertEquals("@oneOf input must have one field set (got 2)", e.getMessage());
    }
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
        /* nullableInt = */ Optional.empty(),
        /* nonNullableInt = */ 4,
        /* nonNullableIntWithDefault = */ Optional.empty()
    );
  }

  private static MyInput myInputMixed() {
    return new MyInput(
        /* nullableInt = */ Optional.of(Optional.empty()),
        /* nonNullableInt = */ 4,
        /* nonNullableIntWithDefault = */ Optional.of(5)
    );
  }
}
