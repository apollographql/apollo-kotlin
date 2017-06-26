package com.example.scalar_types;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ResponseWriter;
import com.apollographql.apollo.api.internal.Optional;
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
public final class TestQuery implements Query<TestQuery.Data, Optional<TestQuery.Data>, Operation.Variables> {
  public static final String OPERATION_DEFINITION = "";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private static final OperationName OPERATION_NAME = new OperationName() {
    @Override
    public String name() {
      return "TestQuery";
    }
  };

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

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public OperationName name() {
    return OPERATION_NAME;
  }

  public static final class Builder {
    Builder() {
    }

    public TestQuery build() {
      return new TestQuery();
    }
  }

  public static class Data implements Operation.Data {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("graphQlString", "graphQlString", null, true),
      ResponseField.forString("graphQlIdNullable", "graphQlIdNullable", null, true),
      ResponseField.forString("graphQlIdNonNullable", "graphQlIdNonNullable", null, false),
      ResponseField.forInt("graphQlIntNullable", "graphQlIntNullable", null, true),
      ResponseField.forInt("graphQlIntNonNullable", "graphQlIntNonNullable", null, false),
      ResponseField.forDouble("graphQlFloatNullable", "graphQlFloatNullable", null, true),
      ResponseField.forDouble("graphQlFloatNonNullable", "graphQlFloatNonNullable", null, false),
      ResponseField.forBoolean("graphQlBooleanNullable", "graphQlBooleanNullable", null, true),
      ResponseField.forBoolean("graphQlBooleanNonNullable", "graphQlBooleanNonNullable", null, false),
      ResponseField.forScalarList("graphQlListOfInt", "graphQlListOfInt", null, true),
      ResponseField.forObjectList("graphQlListOfObjects", "graphQlListOfObjects", null, true)
    };

    final Optional<String> graphQlString;

    final Optional<String> graphQlIdNullable;

    final @Nonnull String graphQlIdNonNullable;

    final Optional<Integer> graphQlIntNullable;

    final int graphQlIntNonNullable;

    final Optional<Double> graphQlFloatNullable;

    final double graphQlFloatNonNullable;

    final Optional<Boolean> graphQlBooleanNullable;

    final boolean graphQlBooleanNonNullable;

    final Optional<List<Integer>> graphQlListOfInt;

    final Optional<List<GraphQlListOfObject>> graphQlListOfObjects;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Data(@Nullable String graphQlString, @Nullable String graphQlIdNullable,
        @Nonnull String graphQlIdNonNullable, @Nullable Integer graphQlIntNullable,
        int graphQlIntNonNullable, @Nullable Double graphQlFloatNullable,
        double graphQlFloatNonNullable, @Nullable Boolean graphQlBooleanNullable,
        boolean graphQlBooleanNonNullable, @Nullable List<Integer> graphQlListOfInt,
        @Nullable List<GraphQlListOfObject> graphQlListOfObjects) {
      this.graphQlString = Optional.fromNullable(graphQlString);
      this.graphQlIdNullable = Optional.fromNullable(graphQlIdNullable);
      this.graphQlIdNonNullable = graphQlIdNonNullable;
      this.graphQlIntNullable = Optional.fromNullable(graphQlIntNullable);
      this.graphQlIntNonNullable = graphQlIntNonNullable;
      this.graphQlFloatNullable = Optional.fromNullable(graphQlFloatNullable);
      this.graphQlFloatNonNullable = graphQlFloatNonNullable;
      this.graphQlBooleanNullable = Optional.fromNullable(graphQlBooleanNullable);
      this.graphQlBooleanNonNullable = graphQlBooleanNonNullable;
      this.graphQlListOfInt = Optional.fromNullable(graphQlListOfInt);
      this.graphQlListOfObjects = Optional.fromNullable(graphQlListOfObjects);
    }

    public Optional<String> graphQlString() {
      return this.graphQlString;
    }

    public Optional<String> graphQlIdNullable() {
      return this.graphQlIdNullable;
    }

    public @Nonnull String graphQlIdNonNullable() {
      return this.graphQlIdNonNullable;
    }

    public Optional<Integer> graphQlIntNullable() {
      return this.graphQlIntNullable;
    }

    public int graphQlIntNonNullable() {
      return this.graphQlIntNonNullable;
    }

    public Optional<Double> graphQlFloatNullable() {
      return this.graphQlFloatNullable;
    }

    public double graphQlFloatNonNullable() {
      return this.graphQlFloatNonNullable;
    }

    public Optional<Boolean> graphQlBooleanNullable() {
      return this.graphQlBooleanNullable;
    }

    public boolean graphQlBooleanNonNullable() {
      return this.graphQlBooleanNonNullable;
    }

    public Optional<List<Integer>> graphQlListOfInt() {
      return this.graphQlListOfInt;
    }

