package com.starwars.api.starship;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import com.apollographql.android.api.graphql.util.UnmodifiableMapBuilder;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class Starship implements Query<Starship.Data, Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query Starship {\n"
      + "  starship(id: 3000) {\n"
      + "    __typename\n"
      + "    name\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private final Operation.Variables variables;

  public Starship() {
    this.variables = Operation.EMPTY_VARIABLES;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public Operation.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<? extends Operation.Data> responseFieldMapper() {
    return new Data.Mapper();
  }

  public static class Data implements Operation.Data {
    private final @Nullable Starship1 starship;

    public Data(@Nullable Starship1 starship) {
      this.starship = starship;
    }

    public @Nullable Starship1 starship() {
      return this.starship;
    }

    @Override
    public String toString() {
      return "Data{"
        + "starship=" + starship
        + "}";
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Data) {
        Data that = (Data) o;
        return ((this.starship == null) ? (that.starship == null) : this.starship.equals(that.starship));
      }
      return false;
    }

    @Override
    public int hashCode() {
      int h = 1;
      h *= 1000003;
      h ^= (starship == null) ? 0 : starship.hashCode();
      return h;
    }

    public static class Starship1 {
      private final @Nonnull String name;

      public Starship1(@Nonnull String name) {
        this.name = name;
      }

      public @Nonnull String name() {
        return this.name;
      }

      @Override
      public String toString() {
        return "Starship1{"
          + "name=" + name
          + "}";
      }

      @Override
      public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof Starship1) {
          Starship1 that = (Starship1) o;
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

      public static final class Mapper implements ResponseFieldMapper<Starship1> {
        final Field[] fields = {
          Field.forString("name", "name", null, false)
        };

        @Override
        public Starship1 map(ResponseReader reader) throws IOException {
          final String name = reader.read(fields[0]);
          return new Starship1(name);
        }
      }
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final Starship1.Mapper starship1FieldMapper = new Starship1.Mapper();

      final Field[] fields = {
        Field.forObject("starship", "starship", new UnmodifiableMapBuilder<String, Object>(1)
          .put("id", "3000.0")
        .build(), true, new Field.ObjectReader<Starship1>() {
          @Override public Starship1 read(final ResponseReader reader) throws IOException {
            return starship1FieldMapper.map(reader);
          }
        })
      };

      @Override
      public Data map(ResponseReader reader) throws IOException {
        final Starship1 starship = reader.read(fields[0]);
        return new Data(starship);
      }
    }
  }
}
