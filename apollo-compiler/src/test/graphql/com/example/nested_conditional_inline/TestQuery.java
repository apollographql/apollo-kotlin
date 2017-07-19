package com.example.nested_conditional_inline;

import com.apollographql.apollo.api.InputFieldMarshaller;
import com.apollographql.apollo.api.InputFieldWriter;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ResponseWriter;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.api.internal.UnmodifiableMapBuilder;
import com.example.nested_conditional_inline.type.Episode;
import java.io.IOException;
import java.lang.Double;
import java.lang.NullPointerException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<TestQuery.Data, Optional<TestQuery.Data>, TestQuery.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery($episode: Episode) {\n"
      + "  hero(episode: $episode) {\n"
      + "    __typename\n"
      + "    name\n"
      + "    ... on Human {\n"
      + "      __typename\n"
      + "      friends {\n"
      + "        __typename\n"
      + "        name\n"
      + "        ... on Human {\n"
      + "          __typename\n"
      + "          height(unit: FOOT)\n"
      + "        }\n"
      + "      }\n"
      + "    }\n"
      + "    ... on Droid {\n"
      + "      __typename\n"
      + "      friends {\n"
      + "        __typename\n"
      + "        name\n"
      + "        ... on Human {\n"
      + "          __typename\n"
      + "          height(unit: METER)\n"
      + "        }\n"
      + "      }\n"
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

  private final TestQuery.Variables variables;

  public TestQuery(@Nullable Episode episode) {
    variables = new TestQuery.Variables(episode);
  }

  @Override
  public String operationId() {
    return null;
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
  public TestQuery.Variables variables() {
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

  public static final class Variables extends Operation.Variables {
    private final @Nullable Episode episode;

    private final transient Map<String, Object> valueMap = new LinkedHashMap<>();

    Variables(@Nullable Episode episode) {
      this.episode = episode;
      this.valueMap.put("episode", episode);
    }

    public @Nullable Episode episode() {
      return episode;
    }

    @Override
    public Map<String, Object> valueMap() {
      return Collections.unmodifiableMap(valueMap);
    }

    @Override
    public InputFieldMarshaller marshaller() {
      return new InputFieldMarshaller() {
        @Override
        public void marshal(InputFieldWriter writer) throws IOException {
          writer.writeString("episode", episode != null ? episode.name() : null);
        }
      };
    }
  }

  public static final class Builder {
    private @Nullable Episode episode;

    Builder() {
    }

    public Builder episode(@Nullable Episode episode) {
      this.episode = episode;
      return this;
    }

    public TestQuery build() {
      return new TestQuery(episode);
    }
  }

  public static class Data implements Operation.Data {
    static final ResponseField[] $responseFields = {
      ResponseField.forObject("hero", "hero", new UnmodifiableMapBuilder<String, Object>(1)
        .put("episode", new UnmodifiableMapBuilder<String, Object>(2)
          .put("kind", "Variable")
          .put("variableName", "episode")
        .build())
      .build(), true)
    };

    final Optional<Hero> hero;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

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

  public static class Hero {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forString("name", "name", null, false),
      ResponseField.forInlineFragment("__typename", "__typename", Arrays.asList("Human")),
      ResponseField.forInlineFragment("__typename", "__typename", Arrays.asList("Droid"))
    };

    final @Nonnull String __typename;

    final @Nonnull String name;

    final Optional<AsHuman> asHuman;

    final Optional<AsDroid> asDroid;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Hero(@Nonnull String __typename, @Nonnull String name, @Nullable AsHuman asHuman,
        @Nullable AsDroid asDroid) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
      this.__typename = __typename;
      if (name == null) {
        throw new NullPointerException("name can't be null");
      }
      this.name = name;
      this.asHuman = Optional.fromNullable(asHuman);
      this.asDroid = Optional.fromNullable(asDroid);
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    public @Nonnull String name() {
      return this.name;
    }

    public Optional<AsHuman> asHuman() {
      return this.asHuman;
    }

    public Optional<AsDroid> asDroid() {
      return this.asDroid;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], name);
          final AsHuman $asHuman = asHuman.isPresent() ? asHuman.get() : null;
          if ($asHuman != null) {
            $asHuman.marshaller().marshal(writer);
          }
          final AsDroid $asDroid = asDroid.isPresent() ? asDroid.get() : null;
          if ($asDroid != null) {
            $asDroid.marshaller().marshal(writer);
          }
        }
      };
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

      @Override
      public Hero map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String name = reader.readString($responseFields[1]);
        final AsHuman asHuman = reader.readConditional((ResponseField.ConditionalTypeField) $responseFields[2], new ResponseReader.ConditionalTypeReader<AsHuman>() {
          @Override
          public AsHuman read(String conditionalType, ResponseReader reader) {
            return asHumanFieldMapper.map(reader);
          }
        });
        final AsDroid asDroid = reader.readConditional((ResponseField.ConditionalTypeField) $responseFields[3], new ResponseReader.ConditionalTypeReader<AsDroid>() {
          @Override
          public AsDroid read(String conditionalType, ResponseReader reader) {
            return asDroidFieldMapper.map(reader);
          }
        });
        return new Hero(__typename, name, asHuman, asDroid);
      }
    }
  }

  public static class AsHuman {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forString("name", "name", null, false),
      ResponseField.forObjectList("friends", "friends", null, true)
    };

    final @Nonnull String __typename;

    final @Nonnull String name;

    final Optional<List<Friend>> friends;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsHuman(@Nonnull String __typename, @Nonnull String name,
        @Nullable List<Friend> friends) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
      this.__typename = __typename;
      if (name == null) {
        throw new NullPointerException("name can't be null");
      }
      this.name = name;
      this.friends = Optional.fromNullable(friends);
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    public @Nonnull String name() {
      return this.name;
    }

    public Optional<List<Friend>> friends() {
      return this.friends;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], name);
          writer.writeList($responseFields[2], friends.isPresent() ? new ResponseWriter.ListWriter() {
            @Override
            public void write(ResponseWriter.ListItemWriter listItemWriter) {
              for (Friend $item : friends.get()) {
                listItemWriter.writeObject($item.marshaller());
              }
            }
          } : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "AsHuman{"
          + "__typename=" + __typename + ", "
          + "name=" + name + ", "
          + "friends=" + friends
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
         && this.friends.equals(that.friends);
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
        h ^= friends.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<AsHuman> {
      final Friend.Mapper friendFieldMapper = new Friend.Mapper();

      @Override
      public AsHuman map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String name = reader.readString($responseFields[1]);
        final List<Friend> friends = reader.readList($responseFields[2], new ResponseReader.ListReader<Friend>() {
          @Override
          public Friend read(ResponseReader.ListItemReader reader) {
            return reader.readObject(new ResponseReader.ObjectReader<Friend>() {
              @Override
              public Friend read(ResponseReader reader) {
                return friendFieldMapper.map(reader);
              }
            });
          }
        });
        return new AsHuman(__typename, name, friends);
      }
    }
  }

  public static class Friend {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forString("name", "name", null, false),
      ResponseField.forInlineFragment("__typename", "__typename", Arrays.asList("Human"))
    };

    final @Nonnull String __typename;

    final @Nonnull String name;

    final Optional<AsHuman1> asHuman;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Friend(@Nonnull String __typename, @Nonnull String name, @Nullable AsHuman1 asHuman) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
      this.__typename = __typename;
      if (name == null) {
        throw new NullPointerException("name can't be null");
      }
      this.name = name;
      this.asHuman = Optional.fromNullable(asHuman);
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    public @Nonnull String name() {
      return this.name;
    }

    public Optional<AsHuman1> asHuman() {
      return this.asHuman;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], name);
          final AsHuman1 $asHuman = asHuman.isPresent() ? asHuman.get() : null;
          if ($asHuman != null) {
            $asHuman.marshaller().marshal(writer);
          }
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Friend{"
          + "__typename=" + __typename + ", "
          + "name=" + name + ", "
          + "asHuman=" + asHuman
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Friend) {
        Friend that = (Friend) o;
        return this.__typename.equals(that.__typename)
         && this.name.equals(that.name)
         && this.asHuman.equals(that.asHuman);
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
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Friend> {
      final AsHuman1.Mapper asHuman1FieldMapper = new AsHuman1.Mapper();

      @Override
      public Friend map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String name = reader.readString($responseFields[1]);
        final AsHuman1 asHuman = reader.readConditional((ResponseField.ConditionalTypeField) $responseFields[2], new ResponseReader.ConditionalTypeReader<AsHuman1>() {
          @Override
          public AsHuman1 read(String conditionalType, ResponseReader reader) {
            return asHuman1FieldMapper.map(reader);
          }
        });
        return new Friend(__typename, name, asHuman);
      }
    }
  }

  public static class AsHuman1 {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forString("name", "name", null, false),
      ResponseField.forDouble("height", "height", new UnmodifiableMapBuilder<String, Object>(1)
        .put("unit", "FOOT")
      .build(), true)
    };

    final @Nonnull String __typename;

    final @Nonnull String name;

    final Optional<Double> height;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsHuman1(@Nonnull String __typename, @Nonnull String name, @Nullable Double height) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
      this.__typename = __typename;
      if (name == null) {
        throw new NullPointerException("name can't be null");
      }
      this.name = name;
      this.height = Optional.fromNullable(height);
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    public @Nonnull String name() {
      return this.name;
    }

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
        $toString = "AsHuman1{"
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
      if (o instanceof AsHuman1) {
        AsHuman1 that = (AsHuman1) o;
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

    public static final class Mapper implements ResponseFieldMapper<AsHuman1> {
      @Override
      public AsHuman1 map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String name = reader.readString($responseFields[1]);
        final Double height = reader.readDouble($responseFields[2]);
        return new AsHuman1(__typename, name, height);
      }
    }
  }

  public static class AsDroid {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forString("name", "name", null, false),
      ResponseField.forObjectList("friends", "friends", null, true)
    };

    final @Nonnull String __typename;

    final @Nonnull String name;

    final Optional<List<Friend1>> friends;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsDroid(@Nonnull String __typename, @Nonnull String name,
        @Nullable List<Friend1> friends) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
      this.__typename = __typename;
      if (name == null) {
        throw new NullPointerException("name can't be null");
      }
      this.name = name;
      this.friends = Optional.fromNullable(friends);
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    public @Nonnull String name() {
      return this.name;
    }

    public Optional<List<Friend1>> friends() {
      return this.friends;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], name);
          writer.writeList($responseFields[2], friends.isPresent() ? new ResponseWriter.ListWriter() {
            @Override
            public void write(ResponseWriter.ListItemWriter listItemWriter) {
              for (Friend1 $item : friends.get()) {
                listItemWriter.writeObject($item.marshaller());
              }
            }
          } : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "AsDroid{"
          + "__typename=" + __typename + ", "
          + "name=" + name + ", "
          + "friends=" + friends
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
         && this.friends.equals(that.friends);
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
        h ^= friends.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<AsDroid> {
      final Friend1.Mapper friend1FieldMapper = new Friend1.Mapper();

      @Override
      public AsDroid map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String name = reader.readString($responseFields[1]);
        final List<Friend1> friends = reader.readList($responseFields[2], new ResponseReader.ListReader<Friend1>() {
          @Override
          public Friend1 read(ResponseReader.ListItemReader reader) {
            return reader.readObject(new ResponseReader.ObjectReader<Friend1>() {
              @Override
              public Friend1 read(ResponseReader reader) {
                return friend1FieldMapper.map(reader);
              }
            });
          }
        });
        return new AsDroid(__typename, name, friends);
      }
    }
  }

  public static class Friend1 {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forString("name", "name", null, false),
      ResponseField.forInlineFragment("__typename", "__typename", Arrays.asList("Human"))
    };

    final @Nonnull String __typename;

    final @Nonnull String name;

    final Optional<AsHuman2> asHuman;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Friend1(@Nonnull String __typename, @Nonnull String name, @Nullable AsHuman2 asHuman) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
      this.__typename = __typename;
      if (name == null) {
        throw new NullPointerException("name can't be null");
      }
      this.name = name;
      this.asHuman = Optional.fromNullable(asHuman);
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    public @Nonnull String name() {
      return this.name;
    }

    public Optional<AsHuman2> asHuman() {
      return this.asHuman;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], name);
          final AsHuman2 $asHuman = asHuman.isPresent() ? asHuman.get() : null;
          if ($asHuman != null) {
            $asHuman.marshaller().marshal(writer);
          }
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Friend1{"
          + "__typename=" + __typename + ", "
          + "name=" + name + ", "
          + "asHuman=" + asHuman
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Friend1) {
        Friend1 that = (Friend1) o;
        return this.__typename.equals(that.__typename)
         && this.name.equals(that.name)
         && this.asHuman.equals(that.asHuman);
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
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Friend1> {
      final AsHuman2.Mapper asHuman2FieldMapper = new AsHuman2.Mapper();

      @Override
      public Friend1 map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String name = reader.readString($responseFields[1]);
        final AsHuman2 asHuman = reader.readConditional((ResponseField.ConditionalTypeField) $responseFields[2], new ResponseReader.ConditionalTypeReader<AsHuman2>() {
          @Override
          public AsHuman2 read(String conditionalType, ResponseReader reader) {
            return asHuman2FieldMapper.map(reader);
          }
        });
        return new Friend1(__typename, name, asHuman);
      }
    }
  }

  public static class AsHuman2 {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forString("name", "name", null, false),
      ResponseField.forDouble("height", "height", new UnmodifiableMapBuilder<String, Object>(1)
        .put("unit", "METER")
      .build(), true)
    };

    final @Nonnull String __typename;

    final @Nonnull String name;

    final Optional<Double> height;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsHuman2(@Nonnull String __typename, @Nonnull String name, @Nullable Double height) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
      this.__typename = __typename;
      if (name == null) {
        throw new NullPointerException("name can't be null");
      }
      this.name = name;
      this.height = Optional.fromNullable(height);
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    public @Nonnull String name() {
      return this.name;
    }

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
        $toString = "AsHuman2{"
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
      if (o instanceof AsHuman2) {
        AsHuman2 that = (AsHuman2) o;
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

    public static final class Mapper implements ResponseFieldMapper<AsHuman2> {
      @Override
      public AsHuman2 map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String name = reader.readString($responseFields[1]);
        final Double height = reader.readDouble($responseFields[2]);
        return new AsHuman2(__typename, name, height);
      }
    }
  }
}
