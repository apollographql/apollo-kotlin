package com.example.unique_type_name;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
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
public final class HeroDetailQuery implements Query<Operation.Variables> {
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

  private final Operation.Variables variables;

  public HeroDetailQuery() {
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
    private final @Nullable HeroDetailQuery1 heroDetailQuery;

    public Data(@Nullable HeroDetailQuery1 heroDetailQuery) {
      this.heroDetailQuery = heroDetailQuery;
    }

    public @Nullable HeroDetailQuery1 heroDetailQuery() {
      return this.heroDetailQuery;
    }

    @Override
    public String toString() {
      return "Data{"
        + "heroDetailQuery=" + heroDetailQuery
        + "}";
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Data) {
        Data that = (Data) o;
        return ((this.heroDetailQuery == null) ? (that.heroDetailQuery == null) : this.heroDetailQuery.equals(that.heroDetailQuery));
      }
      return false;
    }

    @Override
    public int hashCode() {
      int h = 1;
      h *= 1000003;
      h ^= (heroDetailQuery == null) ? 0 : heroDetailQuery.hashCode();
      return h;
    }

    public static class HeroDetailQuery1 {
      private final @Nonnull String name;

      private final @Nullable List<Friend> friends;

      private @Nullable AsHuman asHuman;

      public HeroDetailQuery1(@Nonnull String name, @Nullable List<Friend> friends,
          @Nullable AsHuman asHuman) {
        this.name = name;
        this.friends = friends;
        this.asHuman = asHuman;
      }

      public @Nonnull String name() {
        return this.name;
      }

      public @Nullable List<Friend> friends() {
        return this.friends;
      }

      public @Nullable AsHuman asHuman() {
        return this.asHuman;
      }

      @Override
      public String toString() {
        return "HeroDetailQuery1{"
          + "name=" + name + ", "
          + "friends=" + friends + ", "
          + "asHuman=" + asHuman
          + "}";
      }

      @Override
      public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof HeroDetailQuery1) {
          HeroDetailQuery1 that = (HeroDetailQuery1) o;
          return ((this.name == null) ? (that.name == null) : this.name.equals(that.name))
           && ((this.friends == null) ? (that.friends == null) : this.friends.equals(that.friends))
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
        h ^= (friends == null) ? 0 : friends.hashCode();
        h *= 1000003;
        h ^= (asHuman == null) ? 0 : asHuman.hashCode();
        return h;
      }

      public static class Friend {
        private final @Nonnull String name;

        public Friend(@Nonnull String name) {
          this.name = name;
        }

        public @Nonnull String name() {
          return this.name;
        }

        @Override
        public String toString() {
          return "Friend{"
            + "name=" + name
            + "}";
        }

        @Override
        public boolean equals(Object o) {
          if (o == this) {
            return true;
          }
          if (o instanceof Friend) {
            Friend that = (Friend) o;
            return ((this.name == null) ? (that.name == null) : this.name.equals(that.name));
          }
          return false;
        }

        @Override
        public int hashCode() {
          int h = 1;
          h *= 1000003;
          h ^= (name == null) ? 0 : name.hashCode();
          return h;
        }

        public static final class Mapper implements ResponseFieldMapper<Friend> {
          final Field[] fields = {
            Field.forString("name", "name", null, false)
          };

          @Override
          public Friend map(ResponseReader reader) throws IOException {
            final String name = reader.read(fields[0]);
            return new Friend(name);
          }
        }
      }

      public static class AsHuman {
        private final @Nonnull String name;

        private final @Nullable List<Friend1> friends;

        private final @Nullable Double height;

        public AsHuman(@Nonnull String name, @Nullable List<Friend1> friends,
            @Nullable Double height) {
          this.name = name;
          this.friends = friends;
          this.height = height;
        }

        public @Nonnull String name() {
          return this.name;
        }

        public @Nullable List<Friend1> friends() {
          return this.friends;
        }

        public @Nullable Double height() {
          return this.height;
        }

        @Override
        public String toString() {
          return "AsHuman{"
            + "name=" + name + ", "
            + "friends=" + friends + ", "
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
             && ((this.friends == null) ? (that.friends == null) : this.friends.equals(that.friends))
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
          h ^= (friends == null) ? 0 : friends.hashCode();
          h *= 1000003;
          h ^= (height == null) ? 0 : height.hashCode();
          return h;
        }

        public static class Friend1 {
          private final @Nonnull String name;

          private final @Nonnull List<Episode> appearsIn;

          private final @Nullable List<Friend2> friends;

          public Friend1(@Nonnull String name, @Nonnull List<Episode> appearsIn,
              @Nullable List<Friend2> friends) {
            this.name = name;
            this.appearsIn = appearsIn;
            this.friends = friends;
          }

          public @Nonnull String name() {
            return this.name;
          }

          public @Nonnull List<Episode> appearsIn() {
            return this.appearsIn;
          }

          public @Nullable List<Friend2> friends() {
            return this.friends;
          }

          @Override
          public String toString() {
            return "Friend1{"
              + "name=" + name + ", "
              + "appearsIn=" + appearsIn + ", "
              + "friends=" + friends
              + "}";
          }

