package com.example.pojo_fragment_with_inline_fragment;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import com.example.pojo_fragment_with_inline_fragment.fragment.HeroDetails;
import com.example.pojo_fragment_with_inline_fragment.type.Episode;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery {\n"
      + "  hero {\n"
      + "    __typename\n"
      + "    name\n"
      + "    ...HeroDetails\n"
      + "    appearsIn\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION + "\n"
   + HeroDetails.FRAGMENT_DEFINITION;

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

  public static class Data implements Operation.Data {
    public static final Creator CREATOR = new Creator() {
      @Override
      public @Nonnull Data create(@Nullable Hero hero) {
        return new Data(hero);
      }
    };

    public static final Factory FACTORY = new Factory() {
      @Override
      public @Nonnull Creator creator() {
        return CREATOR;
      }

      @Override
      public @Nonnull Hero.Factory heroFactory() {
        return Hero.FACTORY;
      }
    };

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

    public static class Hero {
      public static final Creator CREATOR = new Creator() {
        @Override
        public @Nonnull Hero create(@Nonnull String name,
            @Nonnull List<? extends Episode> appearsIn, @Nonnull Fragments fragments) {
          return new Hero(name, appearsIn, fragments);
        }
      };

      public static final Factory FACTORY = new Factory() {
        @Override
        public @Nonnull Creator creator() {
          return CREATOR;
        }

        @Override
        public @Nonnull Fragments.Factory fragmentsFactory() {
          return Fragments.FACTORY;
        }
      };

      private final @Nonnull String name;

      private final @Nonnull List<? extends Episode> appearsIn;

      private final Fragments fragments;

      public Hero(@Nonnull String name, @Nonnull List<? extends Episode> appearsIn,
          Fragments fragments) {
        this.name = name;
        this.appearsIn = appearsIn;
        this.fragments = fragments;
      }

      public @Nonnull String name() {
        return this.name;
      }

      public @Nonnull List<? extends Episode> appearsIn() {
        return this.appearsIn;
      }

      public @Nonnull Fragments fragments() {
        return this.fragments;
      }

      @Override
      public String toString() {
        return "Hero{"
          + "name=" + name + ", "
          + "appearsIn=" + appearsIn + ", "
          + "fragments=" + fragments
          + "}";
      }

      public static class Fragments {
        public static final Creator CREATOR = new Creator() {
          @Override
          public @Nonnull Fragments create(@Nullable HeroDetails heroDetails) {
            return new Fragments(heroDetails);
          }
        };

        public static final Factory FACTORY = new Factory() {
          @Override
          public @Nonnull Creator creator() {
            return CREATOR;
          }

          @Override
          public @Nonnull HeroDetails.Factory heroDetailsFactory() {
            return HeroDetails.FACTORY;
          }
        };

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

        public static final class Mapper implements ResponseFieldMapper<Fragments> {
          final Factory factory;

          String conditionalType;

          public Mapper(@Nonnull Factory factory, @Nonnull String conditionalType) {
            this.factory = factory;
            this.conditionalType = conditionalType;
          }

          @Override
          public @Nonnull Fragments map(ResponseReader reader) throws IOException {
            HeroDetails heroDetails = null;
            if (conditionalType.equals(HeroDetails.TYPE_CONDITION)) {
              heroDetails = new HeroDetails.Mapper(factory.heroDetailsFactory()).map(reader);
            }
            return factory.creator().create(heroDetails);
          }
        }

        public interface Factory {
          @Nonnull Creator creator();

          @Nonnull HeroDetails.Factory heroDetailsFactory();
        }

        public interface Creator {
          @Nonnull Fragments create(@Nullable HeroDetails heroDetails);
        }
      }

      public static final class Mapper implements ResponseFieldMapper<Hero> {
        final Factory factory;

        final Field[] fields = {
          Field.forString("name", "name", null, false),
          Field.forList("appearsIn", "appearsIn", null, false, new Field.ListReader<Episode>() {
            @Override public Episode read(final Field.ListItemReader reader) throws IOException {
              return Episode.valueOf(reader.readString());
            }
          }),
          Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<Fragments>() {
            @Override
            public Fragments read(String conditionalType, ResponseReader reader) throws
                IOException {
              return new Fragments.Mapper(factory.fragmentsFactory(), conditionalType).map(reader);
            }
          })
        };

        public Mapper(@Nonnull Factory factory) {
          this.factory = factory;
        }

        @Override
        public Hero map(ResponseReader reader) throws IOException {
          final __ContentValues contentValues = new __ContentValues();
          reader.toBufferedReader().read(new ResponseReader.ValueHandler() {
            @Override
            public void handle(final int fieldIndex, final Object value) throws IOException {
              switch (fieldIndex) {
                case 0: {
                  contentValues.name = (String) value;
                  break;
                }
                case 1: {
                  contentValues.appearsIn = (List<? extends Episode>) value;
                  break;
                }
                case 2: {
                  contentValues.fragments = (Fragments) value;
                  break;
                }
              }
            }
          }, fields);
          return factory.creator().create(contentValues.name, contentValues.appearsIn, contentValues.fragments);
        }

        static final class __ContentValues {
          String name;

          List<? extends Episode> appearsIn;

          Fragments fragments;
        }
      }

      public interface Factory {
        @Nonnull Creator creator();

        @Nonnull Fragments.Factory fragmentsFactory();
      }

      public interface Creator {
        @Nonnull Hero create(@Nonnull String name, @Nonnull List<? extends Episode> appearsIn,
            @Nonnull Fragments fragments);
      }
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final Factory factory;

      final Field[] fields = {
        Field.forObject("hero", "hero", null, true, new Field.ObjectReader<Hero>() {
          @Override public Hero read(final ResponseReader reader) throws IOException {
            return new Hero.Mapper(factory.heroFactory()).map(reader);
          }
        })
      };

      public Mapper(@Nonnull Factory factory) {
        this.factory = factory;
      }

      @Override
      public Data map(ResponseReader reader) throws IOException {
        final __ContentValues contentValues = new __ContentValues();
        reader.read(new ResponseReader.ValueHandler() {
          @Override
          public void handle(final int fieldIndex, final Object value) throws IOException {
            switch (fieldIndex) {
              case 0: {
                contentValues.hero = (Hero) value;
                break;
              }
            }
          }
        }, fields);
        return factory.creator().create(contentValues.hero);
      }

      static final class __ContentValues {
        Hero hero;
      }
    }

    public interface Factory {
      @Nonnull Creator creator();

      @Nonnull Hero.Factory heroFactory();
    }

    public interface Creator {
      @Nonnull Data create(@Nullable Hero hero);
    }
  }
}
