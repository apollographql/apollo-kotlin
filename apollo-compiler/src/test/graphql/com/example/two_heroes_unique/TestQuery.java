package com.example.two_heroes_unique;

import com.apollographql.apollo.api.Field;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.api.internal.UnmodifiableMapBuilder;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<TestQuery.Data, Optional<TestQuery.Data>, Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery {\n"
      + "  r2: hero {\n"
      + "    __typename\n"
      + "    name\n"
      + "  }\n"
      + "  luke: hero(episode: EMPIRE) {\n"
      + "    __typename\n"
      + "    id\n"
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
  public Optional<TestQuery.Data> wrapData(TestQuery.Data data) {
    return Optional.fromNullable(data);
  }

  @Override
  public Operation.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<TestQuery.Data> responseFieldMapper() {
    return new Data.Mapper();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    Builder() {
    }

    public TestQuery build() {
      return new TestQuery();
    }
  }

  public static class Data implements Operation.Data {
    private final Optional<R2> r2;

    private final Optional<Luke> luke;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

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
      if ($toString == null) {
        $toString = "Data{"
          + "r2=" + r2 + ", "
          + "luke=" + luke
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Data) {
        Data that = (Data) o;
        return this.r2.equals(that.r2)
         && this.luke.equals(that.luke);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= r2.hashCode();
        h *= 1000003;
        h ^= luke.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
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

  public static class R2 {
    private final @Nonnull String __typename;

    private final @Nonnull String name;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public R2(@Nonnull String __typename, @Nonnull String name) {
      this.__typename = __typename;
      this.name = name;
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * The name of the character
     */
    public @Nonnull String name() {
      return this.name;
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "R2{"
          + "__typename=" + __typename + ", "
          + "name=" + name
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof R2) {
        R2 that = (R2) o;
        return this.__typename.equals(that.__typename)
         && this.name.equals(that.name);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= __typename.hashCode();
        h *= 1000003;
        h ^= name.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<R2> {
      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forString("name", "name", null, false)
      };

      @Override
      public R2 map(ResponseReader reader) throws IOException {
        final String __typename = reader.read(fields[0]);
        final String name = reader.read(fields[1]);
        return new R2(__typename, name);
      }
    }
  }

  public static class Luke {
    private final @Nonnull String __typename;

    private final @Nonnull String id;

    private final @Nonnull String name;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Luke(@Nonnull String __typename, @Nonnull String id, @Nonnull String name) {
      this.__typename = __typename;
      this.id = id;
      this.name = name;
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * The ID of the character
     */
    public @Nonnull String id() {
      return this.id;
    }

    /**
     * The name of the character
     */
    public @Nonnull String name() {
      return this.name;
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Luke{"
          + "__typename=" + __typename + ", "
          + "id=" + id + ", "
          + "name=" + name
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Luke) {
        Luke that = (Luke) o;
        return this.__typename.equals(that.__typename)
         && this.id.equals(that.id)
         && this.name.equals(that.name);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= __typename.hashCode();
        h *= 1000003;
        h ^= id.hashCode();
        h *= 1000003;
        h ^= name.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Luke> {
      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forString("id", "id", null, false),
        Field.forString("name", "name", null, false)
      };

      @Override
      public Luke map(ResponseReader reader) throws IOException {
        final String __typename = reader.read(fields[0]);
        final String id = reader.read(fields[1]);
        final String name = reader.read(fields[2]);
        return new Luke(__typename, id, name);
      }
    }
  }
}
