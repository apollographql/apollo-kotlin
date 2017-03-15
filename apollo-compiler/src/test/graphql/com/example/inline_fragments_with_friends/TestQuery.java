package com.example.inline_fragments_with_friends;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import com.apollographql.android.api.graphql.internal.Optional;
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
public final class TestQuery implements Query<TestQuery.Data, Operation.Variables> {
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
  public Operation.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<? extends Operation.Data> responseFieldMapper() {
    return new Data.Mapper();
  }

  public static class Data implements Operation.Data {
    private final Optional<Hero> hero;

    public Data(@Nullable Hero hero) {
      this.hero = Optional.fromNullable(hero);
    }

    public Optional<Hero> hero() {
      return this.hero;
    }

    @Override
    public String toString() {
      return "Data{"
        + "hero=" + hero
        + "}";
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
      int h = 1;
      h *= 1000003;
      h ^= (hero == null) ? 0 : hero.hashCode();
      return h;
    }

    public static class Hero {
      private final @Nonnull String name;

      private Optional<AsHuman> asHuman;

      private Optional<AsDroid> asDroid;

      public Hero(@Nonnull String name, @Nullable AsHuman asHuman, @Nullable AsDroid asDroid) {
        this.name = name;
        this.asHuman = Optional.fromNullable(asHuman);
        this.asDroid = Optional.fromNullable(asDroid);
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
        return "Hero{"
          + "name=" + name + ", "
          + "asHuman=" + asHuman + ", "
          + "asDroid=" + asDroid
          + "}";
      }

      @Override
      public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof Hero) {
          Hero that = (Hero) o;
          return ((this.name == null) ? (that.name == null) : this.name.equals(that.name))
           && ((this.asHuman == null) ? (that.asHuman == null) : this.asHuman.equals(that.asHuman))
           && ((this.asDroid == null) ? (that.asDroid == null) : this.asDroid.equals(that.asDroid));
        }
        return false;
      }

      @Override
      public int hashCode() {
        int h = 1;
        h *= 1000003;
        h ^= (name == null) ? 0 : name.hashCode();
        h *= 1000003;
        h ^= (asHuman == null) ? 0 : asHuman.hashCode();
        h *= 1000003;
        h ^= (asDroid == null) ? 0 : asDroid.hashCode();
        return h;
      }

      public static class AsHuman {
        private final @Nonnull String name;

        private final Optional<Double> height;

        private final Optional<List<Friend>> friends;

        public AsHuman(@Nonnull String name, @Nullable Double height,
            @Nullable List<Friend> friends) {
          this.name = name;
          this.height = Optional.fromNullable(height);
          this.friends = Optional.fromNullable(friends);
        }

        public @Nonnull String name() {
          return this.name;
        }

        public Optional<Double> height() {
          return this.height;
        }

        public Optional<List<Friend>> friends() {
          return this.friends;
        }

        @Override
        public String toString() {
          return "AsHuman{"
            + "name=" + name + ", "
            + "height=" + height + ", "
            + "friends=" + friends
            + "}";
        }

        @Override
        public boolean equals(Object o) {
          if (o == this) {
            return true;
          }
          if (o instanceof AsHuman) {
            AsHuman that = (AsHuman) o;
            return ((this.name == null) ? (that.name == null) : this.name.equals(that.name))
             && ((this.height == null) ? (that.height == null) : this.height.equals(that.height))
             && ((this.friends == null) ? (that.friends == null) : this.friends.equals(that.friends));
          }
          return false;
        }

        @Override
        public int hashCode() {
          int h = 1;
          h *= 1000003;
          h ^= (name == null) ? 0 : name.hashCode();
          h *= 1000003;
          h ^= (height == null) ? 0 : height.hashCode();
          h *= 1000003;
          h ^= (friends == null) ? 0 : friends.hashCode();
          return h;
        }

        public static class Friend {
          private final @Nonnull List<Episode> appearsIn;

          public Friend(@Nonnull List<Episode> appearsIn) {
            this.appearsIn = appearsIn;
          }

          public @Nonnull List<Episode> appearsIn() {
            return this.appearsIn;
          }

          @Override
          public String toString() {
            return "Friend{"
              + "appearsIn=" + appearsIn
              + "}";
          }

          @Override
          public boolean equals(Object o) {
            if (o == this) {
              return true;
            }
            if (o instanceof Friend) {
              Friend that = (Friend) o;
              return ((this.appearsIn == null) ? (that.appearsIn == null) : this.appearsIn.equals(that.appearsIn));
            }
            return false;
          }

          @Override
          public int hashCode() {
            int h = 1;
            h *= 1000003;
            h ^= (appearsIn == null) ? 0 : appearsIn.hashCode();
            return h;
          }

