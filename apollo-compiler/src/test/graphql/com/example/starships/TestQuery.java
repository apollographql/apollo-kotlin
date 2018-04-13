package com.example.starships;

import com.apollographql.apollo.api.InputFieldMarshaller;
import com.apollographql.apollo.api.InputFieldWriter;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ResponseWriter;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.api.internal.UnmodifiableMapBuilder;
import com.apollographql.apollo.api.internal.Utils;
import com.example.starships.type.CustomType;
import java.io.IOException;
import java.lang.Double;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<TestQuery.Data, Optional<TestQuery.Data>, TestQuery.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery($id: ID!) {\n"
      + "  starship(id: $id) {\n"
      + "    __typename\n"
      + "    id\n"
      + "    name\n"
      + "    coordinates\n"
      + "  }\n"
      + "}";

  public static final String OPERATION_ID = "7e7fbeaf6d1978c07e66bb041e7022ad5a7b9b8ca84b28b4faadfb1950ae340c";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  public static final OperationName OPERATION_NAME = new OperationName() {
    @Override
    public String name() {
      return "TestQuery";
    }
  };

  private final TestQuery.Variables variables;

  public TestQuery(@Nonnull String id) {
    Utils.checkNotNull(id, "id == null");
    variables = new TestQuery.Variables(id);
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
  public TestQuery.Variables variables() {
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
    private @Nonnull String id;

    Builder() {
    }

    public Builder id(@Nonnull String id) {
      this.id = id;
      return this;
    }

    public TestQuery build() {
      Utils.checkNotNull(id, "id == null");
      return new TestQuery(id);
    }
  }

  public static final class Variables extends Operation.Variables {
    private final @Nonnull String id;

    private final transient Map<String, Object> valueMap = new LinkedHashMap<>();

    Variables(@Nonnull String id) {
      this.id = id;
      this.valueMap.put("id", id);
    }

    public @Nonnull String id() {
      return id;
    }

    @Override
    public Map<String, Object> valueMap() {
      return Collections.unmodifiableMap(valueMap);
    }

    @Override
    public InputFieldMarshaller marshaller() {
      return new InputFieldMarshaller() {
        @Override
        public void marshal(InputFieldWriter writer) throws IOException {
          writer.writeCustom("id", com.example.starships.type.CustomType.ID, id);
        }
      };
    }
  }

  public static class Data implements Operation.Data {
    static final ResponseField[] $responseFields = {
      ResponseField.forObject("starship", "starship", new UnmodifiableMapBuilder<String, Object>(1)
      .put("id", new UnmodifiableMapBuilder<String, Object>(2)
        .put("kind", "Variable")
        .put("variableName", "id")
        .build())
      .build(), true, Collections.<ResponseField.Condition>emptyList())
    };

    final Optional<Starship> starship;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Data(@Nullable Starship starship) {
      this.starship = Optional.fromNullable(starship);
    }

    public Optional<Starship> starship() {
      return this.starship;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeObject($responseFields[0], starship.isPresent() ? starship.get().marshaller() : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Data{"
          + "starship=" + starship
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
        return this.starship.equals(that.starship);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= starship.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final Starship.Mapper starshipFieldMapper = new Starship.Mapper();

      @Override
      public Data map(ResponseReader reader) {
        final Starship starship = reader.readObject($responseFields[0], new ResponseReader.ObjectReader<Starship>() {
          @Override
          public Starship read(ResponseReader reader) {
            return starshipFieldMapper.map(reader);
          }
        });
        return new Data(starship);
      }
    }
  }

  public static class Starship {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forCustomType("id", "id", null, false, CustomType.ID, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("name", "name", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forList("coordinates", "coordinates", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @Nonnull String __typename;

    final @Nonnull String id;

    final @Nonnull String name;

    final Optional<List<List<Double>>> coordinates;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Starship(@Nonnull String __typename, @Nonnull String id, @Nonnull String name,
        @Nullable List<List<Double>> coordinates) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.id = Utils.checkNotNull(id, "id == null");
      this.name = Utils.checkNotNull(name, "name == null");
      this.coordinates = Optional.fromNullable(coordinates);
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * The ID of the starship
     */
    public @Nonnull String id() {
      return this.id;
    }

    /**
     * The name of the starship
     */
    public @Nonnull String name() {
      return this.name;
    }

    public Optional<List<List<Double>>> coordinates() {
      return this.coordinates;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeCustom((ResponseField.CustomTypeField) $responseFields[1], id);
          writer.writeString($responseFields[2], name);
          writer.writeList($responseFields[3], coordinates.isPresent() ? coordinates.get() : null, new ResponseWriter.ListWriter() {
            @Override
            public void write(Object value, ResponseWriter.ListItemWriter listItemWriter) {
              listItemWriter.writeDouble(value);
            }
          });
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Starship{"
          + "__typename=" + __typename + ", "
          + "id=" + id + ", "
          + "name=" + name + ", "
          + "coordinates=" + coordinates
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Starship) {
        Starship that = (Starship) o;
        return this.__typename.equals(that.__typename)
         && this.id.equals(that.id)
         && this.name.equals(that.name)
         && this.coordinates.equals(that.coordinates);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= __typename.hashCode();
        h *= 1000003;
        h ^= id.hashCode();
        h *= 1000003;
        h ^= name.hashCode();
        h *= 1000003;
        h ^= coordinates.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Starship> {
      @Override
      public Starship map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String id = reader.readCustomType((ResponseField.CustomTypeField) $responseFields[1]);
        final String name = reader.readString($responseFields[2]);
        final List<List<Double>> coordinates = reader.readList($responseFields[3], new ResponseReader.ListReader<List<Double>>() {
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
        return new Starship(__typename, id, name, coordinates);
      }
    }
  }
}
