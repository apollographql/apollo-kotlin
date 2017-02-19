package com.example.two_heroes_unique;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
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
      + "  r2: hero {\n"
      + "    __typename\n"
      + "    name\n"
      + "  }\n"
      + "  luke: hero(episode: EMPIRE) {\n"
      + "    __typename\n"
      + "    id\n"
      + "    name\n"
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
      public @Nonnull Data create(@Nullable R2 r2, @Nullable Luke luke) {
        return new Data(r2, luke);
      }
    };

    public static final Factory FACTORY = new Factory() {
      @Override
      public @Nonnull Creator creator() {
        return CREATOR;
      }

      @Override
      public @Nonnull R2.Factory r2Factory() {
        return R2.FACTORY;
      }

      @Override
      public @Nonnull Luke.Factory lukeFactory() {
        return Luke.FACTORY;
      }
    };

    private final @Nullable R2 r2;

    private final @Nullable Luke luke;

    public Data(@Nullable R2 r2, @Nullable Luke luke) {
      this.r2 = r2;
      this.luke = luke;
    }

    public @Nullable R2 r2() {
      return this.r2;
    }

    public @Nullable Luke luke() {
      return this.luke;
    }

    @Override
    public String toString() {
      return "Data{"
        + "r2=" + r2 + ", "
        + "luke=" + luke
        + "}";
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Data) {
        Data that = (Data) o;
        return ((this.r2 == null) ? (that.r2 == null) : this.r2.equals(that.r2))
         && ((this.luke == null) ? (that.luke == null) : this.luke.equals(that.luke));
      }
      return false;
    }

    @Override
    public int hashCode() {
      int h = 1;
      h *= 1000003;
      h ^= (r2 == null) ? 0 : r2.hashCode();
      h *= 1000003;
      h ^= (luke == null) ? 0 : luke.hashCode();
      return h;
    }

    public static class R2 {
      public static final Creator CREATOR = new Creator() {
        @Override
        public @Nonnull R2 create(@Nonnull String name) {
          return new R2(name);
        }
      };

      public static final Factory FACTORY = new Factory() {
        @Override
        public @Nonnull Creator creator() {
          return CREATOR;
        }
      };

      private final @Nonnull String name;

      public R2(@Nonnull String name) {
        this.name = name;
      }

      public @Nonnull String name() {
        return this.name;
      }

      @Override
      public String toString() {
        return "R2{"
          + "name=" + name
          + "}";
      }

      @Override
      public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof R2) {
          R2 that = (R2) o;
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

      public static final class Mapper implements ResponseFieldMapper<R2> {
        final Factory factory;

        final Field[] fields = {
          Field.forString("name", "name", null, false)
        };

        public Mapper(@Nonnull Factory factory) {
          this.factory = factory;
        }

        @Override
        public R2 map(ResponseReader reader) throws IOException {
          final __ContentValues contentValues = new __ContentValues();
          reader.read(new ResponseReader.ValueHandler() {
            @Override
            public void handle(final int fieldIndex, final Object value) throws IOException {
              switch (fieldIndex) {
                case 0: {
                  contentValues.name = (String) value;
                  break;
                }
              }
            }
          }, fields);
          return factory.creator().create(contentValues.name);
        }

        static final class __ContentValues {
          String name;
        }
      }

      public interface Factory {
        @Nonnull Creator creator();
      }

      public interface Creator {
        @Nonnull R2 create(@Nonnull String name);
      }
    }

    public static class Luke {
      public static final Creator CREATOR = new Creator() {
        @Override
        public @Nonnull Luke create(@Nonnull String id, @Nonnull String name) {
          return new Luke(id, name);
        }
      };

      public static final Factory FACTORY = new Factory() {
        @Override
        public @Nonnull Creator creator() {
          return CREATOR;
        }
      };

      private final @Nonnull String id;

      private final @Nonnull String name;

      public Luke(@Nonnull String id, @Nonnull String name) {
        this.id = id;
        this.name = name;
      }

      public @Nonnull String id() {
        return this.id;
      }

      public @Nonnull String name() {
        return this.name;
      }

      @Override
      public String toString() {
        return "Luke{"
          + "id=" + id + ", "
          + "name=" + name
          + "}";
      }

      @Override
      public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof Luke) {
          Luke that = (Luke) o;
          return ((this.id == null) ? (that.id == null) : this.id.equals(that.id))
           && ((this.name == null) ? (that.name == null) : this.name.equals(that.name));
        }
        return false;
      }

      @Override
      public int hashCode() {
        int h = 1;
        h *= 1000003;
        h ^= (id == null) ? 0 : id.hashCode();
        h *= 1000003;
        h ^= (name == null) ? 0 : name.hashCode();
        return h;
      }

      public static final class Mapper implements ResponseFieldMapper<Luke> {
        final Factory factory;

        final Field[] fields = {
          Field.forString("id", "id", null, false),
          Field.forString("name", "name", null, false)
        };

        public Mapper(@Nonnull Factory factory) {
          this.factory = factory;
        }

        @Override
        public Luke map(ResponseReader reader) throws IOException {
          final __ContentValues contentValues = new __ContentValues();
          reader.read(new ResponseReader.ValueHandler() {
            @Override
            public void handle(final int fieldIndex, final Object value) throws IOException {
              switch (fieldIndex) {
                case 0: {
                  contentValues.id = (String) value;
                  break;
                }
                case 1: {
                  contentValues.name = (String) value;
                  break;
                }
              }
            }
          }, fields);
          return factory.creator().create(contentValues.id, contentValues.name);
        }

        static final class __ContentValues {
          String id;

          String name;
        }
      }

      public interface Factory {
        @Nonnull Creator creator();
      }

      public interface Creator {
        @Nonnull Luke create(@Nonnull String id, @Nonnull String name);
      }
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final Factory factory;

      final Field[] fields = {
        Field.forObject("r2", "hero", null, true, new Field.ObjectReader<R2>() {
          @Override public R2 read(final ResponseReader reader) throws IOException {
            return new R2.Mapper(factory.r2Factory()).map(reader);
          }
        }),
        Field.forObject("luke", "hero", null, true, new Field.ObjectReader<Luke>() {
          @Override public Luke read(final ResponseReader reader) throws IOException {
            return new Luke.Mapper(factory.lukeFactory()).map(reader);
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
                contentValues.r2 = (R2) value;
                break;
              }
              case 1: {
                contentValues.luke = (Luke) value;
                break;
              }
            }
          }
        }, fields);
        return factory.creator().create(contentValues.r2, contentValues.luke);
      }

      static final class __ContentValues {
        R2 r2;

        Luke luke;
      }
    }

    public interface Factory {
      @Nonnull Creator creator();

      @Nonnull R2.Factory r2Factory();

      @Nonnull Luke.Factory lukeFactory();
    }

    public interface Creator {
      @Nonnull Data create(@Nullable R2 r2, @Nullable Luke luke);
    }
  }
}
