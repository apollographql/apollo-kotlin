package com.apollographql.android.converter.fragment;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import java.io.IOException;
import java.lang.Double;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public class PlanetFragment {
  public static final Creator CREATOR = new Creator() {
    @Override
    public PlanetFragment create(@Nullable String name, @Nullable List<? extends String> climates,
        @Nullable Double surfaceWater) {
      return new PlanetFragment(name, climates, surfaceWater);
    }
  };

  public static final Factory FACTORY = new Factory() {
    @Override
    public Creator creator() {
      return CREATOR;
    }
  };

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

  public interface Factory {
    Creator creator();
  }

  public interface Creator {
    PlanetFragment create(@Nullable String name, @Nullable List<? extends String> climates,
        @Nullable Double surfaceWater);
  }

  public static final class Mapper implements ResponseFieldMapper<PlanetFragment> {
    final Factory factory;

    final Field[] fields = {
      Field.forString("name", "name", null, true),
      Field.forList("climates", "climates", null, true, new Field.ListReader<String>() {
        @Override public String read(final Field.ListItemReader reader) throws IOException {
          return reader.readString();
        }
      }),
      Field.forDouble("surfaceWater", "surfaceWater", null, true)
    };

    public Mapper(@Nonnull Factory factory) {
      this.factory = factory;
    }

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
      return factory.creator().create(contentValues.name, contentValues.climates, contentValues.surfaceWater);
    }

    static final class __ContentValues {
      String name;

      List<? extends String> climates;

      Double surfaceWater;
    }
  }
}
