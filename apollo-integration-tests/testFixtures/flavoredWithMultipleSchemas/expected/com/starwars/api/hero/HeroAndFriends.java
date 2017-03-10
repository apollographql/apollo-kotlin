package com.starwars.api.hero;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.FragmentResponseFieldMapper;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import com.apollographql.android.api.graphql.util.UnmodifiableMapBuilder;
import com.starwars.api.fragment.HeroDetails;
import com.starwars.api.type.Episode;
import java.io.IOException;
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
public final class HeroAndFriends implements Query<HeroAndFriends.Data, HeroAndFriends.Variables> {
  public static final String OPERATION_DEFINITION = "query HeroAndFriends($episode: Episode) {\n"
      + "  hero(episode: $episode) {\n"
      + "    __typename\n"
      + "    name\n"
      + "    ... on Droid {\n"
      + "      __typename\n"
      + "      appearsIn\n"
      + "    }\n"
      + "    ...HeroDetails\n"
      + "    friends {\n"
      + "      __typename\n"
      + "      name\n"
      + "      ...HeroDetails\n"
      + "    }\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION + "\n"
   + HeroDetails.FRAGMENT_DEFINITION;

  private final HeroAndFriends.Variables variables;

  public HeroAndFriends(HeroAndFriends.Variables variables) {
    this.variables = variables;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public HeroAndFriends.Variables variables() {
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
    private final @Nullable Hero hero;

    public Data(@Nullable Hero hero) {
      this.hero = hero;
    }

    public @Nullable Hero hero() {
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

      private final @Nullable List<Friend> friends;

      private @Nullable AsDroid asDroid;

      private final Fragments fragments;

      public Hero(@Nonnull String name, @Nullable List<Friend> friends, @Nullable AsDroid asDroid,
          Fragments fragments) {
        this.name = name;
        this.friends = friends;
        this.asDroid = asDroid;
        this.fragments = fragments;
      }

      public @Nonnull String name() {
        return this.name;
      }

      public @Nullable List<Friend> friends() {
        return this.friends;
      }

      public @Nullable AsDroid asDroid() {
        return this.asDroid;
      }

      public @Nonnull Fragments fragments() {
        return this.fragments;
      }

      @Override
      public String toString() {
        return "Hero{"
          + "name=" + name + ", "
          + "friends=" + friends + ", "
          + "asDroid=" + asDroid + ", "
          + "fragments=" + fragments
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
           && ((this.friends == null) ? (that.friends == null) : this.friends.equals(that.friends))
           && ((this.asDroid == null) ? (that.asDroid == null) : this.asDroid.equals(that.asDroid))
           && ((this.fragments == null) ? (that.fragments == null) : this.fragments.equals(that.fragments));
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
        h ^= (asDroid == null) ? 0 : asDroid.hashCode();
        h *= 1000003;
        h ^= (fragments == null) ? 0 : fragments.hashCode();
        return h;
      }

      public static class Friend {
        private final @Nonnull String name;

        private final Fragments fragments;

        public Friend(@Nonnull String name, Fragments fragments) {
          this.name = name;
          this.fragments = fragments;
        }

        public @Nonnull String name() {
          return this.name;
        }

        public @Nonnull Fragments fragments() {
          return this.fragments;
        }

        @Override
        public String toString() {
          return "Friend{"
            + "name=" + name + ", "
            + "fragments=" + fragments
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
             && ((this.fragments == null) ? (that.fragments == null) : this.fragments.equals(that.fragments));
          }
          return false;
        }

        @Override
        public int hashCode() {
          int h = 1;
          h *= 1000003;
          h ^= (name == null) ? 0 : name.hashCode();
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

          public static final class Mapper implements FragmentResponseFieldMapper<Fragments> {
            final HeroDetails.Mapper heroDetailsFieldMapper = new HeroDetails.Mapper();

            @Override
            public @Nonnull Fragments map(ResponseReader reader, @Nonnull String conditionalType)
                throws IOException {
              HeroDetails heroDetails = null;
              if (HeroDetails.POSSIBLE_TYPES.contains(conditionalType)) {
                heroDetails = heroDetailsFieldMapper.map(reader);
              }
              return new Fragments(heroDetails);
            }
          }
        }

        public static final class Mapper implements ResponseFieldMapper<Friend> {
          final Fragments.Mapper fragmentsFieldMapper = new Fragments.Mapper();

          final Field[] fields = {
            Field.forString("name", "name", null, false),
            Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<Fragments>() {
              @Override
              public Fragments read(String conditionalType, ResponseReader reader) throws
                  IOException {
                return fragmentsFieldMapper.map(reader, conditionalType);
              }
            })
          };

          @Override
          public Friend map(ResponseReader reader) throws IOException {
            final String name = reader.read(fields[0]);
            final Fragments fragments = reader.read(fields[1]);
            return new Friend(name, fragments);
          }
        }
      }

      public static class AsDroid {
        private final @Nonnull String name;

        private final @Nonnull List<Episode> appearsIn;

        private final @Nullable List<Friend1> friends;

        private final Fragments fragments;

        public AsDroid(@Nonnull String name, @Nonnull List<Episode> appearsIn,
            @Nullable List<Friend1> friends, Fragments fragments) {
          this.name = name;
          this.appearsIn = appearsIn;
          this.friends = friends;
          this.fragments = fragments;
        }

        public @Nonnull String name() {
          return this.name;
        }

        public @Nonnull List<Episode> appearsIn() {
          return this.appearsIn;
        }

        public @Nullable List<Friend1> friends() {
          return this.friends;
        }

        public @Nonnull Fragments fragments() {
          return this.fragments;
        }

        @Override
        public String toString() {
          return "AsDroid{"
            + "name=" + name + ", "
            + "appearsIn=" + appearsIn + ", "
            + "friends=" + friends + ", "
            + "fragments=" + fragments
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
             && ((this.appearsIn == null) ? (that.appearsIn == null) : this.appearsIn.equals(that.appearsIn))
             && ((this.friends == null) ? (that.friends == null) : this.friends.equals(that.friends))
             && ((this.fragments == null) ? (that.fragments == null) : this.fragments.equals(that.fragments));
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
          h *= 1000003;
          h ^= (fragments == null) ? 0 : fragments.hashCode();
          return h;
        }

        public static class Friend1 {
          private final @Nonnull String name;

          private final Fragments fragments;

          public Friend1(@Nonnull String name, Fragments fragments) {
            this.name = name;
            this.fragments = fragments;
          }

          public @Nonnull String name() {
            return this.name;
          }

          public @Nonnull Fragments fragments() {
            return this.fragments;
          }

          @Override
          public String toString() {
            return "Friend1{"
              + "name=" + name + ", "
              + "fragments=" + fragments
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
               && ((this.fragments == null) ? (that.fragments == null) : this.fragments.equals(that.fragments));
            }
            return false;
          }

          @Override
          public int hashCode() {
            int h = 1;
            h *= 1000003;
            h ^= (name == null) ? 0 : name.hashCode();
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

            public static final class Mapper implements FragmentResponseFieldMapper<Fragments> {
              final HeroDetails.Mapper heroDetailsFieldMapper = new HeroDetails.Mapper();

              @Override
              public @Nonnull Fragments map(ResponseReader reader, @Nonnull String conditionalType)
                  throws IOException {
                HeroDetails heroDetails = null;
                if (HeroDetails.POSSIBLE_TYPES.contains(conditionalType)) {
                  heroDetails = heroDetailsFieldMapper.map(reader);
                }
                return new Fragments(heroDetails);
              }
            }
          }

          public static final class Mapper implements ResponseFieldMapper<Friend1> {
            final Fragments.Mapper fragmentsFieldMapper = new Fragments.Mapper();

            final Field[] fields = {
              Field.forString("name", "name", null, false),
              Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<Fragments>() {
                @Override
                public Fragments read(String conditionalType, ResponseReader reader) throws
                    IOException {
                  return fragmentsFieldMapper.map(reader, conditionalType);
                }
              })
            };

            @Override
            public Friend1 map(ResponseReader reader) throws IOException {
              final String name = reader.read(fields[0]);
              final Fragments fragments = reader.read(fields[1]);
              return new Friend1(name, fragments);
            }
          }
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

          public static final class Mapper implements FragmentResponseFieldMapper<Fragments> {
            final HeroDetails.Mapper heroDetailsFieldMapper = new HeroDetails.Mapper();

            @Override
            public @Nonnull Fragments map(ResponseReader reader, @Nonnull String conditionalType)
                throws IOException {
              HeroDetails heroDetails = null;
              if (HeroDetails.POSSIBLE_TYPES.contains(conditionalType)) {
                heroDetails = heroDetailsFieldMapper.map(reader);
              }
              return new Fragments(heroDetails);
            }
          }
        }

