package com.example.fragments_with_type_condition_nullable;

import com.apollographql.apollo.api.FragmentResponseFieldMapper;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ResponseWriter;
import com.apollographql.apollo.api.internal.Utils;
import com.example.fragments_with_type_condition_nullable.fragment.DroidDetails;
import com.example.fragments_with_type_condition_nullable.fragment.HumanDetails;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<TestQuery.Data, TestQuery.Data, Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery {\n"
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
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION + "\n"
   + HumanDetails.FRAGMENT_DEFINITION + "\n"
   + DroidDetails.FRAGMENT_DEFINITION;

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
  public String operationId() {
    return "185ee12f775bf02624bb5f646f5ed2de3009860b79380264ce4716e65fba947d";
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

  public static final class Builder {
    Builder() {
    }

    public TestQuery build() {
      return new TestQuery();
    }
  }

  public static class Data implements Operation.Data {
    static final ResponseField[] $responseFields = {
      ResponseField.forObject("r2", "hero", null, true),
      ResponseField.forObject("luke", "hero", null, true)
    };

    final @Nullable R2 r2;

    final @Nullable Luke luke;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

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
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forFragment("__typename", "__typename", Arrays.asList("Human",
      "Droid"))
    };

    final @Nonnull String __typename;

    private final @Nonnull Fragments fragments;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public R2(@Nonnull String __typename, @Nonnull Fragments fragments) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.fragments = Utils.checkNotNull(fragments, "fragments == null");
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    public @Nonnull Fragments fragments() {
      return this.fragments;
    }

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

      private volatile String $toString;

      private volatile int $hashCode;

      private volatile boolean $hashCodeMemoized;

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
            final HumanDetails $humanDetails = humanDetails;
            if ($humanDetails != null) {
              $humanDetails.marshaller().marshal(writer);
            }
            final DroidDetails $droidDetails = droidDetails;
            if ($droidDetails != null) {
              $droidDetails.marshaller().marshal(writer);
            }
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

      public static final class Mapper implements FragmentResponseFieldMapper<Fragments> {
        final HumanDetails.Mapper humanDetailsFieldMapper = new HumanDetails.Mapper();

        final DroidDetails.Mapper droidDetailsFieldMapper = new DroidDetails.Mapper();

        @Override
        public @Nonnull Fragments map(ResponseReader reader, @Nonnull String conditionalType) {
          HumanDetails humanDetails = null;
          DroidDetails droidDetails = null;
          if (HumanDetails.POSSIBLE_TYPES.contains(conditionalType)) {
            humanDetails = humanDetailsFieldMapper.map(reader);
          }
          if (DroidDetails.POSSIBLE_TYPES.contains(conditionalType)) {
            droidDetails = droidDetailsFieldMapper.map(reader);
          }
          return new Fragments(humanDetails, droidDetails);
        }
      }
    }

    public static final class Mapper implements ResponseFieldMapper<R2> {
      final Fragments.Mapper fragmentsFieldMapper = new Fragments.Mapper();

      @Override
      public R2 map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final Fragments fragments = reader.readConditional((ResponseField.ConditionalTypeField) $responseFields[1], new ResponseReader.ConditionalTypeReader<Fragments>() {
          @Override
          public Fragments read(String conditionalType, ResponseReader reader) {
            return fragmentsFieldMapper.map(reader, conditionalType);
          }
        });
        return new R2(__typename, fragments);
      }
    }
  }

  public static class Luke {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forFragment("__typename", "__typename", Arrays.asList("Human",
      "Droid"))
    };

    final @Nonnull String __typename;

    private final @Nonnull Fragments fragments;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Luke(@Nonnull String __typename, @Nonnull Fragments fragments) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.fragments = Utils.checkNotNull(fragments, "fragments == null");
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    public @Nonnull Fragments fragments() {
      return this.fragments;
    }

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

      private volatile String $toString;

      private volatile int $hashCode;

      private volatile boolean $hashCodeMemoized;

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
            final HumanDetails $humanDetails = humanDetails;
            if ($humanDetails != null) {
              $humanDetails.marshaller().marshal(writer);
            }
            final DroidDetails $droidDetails = droidDetails;
            if ($droidDetails != null) {
              $droidDetails.marshaller().marshal(writer);
            }
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

      public static final class Mapper implements FragmentResponseFieldMapper<Fragments> {
        final HumanDetails.Mapper humanDetailsFieldMapper = new HumanDetails.Mapper();

        final DroidDetails.Mapper droidDetailsFieldMapper = new DroidDetails.Mapper();

        @Override
        public @Nonnull Fragments map(ResponseReader reader, @Nonnull String conditionalType) {
          HumanDetails humanDetails = null;
          DroidDetails droidDetails = null;
          if (HumanDetails.POSSIBLE_TYPES.contains(conditionalType)) {
            humanDetails = humanDetailsFieldMapper.map(reader);
          }
          if (DroidDetails.POSSIBLE_TYPES.contains(conditionalType)) {
            droidDetails = droidDetailsFieldMapper.map(reader);
          }
          return new Fragments(humanDetails, droidDetails);
        }
      }
    }

    public static final class Mapper implements ResponseFieldMapper<Luke> {
      final Fragments.Mapper fragmentsFieldMapper = new Fragments.Mapper();

      @Override
      public Luke map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final Fragments fragments = reader.readConditional((ResponseField.ConditionalTypeField) $responseFields[1], new ResponseReader.ConditionalTypeReader<Fragments>() {
          @Override
          public Fragments read(String conditionalType, ResponseReader reader) {
            return fragmentsFieldMapper.map(reader, conditionalType);
          }
        });
        return new Luke(__typename, fragments);
      }
    }
  }
}
