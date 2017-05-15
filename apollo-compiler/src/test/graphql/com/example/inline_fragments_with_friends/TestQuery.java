package com.example.inline_fragments_with_friends;

import com.apollographql.apollo.api.Field;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.internal.Optional;
import com.example.inline_fragments_with_friends.type.Episode;
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
public final class TestQuery implements Query<TestQuery.Data, Optional<TestQuery.Data>, Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery {\n"
      + "  hero {\n"
      + "    __typename\n"
      + "    name\n"
      + "    ... on Human {\n"
      + "      __typename\n"
      + "      height\n"
      + "      friends {\n"
      + "        __typename\n"
      + "        appearsIn\n"
      + "      }\n"
      + "    }\n"
      + "    ... on Droid {\n"
      + "      __typename\n"
      + "      primaryFunction\n"
      + "      friends {\n"
      + "        __typename\n"
      + "        id\n"
      + "      }\n"
      + "    }\n"
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
        return ((this.hero == null) ? (that.hero == null) : this.hero.equals(that.hero));
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= (hero == null) ? 0 : hero.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final Hero.Mapper heroFieldMapper = new Hero.Mapper();

      final Field[] fields = {
        Field.forObject("hero", "hero", null, true, new Field.ObjectReader<Hero>() {
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
        return ((this.__typename == null) ? (that.__typename == null) : this.__typename.equals(that.__typename))
         && ((this.name == null) ? (that.name == null) : this.name.equals(that.name))
         && ((this.asHuman == null) ? (that.asHuman == null) : this.asHuman.equals(that.asHuman))
         && ((this.asDroid == null) ? (that.asDroid == null) : this.asDroid.equals(that.asDroid));
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= (__typename == null) ? 0 : __typename.hashCode();
        h *= 1000003;
        h ^= (name == null) ? 0 : name.hashCode();
        h *= 1000003;
        h ^= (asHuman == null) ? 0 : asHuman.hashCode();
        h *= 1000003;
        h ^= (asDroid == null) ? 0 : asDroid.hashCode();
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

    private final Optional<Double> height;

    private final Optional<List<Friend>> friends;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsHuman(@Nonnull String __typename, @Nonnull String name, @Nullable Double height,
        @Nullable List<Friend> friends) {
      this.__typename = __typename;
      this.name = name;
      this.height = Optional.fromNullable(height);
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
     * Height in the preferred unit, default is meters
     */
    public Optional<Double> height() {
      return this.height;
    }

    /**
     * This human's friends, or an empty list if they have none
     */
    public Optional<List<Friend>> friends() {
      return this.friends;
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "AsHuman{"
          + "__typename=" + __typename + ", "
          + "name=" + name + ", "
          + "height=" + height + ", "
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
        return ((this.__typename == null) ? (that.__typename == null) : this.__typename.equals(that.__typename))
         && ((this.name == null) ? (that.name == null) : this.name.equals(that.name))
         && ((this.height == null) ? (that.height == null) : this.height.equals(that.height))
         && ((this.friends == null) ? (that.friends == null) : this.friends.equals(that.friends));
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= (__typename == null) ? 0 : __typename.hashCode();
        h *= 1000003;
        h ^= (name == null) ? 0 : name.hashCode();
        h *= 1000003;
        h ^= (height == null) ? 0 : height.hashCode();
        h *= 1000003;
        h ^= (friends == null) ? 0 : friends.hashCode();
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
        Field.forDouble("height", "height", null, true),
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
        final Double height = reader.read(fields[2]);
        final List<Friend> friends = reader.read(fields[3]);
        return new AsHuman(__typename, name, height, friends);
      }
    }
  }

  public static class Friend {
    private final @Nonnull String __typename;

    private final @Nonnull List<Episode> appearsIn;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Friend(@Nonnull String __typename, @Nonnull List<Episode> appearsIn) {
      this.__typename = __typename;
      this.appearsIn = appearsIn;
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * The movies this character appears in
     */
    public @Nonnull List<Episode> appearsIn() {
      return this.appearsIn;
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Friend{"
          + "__typename=" + __typename + ", "
          + "appearsIn=" + appearsIn
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
        return ((this.__typename == null) ? (that.__typename == null) : this.__typename.equals(that.__typename))
         && ((this.appearsIn == null) ? (that.appearsIn == null) : this.appearsIn.equals(that.appearsIn));
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= (__typename == null) ? 0 : __typename.hashCode();
        h *= 1000003;
        h ^= (appearsIn == null) ? 0 : appearsIn.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Friend> {
      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forList("appearsIn", "appearsIn", null, false, new Field.ListReader<Episode>() {
          @Override public Episode read(final Field.ListItemReader reader) throws IOException {
            return Episode.valueOf(reader.readString());
          }
        })
      };

      @Override
      public Friend map(ResponseReader reader) throws IOException {
        final String __typename = reader.read(fields[0]);
        final List<Episode> appearsIn = reader.read(fields[1]);
        return new Friend(__typename, appearsIn);
      }
    }
  }

  public static class AsDroid {
    private final @Nonnull String __typename;

    private final @Nonnull String name;

    private final Optional<List<Friend1>> friends;

    private final Optional<String> primaryFunction;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsDroid(@Nonnull String __typename, @Nonnull String name,
        @Nullable List<Friend1> friends, @Nullable String primaryFunction) {
      this.__typename = __typename;
      this.name = name;
      this.friends = Optional.fromNullable(friends);
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
     * This droid's friends, or an empty list if they have none
     */
    public Optional<List<Friend1>> friends() {
      return this.friends;
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
          + "friends=" + friends + ", "
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
        return ((this.__typename == null) ? (that.__typename == null) : this.__typename.equals(that.__typename))
         && ((this.name == null) ? (that.name == null) : this.name.equals(that.name))
         && ((this.friends == null) ? (that.friends == null) : this.friends.equals(that.friends))
         && ((this.primaryFunction == null) ? (that.primaryFunction == null) : this.primaryFunction.equals(that.primaryFunction));
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= (__typename == null) ? 0 : __typename.hashCode();
        h *= 1000003;
        h ^= (name == null) ? 0 : name.hashCode();
        h *= 1000003;
        h ^= (friends == null) ? 0 : friends.hashCode();
        h *= 1000003;
        h ^= (primaryFunction == null) ? 0 : primaryFunction.hashCode();
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
        }),
        Field.forString("primaryFunction", "primaryFunction", null, true)
      };

      @Override
      public AsDroid map(ResponseReader reader) throws IOException {
        final String __typename = reader.read(fields[0]);
        final String name = reader.read(fields[1]);
        final List<Friend1> friends = reader.read(fields[2]);
        final String primaryFunction = reader.read(fields[3]);
        return new AsDroid(__typename, name, friends, primaryFunction);
      }
    }
  }

  public static class Friend1 {
    private final @Nonnull String __typename;

    private final @Nonnull String id;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Friend1(@Nonnull String __typename, @Nonnull String id) {
      this.__typename = __typename;
      this.id = id;
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

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Friend1{"
          + "__typename=" + __typename + ", "
          + "id=" + id
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
        return ((this.__typename == null) ? (that.__typename == null) : this.__typename.equals(that.__typename))
         && ((this.id == null) ? (that.id == null) : this.id.equals(that.id));
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= (__typename == null) ? 0 : __typename.hashCode();
        h *= 1000003;
        h ^= (id == null) ? 0 : id.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Friend1> {
      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forString("id", "id", null, false)
      };

      @Override
      public Friend1 map(ResponseReader reader) throws IOException {
        final String __typename = reader.read(fields[0]);
        final String id = reader.read(fields[1]);
        return new Friend1(__typename, id);
      }
    }
  }
}