          public static final class Mapper implements ResponseFieldMapper<Friend> {
            final Field[] fields = {
              Field.forList("appearsIn", "appearsIn", null, false, new Field.ListReader<Episode>() {
                @Override public Episode read(final Field.ListItemReader reader) throws IOException {
                  return Episode.valueOf(reader.readString());
                }
              })
            };

            @Override
            public Friend map(ResponseReader reader) throws IOException {
              final List<Episode> appearsIn = reader.read(fields[0]);
              return new Friend(appearsIn);
            }
          }
        }

        public static final class Mapper implements ResponseFieldMapper<AsHuman> {
          final Friend.Mapper friendFieldMapper = new Friend.Mapper();

          final Field[] fields = {
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
            final String name = reader.read(fields[0]);
            final Double height = reader.read(fields[1]);
            final List<Friend> friends = reader.read(fields[2]);
            return new AsHuman(name, height, friends);
          }
        }
      }

      public static class AsDroid {
        private final @Nonnull String name;

        private final Optional<List<Friend>> friends;

        private final Optional<String> primaryFunction;

        public AsDroid(@Nonnull String name, @Nullable List<Friend> friends,
            @Nullable String primaryFunction) {
          this.name = name;
          this.friends = Optional.fromNullable(friends);
          this.primaryFunction = Optional.fromNullable(primaryFunction);
        }

        public @Nonnull String name() {
          return this.name;
        }

        public Optional<List<Friend>> friends() {
          return this.friends;
        }

        public Optional<String> primaryFunction() {
          return this.primaryFunction;
        }

        @Override
        public String toString() {
          return "AsDroid{"
            + "name=" + name + ", "
            + "friends=" + friends + ", "
            + "primaryFunction=" + primaryFunction
            + "}";
        }

        @Override
        public boolean equals(Object o) {
          if (o == this) {
            return true;
          }
          if (o instanceof AsDroid) {
            AsDroid that = (AsDroid) o;
            return ((this.name == null) ? (that.name == null) : this.name.equals(that.name))
             && ((this.friends == null) ? (that.friends == null) : this.friends.equals(that.friends))
             && ((this.primaryFunction == null) ? (that.primaryFunction == null) : this.primaryFunction.equals(that.primaryFunction));
          }
          return false;
        }

        @Override
        public int hashCode() {
          int h = 1;
          h *= 1000003;
          h ^= (name == null) ? 0 : name.hashCode();
          h *= 1000003;
          h ^= (friends == null) ? 0 : friends.hashCode();
          h *= 1000003;
          h ^= (primaryFunction == null) ? 0 : primaryFunction.hashCode();
          return h;
        }

        public static class Friend {
          private final @Nonnull String id;

          public Friend(@Nonnull String id) {
            this.id = id;
          }

          public @Nonnull String id() {
            return this.id;
          }

          @Override
          public String toString() {
            return "Friend{"
              + "id=" + id
              + "}";
          }

          @Override
          public boolean equals(Object o) {
            if (o == this) {
              return true;
            }
            if (o instanceof Friend) {
              Friend that = (Friend) o;
              return ((this.id == null) ? (that.id == null) : this.id.equals(that.id));
            }
            return false;
          }

          @Override
          public int hashCode() {
            int h = 1;
            h *= 1000003;
            h ^= (id == null) ? 0 : id.hashCode();
            return h;
          }

          public static final class Mapper implements ResponseFieldMapper<Friend> {
            final Field[] fields = {
              Field.forString("id", "id", null, false)
            };

            @Override
            public Friend map(ResponseReader reader) throws IOException {
              final String id = reader.read(fields[0]);
              return new Friend(id);
            }
          }
        }

        public static final class Mapper implements ResponseFieldMapper<AsDroid> {
          final Friend.Mapper friendFieldMapper = new Friend.Mapper();

          final Field[] fields = {
            Field.forString("name", "name", null, false),
            Field.forList("friends", "friends", null, true, new Field.ObjectReader<Friend>() {
              @Override public Friend read(final ResponseReader reader) throws IOException {
                return friendFieldMapper.map(reader);
              }
            }),
            Field.forString("primaryFunction", "primaryFunction", null, true)
          };

          @Override
          public AsDroid map(ResponseReader reader) throws IOException {
            final String name = reader.read(fields[0]);
            final List<Friend> friends = reader.read(fields[1]);
            final String primaryFunction = reader.read(fields[2]);
            return new AsDroid(name, friends, primaryFunction);
          }
        }
      }

      public static final class Mapper implements ResponseFieldMapper<Hero> {
        final AsHuman.Mapper asHumanFieldMapper = new AsHuman.Mapper();

        final AsDroid.Mapper asDroidFieldMapper = new AsDroid.Mapper();

        final Field[] fields = {
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
          final String name = reader.read(fields[0]);
          final AsHuman asHuman = reader.read(fields[1]);
          final AsDroid asDroid = reader.read(fields[2]);
          return new Hero(name, asHuman, asDroid);
        }
      }
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
}
