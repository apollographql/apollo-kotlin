package com.example.simple_inline_fragment;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ResponseWriter;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.api.internal.Utils;
import java.lang.Double;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import java.util.Collections;
import javax.annotation.Generated;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<TestQuery.Data, Optional<TestQuery.Data>, Operation.Variables> {
  public static final String OPERATION_ID = "212ff92146147cf94d8e05687d2660c3ead0b054e92d86a52e24eb15880c6dfd";

  public static final String QUERY_DOCUMENT = "query TestQuery {\n"
      + "  hero {\n"
      + "    __typename\n"
      + "    name\n"
      + "    ... on Human {\n"
      + "      height\n"
      + "    }\n"
      + "    ... on Droid {\n"
      + "      primaryFunction\n"
      + "    }\n"
      + "  }\n"
      + "}";

  public static final OperationName OPERATION_NAME = new OperationName() {
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
  public String operationId() {
    return OPERATION_ID;
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
    static final ResponseField[] $responseFields = {
      ResponseField.forObject("hero", "hero", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final Optional<Hero> hero;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

    public Data(@Nullable Hero hero) {
      this.hero = Optional.fromNullable(hero);
    }

    public Optional<Hero> hero() {
      return this.hero;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeObject($responseFields[0], hero.isPresent() ? hero.get().marshaller() : null);
        }
      };
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

      @Override
      public Data map(ResponseReader reader) {
        final Hero hero = reader.readObject($responseFields[0], new ResponseReader.ObjectReader<Hero>() {
          @Override
          public Hero read(ResponseReader reader) {
            return heroFieldMapper.map(reader);
          }
        });
        return new Data(hero);
      }
    }
  }

  public interface Hero {
    @NotNull String __typename();

    /**
     * The name of the character
     */
    @NotNull String name();

    ResponseFieldMarshaller marshaller();

    final class Mapper implements ResponseFieldMapper<Hero> {
      final AsHuman.Mapper asHumanFieldMapper = new AsHuman.Mapper();

      final AsDroid.Mapper asDroidFieldMapper = new AsDroid.Mapper();

      final AsCharacter.Mapper asCharacterFieldMapper = new AsCharacter.Mapper();

      @Override
      public Hero map(ResponseReader reader) {
        final AsHuman asHuman = reader.readConditional(ResponseField.forInlineFragment("__typename", "__typename", Arrays.asList("Human")), new ResponseReader.ConditionalTypeReader<AsHuman>() {
          @Override
          public AsHuman read(String conditionalType, ResponseReader reader) {
            return asHumanFieldMapper.map(reader);
          }
        });
        if (asHuman != null) {
          return asHuman;
        }
        final AsDroid asDroid = reader.readConditional(ResponseField.forInlineFragment("__typename", "__typename", Arrays.asList("Droid")), new ResponseReader.ConditionalTypeReader<AsDroid>() {
          @Override
          public AsDroid read(String conditionalType, ResponseReader reader) {
            return asDroidFieldMapper.map(reader);
          }
        });
        if (asDroid != null) {
          return asDroid;
        }
        return asCharacterFieldMapper.map(reader);
      }
    }
  }

  public static class AsHuman implements Hero {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("name", "name", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forDouble("height", "height", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final @NotNull String name;

    final Optional<Double> height;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

    public AsHuman(@NotNull String __typename, @NotNull String name, @Nullable Double height) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.name = Utils.checkNotNull(name, "name == null");
      this.height = Optional.fromNullable(height);
    }

    public @NotNull String __typename() {
      return this.__typename;
    }

    /**
     * What this human calls themselves
     */
    public @NotNull String name() {
      return this.name;
    }

    /**
     * Height in the preferred unit, default is meters
     */
    public Optional<Double> height() {
      return this.height;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], name);
          writer.writeDouble($responseFields[2], height.isPresent() ? height.get() : null);
        }
      };
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
      @Override
      public AsHuman map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String name = reader.readString($responseFields[1]);
        final Double height = reader.readDouble($responseFields[2]);
        return new AsHuman(__typename, name, height);
      }
    }
  }

  public static class AsDroid implements Hero {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("name", "name", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("primaryFunction", "primaryFunction", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final @NotNull String name;

    final Optional<String> primaryFunction;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

    public AsDroid(@NotNull String __typename, @NotNull String name,
        @Nullable String primaryFunction) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.name = Utils.checkNotNull(name, "name == null");
      this.primaryFunction = Optional.fromNullable(primaryFunction);
    }

    public @NotNull String __typename() {
      return this.__typename;
    }

    /**
     * What others call this droid
     */
    public @NotNull String name() {
      return this.name;
    }

    /**
     * This droid's primary function
     */
    public Optional<String> primaryFunction() {
      return this.primaryFunction;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], name);
          writer.writeString($responseFields[2], primaryFunction.isPresent() ? primaryFunction.get() : null);
        }
      };
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
      @Override
      public AsDroid map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String name = reader.readString($responseFields[1]);
        final String primaryFunction = reader.readString($responseFields[2]);
        return new AsDroid(__typename, name, primaryFunction);
      }
    }
  }

  public static class AsCharacter implements Hero {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("name", "name", null, false, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final @NotNull String name;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

    public AsCharacter(@NotNull String __typename, @NotNull String name) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.name = Utils.checkNotNull(name, "name == null");
    }

    public @NotNull String __typename() {
      return this.__typename;
    }

    /**
     * The name of the character
     */
    public @NotNull String name() {
      return this.name;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], name);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "AsCharacter{"
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
      if (o instanceof AsCharacter) {
        AsCharacter that = (AsCharacter) o;
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

    public static final class Mapper implements ResponseFieldMapper<AsCharacter> {
      @Override
      public AsCharacter map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String name = reader.readString($responseFields[1]);
        return new AsCharacter(__typename, name);
      }
    }
  }
}
