package com.apollostack.converter.pojo;

import com.apollostack.api.graphql.Field;
import com.apollostack.api.graphql.ResponseFieldMapper;
import com.apollostack.api.graphql.ResponseReader;
import java.io.IOException;
import java.lang.Double;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import javax.annotation.Nullable;

public class PlanetFargment {
  private static final ResponseFieldMapper<PlanetFargment> MAPPER = new ResponseFieldMapper<PlanetFargment>() {
    private final Field[] FIELDS = {
      Field.forString("name", "name", null, true),
      Field.forList("climates", "climates", null, true, new Field.ListReader<String>() {
        @Override public String read(final Field.ListItemReader reader) throws IOException {
          return reader.readString();
        }
      }),
      Field.forDouble("surfaceWater", "surfaceWater", null, true)
    };

    @Override
    public void map(final ResponseReader reader, final PlanetFargment instance) throws IOException {
      reader.read(new ResponseReader.ValueHandler() {
        @Override
        public void handle(final int fieldIndex, final Object value) throws IOException {
          switch (fieldIndex) {
            case 0: {
              instance.name = (String) value;
              break;
            }
            case 1: {
              instance.climates = (List<? extends String>) value;
              break;
            }
            case 2: {
              instance.surfaceWater = (Double) value;
              break;
            }
          }
        }
      }, FIELDS);
    }
  };

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
    MAPPER.map(reader, this);
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