          @Override
          public boolean equals(Object o) {
            if (o == this) {
              return true;
            }
            if (o instanceof Friend1) {
              Friend1 that = (Friend1) o;
              return ((this.name == null) ? (that.name == null) : this.name.equals(that.name))
               && ((this.appearsIn == null) ? (that.appearsIn == null) : this.appearsIn.equals(that.appearsIn))
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
            h ^= (appearsIn == null) ? 0 : appearsIn.hashCode();
            h *= 1000003;
            h ^= (friends == null) ? 0 : friends.hashCode();
            return h;
          }

          public static class Friend2 {
            private final Fragments fragments;

            public Friend2(Fragments fragments) {
              this.fragments = fragments;
            }

            public @Nonnull Fragments fragments() {
              return this.fragments;
            }

            @Override
            public String toString() {
              return "Friend2{"
                + "fragments=" + fragments
                + "}";
            }

            @Override
            public boolean equals(Object o) {
              if (o == this) {
                return true;
              }
              if (o instanceof Friend2) {
                Friend2 that = (Friend2) o;
                return ((this.fragments == null) ? (that.fragments == null) : this.fragments.equals(that.fragments));
              }
              return false;
            }

            @Override
            public int hashCode() {
              int h = 1;
              h *= 1000003;
              h ^= (fragments == null) ? 0 : fragments.hashCode();
              return h;
            }

            public static class Fragments {
              private HeroDetails heroDetails;

              public Fragments(HeroDetails heroDetails) {
                this.heroDetails = heroDetails;
              }

              public @Nullable HeroDetails heroDetails() {
                return this.heroDetails;
              }

              @Override
              public String toString() {
                return "Fragments{"
                  + "heroDetails=" + heroDetails
                  + "}";
              }

              @Override
              public boolean equals(Object o) {
                if (o == this) {
                  return true;
                }
                if (o instanceof Fragments) {
                  Fragments that = (Fragments) o;
                  return ((this.heroDetails == null) ? (that.heroDetails == null) : this.heroDetails.equals(that.heroDetails));
                }
                return false;
              }

              @Override
              public int hashCode() {
                int h = 1;
                h *= 1000003;
                h ^= (heroDetails == null) ? 0 : heroDetails.hashCode();
                return h;
              }

              public static final class Mapper implements ResponseFieldMapper<Fragments> {
                private final String conditionalType;

                public Mapper(@Nonnull String conditionalType) {
                  this.conditionalType = conditionalType;
                }

                @Override
                public @Nonnull Fragments map(ResponseReader reader) throws IOException {
                  HeroDetails heroDetails = null;
                  if (conditionalType.equals(HeroDetails.TYPE_CONDITION)) {
                    heroDetails = new HeroDetails.Mapper().map(reader);
                  }
                  return new Fragments(heroDetails);
                }
              }
            }

            public static final class Mapper implements ResponseFieldMapper<Friend2> {
              final Field[] fields = {
                Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<Fragments>() {
                  @Override
                  public Fragments read(String conditionalType, ResponseReader reader) throws
                      IOException {
                    return new Fragments.Mapper(conditionalType).map(reader);
                  }
                })
              };

              @Override
              public Friend2 map(ResponseReader reader) throws IOException {
                final Fragments fragments = reader.read(fields[0]);
                return new Friend2(fragments);
              }
            }
          }

          public static final class Mapper implements ResponseFieldMapper<Friend1> {
            final Field[] fields = {
              Field.forString("name", "name", null, false),
              Field.forList("appearsIn", "appearsIn", null, false, new Field.ListReader<Episode>() {
                @Override public Episode read(final Field.ListItemReader reader) throws IOException {
                  return Episode.valueOf(reader.readString());
                }
              }),
              Field.forList("friends", "friends", null, true, new Field.ObjectReader<Friend2>() {
                @Override public Friend2 read(final ResponseReader reader) throws IOException {
                  return new Friend2.Mapper().map(reader);
                }
              })
            };

            @Override
            public Friend1 map(ResponseReader reader) throws IOException {
              final String name = reader.read(fields[0]);
              final List<Episode> appearsIn = reader.read(fields[1]);
              final List<Friend2> friends = reader.read(fields[2]);
              return new Friend1(name, appearsIn, friends);
            }
          }
        }

        public static final class Mapper implements ResponseFieldMapper<AsHuman> {
          final Field[] fields = {
            Field.forString("name", "name", null, false),
            Field.forList("friends", "friends", null, true, new Field.ObjectReader<Friend1>() {
              @Override public Friend1 read(final ResponseReader reader) throws IOException {
                return new Friend1.Mapper().map(reader);
              }
            }),
            Field.forDouble("height", "height", null, true)
          };

          @Override
          public AsHuman map(ResponseReader reader) throws IOException {
            final String name = reader.read(fields[0]);
            final List<Friend1> friends = reader.read(fields[1]);
            final Double height = reader.read(fields[2]);
            return new AsHuman(name, friends, height);
          }
        }
      }

      public static final class Mapper implements ResponseFieldMapper<HeroDetailQuery1> {
        final Field[] fields = {
          Field.forString("name", "name", null, false),
          Field.forList("friends", "friends", null, true, new Field.ObjectReader<Friend>() {
            @Override public Friend read(final ResponseReader reader) throws IOException {
              return new Friend.Mapper().map(reader);
            }
          }),
          Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<AsHuman>() {
            @Override
            public AsHuman read(String conditionalType, ResponseReader reader) throws IOException {
              if (conditionalType.equals("Human")) {
                return new AsHuman.Mapper().map(reader);
              } else {
                return null;
              }
            }
          })
        };

        @Override
        public HeroDetailQuery1 map(ResponseReader reader) throws IOException {
          final String name = reader.read(fields[0]);
          final List<Friend> friends = reader.read(fields[1]);
          final AsHuman asHuman = reader.read(fields[2]);
          return new HeroDetailQuery1(name, friends, asHuman);
        }
      }
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final Field[] fields = {
        Field.forObject("heroDetailQuery", "heroDetailQuery", null, true, new Field.ObjectReader<HeroDetailQuery1>() {
          @Override public HeroDetailQuery1 read(final ResponseReader reader) throws IOException {
            return new HeroDetailQuery1.Mapper().map(reader);
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
}
