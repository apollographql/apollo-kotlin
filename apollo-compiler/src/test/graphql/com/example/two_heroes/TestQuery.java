package com.example.two_heroes;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import com.apollographql.android.api.graphql.internal.Optional;
import com.apollographql.android.api.graphql.util.UnmodifiableMapBuilder;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<TestQuery.Data, Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery {\n"
      + "  r2: hero {\n"
      + "    __typename\n"
      + "    name\n"
      + "  }\n"
      + "  luke: hero(episode: EMPIRE) {\n"
      + "    __typename\n"
      + "    name\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private final Operation.Variables variables;

  public TestQuery() {
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
    private final Optional<R2> r2;

    private final Optional<Luke> luke;

    public Data(@Nullable R2 r2, @Nullable Luke luke) {
      this.r2 = Optional.fromNullable(r2);
      this.luke = Optional.fromNullable(luke);
    }

    public Optional<R2> r2() {
      return this.r2;
    }

    public Optional<Luke> luke() {
      return this.luke;
    }

    @Override
    public String toString() {
      return "Data{"
        + "r2=" + r2 + ", "
        + "luke=" + luke
        + "}";
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Data) {
        Data that = (Data) o;
        return ((this.r2 == null) ? (that.r2 == null) : this.r2.equals(that.r2))
         && ((this.luke == null) ? (that.luke == null) : this.luke.equals(that.luke));
      }
      return false;
    }

    @Override
    public int hashCode() {
      int h = 1;
      h *= 1000003;
      h ^= (r2 == null) ? 0 : r2.hashCode();
      h *= 1000003;
      h ^= (luke == null) ? 0 : luke.hashCode();
      return h;
    }

    public static class R2 {
      private final @Nonnull String name;

      public R2(@Nonnull String name) {
        this.name = name;
      }

      public @Nonnull String name() {
        return this.name;
      }

      @Override
      public String toString() {
        return "R2{"
          + "name=" + name
          + "}";
      }

      @Override
      public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof R2) {
          R2 that = (R2) o;
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

      public static final class Mapper implements ResponseFieldMapper<R2> {
        final Field[] fields = {
          Field.forString("name", "name", null, false)
        };

        @Override
        public R2 map(ResponseReader reader) throws IOException {
          final String name = reader.read(fields[0]);
          return new R2(name);
        }
      }
    }

    public static class Luke {
      private final @Nonnull String name;

      public Luke(@Nonnull String name) {
        this.name = name;
      }

      public @Nonnull String name() {
        return this.name;
      }

      @Override
      public String toString() {
        return "Luke{"
          + "name=" + name
          + "}";
      }

      @Override
      public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof Luke) {
          Luke that = (Luke) o;
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

      public static final class Mapper implements ResponseFieldMapper<Luke> {
        final Field[] fields = {
          Field.forString("name", "name", null, false)
        };

        @Override
        public Luke map(ResponseReader reader) throws IOException {
          final String name = reader.read(fields[0]);
          return new Luke(name);
        }
      }
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final R2.Mapper r2FieldMapper = new R2.Mapper();

      final Luke.Mapper lukeFieldMapper = new Luke.Mapper();

      final Field[] fields = {
        Field.forObject("r2", "hero", null, true, new Field.ObjectReader<R2>() {
          @Override public R2 read(final ResponseReader reader) throws IOException {
            return r2FieldMapper.map(reader);
          }
        }),
        Field.forObject("luke", "hero", new UnmodifiableMapBuilder<String, Object>(1)
          .put("episode", "EMPIRE")
        .build(), true, new Field.ObjectReader<Luke>() {
          @Override public Luke read(final ResponseReader reader) throws IOException {
            return lukeFieldMapper.map(reader);
          }
        })
      };

      @Override
      public Data map(ResponseReader reader) throws IOException {
        final R2 r2 = reader.read(fields[0]);
        final Luke luke = reader.read(fields[1]);
        return new Data(r2, luke);
      }
    }
  }
}
