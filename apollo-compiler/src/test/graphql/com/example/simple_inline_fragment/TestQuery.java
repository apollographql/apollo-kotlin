package com.example.simple_inline_fragment;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import java.io.IOException;
import java.lang.Double;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query Query {\n"
      + "  hero {\n"
      + "    __typename\n"
      + "    name\n"
      + "    ... on Human {\n"
      + "      height\n"
      + "    }\n"
      + "    ... on Droid {\n"
      + "      primaryFunction\n"
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

      private @Nullable AsHuman asHuman;

      private @Nullable AsDroid asDroid;

      public Hero(@Nonnull String name, @Nullable AsHuman asHuman, @Nullable AsDroid asDroid) {
        this.name = name;
        this.asHuman = asHuman;
        this.asDroid = asDroid;
      }

      public @Nonnull String name() {
        return this.name;
      }

      public @Nullable AsHuman asHuman() {
        return this.asHuman;
      }

      public @Nullable AsDroid asDroid() {
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

        private final @Nullable Double height;

        public AsHuman(@Nonnull String name, @Nullable Double height) {
          this.name = name;
          this.height = height;
        }

        public @Nonnull String name() {
          return this.name;
        }

        public @Nullable Double height() {
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
            Field.forDouble("height", "height", null, true)
          };

          @Override
          public AsHuman map(ResponseReader reader) throws IOException {
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
                    contentValues.height = (Double) value;
                    break;
                  }
                }
              }
            }, fields);
            return new AsHuman(contentValues.name, contentValues.height);
          }

          static final class __ContentValues {
            String name;

            Double height;
          }
        }
      }

      public static class AsDroid {
        private final @Nonnull String name;

        private final @Nullable String primaryFunction;

        public AsDroid(@Nonnull String name, @Nullable String primaryFunction) {
          this.name = name;
          this.primaryFunction = primaryFunction;
        }

        public @Nonnull String name() {
          return this.name;
        }

        public @Nullable String primaryFunction() {
          return this.primaryFunction;
        }

        @Override
        public String toString() {
          return "AsDroid{"
            + "name=" + name + ", "
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
          h ^= (primaryFunction == null) ? 0 : primaryFunction.hashCode();
          return h;
        }

        public static final class Mapper implements ResponseFieldMapper<AsDroid> {
          final Field[] fields = {
            Field.forString("name", "name", null, false),
            Field.forString("primaryFunction", "primaryFunction", null, true)
          };

          @Override
          public AsDroid map(ResponseReader reader) throws IOException {
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
                    contentValues.primaryFunction = (String) value;
                    break;
                  }
                }
              }
            }, fields);
            return new AsDroid(contentValues.name, contentValues.primaryFunction);
          }

          static final class __ContentValues {
            String name;

            String primaryFunction;
          }
        }
      }

      public static final class Mapper implements ResponseFieldMapper<Hero> {
        final Field[] fields = {
          Field.forString("name", "name", null, false),
          Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<AsHuman>() {
            @Override
            public AsHuman read(String conditionalType, ResponseReader reader) throws IOException {
              if (conditionalType.equals("Human")) {
                return new AsHuman.Mapper().map(reader);
              } else {
                return null;
              }
            }
          }),
          Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<AsDroid>() {
            @Override
            public AsDroid read(String conditionalType, ResponseReader reader) throws IOException {
              if (conditionalType.equals("Droid")) {
                return new AsDroid.Mapper().map(reader);
              } else {
                return null;
              }
            }
          })
        };

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
                  contentValues.asHuman = (AsHuman) value;
                  break;
                }
                case 2: {
                  contentValues.asDroid = (AsDroid) value;
                  break;
                }
              }
            }
          }, fields);
          return new Hero(contentValues.name, contentValues.asHuman, contentValues.asDroid);
        }

        static final class __ContentValues {
          String name;

          AsHuman asHuman;

          AsDroid asDroid;
        }
      }
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final Field[] fields = {
        Field.forObject("hero", "hero", null, true, new Field.ObjectReader<Hero>() {
          @Override public Hero read(final ResponseReader reader) throws IOException {
            return new Hero.Mapper().map(reader);
          }
        })
      };

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
        return new Data(contentValues.hero);
      }

      static final class __ContentValues {
        Hero hero;
      }
    }
  }
}
