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

  public interface Data extends Operation.Data {
    @Nullable String graphQlString();

    @Nullable String graphQlIdNullable();

    @Nonnull String graphQlIdNonNullable();

    @Nullable Integer graphQlIntNullable();

    int graphQlIntNonNullable();

    @Nullable Double graphQlFloatNullable();

    double graphQlFloatNonNullable();

    @Nullable Boolean graphQlBooleanNullable();

    boolean graphQlBooleanNonNullable();

    @Nullable List<Integer> graphQlListOfInt();

    @Nullable List<GraphQlListOfObject> graphQlListOfObjects();

    interface GraphQlListOfObject {
      int someField();

      final class Mapper implements ResponseFieldMapper<GraphQlListOfObject> {
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

      interface Factory {
        @Nonnull Creator creator();
      }

      interface Creator {
        @Nonnull GraphQlListOfObject create(int someField);
      }
    }

    final class Mapper implements ResponseFieldMapper<Data> {
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

    interface Factory {
      @Nonnull Creator creator();

      @Nonnull GraphQlListOfObject.Factory graphQlListOfObjectFactory();
    }

    interface Creator {
      @Nonnull Data create(@Nullable String graphQlString, @Nullable String graphQlIdNullable,
          @Nonnull String graphQlIdNonNullable, @Nullable Integer graphQlIntNullable,
          int graphQlIntNonNullable, @Nullable Double graphQlFloatNullable,
          double graphQlFloatNonNullable, @Nullable Boolean graphQlBooleanNullable,
          boolean graphQlBooleanNonNullable, @Nullable List<Integer> graphQlListOfInt,
          @Nullable List<GraphQlListOfObject> graphQlListOfObjects);
    }
  }
}
