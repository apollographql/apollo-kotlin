package com.example;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Date;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import type.CustomType;

@Generated("Apollo GraphQL")
public final class Films implements Query<Films.Data, Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query Films {\n"
      + "  allFilms {\n"
      + "    __typename\n"
      + "    films {\n"
      + "      __typename\n"
      + "      id\n"
      + "      title\n"
      + "      releaseDate\n"
      + "    }\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private final Operation.Variables variables;

  public Films() {
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
    private final @Nullable AllFilm allFilms;

    public Data(@Nullable AllFilm allFilms) {
      this.allFilms = allFilms;
    }

    public @Nullable AllFilm allFilms() {
      return this.allFilms;
    }

    @Override
    public String toString() {
      return "Data{"
        + "allFilms=" + allFilms
        + "}";
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Data) {
        Data that = (Data) o;
        return ((this.allFilms == null) ? (that.allFilms == null) : this.allFilms.equals(that.allFilms));
      }
      return false;
    }

    @Override
    public int hashCode() {
      int h = 1;
      h *= 1000003;
      h ^= (allFilms == null) ? 0 : allFilms.hashCode();
      return h;
    }

    public static class AllFilm {
      private final @Nullable List<Film> films;

      public AllFilm(@Nullable List<Film> films) {
        this.films = films;
      }

      public @Nullable List<Film> films() {
        return this.films;
      }

      @Override
      public String toString() {
        return "AllFilm{"
          + "films=" + films
          + "}";
      }

      @Override
      public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof AllFilm) {
          AllFilm that = (AllFilm) o;
          return ((this.films == null) ? (that.films == null) : this.films.equals(that.films));
        }
        return false;
      }

      @Override
      public int hashCode() {
        int h = 1;
        h *= 1000003;
        h ^= (films == null) ? 0 : films.hashCode();
        return h;
      }

      public static class Film {
        private final @Nonnull String id;

        private final @Nullable String title;

        private final @Nonnull Date releaseDate;

        public Film(@Nonnull String id, @Nullable String title, @Nonnull Date releaseDate) {
          this.id = id;
          this.title = title;
          this.releaseDate = releaseDate;
        }

        public @Nonnull String id() {
          return this.id;
        }

        public @Nullable String title() {
          return this.title;
        }

        public @Nonnull Date releaseDate() {
          return this.releaseDate;
        }

        @Override
        public String toString() {
          return "Film{"
            + "id=" + id + ", "
            + "title=" + title + ", "
            + "releaseDate=" + releaseDate
            + "}";
        }

        @Override
        public boolean equals(Object o) {
          if (o == this) {
            return true;
          }
          if (o instanceof Film) {
            Film that = (Film) o;
            return ((this.id == null) ? (that.id == null) : this.id.equals(that.id))
             && ((this.title == null) ? (that.title == null) : this.title.equals(that.title))
             && ((this.releaseDate == null) ? (that.releaseDate == null) : this.releaseDate.equals(that.releaseDate));
          }
          return false;
        }

        @Override
        public int hashCode() {
          int h = 1;
          h *= 1000003;
          h ^= (id == null) ? 0 : id.hashCode();
          h *= 1000003;
          h ^= (title == null) ? 0 : title.hashCode();
          h *= 1000003;
          h ^= (releaseDate == null) ? 0 : releaseDate.hashCode();
          return h;
        }

        public static final class Mapper implements ResponseFieldMapper<Film> {
          final Field[] fields = {
            Field.forString("id", "id", null, false),
            Field.forString("title", "title", null, true),
            Field.forCustomType("releaseDate", "releaseDate", null, false, CustomType.DATETIME)
          };

          @Override
          public Film map(ResponseReader reader) throws IOException {
            final String id = reader.read(fields[0]);
            final String title = reader.read(fields[1]);
            final Date releaseDate = reader.read(fields[2]);
            return new Film(id, title, releaseDate);
          }
        }
      }

      public static final class Mapper implements ResponseFieldMapper<AllFilm> {
        final Film.Mapper filmFieldMapper = new Film.Mapper();

        final Field[] fields = {
          Field.forList("films", "films", null, true, new Field.ObjectReader<Film>() {
            @Override public Film read(final ResponseReader reader) throws IOException {
              return filmFieldMapper.map(reader);
            }
          })
        };

        @Override
        public AllFilm map(ResponseReader reader) throws IOException {
          final List<Film> films = reader.read(fields[0]);
          return new AllFilm(films);
        }
      }
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final AllFilm.Mapper allFilmFieldMapper = new AllFilm.Mapper();

      final Field[] fields = {
        Field.forObject("allFilms", "allFilms", null, true, new Field.ObjectReader<AllFilm>() {
          @Override public AllFilm read(final ResponseReader reader) throws IOException {
            return allFilmFieldMapper.map(reader);
          }
        })
      };

      @Override
      public Data map(ResponseReader reader) throws IOException {
        final AllFilm allFilms = reader.read(fields[0]);
        return new Data(allFilms);
      }
    }
  }
}
