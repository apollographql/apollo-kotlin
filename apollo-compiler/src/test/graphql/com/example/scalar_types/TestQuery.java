package com.example.scalar_types;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import java.io.IOException;
import java.lang.Boolean;
import java.lang.Double;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<Operation.Variables> {
  public static final String OPERATION_DEFINITION = "";

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
      public @Nonnull Data create(@Nullable String graphQlString,
          @Nullable String graphQlIdNullable, @Nonnull String graphQlIdNonNullable,
          @Nullable Integer graphQlIntNullable, int graphQlIntNonNullable,
          @Nullable Double graphQlFloatNullable, double graphQlFloatNonNullable,
          @Nullable Boolean graphQlBooleanNullable, boolean graphQlBooleanNonNullable,
          @Nullable List<Integer> graphQlListOfInt,
          @Nullable List<GraphQlListOfObject> graphQlListOfObjects) {
        return new Data(graphQlString, graphQlIdNullable, graphQlIdNonNullable, graphQlIntNullable, graphQlIntNonNullable, graphQlFloatNullable, graphQlFloatNonNullable, graphQlBooleanNullable, graphQlBooleanNonNullable, graphQlListOfInt, graphQlListOfObjects);
      }
    };

    public static final Factory FACTORY = new Factory() {
      @Override
      public @Nonnull Creator creator() {
        return CREATOR;
      }

      @Override
      public @Nonnull GraphQlListOfObject.Factory graphQlListOfObjectFactory() {
        return GraphQlListOfObject.FACTORY;
      }
    };

    private final @Nullable String graphQlString;

    private final @Nullable String graphQlIdNullable;

    private final @Nonnull String graphQlIdNonNullable;

    private final @Nullable Integer graphQlIntNullable;

    private final int graphQlIntNonNullable;

    private final @Nullable Double graphQlFloatNullable;

    private final double graphQlFloatNonNullable;

    private final @Nullable Boolean graphQlBooleanNullable;

    private final boolean graphQlBooleanNonNullable;

    private final @Nullable List<Integer> graphQlListOfInt;

    private final @Nullable List<GraphQlListOfObject> graphQlListOfObjects;

    public Data(@Nullable String graphQlString, @Nullable String graphQlIdNullable,
        @Nonnull String graphQlIdNonNullable, @Nullable Integer graphQlIntNullable,
        int graphQlIntNonNullable, @Nullable Double graphQlFloatNullable,
        double graphQlFloatNonNullable, @Nullable Boolean graphQlBooleanNullable,
        boolean graphQlBooleanNonNullable, @Nullable List<Integer> graphQlListOfInt,
        @Nullable List<GraphQlListOfObject> graphQlListOfObjects) {
      this.graphQlString = graphQlString;
      this.graphQlIdNullable = graphQlIdNullable;
      this.graphQlIdNonNullable = graphQlIdNonNullable;
      this.graphQlIntNullable = graphQlIntNullable;
      this.graphQlIntNonNullable = graphQlIntNonNullable;
      this.graphQlFloatNullable = graphQlFloatNullable;
      this.graphQlFloatNonNullable = graphQlFloatNonNullable;
      this.graphQlBooleanNullable = graphQlBooleanNullable;
      this.graphQlBooleanNonNullable = graphQlBooleanNonNullable;
      this.graphQlListOfInt = graphQlListOfInt;
      this.graphQlListOfObjects = graphQlListOfObjects;
    }

    public @Nullable String graphQlString() {
      return this.graphQlString;
    }

    public @Nullable String graphQlIdNullable() {
      return this.graphQlIdNullable;
    }

    public @Nonnull String graphQlIdNonNullable() {
      return this.graphQlIdNonNullable;
    }

    public @Nullable Integer graphQlIntNullable() {
      return this.graphQlIntNullable;
    }

    public int graphQlIntNonNullable() {
      return this.graphQlIntNonNullable;
    }

    public @Nullable Double graphQlFloatNullable() {
      return this.graphQlFloatNullable;
    }

    public double graphQlFloatNonNullable() {
      return this.graphQlFloatNonNullable;
    }

    public @Nullable Boolean graphQlBooleanNullable() {
      return this.graphQlBooleanNullable;
    }

    public boolean graphQlBooleanNonNullable() {
      return this.graphQlBooleanNonNullable;
    }

    public @Nullable List<Integer> graphQlListOfInt() {
      return this.graphQlListOfInt;
    }

    public @Nullable List<GraphQlListOfObject> graphQlListOfObjects() {
      return this.graphQlListOfObjects;
    }

    @Override
    public String toString() {
      return "Data{"
        + "graphQlString=" + graphQlString + ", "
        + "graphQlIdNullable=" + graphQlIdNullable + ", "
        + "graphQlIdNonNullable=" + graphQlIdNonNullable + ", "
        + "graphQlIntNullable=" + graphQlIntNullable + ", "
        + "graphQlIntNonNullable=" + graphQlIntNonNullable + ", "
        + "graphQlFloatNullable=" + graphQlFloatNullable + ", "
        + "graphQlFloatNonNullable=" + graphQlFloatNonNullable + ", "
        + "graphQlBooleanNullable=" + graphQlBooleanNullable + ", "
        + "graphQlBooleanNonNullable=" + graphQlBooleanNonNullable + ", "
        + "graphQlListOfInt=" + graphQlListOfInt + ", "
        + "graphQlListOfObjects=" + graphQlListOfObjects
        + "}";
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Data) {
        Data that = (Data) o;
        return ((this.graphQlString == null) ? (that.graphQlString == null) : this.graphQlString.equals(that.graphQlString))
         && ((this.graphQlIdNullable == null) ? (that.graphQlIdNullable == null) : this.graphQlIdNullable.equals(that.graphQlIdNullable))
         && ((this.graphQlIdNonNullable == null) ? (that.graphQlIdNonNullable == null) : this.graphQlIdNonNullable.equals(that.graphQlIdNonNullable))
         && ((this.graphQlIntNullable == null) ? (that.graphQlIntNullable == null) : this.graphQlIntNullable.equals(that.graphQlIntNullable))
         && this.graphQlIntNonNullable == that.graphQlIntNonNullable
         && ((this.graphQlFloatNullable == null) ? (that.graphQlFloatNullable == null) : this.graphQlFloatNullable.equals(that.graphQlFloatNullable))
         && Double.doubleToLongBits(this.graphQlFloatNonNullable) == Double.doubleToLongBits(that.graphQlFloatNonNullable)
         && ((this.graphQlBooleanNullable == null) ? (that.graphQlBooleanNullable == null) : this.graphQlBooleanNullable.equals(that.graphQlBooleanNullable))
         && this.graphQlBooleanNonNullable == that.graphQlBooleanNonNullable
         && ((this.graphQlListOfInt == null) ? (that.graphQlListOfInt == null) : this.graphQlListOfInt.equals(that.graphQlListOfInt))
         && ((this.graphQlListOfObjects == null) ? (that.graphQlListOfObjects == null) : this.graphQlListOfObjects.equals(that.graphQlListOfObjects));
      }
      return false;
    }

    @Override
    public int hashCode() {
      int h = 1;
      h *= 1000003;
      h ^= (graphQlString == null) ? 0 : graphQlString.hashCode();
      h *= 1000003;
      h ^= (graphQlIdNullable == null) ? 0 : graphQlIdNullable.hashCode();
      h *= 1000003;
      h ^= (graphQlIdNonNullable == null) ? 0 : graphQlIdNonNullable.hashCode();
      h *= 1000003;
      h ^= (graphQlIntNullable == null) ? 0 : graphQlIntNullable.hashCode();
      h *= 1000003;
      h ^= graphQlIntNonNullable;
      h *= 1000003;
      h ^= (graphQlFloatNullable == null) ? 0 : graphQlFloatNullable.hashCode();
      h *= 1000003;
      h ^= Double.valueOf(graphQlFloatNonNullable).hashCode();
      h *= 1000003;
      h ^= (graphQlBooleanNullable == null) ? 0 : graphQlBooleanNullable.hashCode();
      h *= 1000003;
      h ^= Boolean.valueOf(graphQlBooleanNonNullable).hashCode();
      h *= 1000003;
      h ^= (graphQlListOfInt == null) ? 0 : graphQlListOfInt.hashCode();
      h *= 1000003;
      h ^= (graphQlListOfObjects == null) ? 0 : graphQlListOfObjects.hashCode();
      return h;
    }

    public static class GraphQlListOfObject {
      public static final Creator CREATOR = new Creator() {
        @Override
        public @Nonnull GraphQlListOfObject create(int someField) {
          return new GraphQlListOfObject(someField);
        }
      };

      public static final Factory FACTORY = new Factory() {
        @Override
        public @Nonnull Creator creator() {
          return CREATOR;
        }
      };

      private final int someField;

      public GraphQlListOfObject(int someField) {
        this.someField = someField;
      }

      public int someField() {
        return this.someField;
      }

      @Override
      public String toString() {
        return "GraphQlListOfObject{"
          + "someField=" + someField
          + "}";
      }

      @Override
      public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof GraphQlListOfObject) {
          GraphQlListOfObject that = (GraphQlListOfObject) o;
          return this.someField == that.someField;
        }
        return false;
      }

      @Override
      public int hashCode() {
        int h = 1;
        h *= 1000003;
        h ^= someField;
        return h;
      }

      public static final class Mapper implements ResponseFieldMapper<GraphQlListOfObject> {
        final Factory factory;

        final Field[] fields = {
          Field.forInt("someField", "someField", null, false)
        };

        public Mapper(@Nonnull Factory factory) {
          this.factory = factory;
        }

        @Override
        public GraphQlListOfObject map(ResponseReader reader) throws IOException {
          final __ContentValues contentValues = new __ContentValues();
          reader.read(new ResponseReader.ValueHandler() {
            @Override
            public void handle(final int fieldIndex, final Object value) throws IOException {
              switch (fieldIndex) {
                case 0: {
                  contentValues.someField = (int) value;
                  break;
                }
              }
            }
          }, fields);
          return factory.creator().create(contentValues.someField);
        }

        static final class __ContentValues {
          int someField;
        }
      }

      public interface Factory {
        @Nonnull Creator creator();
      }

      public interface Creator {
        @Nonnull GraphQlListOfObject create(int someField);
      }
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final Factory factory;

      final Field[] fields = {
        Field.forString("graphQlString", "graphQlString", null, true),
        Field.forString("graphQlIdNullable", "graphQlIdNullable", null, true),
        Field.forString("graphQlIdNonNullable", "graphQlIdNonNullable", null, false),
        Field.forInt("graphQlIntNullable", "graphQlIntNullable", null, true),
        Field.forInt("graphQlIntNonNullable", "graphQlIntNonNullable", null, false),
        Field.forDouble("graphQlFloatNullable", "graphQlFloatNullable", null, true),
        Field.forDouble("graphQlFloatNonNullable", "graphQlFloatNonNullable", null, false),
        Field.forBoolean("graphQlBooleanNullable", "graphQlBooleanNullable", null, true),
        Field.forBoolean("graphQlBooleanNonNullable", "graphQlBooleanNonNullable", null, false),
        Field.forList("graphQlListOfInt", "graphQlListOfInt", null, true, new Field.ListReader<Integer>() {
          @Override public Integer read(final Field.ListItemReader reader) throws IOException {
            return reader.readInt();
          }
        }),
        Field.forList("graphQlListOfObjects", "graphQlListOfObjects", null, true, new Field.ObjectReader<GraphQlListOfObject>() {
          @Override public GraphQlListOfObject read(final ResponseReader reader) throws IOException {
            return new GraphQlListOfObject.Mapper(factory.graphQlListOfObjectFactory()).map(reader);
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
                contentValues.graphQlString = (String) value;
                break;
              }
              case 1: {
                contentValues.graphQlIdNullable = (String) value;
                break;
              }
              case 2: {
                contentValues.graphQlIdNonNullable = (String) value;
                break;
              }
              case 3: {
                contentValues.graphQlIntNullable = (Integer) value;
                break;
              }
              case 4: {
                contentValues.graphQlIntNonNullable = (int) value;
                break;
              }
              case 5: {
                contentValues.graphQlFloatNullable = (Double) value;
                break;
              }
              case 6: {
                contentValues.graphQlFloatNonNullable = (double) value;
                break;
              }
              case 7: {
                contentValues.graphQlBooleanNullable = (Boolean) value;
                break;
              }
              case 8: {
                contentValues.graphQlBooleanNonNullable = (boolean) value;
                break;
              }
              case 9: {
                contentValues.graphQlListOfInt = (List<Integer>) value;
                break;
              }
              case 10: {
                contentValues.graphQlListOfObjects = (List<GraphQlListOfObject>) value;
                break;
              }
            }
          }
        }, fields);
        return factory.creator().create(contentValues.graphQlString, contentValues.graphQlIdNullable, contentValues.graphQlIdNonNullable, contentValues.graphQlIntNullable, contentValues.graphQlIntNonNullable, contentValues.graphQlFloatNullable, contentValues.graphQlFloatNonNullable, contentValues.graphQlBooleanNullable, contentValues.graphQlBooleanNonNullable, contentValues.graphQlListOfInt, contentValues.graphQlListOfObjects);
      }

      static final class __ContentValues {
        String graphQlString;

        String graphQlIdNullable;

        String graphQlIdNonNullable;

        Integer graphQlIntNullable;

        int graphQlIntNonNullable;

        Double graphQlFloatNullable;

        double graphQlFloatNonNullable;

        Boolean graphQlBooleanNullable;

        boolean graphQlBooleanNonNullable;

        List<Integer> graphQlListOfInt;

        List<GraphQlListOfObject> graphQlListOfObjects;
      }
    }

    public interface Factory {
      @Nonnull Creator creator();

      @Nonnull GraphQlListOfObject.Factory graphQlListOfObjectFactory();
    }

    public interface Creator {
      @Nonnull Data create(@Nullable String graphQlString, @Nullable String graphQlIdNullable,
          @Nonnull String graphQlIdNonNullable, @Nullable Integer graphQlIntNullable,
          int graphQlIntNonNullable, @Nullable Double graphQlFloatNullable,
          double graphQlFloatNonNullable, @Nullable Boolean graphQlBooleanNullable,
          boolean graphQlBooleanNonNullable, @Nullable List<Integer> graphQlListOfInt,
          @Nullable List<GraphQlListOfObject> graphQlListOfObjects);
    }
  }
}
