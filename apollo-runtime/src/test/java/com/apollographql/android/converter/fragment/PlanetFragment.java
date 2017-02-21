package com.apollographql.android.converter.fragment;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;

public class PlanetFragment {
  public static final String FRAGMENT_DEFINITION = "fragment PlanetFragment on Planet {\n"
      + "  name\n"
      + "  climates\n"
      + "  surfaceWater\n"
      + "}";

  public static final String TYPE_CONDITION = "Planet";

  private @Nullable String name;

  private @Nullable List<? extends String> climates;

  private @Nullable Double surfaceWater;

  public PlanetFragment(@Nullable String name, @Nullable List<? extends String> climates,
      @Nullable Double surfaceWater) {
    this.name = name;
    this.climates = climates;
    this.surfaceWater = surfaceWater;
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

  public static final class Mapper implements ResponseFieldMapper<PlanetFragment> {
    final Field[] fields = {
      Field.forString("name", "name", null, true),
      Field.forList("climates", "climates", null, true, new Field.ListReader<String>() {
        @Override public String read(final Field.ListItemReader reader) throws IOException {
          return reader.readString();
        }
      }),
      Field.forDouble("surfaceWater", "surfaceWater", null, true)
    };

    @Override
    public PlanetFragment map(ResponseReader reader) throws IOException {
      final __ContentValues contentValues = new __ContentValues();
      reader.read(new ResponseReader.ValueHandler() {
        @Override
        public void handle(final int fieldIndex, final Object value) throws IOException {
          switch (fieldIndex) {
            case 0: {
              contentValues.name = (String) value;
              break;
            }
            case 1: {
              contentValues.climates = (List<? extends String>) value;
              break;
            }
            case 2: {
              contentValues.surfaceWater = (Double) value;
              break;
            }
          }
        }
      }, fields);
      return new PlanetFragment(contentValues.name, contentValues.climates, contentValues.surfaceWater);
    }

    static final class __ContentValues {
      String name;

      List<? extends String> climates;

      Double surfaceWater;
    }
  }
}
