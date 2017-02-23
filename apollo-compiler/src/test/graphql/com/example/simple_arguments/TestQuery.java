package com.example.simple_arguments;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import com.example.simple_arguments.type.Episode;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Generated;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<TestQuery.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery($episode: Episode, $includeName: Boolean!) {\n"
      + "  hero(episode: $episode) {\n"
      + "    __typename\n"
      + "    name @include(if: $includeName)\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private final TestQuery.Variables variables;

  public TestQuery(TestQuery.Variables variables) {
    this.variables = variables;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public TestQuery.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<? extends Operation.Data> responseFieldMapper() {
    return new Data.Mapper();
  }

  public static final class Variables extends Operation.Variables {
    private final @Nullable Episode episode;

    private final boolean includeName;

    private final Map<String, Object> valueMap = new LinkedHashMap<>();

    Variables(@Nullable Episode episode, boolean includeName) {
      this.episode = episode;
      this.includeName = includeName;
      this.valueMap.put("episode", episode);
      this.valueMap.put("includeName", includeName);
    }

    public @Nullable Episode episode() {
      return episode;
    }

    public boolean includeName() {
      return includeName;
    }

    @Override
    public Map<String, Object> valueMap() {
      return Collections.unmodifiableMap(valueMap);
    }

    public static Builder builder() {
      return new Builder();
    }

    public static final class Builder {
      private @Nullable Episode episode;

      private boolean includeName;

      Builder() {
      }

      public Builder episode(@Nullable Episode episode) {
        this.episode = episode;
        return this;
      }

      public Builder includeName(boolean includeName) {
        this.includeName = includeName;
        return this;
      }

      public Variables build() {
        return new Variables(episode, includeName);
      }
    }
  }

  public static class Data implements Operation.Data {
    private final @Nullable Hero hero;

    public Data(@Nullable Hero hero) {
      this.hero = hero;
    }

    public @Nullable Hero hero() {
      return this.hero;
    }

    @Override
    public String toString() {
      return "Data{"
        + "hero=" + hero
        + "}";
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Data) {
        Data that = (Data) o;
        return ((this.hero == null) ? (that.hero == null) : this.hero.equals(that.hero));
      }
      return false;
    }

    @Override
    public int hashCode() {
      int h = 1;
      h *= 1000003;
      h ^= (hero == null) ? 0 : hero.hashCode();
      return h;
    }

    public static class Hero {
      private final @Nullable String name;

      public Hero(@Nullable String name) {
        this.name = name;
      }

      public @Nullable String name() {
        return this.name;
      }

      @Override
      public String toString() {
        return "Hero{"
          + "name=" + name
          + "}";
      }

      @Override
      public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof Hero) {
          Hero that = (Hero) o;
          return ((this.name == null) ? (that.name == null) : this.name.equals(that.name));
        }
        return false;
      }

      @Override
      public int hashCode() {
        int h = 1;
        h *= 1000003;
        h ^= (name == null) ? 0 : name.hashCode();
        return h;
      }

      public static final class Mapper implements ResponseFieldMapper<Hero> {
        final Field[] fields = {
          Field.forString("name", "name", null, true)
        };

        @Override
        public Hero map(ResponseReader reader) throws IOException {
          final __ContentValues contentValues = new __ContentValues();
          reader.read(new ResponseReader.ValueHandler() {
            @Override
            public void handle(final int fieldIndex, final Object value) throws IOException {
              switch (fieldIndex) {
                case 0: {
                  contentValues.name = (String) value;
                  break;
                }
              }
            }
          }, fields);
          return new Hero(contentValues.name);
        }

        static final class __ContentValues {
          String name;
        }
      }
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final Field[] fields = {
        Field.forObject("hero", "hero", null, true, new Field.ObjectReader<Hero>() {
          @Override public Hero read(final ResponseReader reader) throws IOException {
            return new Hero.Mapper().map(reader);
          }
        })
      };

      @Override
      public Data map(ResponseReader reader) throws IOException {
        final __ContentValues contentValues = new __ContentValues();
        reader.read(new ResponseReader.ValueHandler() {
          @Override
          public void handle(final int fieldIndex, final Object value) throws IOException {
            switch (fieldIndex) {
              case 0: {
                contentValues.hero = (Hero) value;
                break;
              }
            }
          }
        }, fields);
        return new Data(contentValues.hero);
      }

      static final class __ContentValues {
        Hero hero;
      }
    }
  }
}