        public static final class Mapper implements ResponseFieldMapper<AsDroid> {
          final Friend1.Mapper friend1FieldMapper = new Friend1.Mapper();

          final Fragments.Mapper fragmentsFieldMapper = new Fragments.Mapper();

          final Field[] fields = {
            Field.forString("name", "name", null, false),
            Field.forList("appearsIn", "appearsIn", null, false, new Field.ListReader<Episode>() {
              @Override public Episode read(final Field.ListItemReader reader) throws IOException {
                return Episode.valueOf(reader.readString());
              }
            }),
            Field.forList("friends", "friends", null, true, new Field.ObjectReader<Friend1>() {
              @Override public Friend1 read(final ResponseReader reader) throws IOException {
                return friend1FieldMapper.map(reader);
              }
            }),
            Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<Fragments>() {
              @Override
              public Fragments read(String conditionalType, ResponseReader reader) throws
                  IOException {
                return fragmentsFieldMapper.map(reader, conditionalType);
              }
            })
          };

          @Override
          public AsDroid map(ResponseReader reader) throws IOException {
            final String name = reader.read(fields[0]);
            final List<Episode> appearsIn = reader.read(fields[1]);
            final List<Friend1> friends = reader.read(fields[2]);
            final Fragments fragments = reader.read(fields[3]);
            return new AsDroid(name, appearsIn, friends, fragments);
          }
        }
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

        public static final class Mapper implements FragmentResponseFieldMapper<Fragments> {
          final HeroDetails.Mapper heroDetailsFieldMapper = new HeroDetails.Mapper();

          @Override
          public @Nonnull Fragments map(ResponseReader reader, @Nonnull String conditionalType)
              throws IOException {
            HeroDetails heroDetails = null;
            if (HeroDetails.POSSIBLE_TYPES.contains(conditionalType)) {
              heroDetails = heroDetailsFieldMapper.map(reader);
            }
            return new Fragments(heroDetails);
          }
        }
      }

      public static final class Mapper implements ResponseFieldMapper<Hero> {
        final Friend.Mapper friendFieldMapper = new Friend.Mapper();

        final AsDroid.Mapper asDroidFieldMapper = new AsDroid.Mapper();

        final Fragments.Mapper fragmentsFieldMapper = new Fragments.Mapper();

        final Field[] fields = {
          Field.forString("name", "name", null, false),
          Field.forList("friends", "friends", null, true, new Field.ObjectReader<Friend>() {
            @Override public Friend read(final ResponseReader reader) throws IOException {
              return friendFieldMapper.map(reader);
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
          }),
          Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<Fragments>() {
            @Override
            public Fragments read(String conditionalType, ResponseReader reader) throws
                IOException {
              return fragmentsFieldMapper.map(reader, conditionalType);
            }
          })
        };

        @Override
        public Hero map(ResponseReader reader) throws IOException {
          final String name = reader.read(fields[0]);
          final List<Friend> friends = reader.read(fields[1]);
          final AsDroid asDroid = reader.read(fields[2]);
          final Fragments fragments = reader.read(fields[3]);
          return new Hero(name, friends, asDroid, fragments);
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
