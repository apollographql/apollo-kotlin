package com.apollographql.converter.pojo;

import com.apollographql.api.graphql.Field;
import com.apollographql.api.graphql.ResponseFieldMapper;
import com.apollographql.api.graphql.ResponseReader;
import java.io.IOException;
import java.util.List;
import javax.annotation.Nullable;

public class PlanetFragment {
  private static final ResponseFieldMapper<PlanetFragment> MAPPER = new ResponseFieldMapper<PlanetFragment>() {
    private final Field[] FIELDS = {
      Field.forString("name", "name", null, true),
      Field.forList("climates", "climates", null, true, new Field.ListReader<String>() {
        @Override public String read(final Field.ListItemReader reader) throws IOException {
          return reader.readString();
        }
      }),
      Field.forDouble("surfaceWater", "surfaceWater", null, true)
    };

    @Override public void map(final ResponseReader reader, final PlanetFragment instance) throws IOException {
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

  public static final String FRAGMENT_DEFINITION = "fragment PlanetFragment on Planet {\n"
      + "  name\n"
      + "  climates\n"
      + "  surfaceWater\n"
      + "}";

  public static final String TYPE_CONDITION = "Planet";

  @Nullable private String name;

  @Nullable private List<? extends String> climates;

  @Nullable private Double surfaceWater;

  public PlanetFragment(ResponseReader reader) throws IOException {
    MAPPER.map(reader, this);
  }

  @Nullable public String name() {
    return this.name;
  }

  @Nullable public List<? extends String> climates() {
    return this.climates;
  }

  @Nullable public Double surfaceWater() {
    return this.surfaceWater;
  }
}
