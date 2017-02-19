package com.example.enum_type;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import com.example.enum_type.type.Episode;
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
      + "    appearsIn\n"
      + "    firstAppearsIn\n"
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
        public @Nonnull Hero create(@Nonnull String name, @Nonnull List<Episode> appearsIn,
            @Nonnull Episode firstAppearsIn) {
          return new Hero(name, appearsIn, firstAppearsIn);
        }
      };

      public static final Factory FACTORY = new Factory() {
        @Override
        public @Nonnull Creator creator() {
          return CREATOR;
        }
      };

      private final @Nonnull String name;

      private final @Nonnull List<Episode> appearsIn;

      private final @Nonnull Episode firstAppearsIn;

      public Hero(@Nonnull String name, @Nonnull List<Episode> appearsIn,
          @Nonnull Episode firstAppearsIn) {
        this.name = name;
        this.appearsIn = appearsIn;
        this.firstAppearsIn = firstAppearsIn;
      }

      public @Nonnull String name() {
        return this.name;
      }

      public @Nonnull List<Episode> appearsIn() {
        return this.appearsIn;
      }

      public @Nonnull Episode firstAppearsIn() {
        return this.firstAppearsIn;
      }

      @Override
      public String toString() {
        return "Hero{"
          + "name=" + name + ", "
          + "appearsIn=" + appearsIn + ", "
          + "firstAppearsIn=" + firstAppearsIn
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
           && ((this.appearsIn == null) ? (that.appearsIn == null) : this.appearsIn.equals(that.appearsIn))
           && ((this.firstAppearsIn == null) ? (that.firstAppearsIn == null) : this.firstAppearsIn.equals(that.firstAppearsIn));
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
        h ^= (firstAppearsIn == null) ? 0 : firstAppearsIn.hashCode();
        return h;
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
          Field.forString("firstAppearsIn", "firstAppearsIn", null, false)
        };

        public Mapper(@Nonnull Factory factory) {
          this.factory = factory;
        }

        @Override
        public Hero map(ResponseReader reader) throws IOException {
          final __ContentValues contentValues = new __ContentValues();
          reader.read(new ResponseReader.ValueHandler() {
            @Override
            public void handle(final int fieldIndex, final Object value) throws IOException {
              switch (fieldIndex) {
                case 0: {
                  contentValues.name = (String) value;
                  break;
                }
                case 1: {
                  contentValues.appearsIn = (List<Episode>) value;
                  break;
                }
                case 2: {
                  if (value != null) {
                    contentValues.firstAppearsIn = Episode.valueOf((String) value);
                  }
                  break;
                }
              }
            }
          }, fields);
          return factory.creator().create(contentValues.name, contentValues.appearsIn, contentValues.firstAppearsIn);
        }

        static final class __ContentValues {
          String name;

          List<Episode> appearsIn;

          Episode firstAppearsIn;
        }
      }

      public interface Factory {
        @Nonnull Creator creator();
      }

      public interface Creator {
        @Nonnull Hero create(@Nonnull String name, @Nonnull List<Episode> appearsIn,
            @Nonnull Episode firstAppearsIn);
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
