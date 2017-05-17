package com.example.nested_conditional_inline;

import com.apollographql.apollo.api.Field;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.api.internal.UnmodifiableMapBuilder;
import com.example.nested_conditional_inline.type.Episode;
import java.io.IOException;
import java.lang.Double;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
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

  private final TestQuery.Variables variables;

  public TestQuery(@Nullable Episode episode) {
    variables = new TestQuery.Variables(episode);
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
        Field.forObject("hero", "hero", new UnmodifiableMapBuilder<String, Object>(1)
          .put("episode", new UnmodifiableMapBuilder<String, Object>(2)
            .put("kind", "Variable")
            .put("variableName", "episode")
          .build())
        .build(), true, new Field.ObjectReader<Hero>() {
          @Override public Hero read(final ResponseReader reader) throws IOException {
            return heroFieldMapper.map(reader);
          }
        })
      };

      @Override
      public Data map(ResponseReader reader) throws IOException {
        final Hero hero = reader.read(fields[0]);
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
        Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<AsHuman>() {
          @Override
          public AsHuman read(String conditionalType, ResponseReader reader) throws IOException {
            if (conditionalType.equals("Human")) {
              return asHumanFieldMapper.map(reader);
            } else {
              return null;
            }
          }
        }),
        Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<AsDroid>() {
          @Override
          public AsDroid read(String conditionalType, ResponseReader reader) throws IOException {
            if (conditionalType.equals("Droid")) {
              return asDroidFieldMapper.map(reader);
            } else {
              return null;
            }
          }
        })
      };

      @Override
      public Hero map(ResponseReader reader) throws IOException {
        final String __typename = reader.read(fields[0]);
        final String name = reader.read(fields[1]);
        final AsHuman asHuman = reader.read(fields[2]);
        final AsDroid asDroid = reader.read(fields[3]);
        return new Hero(__typename, name, asHuman, asDroid);
      }
    }
  }

  public static class AsHuman {
    private final @Nonnull String __typename;

    private final @Nonnull String name;

    private final Optional<List<Friend>> friends;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsHuman(@Nonnull String __typename, @Nonnull String name,
        @Nullable List<Friend> friends) {
      this.__typename = __typename;
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

      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forString("name", "name", null, false),
        Field.forList("friends", "friends", null, true, new Field.ObjectReader<Friend>() {
          @Override public Friend read(final ResponseReader reader) throws IOException {
            return friendFieldMapper.map(reader);
          }
        })
      };

      @Override
      public AsHuman map(ResponseReader reader) throws IOException {
        final String __typename = reader.read(fields[0]);
        final String name = reader.read(fields[1]);
        final List<Friend> friends = reader.read(fields[2]);
        return new AsHuman(__typename, name, friends);
      }
    }
  }

  public static class Friend {
    private final @Nonnull String __typename;

    private final @Nonnull String name;

    private final Optional<AsHuman1> asHuman;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Friend(@Nonnull String __typename, @Nonnull String name, @Nullable AsHuman1 asHuman) {
      this.__typename = __typename;
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

      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forString("name", "name", null, false),
        Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<AsHuman1>() {
          @Override
          public AsHuman1 read(String conditionalType, ResponseReader reader) throws IOException {
            if (conditionalType.equals("Human")) {
              return asHuman1FieldMapper.map(reader);
            } else {
              return null;
            }
          }
        })
      };

      @Override
      public Friend map(ResponseReader reader) throws IOException {
        final String __typename = reader.read(fields[0]);
        final String name = reader.read(fields[1]);
        final AsHuman1 asHuman = reader.read(fields[2]);
        return new Friend(__typename, name, asHuman);
      }
    }
  }

  public static class AsHuman1 {
    private final @Nonnull String __typename;

    private final @Nonnull String name;

    private final Optional<Double> height;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsHuman1(@Nonnull String __typename, @Nonnull String name, @Nullable Double height) {
      this.__typename = __typename;
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
      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forString("name", "name", null, false),
        Field.forDouble("height", "height", new UnmodifiableMapBuilder<String, Object>(1)
          .put("unit", "FOOT")
        .build(), true)
      };

      @Override
      public AsHuman1 map(ResponseReader reader) throws IOException {
        final String __typename = reader.read(fields[0]);
        final String name = reader.read(fields[1]);
        final Double height = reader.read(fields[2]);
        return new AsHuman1(__typename, name, height);
      }
    }
  }

  public static class AsDroid {
    private final @Nonnull String __typename;

    private final @Nonnull String name;

    private final Optional<List<Friend1>> friends;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsDroid(@Nonnull String __typename, @Nonnull String name,
        @Nullable List<Friend1> friends) {
      this.__typename = __typename;
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

      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forString("name", "name", null, false),
        Field.forList("friends", "friends", null, true, new Field.ObjectReader<Friend1>() {
          @Override public Friend1 read(final ResponseReader reader) throws IOException {
            return friend1FieldMapper.map(reader);
          }
        })
      };

      @Override
      public AsDroid map(ResponseReader reader) throws IOException {
        final String __typename = reader.read(fields[0]);
        final String name = reader.read(fields[1]);
        final List<Friend1> friends = reader.read(fields[2]);
        return new AsDroid(__typename, name, friends);
      }
    }
  }

  public static class Friend1 {
    private final @Nonnull String __typename;

    private final @Nonnull String name;

    private final Optional<AsHuman2> asHuman;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Friend1(@Nonnull String __typename, @Nonnull String name, @Nullable AsHuman2 asHuman) {
      this.__typename = __typename;
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

      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forString("name", "name", null, false),
        Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<AsHuman2>() {
          @Override
          public AsHuman2 read(String conditionalType, ResponseReader reader) throws IOException {
            if (conditionalType.equals("Human")) {
              return asHuman2FieldMapper.map(reader);
            } else {
              return null;
            }
          }
        })
      };

      @Override
      public Friend1 map(ResponseReader reader) throws IOException {
        final String __typename = reader.read(fields[0]);
        final String name = reader.read(fields[1]);
        final AsHuman2 asHuman = reader.read(fields[2]);
        return new Friend1(__typename, name, asHuman);
      }
    }
  }

  public static class AsHuman2 {
    private final @Nonnull String __typename;

    private final @Nonnull String name;

    private final Optional<Double> height;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsHuman2(@Nonnull String __typename, @Nonnull String name, @Nullable Double height) {
      this.__typename = __typename;
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
      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forString("name", "name", null, false),
        Field.forDouble("height", "height", new UnmodifiableMapBuilder<String, Object>(1)
          .put("unit", "METER")
        .build(), true)
      };

      @Override
      public AsHuman2 map(ResponseReader reader) throws IOException {
        final String __typename = reader.read(fields[0]);
        final String name = reader.read(fields[1]);
        final Double height = reader.read(fields[2]);
        return new AsHuman2(__typename, name, height);
      }
    }
  }
}
