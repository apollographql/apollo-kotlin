package com.apollostack.converter.pojo;

import com.apollostack.api.graphql.Field;
import com.apollostack.api.graphql.ResponseReader;
import java.io.IOException;
import java.lang.Double;
import java.lang.String;
import java.util.List;
import javax.annotation.Nullable;

public class PlanetFargment {
  public static final String FRAGMENT_DEFINITION = "fragment PlanetFargment on Planet {\n"
      + "  name\n"
      + "  climates\n"
      + "  surfaceWater\n"
      + "}";

  public static final String TYPE_CONDITION = "Planet";

  private @Nullable String name;

  private @Nullable List<? extends String> climates;

  private @Nullable Double surfaceWater;

  public PlanetFargment(ResponseReader reader) throws IOException {
    reader.toBufferedReader().read(
      new ResponseReader.ValueHandler() {
        @Override public void handle(int fieldIndex__, Object value__) throws IOException {
          switch (fieldIndex__) {
            case 0: {
              name = (String) value__;
              break;
            }
            case 1: {
              climates = (List<? extends String>) value__;
              break;
            }
            case 2: {
              surfaceWater = (Double) value__;
              break;
            }
          }
        }
      },
      Field.forString("name", "name", null, true),
      Field.forList("climates", "climates", null, true, new Field.ListReader<String>() {
        @Override public String read(Field.ListItemReader reader) throws IOException {
          return reader.readString();
        }
      }),
      Field.forDouble("surfaceWater", "surfaceWater", null, true)
    );
  }

  public @Nullable String name() {
    return this.name;
  }

  public @Nullable List<? extends String> climates() {
    return this.climates;
  }

  public @Nullable Double surfaceWater() {
    return this.surfaceWater;
  }
}
