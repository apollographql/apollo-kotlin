package test;

import annotations.jetbrains.MyQuery;
import annotations.jetbrains.type.MyInput;
import com.apollographql.apollo.api.CustomScalarAdapters;
import com.apollographql.apollo.api.Optional;
import com.apollographql.apollo.api.json.BufferedSourceJsonReader;
import com.apollographql.apollo.api.json.JsonReader;
import com.apollographql.apollo.api.json.MapJsonWriter;
import okio.Buffer;
import annotations.jetbrains.type.FindUserInput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static test.MapUtils.entry;
import static test.MapUtils.mapOf;

@SuppressWarnings("unchecked")
public class JetbrainsAnnotationsTest {
  @Test
  public void serializeVariablesPresent() throws Exception {
    MyQuery query = MyQuery.builder()
        .nullableInt(0)
        .nonNullableInt(1)
        .nonNullableIntWithDefault(2)
        .nullableInput(myInputPresent())
        .nonNullableInput(myInputPresent())
        .nonNullableInputWithDefault(myInputPresent())
        .build();
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
    MyQuery query = MyQuery.builder()
        .nonNullableInt(1)
        .nonNullableInput(myInputOptionalAbsent())
        .build();
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
    MyQuery query = MyQuery.builder()
        .nullableInt(null)
        .nonNullableInt(1)
        .nonNullableIntWithDefault(2)
        .nullableInput(null)
        .nonNullableInput(myInputMixed())
        .nonNullableInputWithDefault(myInputMixed())
        .build();
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
    MyQuery query = new MyQuery(Optional.absent(), 0, Optional.absent(), Optional.absent(), myInputOptionalAbsent(), Optional.absent());
    Buffer buffer = new Buffer();
    buffer.writeUtf8("{\n" +
        "          \"nullableInt\": null,\n" +
        "          \"nonNullableInt\": 1,\n" +
        "          \"nullableMyType\": null,\n" +
        "          \"nonNullableMyType\": {\n" +
        "            \"nullableInt\": null,\n" +
        "            \"nonNullableInt\": 2\n" +
        "          }\n" +
        "      }");
    JsonReader jsonReader = new BufferedSourceJsonReader(buffer);
    MyQuery.Data actualData = query.adapter().fromJson(jsonReader, CustomScalarAdapters.Empty);
    Assert.assertEquals(
        new MyQuery.Data(
            /* nullableInt = */ null,
            /* nonNullableInt = */ 1,
            /* nullableMyType = */ null,
            /* nonNullableMyType = */
            new MyQuery.NonNullableMyType(
                /* nullableInt = */ null,
                /* nonNullableInt = */ 2
            ),
            /* nullableListOfNullableString = */ null,
            /* nullableListOfNonNullableString = */ null,
            /* myUnion = */ null,
            /* myEnum = */ null
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
        "          }\n" +
        "      }");
    jsonReader = new BufferedSourceJsonReader(buffer);
    actualData = query.adapter().fromJson(jsonReader, CustomScalarAdapters.Empty);
    Assert.assertEquals(
        new MyQuery.Data(
            /* nullableInt = */ 0,
            /* nonNullableInt = */ 1,
            /* nullableMyType = */
            new MyQuery.NullableMyType(
                /* nullableInt = */ 2,
                /* nonNullableInt = */ 3
            ),
            /* nonNullableMyType = */
            new MyQuery.NonNullableMyType(
                /* nullableInt = */ null,
                /* nonNullableInt = */ 4
            ),
            /* nullableListOfNullableString = */ null,
            /* nullableListOfNonNullableString = */ null,
            /* myUnion = */ null,
            /* myEnum = */ null
        ),
        actualData
    );
  }

  /**
   * Not a test, but demonstrates annotations on the generated code by displaying warnings in the IDE.
   */
  private void annotationWarnings() {
    List<@Nullable String> listOfNullableString = new ArrayList<>();
    List<@NotNull String> listOfNotNullString = new ArrayList<>();
    MyQuery.Data data = MyQuery.Data.builder()
        .nullableInt(null)
        .nonNullableInt(null) // warning
        .nullableMyType(null)
        .nonNullableMyType(null) // warning
        .nullableListOfNullableString(listOfNotNullString)
        .nullableListOfNonNullableString(listOfNullableString) // warning
        .build();

    Optional<@Nullable Integer> optionalOfNullableInteger = Optional.present(null);
    Optional<@Nullable MyInput> optionalOfNullableMyInput = Optional.present(null);
    MyQuery query = new MyQuery(
        optionalOfNullableInteger,
        0,
        optionalOfNullableInteger,
        optionalOfNullableMyInput,
        myInputPresent(),
        optionalOfNullableMyInput
    );
  }

  @Test
  public void oneOfWithConstructor() {
    FindUserInput findUserInput = new FindUserInput(
        /* email = */ Optional.present("test@example.com"),
        /* name = */ Optional.absent(),
        /* identity = */ Optional.absent(),
        /* friends = */ Optional.absent()
    );

    try {
      new FindUserInput(
          /* email = */ Optional.present("test@example.com"),
          /* name = */ Optional.present("Test User"),
          /* identity = */ Optional.absent(),
          /* friends = */ Optional.absent()
      );
      Assert.fail();
    } catch (IllegalArgumentException e) {
      Assert.assertEquals("@oneOf input must have one field set (got 2)", e.getMessage());
    }

    try {
      new FindUserInput(
          /* email = */ Optional.absent(),
          /* name = */ Optional.absent(),
          /* identity = */ Optional.absent(),
          /* friends = */ Optional.absent()
      );
      Assert.fail();
    } catch (IllegalArgumentException e) {
      Assert.assertEquals("@oneOf input must have one field set (got 0)", e.getMessage());
    }

    try {
      new FindUserInput(
          /* email = */ Optional.present(null),
          /* name = */ Optional.absent(),
          /* identity = */ Optional.absent(),
          /* friends = */ Optional.absent()
      );
      Assert.fail();
    } catch (IllegalArgumentException e) {
      Assert.assertEquals("The value set on @oneOf input field must be non-null", e.getMessage());
    }

    try {
      new FindUserInput(
          /* email = */ Optional.present(null),
          /* name = */ Optional.present("Test User"),
          /* identity = */ Optional.absent(),
          /* friends = */ Optional.absent()
      );
      Assert.fail();
    } catch (IllegalArgumentException e) {
      Assert.assertEquals("@oneOf input must have one field set (got 2)", e.getMessage());
    }
  }

  @Test
  public void oneOfWithBuilder() {
    FindUserInput.builder()
        .email("test@example.com")
        .build();

    try {
      FindUserInput.builder()
          .email("test@example.com")
          .name("Test User")
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
          .email(null)
          .build();
      Assert.fail();
    } catch (IllegalArgumentException e) {
      Assert.assertEquals("The value set on @oneOf input field must be non-null", e.getMessage());
    }

    try {
      FindUserInput.builder()
          .email(null)
          .name("Test User")
          .build();
      Assert.fail();
    } catch (IllegalArgumentException e) {
      Assert.assertEquals("@oneOf input must have one field set (got 2)", e.getMessage());
    }
  }

  private static MyInput myInputPresent() {
    return MyInput.builder()
        .nullableInt(3)
        .nonNullableInt(4)
        .nonNullableIntWithDefault(5)
        .build();
  }

  private static MyInput myInputOptionalAbsent() {
    return MyInput.builder()
        .nonNullableInt(4)
        .build();
  }

  private static MyInput myInputMixed() {
    return MyInput.builder()
        .nullableInt(null)
        .nonNullableInt(4)
        .nonNullableIntWithDefault(5)
        .build();
  }
}
