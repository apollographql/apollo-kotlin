package com.example.nested_conditional_inline;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import com.apollographql.android.api.graphql.internal.Optional;
import com.apollographql.android.api.graphql.util.UnmodifiableMapBuilder;
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
public final class TestQuery implements Query<TestQuery.Data, TestQuery.Variables> {
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

  public TestQuery(TestQuery.Variables variables) {
    this.variables = variables;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public TestQuery.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<? extends Operation.Data> responseFieldMapper() {
    return new Data.Mapper();
  }

  public static final class Variables extends Operation.Variables {
    private final @Nullable Episode episode;

    private final Map<String, Object> valueMap = new LinkedHashMap<>();

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

    public static Builder builder() {
      return new Builder();
    }

    public static final class Builder {
      private @Nullable Episode episode;

      Builder() {
      }

      public Builder episode(@Nullable Episode episode) {
        this.episode = episode;
        return this;
      }

      public Variables build() {
        return new Variables(episode);
      }
    }
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

        private final Optional<List<Friend>> friends;

        public AsHuman(@Nonnull String name, @Nullable List<Friend> friends) {
          this.name = name;
          this.friends = Optional.fromNullable(friends);
        }

        public @Nonnull String name() {
          return this.name;
        }

        public Optional<List<Friend>> friends() {
          return this.friends;
        }

        @Override
        public String toString() {
          return "AsHuman{"
            + "name=" + name + ", "
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
          h ^= (friends == null) ? 0 : friends.hashCode();
          return h;
        }

        public static class Friend {
          private final @Nonnull String name;

          private Optional<AsHuman1> asHuman;

          public Friend(@Nonnull String name, @Nullable AsHuman1 asHuman) {
            this.name = name;
            this.asHuman = Optional.fromNullable(asHuman);
          }

          public @Nonnull String name() {
            return this.name;
          }

          public Optional<AsHuman1> asHuman() {
            return this.asHuman;
          }

          @Override
          public String toString() {
            return "Friend{"
              + "name=" + name + ", "
              + "asHuman=" + asHuman
              + "}";
          }

          @Override
          public boolean equals(Object o) {
            if (o == this) {
              return true;
            }
            if (o instanceof Friend) {
              Friend that = (Friend) o;
              return ((this.name == null) ? (that.name == null) : this.name.equals(that.name))
               && ((this.asHuman == null) ? (that.asHuman == null) : this.asHuman.equals(that.asHuman));
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
            return h;
          }

          public static class AsHuman1 {
            private final @Nonnull String name;

            private final Optional<Double> height;

            public AsHuman1(@Nonnull String name, @Nullable Double height) {
              this.name = name;
              this.height = Optional.fromNullable(height);
            }

            public @Nonnull String name() {
              return this.name;
            }

            public Optional<Double> height() {
              return this.height;
            }

            @Override
            public String toString() {
              return "AsHuman1{"
                + "name=" + name + ", "
                + "height=" + height
                + "}";
            }

            @Override
            public boolean equals(Object o) {
              if (o == this) {
                return true;
              }
              if (o instanceof AsHuman1) {
                AsHuman1 that = (AsHuman1) o;
                return ((this.name == null) ? (that.name == null) : this.name.equals(that.name))
                 && ((this.height == null) ? (that.height == null) : this.height.equals(that.height));
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
              return h;
            }

            public static final class Mapper implements ResponseFieldMapper<AsHuman1> {
              final Field[] fields = {
                Field.forString("name", "name", null, false),
                Field.forDouble("height", "height", new UnmodifiableMapBuilder<String, Object>(1)
                  .put("unit", "FOOT")
                .build(), true)
              };

              @Override
              public AsHuman1 map(ResponseReader reader) throws IOException {
                final String name = reader.read(fields[0]);
                final Double height = reader.read(fields[1]);
                return new AsHuman1(name, height);
              }
            }
          }

          public static final class Mapper implements ResponseFieldMapper<Friend> {
            final AsHuman1.Mapper asHuman1FieldMapper = new AsHuman1.Mapper();

            final Field[] fields = {
              Field.forString("name", "name", null, false),
              Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<AsHuman1>() {
                @Override
                public AsHuman1 read(String conditionalType, ResponseReader reader) throws
                    IOException {
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
              final String name = reader.read(fields[0]);
              final AsHuman1 asHuman = reader.read(fields[1]);
              return new Friend(name, asHuman);
            }
          }
        }

        public static final class Mapper implements ResponseFieldMapper<AsHuman> {
          final Friend.Mapper friendFieldMapper = new Friend.Mapper();

          final Field[] fields = {
            Field.forString("name", "name", null, false),
            Field.forList("friends", "friends", null, true, new Field.ObjectReader<Friend>() {
              @Override public Friend read(final ResponseReader reader) throws IOException {
                return friendFieldMapper.map(reader);
              }
            })
          };

          @Override
          public AsHuman map(ResponseReader reader) throws IOException {
            final String name = reader.read(fields[0]);
            final List<Friend> friends = reader.read(fields[1]);
            return new AsHuman(name, friends);
          }
        }
      }

      public static class AsDroid {
        private final @Nonnull String name;

        private final Optional<List<Friend>> friends;

        public AsDroid(@Nonnull String name, @Nullable List<Friend> friends) {
          this.name = name;
          this.friends = Optional.fromNullable(friends);
        }

        public @Nonnull String name() {
          return this.name;
        }

        public Optional<List<Friend>> friends() {
          return this.friends;
        }

        @Override
        public String toString() {
          return "AsDroid{"
            + "name=" + name + ", "
            + "friends=" + friends
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
          h ^= (friends == null) ? 0 : friends.hashCode();
          return h;
        }

        public static class Friend {
          private final @Nonnull String name;

          private Optional<AsHuman> asHuman;

          public Friend(@Nonnull String name, @Nullable AsHuman asHuman) {
            this.name = name;
            this.asHuman = Optional.fromNullable(asHuman);
          }

          public @Nonnull String name() {
            return this.name;
          }

          public Optional<AsHuman> asHuman() {
            return this.asHuman;
          }

          @Override
          public String toString() {
            return "Friend{"
              + "name=" + name + ", "
              + "asHuman=" + asHuman
              + "}";
          }

          @Override
          public boolean equals(Object o) {
            if (o == this) {
              return true;
            }
            if (o instanceof Friend) {
              Friend that = (Friend) o;
              return ((this.name == null) ? (that.name == null) : this.name.equals(that.name))
               && ((this.asHuman == null) ? (that.asHuman == null) : this.asHuman.equals(that.asHuman));
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
            return h;
          }

          public static class AsHuman {
            private final @Nonnull String name;

            private final Optional<Double> height;

            public AsHuman(@Nonnull String name, @Nullable Double height) {
              this.name = name;
              this.height = Optional.fromNullable(height);
            }

            public @Nonnull String name() {
              return this.name;
            }

            public Optional<Double> height() {
              return this.height;
            }

            @Override
            public String toString() {
              return "AsHuman{"
                + "name=" + name + ", "
                + "height=" + height
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
                 && ((this.height == null) ? (that.height == null) : this.height.equals(that.height));
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
              return h;
            }

            public static final class Mapper implements ResponseFieldMapper<AsHuman> {
              final Field[] fields = {
                Field.forString("name", "name", null, false),
                Field.forDouble("height", "height", new UnmodifiableMapBuilder<String, Object>(1)
                  .put("unit", "METER")
                .build(), true)
              };

              @Override
              public AsHuman map(ResponseReader reader) throws IOException {
                final String name = reader.read(fields[0]);
                final Double height = reader.read(fields[1]);
                return new AsHuman(name, height);
              }
            }
          }

          public static final class Mapper implements ResponseFieldMapper<Friend> {
            final AsHuman.Mapper asHumanFieldMapper = new AsHuman.Mapper();

            final Field[] fields = {
              Field.forString("name", "name", null, false),
              Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<AsHuman>() {
                @Override
                public AsHuman read(String conditionalType, ResponseReader reader) throws
                    IOException {
                  if (conditionalType.equals("Human")) {
                    return asHumanFieldMapper.map(reader);
                  } else {
                    return null;
                  }
                }
              })
            };

            @Override
            public Friend map(ResponseReader reader) throws IOException {
              final String name = reader.read(fields[0]);
              final AsHuman asHuman = reader.read(fields[1]);
              return new Friend(name, asHuman);
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
            })
          };

          @Override
          public AsDroid map(ResponseReader reader) throws IOException {
            final String name = reader.read(fields[0]);
            final List<Friend> friends = reader.read(fields[1]);
            return new AsDroid(name, friends);
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
}
