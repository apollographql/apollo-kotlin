package com.example.unique_type_name;

import com.apollographql.apollo.api.Field;
import com.apollographql.apollo.api.FragmentResponseFieldMapper;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.internal.Optional;
import com.example.unique_type_name.fragment.HeroDetails;
import com.example.unique_type_name.type.Episode;
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
public final class HeroDetailQuery implements Query<HeroDetailQuery.Data, Optional<HeroDetailQuery.Data>, Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query HeroDetailQuery {\n"
      + "  heroDetailQuery {\n"
      + "    __typename\n"
      + "    name\n"
      + "    friends {\n"
      + "      __typename\n"
      + "      name\n"
      + "    }\n"
      + "    ... on Human {\n"
      + "      __typename\n"
      + "      height\n"
      + "      friends {\n"
      + "        __typename\n"
      + "        appearsIn\n"
      + "        friends {\n"
      + "          __typename\n"
      + "          ...HeroDetails\n"
      + "        }\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION + "\n"
   + HeroDetails.FRAGMENT_DEFINITION;

  private static final OperationName OPERATION_NAME = new OperationName() {
    @Override
    public String name() {
      return "HeroDetailQuery";
    }
  };

  private final Operation.Variables variables;

  public HeroDetailQuery() {
    this.variables = Operation.EMPTY_VARIABLES;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public Optional<HeroDetailQuery.Data> wrapData(HeroDetailQuery.Data data) {
    return Optional.fromNullable(data);
  }

  @Override
  public Operation.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<HeroDetailQuery.Data> responseFieldMapper() {
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

    public HeroDetailQuery build() {
      return new HeroDetailQuery();
    }
  }

  public static class Data implements Operation.Data {
    private final Optional<HeroDetailQuery1> heroDetailQuery;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Data(@Nullable HeroDetailQuery1 heroDetailQuery) {
      this.heroDetailQuery = Optional.fromNullable(heroDetailQuery);
    }

    public Optional<HeroDetailQuery1> heroDetailQuery() {
      return this.heroDetailQuery;
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Data{"
          + "heroDetailQuery=" + heroDetailQuery
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
        return this.heroDetailQuery.equals(that.heroDetailQuery);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= heroDetailQuery.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final HeroDetailQuery1.Mapper heroDetailQuery1FieldMapper = new HeroDetailQuery1.Mapper();

      final Field[] fields = {
        Field.forObject("heroDetailQuery", "heroDetailQuery", null, true, new Field.ObjectReader<HeroDetailQuery1>() {
          @Override public HeroDetailQuery1 read(final ResponseReader reader) throws IOException {
            return heroDetailQuery1FieldMapper.map(reader);
          }
        })
      };

      @Override
      public Data map(ResponseReader reader) throws IOException {
        final HeroDetailQuery1 heroDetailQuery = reader.read(fields[0]);
        return new Data(heroDetailQuery);
      }
    }
  }

  public static class HeroDetailQuery1 {
    private final @Nonnull String __typename;

    private final @Nonnull String name;

    private final Optional<List<Friend>> friends;

    private final Optional<AsHuman> asHuman;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public HeroDetailQuery1(@Nonnull String __typename, @Nonnull String name,
        @Nullable List<Friend> friends, @Nullable AsHuman asHuman) {
      this.__typename = __typename;
      this.name = name;
      this.friends = Optional.fromNullable(friends);
      this.asHuman = Optional.fromNullable(asHuman);
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

    /**
     * The friends of the character, or an empty list if they have none
     */
    public Optional<List<Friend>> friends() {
      return this.friends;
    }

    public Optional<AsHuman> asHuman() {
      return this.asHuman;
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "HeroDetailQuery1{"
          + "__typename=" + __typename + ", "
          + "name=" + name + ", "
          + "friends=" + friends + ", "
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
      if (o instanceof HeroDetailQuery1) {
        HeroDetailQuery1 that = (HeroDetailQuery1) o;
        return this.__typename.equals(that.__typename)
         && this.name.equals(that.name)
         && this.friends.equals(that.friends)
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
        h ^= friends.hashCode();
        h *= 1000003;
        h ^= asHuman.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<HeroDetailQuery1> {
      final Friend.Mapper friendFieldMapper = new Friend.Mapper();

      final AsHuman.Mapper asHumanFieldMapper = new AsHuman.Mapper();

      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forString("name", "name", null, false),
        Field.forList("friends", "friends", null, true, new Field.ObjectReader<Friend>() {
          @Override public Friend read(final ResponseReader reader) throws IOException {
            return friendFieldMapper.map(reader);
          }
        }),
        Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<AsHuman>() {
          @Override
          public AsHuman read(String conditionalType, ResponseReader reader) throws IOException {
            if (conditionalType.equals("Human")) {
              return asHumanFieldMapper.map(reader);
            } else {
              return null;
            }
          }
        })
      };

      @Override
      public HeroDetailQuery1 map(ResponseReader reader) throws IOException {
        final String __typename = reader.read(fields[0]);
        final String name = reader.read(fields[1]);
        final List<Friend> friends = reader.read(fields[2]);
        final AsHuman asHuman = reader.read(fields[3]);
        return new HeroDetailQuery1(__typename, name, friends, asHuman);
      }
    }
  }

  public static class Friend {
    private final @Nonnull String __typename;

    private final @Nonnull String name;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Friend(@Nonnull String __typename, @Nonnull String name) {
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
        $toString = "Friend{"
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
      if (o instanceof Friend) {
        Friend that = (Friend) o;
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

    public static final class Mapper implements ResponseFieldMapper<Friend> {
      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forString("name", "name", null, false)
      };

      @Override
      public Friend map(ResponseReader reader) throws IOException {
        final String __typename = reader.read(fields[0]);
        final String name = reader.read(fields[1]);
        return new Friend(__typename, name);
      }
    }
  }

  public static class AsHuman {
    private final @Nonnull String __typename;

    private final @Nonnull String name;

    private final Optional<List<Friend1>> friends;

    private final Optional<Double> height;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsHuman(@Nonnull String __typename, @Nonnull String name,
        @Nullable List<Friend1> friends, @Nullable Double height) {
      this.__typename = __typename;
      this.name = name;
      this.friends = Optional.fromNullable(friends);
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
     * This human's friends, or an empty list if they have none
     */
    public Optional<List<Friend1>> friends() {
      return this.friends;
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
          + "friends=" + friends + ", "
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
         && this.friends.equals(that.friends)
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
        h ^= friends.hashCode();
        h *= 1000003;
        h ^= height.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<AsHuman> {
      final Friend1.Mapper friend1FieldMapper = new Friend1.Mapper();

      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forString("name", "name", null, false),
        Field.forList("friends", "friends", null, true, new Field.ObjectReader<Friend1>() {
          @Override public Friend1 read(final ResponseReader reader) throws IOException {
            return friend1FieldMapper.map(reader);
          }
        }),
        Field.forDouble("height", "height", null, true)
      };

      @Override
      public AsHuman map(ResponseReader reader) throws IOException {
        final String __typename = reader.read(fields[0]);
        final String name = reader.read(fields[1]);
        final List<Friend1> friends = reader.read(fields[2]);
        final Double height = reader.read(fields[3]);
        return new AsHuman(__typename, name, friends, height);
      }
    }
  }

  public static class Friend1 {
    private final @Nonnull String __typename;

    private final @Nonnull String name;

    private final @Nonnull List<Episode> appearsIn;

    private final Optional<List<Friend2>> friends;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Friend1(@Nonnull String __typename, @Nonnull String name,
        @Nonnull List<Episode> appearsIn, @Nullable List<Friend2> friends) {
      this.__typename = __typename;
      this.name = name;
      this.appearsIn = appearsIn;
      this.friends = Optional.fromNullable(friends);
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

    /**
     * The movies this character appears in
     */
    public @Nonnull List<Episode> appearsIn() {
      return this.appearsIn;
    }

    /**
     * The friends of the character, or an empty list if they have none
     */
    public Optional<List<Friend2>> friends() {
      return this.friends;
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Friend1{"
          + "__typename=" + __typename + ", "
          + "name=" + name + ", "
          + "appearsIn=" + appearsIn + ", "
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
      if (o instanceof Friend1) {
        Friend1 that = (Friend1) o;
        return this.__typename.equals(that.__typename)
         && this.name.equals(that.name)
         && this.appearsIn.equals(that.appearsIn)
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
        h ^= appearsIn.hashCode();
        h *= 1000003;
        h ^= friends.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Friend1> {
      final Friend2.Mapper friend2FieldMapper = new Friend2.Mapper();

      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forString("name", "name", null, false),
        Field.forList("appearsIn", "appearsIn", null, false, new Field.ListReader<Episode>() {
          @Override public Episode read(final Field.ListItemReader reader) throws IOException {
            return Episode.valueOf(reader.readString());
          }
        }),
        Field.forList("friends", "friends", null, true, new Field.ObjectReader<Friend2>() {
          @Override public Friend2 read(final ResponseReader reader) throws IOException {
            return friend2FieldMapper.map(reader);
          }
        })
      };

      @Override
      public Friend1 map(ResponseReader reader) throws IOException {
        final String __typename = reader.read(fields[0]);
        final String name = reader.read(fields[1]);
        final List<Episode> appearsIn = reader.read(fields[2]);
        final List<Friend2> friends = reader.read(fields[3]);
        return new Friend1(__typename, name, appearsIn, friends);
      }
    }
  }

  public static class Friend2 {
    private final @Nonnull String __typename;

    private final @Nonnull Fragments fragments;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Friend2(@Nonnull String __typename, @Nonnull Fragments fragments) {
      this.__typename = __typename;
      this.fragments = fragments;
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    public @Nonnull Fragments fragments() {
      return this.fragments;
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Friend2{"
          + "__typename=" + __typename + ", "
          + "fragments=" + fragments
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Friend2) {
        Friend2 that = (Friend2) o;
        return this.__typename.equals(that.__typename)
         && this.fragments.equals(that.fragments);
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
        h ^= fragments.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static class Fragments {
      private final @Nonnull HeroDetails heroDetails;

      private volatile String $toString;

      private volatile int $hashCode;

      private volatile boolean $hashCodeMemoized;

      public Fragments(@Nonnull HeroDetails heroDetails) {
        this.heroDetails = heroDetails;
      }

      public @Nonnull HeroDetails heroDetails() {
        return this.heroDetails;
      }

      @Override
      public String toString() {
        if ($toString == null) {
          $toString = "Fragments{"
            + "heroDetails=" + heroDetails
            + "}";
        }
        return $toString;
      }

      @Override
      public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof Fragments) {
          Fragments that = (Fragments) o;
          return this.heroDetails.equals(that.heroDetails);
        }
        return false;
      }

      @Override
      public int hashCode() {
        if (!$hashCodeMemoized) {
          int h = 1;
          h *= 1000003;
          h ^= heroDetails.hashCode();
          $hashCode = h;
          $hashCodeMemoized = true;
        }
        return $hashCode;
      }

      public static final class Mapper implements FragmentResponseFieldMapper<Fragments> {
        final HeroDetails.Mapper heroDetailsFieldMapper = new HeroDetails.Mapper();

        @Override
        public @Nonnull Fragments map(ResponseReader reader, @Nonnull String conditionalType) throws
            IOException {
          HeroDetails heroDetails = null;
          if (HeroDetails.POSSIBLE_TYPES.contains(conditionalType)) {
            heroDetails = heroDetailsFieldMapper.map(reader);
          }
          return new Fragments(heroDetails);
        }
      }
    }

    public static final class Mapper implements ResponseFieldMapper<Friend2> {
      final Fragments.Mapper fragmentsFieldMapper = new Fragments.Mapper();

      final Field[] fields = {
        Field.forString("__typename", "__typename", null, false),
        Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<Fragments>() {
          @Override
          public Fragments read(String conditionalType, ResponseReader reader) throws IOException {
            return fragmentsFieldMapper.map(reader, conditionalType);
          }
        })
      };

      @Override
      public Friend2 map(ResponseReader reader) throws IOException {
        final String __typename = reader.read(fields[0]);
        final Fragments fragments = reader.read(fields[1]);
        return new Friend2(__typename, fragments);
      }
    }
  }
}
