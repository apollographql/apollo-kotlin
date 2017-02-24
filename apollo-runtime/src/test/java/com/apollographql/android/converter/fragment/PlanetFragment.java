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

  private @Nullable List<String> climates;

  private @Nullable Double surfaceWater;

  public PlanetFragment(@Nullable String name, @Nullable List<String> climates,
      @Nullable Double surfaceWater) {
    this.name = name;
    this.climates = climates;
    this.surfaceWater = surfaceWater;
  }

  public @Nullable String name() {
    return this.name;
  }

  public @Nullable List<String> climates() {
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
      final String name = reader.read(fields[0]);
      final List<String> climates = reader.read(fields[1]);
      final Double surfaceWater = reader.read(fields[2]);
      return new PlanetFragment(name, climates, surfaceWater);
    }
  }
}
