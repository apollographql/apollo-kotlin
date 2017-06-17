package com.example.simple_inline_fragment;

import com.apollographql.apollo.api.Field;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.internal.Optional;
import java.io.IOException;
import java.lang.Double;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<TestQuery.Data, Optional<TestQuery.Data>, Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery {\n"
      + "  hero {\n"
      + "    __typename\n"
      + "    name\n"
      + "    ... on Human {\n"
      + "      __typename\n"
      + "      height\n"
      + "    }\n"
      + "    ... on Droid {\n"
      + "      __typename\n"
      + "      primaryFunction\n"
      + "    }\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private static final OperationName OPERATION_NAME = new OperationName() {
    @Override
    public String name() {
      return "TestQuery";
    }
  };

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

  @Override
  public OperationName name() {
    return OPERATION_NAME;
  }

  public static final class Builder {
    Builder() {
    }

    public TestQuery build() {
      return new TestQuery();
    }
  }

  public static class Data implements Operation.Data {
    private final Optional<Hero> hero;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Data(@Nullable Hero hero) {
      this.hero = Optional.fromNullable(hero);
    }

    public Optional<Hero> hero() {
      return this.hero;
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Data{"
          + "hero=" + hero
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
        return this.hero.equals(that.hero);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= hero.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final Hero.Mapper heroFieldMapper = new Hero.Mapper();

      final Field[] fields = {
        Field.forObject("hero", "hero", null, true)
      };

      @Override
      public Data map(ResponseReader reader) throws IOException {
        final Hero hero = reader.readObject(fields[0], new ResponseReader.ObjectReader<Hero>() {
          @Override
          public Hero read(ResponseReader reader) throws IOException {
            return heroFieldMapper.map(reader);
          }
        });
        return new Data(hero);
      }
    }
  }

  public static class Hero {
    private final @Nonnull String __typename;

    private final @Nonnull String name;

    private final Optional<AsHuman> asHuman;

    private final Optional<AsDroid> asDroid;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Hero(@Nonnull String __typename, @Nonnull String name, @Nullable AsHuman asHuman,
        @Nullable AsDroid asDroid) {
      this.__typename = __typename;
      this.name = name;
      this.asHuman = Optional.fromNullable(asHuman);
      this.asDroid = Optional.fromNullable(asDroid);
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

    public Optional<AsHuman> asHuman() {
      return this.asHuman;
    }

    public Optional<AsDroid> asDroid() {
      return this.asDroid;
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Hero{"
          + "__typename=" + __typename + ", "
          + "name=" + name + ", "
          + "asHuman=" + asHuman + ", "
          + "asDroid=" + asDroid
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Hero) {
        Hero that = (Hero) o;
        return this.__typename.equals(that.__typename)
         && this.name.equals(that.name)
         && this.asHuman.equals(that.asHuman)
         && this.asDroid.equals(that.asDroid);
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
        h *= 1000003;
        h ^= asHuman.hashCode();
        h *= 1000003;
        h ^= asDroid.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Hero> {
      final AsHuman.Mapper asHumanFieldMapper = new AsHuman.Mapper();

      final AsDroid.Mapper asDroidFieldMapper = new AsDroid.Mapper();

      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forString("name", "name", null, false),
        Field.forInlineFragment("__typename", "__typename", Arrays.asList("Human")),
        Field.forInlineFragment("__typename", "__typename", Arrays.asList("Droid"))
      };

      @Override
      public Hero map(ResponseReader reader) throws IOException {
        final String __typename = reader.readString(fields[0]);
        final String name = reader.readString(fields[1]);
        final AsHuman asHuman = reader.readConditional((Field.ConditionalTypeField) fields[2], new ResponseReader.ConditionalTypeReader<AsHuman>() {
          @Override
          public AsHuman read(String conditionalType, ResponseReader reader) throws IOException {
            return asHumanFieldMapper.map(reader);
          }
        });
        final AsDroid asDroid = reader.readConditional((Field.ConditionalTypeField) fields[3], new ResponseReader.ConditionalTypeReader<AsDroid>() {
          @Override
          public AsDroid read(String conditionalType, ResponseReader reader) throws IOException {
            return asDroidFieldMapper.map(reader);
          }
        });
        return new Hero(__typename, name, asHuman, asDroid);
      }
    }
  }

  public static class AsHuman {
    private final @Nonnull String __typename;

    private final @Nonnull String name;

    private final Optional<Double> height;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsHuman(@Nonnull String __typename, @Nonnull String name, @Nullable Double height) {
      this.__typename = __typename;
      this.name = name;
      this.height = Optional.fromNullable(height);
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * What this human calls themselves
     */
    public @Nonnull String name() {
      return this.name;
    }

    /**
     * Height in the preferred unit, default is meters
     */
    public Optional<Double> height() {
      return this.height;
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "AsHuman{"
          + "__typename=" + __typename + ", "
          + "name=" + name + ", "
          + "height=" + height
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof AsHuman) {
        AsHuman that = (AsHuman) o;
        return this.__typename.equals(that.__typename)
         && this.name.equals(that.name)
         && this.height.equals(that.height);
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
        h *= 1000003;
        h ^= height.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<AsHuman> {
      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forString("name", "name", null, false),
        Field.forDouble("height", "height", null, true)
      };

      @Override
      public AsHuman map(ResponseReader reader) throws IOException {
        final String __typename = reader.readString(fields[0]);
        final String name = reader.readString(fields[1]);
        final Double height = reader.readDouble(fields[2]);
        return new AsHuman(__typename, name, height);
      }
    }
  }

  public static class AsDroid {
    private final @Nonnull String __typename;

    private final @Nonnull String name;

    private final Optional<String> primaryFunction;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsDroid(@Nonnull String __typename, @Nonnull String name,
        @Nullable String primaryFunction) {
      this.__typename = __typename;
      this.name = name;
      this.primaryFunction = Optional.fromNullable(primaryFunction);
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * What others call this droid
     */
    public @Nonnull String name() {
      return this.name;
    }

    /**
     * This droid's primary function
     */
    public Optional<String> primaryFunction() {
      return this.primaryFunction;
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "AsDroid{"
          + "__typename=" + __typename + ", "
          + "name=" + name + ", "
          + "primaryFunction=" + primaryFunction
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof AsDroid) {
        AsDroid that = (AsDroid) o;
        return this.__typename.equals(that.__typename)
         && this.name.equals(that.name)
         && this.primaryFunction.equals(that.primaryFunction);
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
        h *= 1000003;
        h ^= primaryFunction.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<AsDroid> {
      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forString("name", "name", null, false),
        Field.forString("primaryFunction", "primaryFunction", null, true)
      };

      @Override
      public AsDroid map(ResponseReader reader) throws IOException {
        final String __typename = reader.readString(fields[0]);
        final String name = reader.readString(fields[1]);
        final String primaryFunction = reader.readString(fields[2]);
        return new AsDroid(__typename, name, primaryFunction);
      }
    }
  }
}
