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
import com.apollographql.apollo.api.internal.Utils;
import com.example.scalar_types.type.CustomType;
import java.lang.Boolean;
import java.lang.Double;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<TestQuery.Data, Optional<TestQuery.Data>, Operation.Variables> {
  public static final String OPERATION_DEFINITION = "";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  public static final OperationName OPERATION_NAME = new OperationName() {
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
  public String operationId() {
    return null;
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
      ResponseField.forString("graphQlString", "graphQlString", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forCustomType("graphQlIdNullable", "graphQlIdNullable", null, true, CustomType.ID, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forCustomType("graphQlIdNonNullable", "graphQlIdNonNullable", null, false, CustomType.ID, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forLong("graphQlIntNullable", "graphQlIntNullable", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forLong("graphQlIntNonNullable", "graphQlIntNonNullable", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forDouble("graphQlFloatNullable", "graphQlFloatNullable", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forDouble("graphQlFloatNonNullable", "graphQlFloatNonNullable", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forBoolean("graphQlBooleanNullable", "graphQlBooleanNullable", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forBoolean("graphQlBooleanNonNullable", "graphQlBooleanNonNullable", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("graphQlListOfInt", "graphQlListOfInt", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("graphQlListOfObjects", "graphQlListOfObjects", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final Optional<String> graphQlString;

    final Optional<String> graphQlIdNullable;

    final @Nonnull String graphQlIdNonNullable;

    final Optional<Long> graphQlIntNullable;

    final long graphQlIntNonNullable;

    final Optional<Double> graphQlFloatNullable;

    final double graphQlFloatNonNullable;

    final Optional<Boolean> graphQlBooleanNullable;

    final boolean graphQlBooleanNonNullable;

    final Optional<List<Long>> graphQlListOfInt;

    final Optional<List<GraphQlListOfObject>> graphQlListOfObjects;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Data(@Nullable String graphQlString, @Nullable String graphQlIdNullable,
        @Nonnull String graphQlIdNonNullable, @Nullable Long graphQlIntNullable,
        long graphQlIntNonNullable, @Nullable Double graphQlFloatNullable,
        double graphQlFloatNonNullable, @Nullable Boolean graphQlBooleanNullable,
        boolean graphQlBooleanNonNullable, @Nullable List<Long> graphQlListOfInt,
        @Nullable List<GraphQlListOfObject> graphQlListOfObjects) {
      this.graphQlString = Optional.fromNullable(graphQlString);
      this.graphQlIdNullable = Optional.fromNullable(graphQlIdNullable);
      this.graphQlIdNonNullable = Utils.checkNotNull(graphQlIdNonNullable, "graphQlIdNonNullable == null");
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

    public Optional<Long> graphQlIntNullable() {
      return this.graphQlIntNullable;
    }

    public long graphQlIntNonNullable() {
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

    public Optional<List<Long>> graphQlListOfInt() {
      return this.graphQlListOfInt;
    }

    public Optional<List<GraphQlListOfObject>> graphQlListOfObjects() {
      return this.graphQlListOfObjects;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], graphQlString.isPresent() ? graphQlString.get() : null);
          writer.writeCustom((ResponseField.CustomTypeField) $responseFields[1], graphQlIdNullable.isPresent() ? graphQlIdNullable.get() : null);
          writer.writeCustom((ResponseField.CustomTypeField) $responseFields[2], graphQlIdNonNullable);
          writer.writeLong($responseFields[3], graphQlIntNullable.isPresent() ? graphQlIntNullable.get() : null);
          writer.writeLong($responseFields[4], graphQlIntNonNullable);
          writer.writeDouble($responseFields[5], graphQlFloatNullable.isPresent() ? graphQlFloatNullable.get() : null);
          writer.writeDouble($responseFields[6], graphQlFloatNonNullable);
          writer.writeBoolean($responseFields[7], graphQlBooleanNullable.isPresent() ? graphQlBooleanNullable.get() : null);
          writer.writeBoolean($responseFields[8], graphQlBooleanNonNullable);
          writer.writeList($responseFields[9], graphQlListOfInt.isPresent() ? graphQlListOfInt.get() : null, new ResponseWriter.ListWriter() {
            @Override
            public void write(Object value, ResponseWriter.ListItemWriter listItemWriter) {
              listItemWriter.writeLong(value);
            }
          });
          writer.writeList($responseFields[10], graphQlListOfObjects.isPresent() ? graphQlListOfObjects.get() : null, new ResponseWriter.ListWriter() {
            @Override
            public void write(Object value, ResponseWriter.ListItemWriter listItemWriter) {
              listItemWriter.writeObject(((GraphQlListOfObject) value).marshaller());
            }
          });
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
      public Data map(ResponseReader reader) {
        final String graphQlString = reader.readString($responseFields[0]);
        final String graphQlIdNullable = reader.readCustomType((ResponseField.CustomTypeField) $responseFields[1]);
        final String graphQlIdNonNullable = reader.readCustomType((ResponseField.CustomTypeField) $responseFields[2]);
        final Long graphQlIntNullable = reader.readLong($responseFields[3]);
        final long graphQlIntNonNullable = reader.readLong($responseFields[4]);
        final Double graphQlFloatNullable = reader.readDouble($responseFields[5]);
        final double graphQlFloatNonNullable = reader.readDouble($responseFields[6]);
        final Boolean graphQlBooleanNullable = reader.readBoolean($responseFields[7]);
        final boolean graphQlBooleanNonNullable = reader.readBoolean($responseFields[8]);
        final List<Long> graphQlListOfInt = reader.readList($responseFields[9], new ResponseReader.ListReader<Long>() {
          @Override
          public Long read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readLong();
          }
        });
        final List<GraphQlListOfObject> graphQlListOfObjects = reader.readList($responseFields[10], new ResponseReader.ListReader<GraphQlListOfObject>() {
          @Override
          public GraphQlListOfObject read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readObject(new ResponseReader.ObjectReader<GraphQlListOfObject>() {
              @Override
              public GraphQlListOfObject read(ResponseReader reader) {
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
      ResponseField.forLong("someField", "someField", null, false, Collections.<ResponseField.Condition>emptyList())
    };

    final long someField;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public GraphQlListOfObject(long someField) {
      this.someField = someField;
    }

    public long someField() {
      return this.someField;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeLong($responseFields[0], someField);
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
      public GraphQlListOfObject map(ResponseReader reader) {
        final long someField = reader.readLong($responseFields[0]);
        return new GraphQlListOfObject(someField);
      }
    }
  }
}
