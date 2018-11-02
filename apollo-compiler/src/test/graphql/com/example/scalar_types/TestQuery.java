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
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<TestQuery.Data, Optional<TestQuery.Data>, Operation.Variables> {
  public static final String OPERATION_DEFINITION = "";

  public static final String OPERATION_ID = null;

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
    return OPERATION_ID;
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
      ResponseField.forInt("graphQlIntNullable", "graphQlIntNullable", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("graphQlIntNonNullable", "graphQlIntNonNullable", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forDouble("graphQlFloatNullable", "graphQlFloatNullable", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forDouble("graphQlFloatNonNullable", "graphQlFloatNonNullable", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forBoolean("graphQlBooleanNullable", "graphQlBooleanNullable", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forBoolean("graphQlBooleanNonNullable", "graphQlBooleanNonNullable", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("graphQlListOfStringNullable", "graphQlListOfStringNullable", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("graphQlListOfStringNonNullable", "graphQlListOfStringNonNullable", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("graphQlListOfIdNullable", "graphQlListOfIdNullable", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("graphQlListOfIdNonNullable", "graphQlListOfIdNonNullable", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("graphQlListOfIntNullable", "graphQlListOfIntNullable", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("graphQlListOfIntNonNullable", "graphQlListOfIntNonNullable", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("graphQlListOfFloatNullable", "graphQlListOfFloatNullable", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("graphQlListOfFloatNonNullable", "graphQlListOfFloatNonNullable", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("graphQlListOfBooleanNullable", "graphQlListOfBooleanNullable", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("graphQlListOfListOfString", "graphQlListOfListOfString", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("graphQlListOfListOfId", "graphQlListOfListOfId", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("graphQlListOfListOfInt", "graphQlListOfListOfInt", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("graphQlListOfListOfFloat", "graphQlListOfListOfFloat", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("graphQlListOfListOfBoolean", "graphQlListOfListOfBoolean", null, false, Collections.<ResponseField.Condition>emptyList())
    };

    final Optional<String> graphQlString;

    final Optional<String> graphQlIdNullable;

    final @NotNull String graphQlIdNonNullable;

    final Optional<Integer> graphQlIntNullable;

    final int graphQlIntNonNullable;

    final Optional<Double> graphQlFloatNullable;

    final double graphQlFloatNonNullable;

    final Optional<Boolean> graphQlBooleanNullable;

    final boolean graphQlBooleanNonNullable;

    final @NotNull List<String> graphQlListOfStringNullable;

    final @NotNull List<String> graphQlListOfStringNonNullable;

    final Optional<List<String>> graphQlListOfIdNullable;

    final @NotNull List<String> graphQlListOfIdNonNullable;

    final Optional<List<Integer>> graphQlListOfIntNullable;

    final @NotNull List<Integer> graphQlListOfIntNonNullable;

    final Optional<List<Double>> graphQlListOfFloatNullable;

    final @NotNull List<Double> graphQlListOfFloatNonNullable;

    final Optional<List<Boolean>> graphQlListOfBooleanNullable;

    final @NotNull List<List<String>> graphQlListOfListOfString;

    final @NotNull List<List<String>> graphQlListOfListOfId;

    final @NotNull List<List<Integer>> graphQlListOfListOfInt;

    final @NotNull List<List<Double>> graphQlListOfListOfFloat;

    final @NotNull List<List<Boolean>> graphQlListOfListOfBoolean;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

    public Data(@Nullable String graphQlString, @Nullable String graphQlIdNullable,
        @NotNull String graphQlIdNonNullable, @Nullable Integer graphQlIntNullable,
        int graphQlIntNonNullable, @Nullable Double graphQlFloatNullable,
        double graphQlFloatNonNullable, @Nullable Boolean graphQlBooleanNullable,
        boolean graphQlBooleanNonNullable, @NotNull List<String> graphQlListOfStringNullable,
        @NotNull List<String> graphQlListOfStringNonNullable,
        @Nullable List<String> graphQlListOfIdNullable,
        @NotNull List<String> graphQlListOfIdNonNullable,
        @Nullable List<Integer> graphQlListOfIntNullable,
        @NotNull List<Integer> graphQlListOfIntNonNullable,
        @Nullable List<Double> graphQlListOfFloatNullable,
        @NotNull List<Double> graphQlListOfFloatNonNullable,
        @Nullable List<Boolean> graphQlListOfBooleanNullable,
        @NotNull List<List<String>> graphQlListOfListOfString,
        @NotNull List<List<String>> graphQlListOfListOfId,
        @NotNull List<List<Integer>> graphQlListOfListOfInt,
        @NotNull List<List<Double>> graphQlListOfListOfFloat,
        @NotNull List<List<Boolean>> graphQlListOfListOfBoolean) {
      this.graphQlString = Optional.fromNullable(graphQlString);
      this.graphQlIdNullable = Optional.fromNullable(graphQlIdNullable);
      this.graphQlIdNonNullable = Utils.checkNotNull(graphQlIdNonNullable, "graphQlIdNonNullable == null");
      this.graphQlIntNullable = Optional.fromNullable(graphQlIntNullable);
      this.graphQlIntNonNullable = graphQlIntNonNullable;
      this.graphQlFloatNullable = Optional.fromNullable(graphQlFloatNullable);
      this.graphQlFloatNonNullable = graphQlFloatNonNullable;
      this.graphQlBooleanNullable = Optional.fromNullable(graphQlBooleanNullable);
      this.graphQlBooleanNonNullable = graphQlBooleanNonNullable;
      this.graphQlListOfStringNullable = Utils.checkNotNull(graphQlListOfStringNullable, "graphQlListOfStringNullable == null");
      this.graphQlListOfStringNonNullable = Utils.checkNotNull(graphQlListOfStringNonNullable, "graphQlListOfStringNonNullable == null");
      this.graphQlListOfIdNullable = Optional.fromNullable(graphQlListOfIdNullable);
      this.graphQlListOfIdNonNullable = Utils.checkNotNull(graphQlListOfIdNonNullable, "graphQlListOfIdNonNullable == null");
      this.graphQlListOfIntNullable = Optional.fromNullable(graphQlListOfIntNullable);
      this.graphQlListOfIntNonNullable = Utils.checkNotNull(graphQlListOfIntNonNullable, "graphQlListOfIntNonNullable == null");
      this.graphQlListOfFloatNullable = Optional.fromNullable(graphQlListOfFloatNullable);
      this.graphQlListOfFloatNonNullable = Utils.checkNotNull(graphQlListOfFloatNonNullable, "graphQlListOfFloatNonNullable == null");
      this.graphQlListOfBooleanNullable = Optional.fromNullable(graphQlListOfBooleanNullable);
      this.graphQlListOfListOfString = Utils.checkNotNull(graphQlListOfListOfString, "graphQlListOfListOfString == null");
      this.graphQlListOfListOfId = Utils.checkNotNull(graphQlListOfListOfId, "graphQlListOfListOfId == null");
      this.graphQlListOfListOfInt = Utils.checkNotNull(graphQlListOfListOfInt, "graphQlListOfListOfInt == null");
      this.graphQlListOfListOfFloat = Utils.checkNotNull(graphQlListOfListOfFloat, "graphQlListOfListOfFloat == null");
      this.graphQlListOfListOfBoolean = Utils.checkNotNull(graphQlListOfListOfBoolean, "graphQlListOfListOfBoolean == null");
    }

    public Optional<String> graphQlString() {
      return this.graphQlString;
    }

    public Optional<String> graphQlIdNullable() {
      return this.graphQlIdNullable;
    }

    public @NotNull String graphQlIdNonNullable() {
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

    public @NotNull List<String> graphQlListOfStringNullable() {
      return this.graphQlListOfStringNullable;
    }

    public @NotNull List<String> graphQlListOfStringNonNullable() {
      return this.graphQlListOfStringNonNullable;
    }

    public Optional<List<String>> graphQlListOfIdNullable() {
      return this.graphQlListOfIdNullable;
    }

    public @NotNull List<String> graphQlListOfIdNonNullable() {
      return this.graphQlListOfIdNonNullable;
    }

    public Optional<List<Integer>> graphQlListOfIntNullable() {
      return this.graphQlListOfIntNullable;
    }

    public @NotNull List<Integer> graphQlListOfIntNonNullable() {
      return this.graphQlListOfIntNonNullable;
    }

    public Optional<List<Double>> graphQlListOfFloatNullable() {
      return this.graphQlListOfFloatNullable;
    }

    public @NotNull List<Double> graphQlListOfFloatNonNullable() {
      return this.graphQlListOfFloatNonNullable;
    }

    public Optional<List<Boolean>> graphQlListOfBooleanNullable() {
      return this.graphQlListOfBooleanNullable;
    }

    public @NotNull List<List<String>> graphQlListOfListOfString() {
      return this.graphQlListOfListOfString;
    }

    public @NotNull List<List<String>> graphQlListOfListOfId() {
      return this.graphQlListOfListOfId;
    }

    public @NotNull List<List<Integer>> graphQlListOfListOfInt() {
      return this.graphQlListOfListOfInt;
    }

    public @NotNull List<List<Double>> graphQlListOfListOfFloat() {
      return this.graphQlListOfListOfFloat;
    }

    public @NotNull List<List<Boolean>> graphQlListOfListOfBoolean() {
      return this.graphQlListOfListOfBoolean;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], graphQlString.isPresent() ? graphQlString.get() : null);
          writer.writeCustom((ResponseField.CustomTypeField) $responseFields[1], graphQlIdNullable.isPresent() ? graphQlIdNullable.get() : null);
          writer.writeCustom((ResponseField.CustomTypeField) $responseFields[2], graphQlIdNonNullable);
          writer.writeInt($responseFields[3], graphQlIntNullable.isPresent() ? graphQlIntNullable.get() : null);
          writer.writeInt($responseFields[4], graphQlIntNonNullable);
          writer.writeDouble($responseFields[5], graphQlFloatNullable.isPresent() ? graphQlFloatNullable.get() : null);
          writer.writeDouble($responseFields[6], graphQlFloatNonNullable);
          writer.writeBoolean($responseFields[7], graphQlBooleanNullable.isPresent() ? graphQlBooleanNullable.get() : null);
          writer.writeBoolean($responseFields[8], graphQlBooleanNonNullable);
          writer.writeList($responseFields[9], graphQlListOfStringNullable, new ResponseWriter.ListWriter() {
            @Override
            public void write(List items, ResponseWriter.ListItemWriter listItemWriter) {
              for (Object item : items) {
                listItemWriter.writeString((String) item);
              }
            }
          });
          writer.writeList($responseFields[10], graphQlListOfStringNonNullable, new ResponseWriter.ListWriter() {
            @Override
            public void write(List items, ResponseWriter.ListItemWriter listItemWriter) {
              for (Object item : items) {
                listItemWriter.writeString((String) item);
              }
            }
          });
          writer.writeList($responseFields[11], graphQlListOfIdNullable.isPresent() ? graphQlListOfIdNullable.get() : null, new ResponseWriter.ListWriter() {
            @Override
            public void write(List items, ResponseWriter.ListItemWriter listItemWriter) {
              for (Object item : items) {
                listItemWriter.writeCustom(CustomType.ID, item);
              }
            }
          });
          writer.writeList($responseFields[12], graphQlListOfIdNonNullable, new ResponseWriter.ListWriter() {
            @Override
            public void write(List items, ResponseWriter.ListItemWriter listItemWriter) {
              for (Object item : items) {
                listItemWriter.writeCustom(CustomType.ID, item);
              }
            }
          });
          writer.writeList($responseFields[13], graphQlListOfIntNullable.isPresent() ? graphQlListOfIntNullable.get() : null, new ResponseWriter.ListWriter() {
            @Override
            public void write(List items, ResponseWriter.ListItemWriter listItemWriter) {
              for (Object item : items) {
                listItemWriter.writeInt((Integer) item);
              }
            }
          });
          writer.writeList($responseFields[14], graphQlListOfIntNonNullable, new ResponseWriter.ListWriter() {
            @Override
            public void write(List items, ResponseWriter.ListItemWriter listItemWriter) {
              for (Object item : items) {
                listItemWriter.writeInt((Integer) item);
              }
            }
          });
          writer.writeList($responseFields[15], graphQlListOfFloatNullable.isPresent() ? graphQlListOfFloatNullable.get() : null, new ResponseWriter.ListWriter() {
            @Override
            public void write(List items, ResponseWriter.ListItemWriter listItemWriter) {
              for (Object item : items) {
                listItemWriter.writeDouble((Double) item);
              }
            }
          });
          writer.writeList($responseFields[16], graphQlListOfFloatNonNullable, new ResponseWriter.ListWriter() {
            @Override
            public void write(List items, ResponseWriter.ListItemWriter listItemWriter) {
              for (Object item : items) {
                listItemWriter.writeDouble((Double) item);
              }
            }
          });
          writer.writeList($responseFields[17], graphQlListOfBooleanNullable.isPresent() ? graphQlListOfBooleanNullable.get() : null, new ResponseWriter.ListWriter() {
            @Override
            public void write(List items, ResponseWriter.ListItemWriter listItemWriter) {
              for (Object item : items) {
                listItemWriter.writeBoolean((Boolean) item);
              }
            }
          });
          writer.writeList($responseFields[18], graphQlListOfListOfString, new ResponseWriter.ListWriter() {
            @Override
            public void write(List items, ResponseWriter.ListItemWriter listItemWriter) {
              for (Object item : items) {
                listItemWriter.writeList((List) item, new ResponseWriter.ListWriter() {
                  @Override
                  public void write(List items, ResponseWriter.ListItemWriter listItemWriter) {
                    for (Object item : items) {
                      listItemWriter.writeString((String) item);
                    }
                  }
                });
              }
            }
          });
          writer.writeList($responseFields[19], graphQlListOfListOfId, new ResponseWriter.ListWriter() {
            @Override
            public void write(List items, ResponseWriter.ListItemWriter listItemWriter) {
              for (Object item : items) {
                listItemWriter.writeList((List) item, new ResponseWriter.ListWriter() {
                  @Override
                  public void write(List items, ResponseWriter.ListItemWriter listItemWriter) {
                    for (Object item : items) {
                      listItemWriter.writeCustom(CustomType.ID, item);
                    }
                  }
                });
              }
            }
          });
          writer.writeList($responseFields[20], graphQlListOfListOfInt, new ResponseWriter.ListWriter() {
            @Override
            public void write(List items, ResponseWriter.ListItemWriter listItemWriter) {
              for (Object item : items) {
                listItemWriter.writeList((List) item, new ResponseWriter.ListWriter() {
                  @Override
                  public void write(List items, ResponseWriter.ListItemWriter listItemWriter) {
                    for (Object item : items) {
                      listItemWriter.writeInt((Integer) item);
                    }
                  }
                });
              }
            }
          });
          writer.writeList($responseFields[21], graphQlListOfListOfFloat, new ResponseWriter.ListWriter() {
            @Override
            public void write(List items, ResponseWriter.ListItemWriter listItemWriter) {
              for (Object item : items) {
                listItemWriter.writeList((List) item, new ResponseWriter.ListWriter() {
                  @Override
                  public void write(List items, ResponseWriter.ListItemWriter listItemWriter) {
                    for (Object item : items) {
                      listItemWriter.writeDouble((Double) item);
                    }
                  }
                });
              }
            }
          });
          writer.writeList($responseFields[22], graphQlListOfListOfBoolean, new ResponseWriter.ListWriter() {
            @Override
            public void write(List items, ResponseWriter.ListItemWriter listItemWriter) {
              for (Object item : items) {
                listItemWriter.writeList((List) item, new ResponseWriter.ListWriter() {
                  @Override
                  public void write(List items, ResponseWriter.ListItemWriter listItemWriter) {
                    for (Object item : items) {
                      listItemWriter.writeBoolean((Boolean) item);
                    }
                  }
                });
              }
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
          + "graphQlListOfStringNullable=" + graphQlListOfStringNullable + ", "
          + "graphQlListOfStringNonNullable=" + graphQlListOfStringNonNullable + ", "
          + "graphQlListOfIdNullable=" + graphQlListOfIdNullable + ", "
          + "graphQlListOfIdNonNullable=" + graphQlListOfIdNonNullable + ", "
          + "graphQlListOfIntNullable=" + graphQlListOfIntNullable + ", "
          + "graphQlListOfIntNonNullable=" + graphQlListOfIntNonNullable + ", "
          + "graphQlListOfFloatNullable=" + graphQlListOfFloatNullable + ", "
          + "graphQlListOfFloatNonNullable=" + graphQlListOfFloatNonNullable + ", "
          + "graphQlListOfBooleanNullable=" + graphQlListOfBooleanNullable + ", "
          + "graphQlListOfListOfString=" + graphQlListOfListOfString + ", "
          + "graphQlListOfListOfId=" + graphQlListOfListOfId + ", "
          + "graphQlListOfListOfInt=" + graphQlListOfListOfInt + ", "
          + "graphQlListOfListOfFloat=" + graphQlListOfListOfFloat + ", "
          + "graphQlListOfListOfBoolean=" + graphQlListOfListOfBoolean
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
         && this.graphQlListOfStringNullable.equals(that.graphQlListOfStringNullable)
         && this.graphQlListOfStringNonNullable.equals(that.graphQlListOfStringNonNullable)
         && this.graphQlListOfIdNullable.equals(that.graphQlListOfIdNullable)
         && this.graphQlListOfIdNonNullable.equals(that.graphQlListOfIdNonNullable)
         && this.graphQlListOfIntNullable.equals(that.graphQlListOfIntNullable)
         && this.graphQlListOfIntNonNullable.equals(that.graphQlListOfIntNonNullable)
         && this.graphQlListOfFloatNullable.equals(that.graphQlListOfFloatNullable)
         && this.graphQlListOfFloatNonNullable.equals(that.graphQlListOfFloatNonNullable)
         && this.graphQlListOfBooleanNullable.equals(that.graphQlListOfBooleanNullable)
         && this.graphQlListOfListOfString.equals(that.graphQlListOfListOfString)
         && this.graphQlListOfListOfId.equals(that.graphQlListOfListOfId)
         && this.graphQlListOfListOfInt.equals(that.graphQlListOfListOfInt)
         && this.graphQlListOfListOfFloat.equals(that.graphQlListOfListOfFloat)
         && this.graphQlListOfListOfBoolean.equals(that.graphQlListOfListOfBoolean);
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
        h ^= graphQlListOfStringNullable.hashCode();
        h *= 1000003;
        h ^= graphQlListOfStringNonNullable.hashCode();
        h *= 1000003;
        h ^= graphQlListOfIdNullable.hashCode();
        h *= 1000003;
        h ^= graphQlListOfIdNonNullable.hashCode();
        h *= 1000003;
        h ^= graphQlListOfIntNullable.hashCode();
        h *= 1000003;
        h ^= graphQlListOfIntNonNullable.hashCode();
        h *= 1000003;
        h ^= graphQlListOfFloatNullable.hashCode();
        h *= 1000003;
        h ^= graphQlListOfFloatNonNullable.hashCode();
        h *= 1000003;
        h ^= graphQlListOfBooleanNullable.hashCode();
        h *= 1000003;
        h ^= graphQlListOfListOfString.hashCode();
        h *= 1000003;
        h ^= graphQlListOfListOfId.hashCode();
        h *= 1000003;
        h ^= graphQlListOfListOfInt.hashCode();
        h *= 1000003;
        h ^= graphQlListOfListOfFloat.hashCode();
        h *= 1000003;
        h ^= graphQlListOfListOfBoolean.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      @Override
      public Data map(ResponseReader reader) {
        final String graphQlString = reader.readString($responseFields[0]);
        final String graphQlIdNullable = reader.readCustomType((ResponseField.CustomTypeField) $responseFields[1]);
        final String graphQlIdNonNullable = reader.readCustomType((ResponseField.CustomTypeField) $responseFields[2]);
        final Integer graphQlIntNullable = reader.readInt($responseFields[3]);
        final int graphQlIntNonNullable = reader.readInt($responseFields[4]);
        final Double graphQlFloatNullable = reader.readDouble($responseFields[5]);
        final double graphQlFloatNonNullable = reader.readDouble($responseFields[6]);
        final Boolean graphQlBooleanNullable = reader.readBoolean($responseFields[7]);
        final boolean graphQlBooleanNonNullable = reader.readBoolean($responseFields[8]);
        final List<String> graphQlListOfStringNullable = reader.readList($responseFields[9], new ResponseReader.ListReader<String>() {
          @Override
          public String read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readString();
          }
        });
        final List<String> graphQlListOfStringNonNullable = reader.readList($responseFields[10], new ResponseReader.ListReader<String>() {
          @Override
          public String read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readString();
          }
        });
        final List<String> graphQlListOfIdNullable = reader.readList($responseFields[11], new ResponseReader.ListReader<String>() {
          @Override
          public String read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readCustomType(CustomType.ID);
          }
        });
        final List<String> graphQlListOfIdNonNullable = reader.readList($responseFields[12], new ResponseReader.ListReader<String>() {
          @Override
          public String read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readCustomType(CustomType.ID);
          }
        });
        final List<Integer> graphQlListOfIntNullable = reader.readList($responseFields[13], new ResponseReader.ListReader<Integer>() {
          @Override
          public Integer read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readInt();
          }
        });
        final List<Integer> graphQlListOfIntNonNullable = reader.readList($responseFields[14], new ResponseReader.ListReader<Integer>() {
          @Override
          public Integer read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readInt();
          }
        });
        final List<Double> graphQlListOfFloatNullable = reader.readList($responseFields[15], new ResponseReader.ListReader<Double>() {
          @Override
          public Double read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readDouble();
          }
        });
        final List<Double> graphQlListOfFloatNonNullable = reader.readList($responseFields[16], new ResponseReader.ListReader<Double>() {
          @Override
          public Double read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readDouble();
          }
        });
        final List<Boolean> graphQlListOfBooleanNullable = reader.readList($responseFields[17], new ResponseReader.ListReader<Boolean>() {
          @Override
          public Boolean read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readBoolean();
          }
        });
        final List<List<String>> graphQlListOfListOfString = reader.readList($responseFields[18], new ResponseReader.ListReader<List<String>>() {
          @Override
          public List<String> read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readList(new ResponseReader.ListReader<String>() {
              @Override
              public String read(ResponseReader.ListItemReader listItemReader) {
                return listItemReader.readString();
              }
            });
          }
        });
        final List<List<String>> graphQlListOfListOfId = reader.readList($responseFields[19], new ResponseReader.ListReader<List<String>>() {
          @Override
          public List<String> read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readList(new ResponseReader.ListReader<String>() {
              @Override
              public String read(ResponseReader.ListItemReader listItemReader) {
                return listItemReader.readCustomType(CustomType.ID);
              }
            });
          }
        });
        final List<List<Integer>> graphQlListOfListOfInt = reader.readList($responseFields[20], new ResponseReader.ListReader<List<Integer>>() {
          @Override
          public List<Integer> read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readList(new ResponseReader.ListReader<Integer>() {
              @Override
              public Integer read(ResponseReader.ListItemReader listItemReader) {
                return listItemReader.readInt();
              }
            });
          }
        });
        final List<List<Double>> graphQlListOfListOfFloat = reader.readList($responseFields[21], new ResponseReader.ListReader<List<Double>>() {
          @Override
          public List<Double> read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readList(new ResponseReader.ListReader<Double>() {
              @Override
              public Double read(ResponseReader.ListItemReader listItemReader) {
                return listItemReader.readDouble();
              }
            });
          }
        });
        final List<List<Boolean>> graphQlListOfListOfBoolean = reader.readList($responseFields[22], new ResponseReader.ListReader<List<Boolean>>() {
          @Override
          public List<Boolean> read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readList(new ResponseReader.ListReader<Boolean>() {
              @Override
              public Boolean read(ResponseReader.ListItemReader listItemReader) {
                return listItemReader.readBoolean();
              }
            });
          }
        });
        return new Data(graphQlString, graphQlIdNullable, graphQlIdNonNullable, graphQlIntNullable, graphQlIntNonNullable, graphQlFloatNullable, graphQlFloatNonNullable, graphQlBooleanNullable, graphQlBooleanNonNullable, graphQlListOfStringNullable, graphQlListOfStringNonNullable, graphQlListOfIdNullable, graphQlListOfIdNonNullable, graphQlListOfIntNullable, graphQlListOfIntNonNullable, graphQlListOfFloatNullable, graphQlListOfFloatNonNullable, graphQlListOfBooleanNullable, graphQlListOfListOfString, graphQlListOfListOfId, graphQlListOfListOfInt, graphQlListOfListOfFloat, graphQlListOfListOfBoolean);
      }
    }
  }
}
