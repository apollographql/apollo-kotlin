package com.example.enum_type;

import com.apollographql.apollo.api.Field;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.internal.Optional;
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
public final class TestQuery implements Query<TestQuery.Data, Optional<TestQuery.Data>, Operation.Variables> {
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
        final Field[] fields = {
          Field.forString("name", "name", null, false),
          Field.forList("appearsIn", "appearsIn", null, false, new Field.ListReader<Episode>() {
            @Override public Episode read(final Field.ListItemReader reader) throws IOException {
              return Episode.valueOf(reader.readString());
            }
          }),
          Field.forString("firstAppearsIn", "firstAppearsIn", null, false)
        };

        @Override
        public Hero map(ResponseReader reader) throws IOException {
          final String name = reader.read(fields[0]);
          final List<Episode> appearsIn = reader.read(fields[1]);
          final String firstAppearsInStr = reader.read(fields[2]);
          final Episode firstAppearsIn;
          if (firstAppearsInStr != null) {
            firstAppearsIn = Episode.valueOf(firstAppearsInStr);
          } else {
            firstAppearsIn = null;
          }
          return new Hero(name, appearsIn, firstAppearsIn);
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
