package com.example.fragment_friends_connection;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import com.example.fragment_friends_connection.fragment.HeroDetails;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery {\n"
      + "  hero {\n"
      + "    __typename\n"
      + "    ...HeroDetails\n"
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
      public static final Creator CREATOR = new Creator() {
        @Override
        public @Nonnull Hero create(@Nonnull Fragments fragments) {
          return new Hero(fragments);
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

      private final Fragments fragments;

      public Hero(Fragments fragments) {
        this.fragments = fragments;
      }

      public @Nonnull Fragments fragments() {
        return this.fragments;
      }

      @Override
      public String toString() {
        return "Hero{"
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
                  contentValues.fragments = (Fragments) value;
                  break;
                }
              }
            }
          }, fields);
          return factory.creator().create(contentValues.fragments);
        }

        static final class __ContentValues {
          Fragments fragments;
        }
      }

      public interface Factory {
        @Nonnull Creator creator();

        @Nonnull Fragments.Factory fragmentsFactory();
      }

      public interface Creator {
        @Nonnull Hero create(@Nonnull Fragments fragments);
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