    public Optional<List<GraphQlListOfObject>> graphQlListOfObjects() {
      return this.graphQlListOfObjects;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) throws IOException {
          writer.writeString($responseFields[0], graphQlString.isPresent() ? graphQlString.get() : null);
          writer.writeString($responseFields[1], graphQlIdNullable.isPresent() ? graphQlIdNullable.get() : null);
          writer.writeString($responseFields[2], graphQlIdNonNullable);
          writer.writeInt($responseFields[3], graphQlIntNullable.isPresent() ? graphQlIntNullable.get() : null);
          writer.writeInt($responseFields[4], graphQlIntNonNullable);
          writer.writeDouble($responseFields[5], graphQlFloatNullable.isPresent() ? graphQlFloatNullable.get() : null);
          writer.writeDouble($responseFields[6], graphQlFloatNonNullable);
          writer.writeBoolean($responseFields[7], graphQlBooleanNullable.isPresent() ? graphQlBooleanNullable.get() : null);
          writer.writeBoolean($responseFields[8], graphQlBooleanNonNullable);
          writer.writeList($responseFields[9], graphQlListOfInt.isPresent() ? new ResponseWriter.ListWriter() {
            @Override
            public void write(ResponseWriter.ListItemWriter listItemWriter) throws IOException {
              for (Integer $item : graphQlListOfInt.get()) {
                listItemWriter.writeInt($item);
              }
            }
          } : null);
          writer.writeList($responseFields[10], graphQlListOfObjects.isPresent() ? new ResponseWriter.ListWriter() {
            @Override
            public void write(ResponseWriter.ListItemWriter listItemWriter) throws IOException {
              for (GraphQlListOfObject $item : graphQlListOfObjects.get()) {
                listItemWriter.writeObject($item.marshaller());
              }
            }
          } : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Data{"
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
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Data) {
        Data that = (Data) o;
        return this.graphQlString.equals(that.graphQlString)
         && this.graphQlIdNullable.equals(that.graphQlIdNullable)
         && this.graphQlIdNonNullable.equals(that.graphQlIdNonNullable)
         && this.graphQlIntNullable.equals(that.graphQlIntNullable)
         && this.graphQlIntNonNullable == that.graphQlIntNonNullable
         && this.graphQlFloatNullable.equals(that.graphQlFloatNullable)
         && Double.doubleToLongBits(this.graphQlFloatNonNullable) == Double.doubleToLongBits(that.graphQlFloatNonNullable)
         && this.graphQlBooleanNullable.equals(that.graphQlBooleanNullable)
         && this.graphQlBooleanNonNullable == that.graphQlBooleanNonNullable
         && this.graphQlListOfInt.equals(that.graphQlListOfInt)
         && this.graphQlListOfObjects.equals(that.graphQlListOfObjects);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= graphQlString.hashCode();
        h *= 1000003;
        h ^= graphQlIdNullable.hashCode();
        h *= 1000003;
        h ^= graphQlIdNonNullable.hashCode();
        h *= 1000003;
        h ^= graphQlIntNullable.hashCode();
        h *= 1000003;
        h ^= graphQlIntNonNullable;
        h *= 1000003;
        h ^= graphQlFloatNullable.hashCode();
        h *= 1000003;
        h ^= Double.valueOf(graphQlFloatNonNullable).hashCode();
        h *= 1000003;
        h ^= graphQlBooleanNullable.hashCode();
        h *= 1000003;
        h ^= Boolean.valueOf(graphQlBooleanNonNullable).hashCode();
        h *= 1000003;
        h ^= graphQlListOfInt.hashCode();
        h *= 1000003;
        h ^= graphQlListOfObjects.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final GraphQlListOfObject.Mapper graphQlListOfObjectFieldMapper = new GraphQlListOfObject.Mapper();

      @Override
      public Data map(ResponseReader reader) throws IOException {
        final String graphQlString = reader.readString($responseFields[0]);
        final String graphQlIdNullable = reader.readString($responseFields[1]);
        final String graphQlIdNonNullable = reader.readString($responseFields[2]);
        final Integer graphQlIntNullable = reader.readInt($responseFields[3]);
        final int graphQlIntNonNullable = reader.readInt($responseFields[4]);
        final Double graphQlFloatNullable = reader.readDouble($responseFields[5]);
        final double graphQlFloatNonNullable = reader.readDouble($responseFields[6]);
        final Boolean graphQlBooleanNullable = reader.readBoolean($responseFields[7]);
        final boolean graphQlBooleanNonNullable = reader.readBoolean($responseFields[8]);
        final List<Integer> graphQlListOfInt = reader.readList($responseFields[9], new ResponseReader.ListReader<Integer>() {
          @Override
          public Integer read(ResponseReader.ListItemReader reader) throws IOException {
            return reader.readInt();
          }
        });
        final List<GraphQlListOfObject> graphQlListOfObjects = reader.readList($responseFields[10], new ResponseReader.ListReader<GraphQlListOfObject>() {
          @Override
          public GraphQlListOfObject read(ResponseReader.ListItemReader reader) throws IOException {
            return reader.readObject(new ResponseReader.ObjectReader<GraphQlListOfObject>() {
              @Override
              public GraphQlListOfObject read(ResponseReader reader) throws IOException {
                return graphQlListOfObjectFieldMapper.map(reader);
              }
            });
          }
        });
        return new Data(graphQlString, graphQlIdNullable, graphQlIdNonNullable, graphQlIntNullable, graphQlIntNonNullable, graphQlFloatNullable, graphQlFloatNonNullable, graphQlBooleanNullable, graphQlBooleanNonNullable, graphQlListOfInt, graphQlListOfObjects);
      }
    }
  }

  public static class GraphQlListOfObject {
    static final ResponseField[] $responseFields = {
      ResponseField.forInt("someField", "someField", null, false)
    };

    final int someField;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public GraphQlListOfObject(int someField) {
      this.someField = someField;
    }

    public int someField() {
      return this.someField;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) throws IOException {
          writer.writeInt($responseFields[0], someField);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "GraphQlListOfObject{"
          + "someField=" + someField
          + "}";
      }
      return $toString;
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
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= someField;
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<GraphQlListOfObject> {
      @Override
      public GraphQlListOfObject map(ResponseReader reader) throws IOException {
        final int someField = reader.readInt($responseFields[0]);
        return new GraphQlListOfObject(someField);
      }
    }
  }
}
