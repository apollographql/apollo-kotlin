// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.fragments_with_type_condition_nullable;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ScalarTypeAdapters;
import com.apollographql.apollo.api.internal.OperationRequestBodyComposer;
import com.apollographql.apollo.api.internal.QueryDocumentMinifier;
import com.apollographql.apollo.api.internal.ResponseFieldMapper;
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller;
import com.apollographql.apollo.api.internal.ResponseReader;
import com.apollographql.apollo.api.internal.ResponseWriter;
import com.apollographql.apollo.api.internal.SimpleOperationResponseParser;
import com.apollographql.apollo.api.internal.Utils;
import com.example.fragments_with_type_condition_nullable.fragment.DroidDetails;
import com.example.fragments_with_type_condition_nullable.fragment.HumanDetails;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Arrays;
import java.util.Collections;
import okio.BufferedSource;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TestQuery implements Query<TestQuery.Data, TestQuery.Data, Operation.Variables> {
  public static final String OPERATION_ID = "919cec7210259fa24fc6026fe680b96f357c14ebf3c8a734979dcfb819685d6a";

  public static final String QUERY_DOCUMENT = QueryDocumentMinifier.minify(
    "query TestQuery {\n"
        + "  r2: hero {\n"
        + "    __typename\n"
        + "    ...HumanDetails\n"
        + "    ...DroidDetails\n"
        + "  }\n"
        + "  luke: hero {\n"
        + "    __typename\n"
        + "    ...HumanDetails\n"
        + "    ...DroidDetails\n"
        + "  }\n"
        + "}\n"
        + "fragment HumanDetails on Human {\n"
        + "  __typename\n"
        + "  name\n"
        + "  height\n"
        + "}\n"
        + "fragment DroidDetails on Droid {\n"
        + "  __typename\n"
        + "  name\n"
        + "  primaryFunction\n"
        + "}"
  );

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
  public TestQuery.Data wrapData(TestQuery.Data data) {
    return data;
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

  @Override
  @NotNull
  public Response<TestQuery.Data> parse(@NotNull final BufferedSource source,
      @NotNull final ScalarTypeAdapters scalarTypeAdapters) throws IOException {
    return SimpleOperationResponseParser.parse(source, this, scalarTypeAdapters);
  }

  @Override
  @NotNull
  public Response<TestQuery.Data> parse(@NotNull final BufferedSource source) throws IOException {
    return parse(source, ScalarTypeAdapters.DEFAULT);
  }

  @Override
  @NotNull
  public ByteString composeRequestBody(@NotNull final ScalarTypeAdapters scalarTypeAdapters) {
    return OperationRequestBodyComposer.compose(this, false, true, scalarTypeAdapters);
  }

  @NotNull
  public ByteString composeRequestBody() {
    return OperationRequestBodyComposer.compose(this, false, true, ScalarTypeAdapters.DEFAULT);
  }

  @Override
  @NotNull
  public ByteString composeRequestBody(final boolean autoPersistQueries,
      final boolean withQueryDocument, @NotNull final ScalarTypeAdapters scalarTypeAdapters) {
    return OperationRequestBodyComposer.compose(this, autoPersistQueries, withQueryDocument, scalarTypeAdapters);
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
      ResponseField.forObject("r2", "hero", null, true, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forObject("luke", "hero", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @Nullable R2 r2;

    final @Nullable Luke luke;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

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

    @SuppressWarnings({"rawtypes", "unchecked"})
    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeObject($responseFields[0], r2 != null ? r2.marshaller() : null);
          writer.writeObject($responseFields[1], luke != null ? luke.marshaller() : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Data{"
          + "r2=" + r2 + ", "
          + "luke=" + luke
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
        return ((this.r2 == null) ? (that.r2 == null) : this.r2.equals(that.r2))
         && ((this.luke == null) ? (that.luke == null) : this.luke.equals(that.luke));
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= (r2 == null) ? 0 : r2.hashCode();
        h *= 1000003;
        h ^= (luke == null) ? 0 : luke.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final R2.Mapper r2FieldMapper = new R2.Mapper();

      final Luke.Mapper lukeFieldMapper = new Luke.Mapper();

      @Override
      public Data map(ResponseReader reader) {
        final R2 r2 = reader.readObject($responseFields[0], new ResponseReader.ObjectReader<R2>() {
          @Override
          public R2 read(ResponseReader reader) {
            return r2FieldMapper.map(reader);
          }
        });
        final Luke luke = reader.readObject($responseFields[1], new ResponseReader.ObjectReader<Luke>() {
          @Override
          public Luke read(ResponseReader reader) {
            return lukeFieldMapper.map(reader);
          }
        });
        return new Data(r2, luke);
      }
    }
  }

  public static class R2 {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    private final @NotNull Fragments fragments;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

    public R2(@NotNull String __typename, @NotNull Fragments fragments) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.fragments = Utils.checkNotNull(fragments, "fragments == null");
    }

    public @NotNull String __typename() {
      return this.__typename;
    }

    public @NotNull Fragments fragments() {
      return this.fragments;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          fragments.marshaller().marshal(writer);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "R2{"
          + "__typename=" + __typename + ", "
          + "fragments=" + fragments
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof R2) {
        R2 that = (R2) o;
        return this.__typename.equals(that.__typename)
         && this.fragments.equals(that.fragments);
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
        h ^= fragments.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static class Fragments {
      final @Nullable HumanDetails humanDetails;

      final @Nullable DroidDetails droidDetails;

      private transient volatile String $toString;

      private transient volatile int $hashCode;

      private transient volatile boolean $hashCodeMemoized;

      public Fragments(@Nullable HumanDetails humanDetails, @Nullable DroidDetails droidDetails) {
        this.humanDetails = humanDetails;
        this.droidDetails = droidDetails;
      }

      public @Nullable HumanDetails humanDetails() {
        return this.humanDetails;
      }

      public @Nullable DroidDetails droidDetails() {
        return this.droidDetails;
      }

      public ResponseFieldMarshaller marshaller() {
        return new ResponseFieldMarshaller() {
          @Override
          public void marshal(ResponseWriter writer) {
            writer.writeFragment(humanDetails.marshaller());
            writer.writeFragment(droidDetails.marshaller());
          }
        };
      }

      @Override
      public String toString() {
        if ($toString == null) {
          $toString = "Fragments{"
            + "humanDetails=" + humanDetails + ", "
            + "droidDetails=" + droidDetails
            + "}";
        }
        return $toString;
      }

      @Override
      public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof Fragments) {
          Fragments that = (Fragments) o;
          return ((this.humanDetails == null) ? (that.humanDetails == null) : this.humanDetails.equals(that.humanDetails))
           && ((this.droidDetails == null) ? (that.droidDetails == null) : this.droidDetails.equals(that.droidDetails));
        }
        return false;
      }

      @Override
      public int hashCode() {
        if (!$hashCodeMemoized) {
          int h = 1;
          h *= 1000003;
          h ^= (humanDetails == null) ? 0 : humanDetails.hashCode();
          h *= 1000003;
          h ^= (droidDetails == null) ? 0 : droidDetails.hashCode();
          $hashCode = h;
          $hashCodeMemoized = true;
        }
        return $hashCode;
      }

      public static final class Mapper implements ResponseFieldMapper<Fragments> {
        static final ResponseField[] $responseFields = {
          ResponseField.forFragment("__typename", "__typename", Arrays.<ResponseField.Condition>asList(
            ResponseField.Condition.typeCondition(new String[] {"Human"})
          )),
          ResponseField.forFragment("__typename", "__typename", Arrays.<ResponseField.Condition>asList(
            ResponseField.Condition.typeCondition(new String[] {"Droid"})
          ))
        };

        final HumanDetails.Mapper humanDetailsFieldMapper = new HumanDetails.Mapper();

        final DroidDetails.Mapper droidDetailsFieldMapper = new DroidDetails.Mapper();

        @Override
        public @NotNull Fragments map(ResponseReader reader) {
          final HumanDetails humanDetails = reader.readFragment($responseFields[0], new ResponseReader.ObjectReader<HumanDetails>() {
            @Override
            public HumanDetails read(ResponseReader reader) {
              return humanDetailsFieldMapper.map(reader);
            }
          });
          final DroidDetails droidDetails = reader.readFragment($responseFields[1], new ResponseReader.ObjectReader<DroidDetails>() {
            @Override
            public DroidDetails read(ResponseReader reader) {
              return droidDetailsFieldMapper.map(reader);
            }
          });
          return new Fragments(humanDetails, droidDetails);
        }
      }
    }

    public static final class Mapper implements ResponseFieldMapper<R2> {
      final Fragments.Mapper fragmentsFieldMapper = new Fragments.Mapper();

      @Override
      public R2 map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final Fragments fragments = fragmentsFieldMapper.map(reader);
        return new R2(__typename, fragments);
      }
    }
  }

  public static class Luke {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    private final @NotNull Fragments fragments;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

    public Luke(@NotNull String __typename, @NotNull Fragments fragments) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.fragments = Utils.checkNotNull(fragments, "fragments == null");
    }

    public @NotNull String __typename() {
      return this.__typename;
    }

    public @NotNull Fragments fragments() {
      return this.fragments;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          fragments.marshaller().marshal(writer);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Luke{"
          + "__typename=" + __typename + ", "
          + "fragments=" + fragments
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Luke) {
        Luke that = (Luke) o;
        return this.__typename.equals(that.__typename)
         && this.fragments.equals(that.fragments);
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
        h ^= fragments.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static class Fragments {
      final @Nullable HumanDetails humanDetails;

      final @Nullable DroidDetails droidDetails;

      private transient volatile String $toString;

      private transient volatile int $hashCode;

      private transient volatile boolean $hashCodeMemoized;

      public Fragments(@Nullable HumanDetails humanDetails, @Nullable DroidDetails droidDetails) {
        this.humanDetails = humanDetails;
        this.droidDetails = droidDetails;
      }

      public @Nullable HumanDetails humanDetails() {
        return this.humanDetails;
      }

      public @Nullable DroidDetails droidDetails() {
        return this.droidDetails;
      }

      public ResponseFieldMarshaller marshaller() {
        return new ResponseFieldMarshaller() {
          @Override
          public void marshal(ResponseWriter writer) {
            writer.writeFragment(humanDetails.marshaller());
            writer.writeFragment(droidDetails.marshaller());
          }
        };
      }

      @Override
      public String toString() {
        if ($toString == null) {
          $toString = "Fragments{"
            + "humanDetails=" + humanDetails + ", "
            + "droidDetails=" + droidDetails
            + "}";
        }
        return $toString;
      }

      @Override
      public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof Fragments) {
          Fragments that = (Fragments) o;
          return ((this.humanDetails == null) ? (that.humanDetails == null) : this.humanDetails.equals(that.humanDetails))
           && ((this.droidDetails == null) ? (that.droidDetails == null) : this.droidDetails.equals(that.droidDetails));
        }
        return false;
      }

      @Override
      public int hashCode() {
        if (!$hashCodeMemoized) {
          int h = 1;
          h *= 1000003;
          h ^= (humanDetails == null) ? 0 : humanDetails.hashCode();
          h *= 1000003;
          h ^= (droidDetails == null) ? 0 : droidDetails.hashCode();
          $hashCode = h;
          $hashCodeMemoized = true;
        }
        return $hashCode;
      }

      public static final class Mapper implements ResponseFieldMapper<Fragments> {
        static final ResponseField[] $responseFields = {
          ResponseField.forFragment("__typename", "__typename", Arrays.<ResponseField.Condition>asList(
            ResponseField.Condition.typeCondition(new String[] {"Human"})
          )),
          ResponseField.forFragment("__typename", "__typename", Arrays.<ResponseField.Condition>asList(
            ResponseField.Condition.typeCondition(new String[] {"Droid"})
          ))
        };

        final HumanDetails.Mapper humanDetailsFieldMapper = new HumanDetails.Mapper();

        final DroidDetails.Mapper droidDetailsFieldMapper = new DroidDetails.Mapper();

        @Override
        public @NotNull Fragments map(ResponseReader reader) {
          final HumanDetails humanDetails = reader.readFragment($responseFields[0], new ResponseReader.ObjectReader<HumanDetails>() {
            @Override
            public HumanDetails read(ResponseReader reader) {
              return humanDetailsFieldMapper.map(reader);
            }
          });
          final DroidDetails droidDetails = reader.readFragment($responseFields[1], new ResponseReader.ObjectReader<DroidDetails>() {
            @Override
            public DroidDetails read(ResponseReader reader) {
              return droidDetailsFieldMapper.map(reader);
            }
          });
          return new Fragments(humanDetails, droidDetails);
        }
      }
    }

    public static final class Mapper implements ResponseFieldMapper<Luke> {
      final Fragments.Mapper fragmentsFieldMapper = new Fragments.Mapper();

      @Override
      public Luke map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final Fragments fragments = fragmentsFieldMapper.map(reader);
        return new Luke(__typename, fragments);
      }
    }
  }
}
