package com.example.nested_conditional_inline;

import com.apollographql.apollo.api.Input;
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
import com.apollographql.apollo.api.internal.Utils;
import com.example.nested_conditional_inline.type.Episode;
import java.io.IOException;
import java.lang.Double;
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
      + "      friends {\n"
      + "        __typename\n"
      + "        name\n"
      + "        ... on Human {\n"
      + "          height(unit: FOOT)\n"
      + "        }\n"
      + "      }\n"
      + "    }\n"
      + "    ... on Droid {\n"
      + "      friends {\n"
      + "        __typename\n"
      + "        name\n"
      + "        ... on Human {\n"
      + "          height(unit: METER)\n"
      + "        }\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "}";

  public static final String OPERATION_ID = "071e064b3415e8b92bed3befa46bf04501c7194cde77ede0ebf50429624796cc";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  public static final OperationName OPERATION_NAME = new OperationName() {
    @Override
    public String name() {
      return "TestQuery";
    }
  };

  private final TestQuery.Variables variables;

  public TestQuery(@Nonnull Input<Episode> episode) {
    Utils.checkNotNull(episode, "episode == null");
    variables = new TestQuery.Variables(episode);
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

  public static final class Builder {
    private Input<Episode> episode = Input.absent();

    Builder() {
    }

    public Builder episode(@Nullable Episode episode) {
      this.episode = Input.fromNullable(episode);
      return this;
    }

    public Builder episodeInput(@Nonnull Input<Episode> episode) {
      this.episode = Utils.checkNotNull(episode, "episode == null");
      return this;
    }

    public TestQuery build() {
      return new TestQuery(episode);
    }
  }

  public static final class Variables extends Operation.Variables {
    private final Input<Episode> episode;

    private final transient Map<String, Object> valueMap = new LinkedHashMap<>();

    Variables(Input<Episode> episode) {
      this.episode = episode;
      if (episode.defined) {
        this.valueMap.put("episode", episode.value);
      }
    }

    public Input<Episode> episode() {
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
          if (episode.defined) {
            writer.writeString("episode", episode.value != null ? episode.value.rawValue() : null);
          }
        }
      };
    }
  }

  public static class Data implements Operation.Data {
    static final ResponseField[] $responseFields = {
      ResponseField.forObject("hero", "hero", new UnmodifiableMapBuilder<String, Object>(1)
        .put("episode", new UnmodifiableMapBuilder<String, Object>(2)
          .put("kind", "Variable")
          .put("variableName", "episode")
        .build())
      .build(), true, Collections.<ResponseField.Condition>emptyList())
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

  public interface Hero {
    @Nonnull String __typename();

    /**
     * The name of the character
     */
    @Nonnull String name();

    ResponseFieldMarshaller marshaller();

    final class Mapper implements ResponseFieldMapper<Hero> {
      final AsHuman.Mapper asHumanFieldMapper = new AsHuman.Mapper();

      final AsDroid.Mapper asDroidFieldMapper = new AsDroid.Mapper();

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
        return null;
      }
    }
  }

  public static class AsHuman implements Hero {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("name", "name", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("friends", "friends", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @Nonnull String __typename;

    final @Nonnull String name;

    final Optional<List<Friend>> friends;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsHuman(@Nonnull String __typename, @Nonnull String name,
        @Nullable List<Friend> friends) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.name = Utils.checkNotNull(name, "name == null");
      this.friends = Optional.fromNullable(friends);
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
     * This human's friends, or an empty list if they have none
     */
    public Optional<List<Friend>> friends() {
      return this.friends;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], name);
          writer.writeList($responseFields[2], friends.isPresent() ? friends.get() : null, new ResponseWriter.ListWriter() {
            @Override
            public void write(Object value, ResponseWriter.ListItemWriter listItemWriter) {
              listItemWriter.writeObject(((Friend) value).marshaller());
            }
          });
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
          public Friend read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readObject(new ResponseReader.ObjectReader<Friend>() {
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

  public interface Friend {
    @Nonnull String __typename();

    /**
     * The name of the character
     */
    @Nonnull String name();

    ResponseFieldMarshaller marshaller();

    final class Mapper implements ResponseFieldMapper<Friend> {
      final AsHuman1.Mapper asHuman1FieldMapper = new AsHuman1.Mapper();

      @Override
      public Friend map(ResponseReader reader) {
        final AsHuman1 asHuman = reader.readConditional(ResponseField.forInlineFragment("__typename", "__typename", Arrays.asList("Human")), new ResponseReader.ConditionalTypeReader<AsHuman1>() {
          @Override
          public AsHuman1 read(String conditionalType, ResponseReader reader) {
            return asHuman1FieldMapper.map(reader);
          }
        });
        if (asHuman != null) {
          return asHuman;
        }
        return null;
      }
    }
  }

  public static class AsHuman1 implements Friend {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("name", "name", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forDouble("height", "height", new UnmodifiableMapBuilder<String, Object>(1)
        .put("unit", "FOOT")
      .build(), true, Collections.<ResponseField.Condition>emptyList())
    };

    final @Nonnull String __typename;

    final @Nonnull String name;

    final Optional<Double> height;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsHuman1(@Nonnull String __typename, @Nonnull String name, @Nullable Double height) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.name = Utils.checkNotNull(name, "name == null");
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

  public static class AsDroid implements Hero {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("name", "name", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("friends", "friends", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @Nonnull String __typename;

    final @Nonnull String name;

    final Optional<List<Friend1>> friends;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsDroid(@Nonnull String __typename, @Nonnull String name,
        @Nullable List<Friend1> friends) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.name = Utils.checkNotNull(name, "name == null");
      this.friends = Optional.fromNullable(friends);
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
     * This droid's friends, or an empty list if they have none
     */
    public Optional<List<Friend1>> friends() {
      return this.friends;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], name);
          writer.writeList($responseFields[2], friends.isPresent() ? friends.get() : null, new ResponseWriter.ListWriter() {
            @Override
            public void write(Object value, ResponseWriter.ListItemWriter listItemWriter) {
              listItemWriter.writeObject(((Friend1) value).marshaller());
            }
          });
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
          public Friend1 read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readObject(new ResponseReader.ObjectReader<Friend1>() {
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

  public interface Friend1 {
    @Nonnull String __typename();

    /**
     * The name of the character
     */
    @Nonnull String name();

    ResponseFieldMarshaller marshaller();

    final class Mapper implements ResponseFieldMapper<Friend1> {
      final AsHuman2.Mapper asHuman2FieldMapper = new AsHuman2.Mapper();

      @Override
      public Friend1 map(ResponseReader reader) {
        final AsHuman2 asHuman = reader.readConditional(ResponseField.forInlineFragment("__typename", "__typename", Arrays.asList("Human")), new ResponseReader.ConditionalTypeReader<AsHuman2>() {
          @Override
          public AsHuman2 read(String conditionalType, ResponseReader reader) {
            return asHuman2FieldMapper.map(reader);
          }
        });
        if (asHuman != null) {
          return asHuman;
        }
        return null;
      }
    }
  }

  public static class AsHuman2 implements Friend1 {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("name", "name", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forDouble("height", "height", new UnmodifiableMapBuilder<String, Object>(1)
        .put("unit", "METER")
      .build(), true, Collections.<ResponseField.Condition>emptyList())
    };

    final @Nonnull String __typename;

    final @Nonnull String name;

    final Optional<Double> height;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsHuman2(@Nonnull String __typename, @Nonnull String name, @Nullable Double height) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.name = Utils.checkNotNull(name, "name == null");
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
