package com.example.fragments_with_type_condition;

import com.apollographql.apollo.api.FragmentResponseFieldMapper;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ResponseWriter;
import com.apollographql.apollo.api.internal.Optional;
import com.example.fragments_with_type_condition.fragment.DroidDetails;
import com.example.fragments_with_type_condition.fragment.HumanDetails;
import java.lang.Double;
import java.lang.NullPointerException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<TestQuery.Data, Optional<TestQuery.Data>, Operation.Variables> {
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
      ResponseField.forObject("r2", "hero", null, true),
      ResponseField.forObject("luke", "hero", null, true)
    };

    final Optional<R2> r2;

    final Optional<Luke> luke;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Data(@Nullable R2 r2, @Nullable Luke luke) {
      this.r2 = Optional.fromNullable(r2);
      this.luke = Optional.fromNullable(luke);
    }

    public Optional<R2> r2() {
      return this.r2;
    }

    public Optional<Luke> luke() {
      return this.luke;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeObject($responseFields[0], r2.isPresent() ? r2.get().marshaller() : null);
          writer.writeObject($responseFields[1], luke.isPresent() ? luke.get().marshaller() : null);
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
        return this.r2.equals(that.r2)
         && this.luke.equals(that.luke);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= r2.hashCode();
        h *= 1000003;
        h ^= luke.hashCode();
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
      ResponseField.forInlineFragment("__typename", "__typename", Arrays.asList("Human")),
      ResponseField.forInlineFragment("__typename", "__typename", Arrays.asList("Droid")),
      ResponseField.forFragment("__typename", "__typename", Arrays.asList("Human",
      "Droid"))
    };

    final @Nonnull String __typename;

    final Optional<AsHuman> asHuman;

    final Optional<AsDroid> asDroid;

    private final @Nonnull Fragments fragments;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public R2(@Nonnull String __typename, @Nullable AsHuman asHuman, @Nullable AsDroid asDroid,
        @Nonnull Fragments fragments) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
      this.__typename = __typename;
      this.asHuman = Optional.fromNullable(asHuman);
      this.asDroid = Optional.fromNullable(asDroid);
      if (fragments == null) {
        throw new NullPointerException("fragments can't be null");
      }
      this.fragments = fragments;
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    public Optional<AsHuman> asHuman() {
      return this.asHuman;
    }

    public Optional<AsDroid> asDroid() {
      return this.asDroid;
    }

    public @Nonnull Fragments fragments() {
      return this.fragments;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          final AsHuman $asHuman = asHuman.isPresent() ? asHuman.get() : null;
          if ($asHuman != null) {
            $asHuman.marshaller().marshal(writer);
          }
          final AsDroid $asDroid = asDroid.isPresent() ? asDroid.get() : null;
          if ($asDroid != null) {
            $asDroid.marshaller().marshal(writer);
          }
          fragments.marshaller().marshal(writer);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "R2{"
          + "__typename=" + __typename + ", "
          + "asHuman=" + asHuman + ", "
          + "asDroid=" + asDroid + ", "
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
         && this.asHuman.equals(that.asHuman)
         && this.asDroid.equals(that.asDroid)
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
        h ^= asHuman.hashCode();
        h *= 1000003;
        h ^= asDroid.hashCode();
        h *= 1000003;
        h ^= fragments.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static class Fragments {
      final Optional<HumanDetails> humanDetails;

      final Optional<DroidDetails> droidDetails;

      private volatile String $toString;

      private volatile int $hashCode;

      private volatile boolean $hashCodeMemoized;

      public Fragments(@Nullable HumanDetails humanDetails, @Nullable DroidDetails droidDetails) {
        this.humanDetails = Optional.fromNullable(humanDetails);
        this.droidDetails = Optional.fromNullable(droidDetails);
      }

      public Optional<HumanDetails> humanDetails() {
        return this.humanDetails;
      }

      public Optional<DroidDetails> droidDetails() {
        return this.droidDetails;
      }

      public ResponseFieldMarshaller marshaller() {
        return new ResponseFieldMarshaller() {
          @Override
          public void marshal(ResponseWriter writer) {
            final HumanDetails $humanDetails = humanDetails.isPresent() ? humanDetails.get() : null;
            if ($humanDetails != null) {
              $humanDetails.marshaller().marshal(writer);
            }
            final DroidDetails $droidDetails = droidDetails.isPresent() ? droidDetails.get() : null;
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
          return this.humanDetails.equals(that.humanDetails)
           && this.droidDetails.equals(that.droidDetails);
        }
        return false;
      }

      @Override
      public int hashCode() {
        if (!$hashCodeMemoized) {
          int h = 1;
          h *= 1000003;
          h ^= humanDetails.hashCode();
          h *= 1000003;
          h ^= droidDetails.hashCode();
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
      final AsHuman.Mapper asHumanFieldMapper = new AsHuman.Mapper();

      final AsDroid.Mapper asDroidFieldMapper = new AsDroid.Mapper();

      final Fragments.Mapper fragmentsFieldMapper = new Fragments.Mapper();

      @Override
      public R2 map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final AsHuman asHuman = reader.readConditional((ResponseField.ConditionalTypeField) $responseFields[1], new ResponseReader.ConditionalTypeReader<AsHuman>() {
          @Override
          public AsHuman read(String conditionalType, ResponseReader reader) {
            return asHumanFieldMapper.map(reader);
          }
        });
        final AsDroid asDroid = reader.readConditional((ResponseField.ConditionalTypeField) $responseFields[2], new ResponseReader.ConditionalTypeReader<AsDroid>() {
          @Override
          public AsDroid read(String conditionalType, ResponseReader reader) {
            return asDroidFieldMapper.map(reader);
          }
        });
        final Fragments fragments = reader.readConditional((ResponseField.ConditionalTypeField) $responseFields[3], new ResponseReader.ConditionalTypeReader<Fragments>() {
          @Override
          public Fragments read(String conditionalType, ResponseReader reader) {
            return fragmentsFieldMapper.map(reader, conditionalType);
          }
        });
        return new R2(__typename, asHuman, asDroid, fragments);
      }
    }
  }

  public static class AsHuman {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forString("name", "name", null, false),
      ResponseField.forDouble("height", "height", null, true),
      ResponseField.forFragment("__typename", "__typename", Arrays.asList("Human",
      "Droid"))
    };

    final @Nonnull String __typename;

    final @Nonnull String name;

    final Optional<Double> height;

    private final @Nonnull Fragments fragments;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsHuman(@Nonnull String __typename, @Nonnull String name, @Nullable Double height,
        @Nonnull Fragments fragments) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
      this.__typename = __typename;
      if (name == null) {
        throw new NullPointerException("name can't be null");
      }
      this.name = name;
      this.height = Optional.fromNullable(height);
      if (fragments == null) {
        throw new NullPointerException("fragments can't be null");
      }
      this.fragments = fragments;
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * What this human calls themselves
     */
    public @Nonnull String name() {
      return this.name;
    }

    /**
     * Height in the preferred unit, default is meters
     */
    public Optional<Double> height() {
      return this.height;
    }

    public @Nonnull Fragments fragments() {
      return this.fragments;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], name);
          writer.writeDouble($responseFields[2], height.isPresent() ? height.get() : null);
          fragments.marshaller().marshal(writer);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "AsHuman{"
          + "__typename=" + __typename + ", "
          + "name=" + name + ", "
          + "height=" + height + ", "
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
      if (o instanceof AsHuman) {
        AsHuman that = (AsHuman) o;
        return this.__typename.equals(that.__typename)
         && this.name.equals(that.name)
         && this.height.equals(that.height)
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
        h ^= name.hashCode();
        h *= 1000003;
        h ^= height.hashCode();
        h *= 1000003;
        h ^= fragments.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static class Fragments {
      final Optional<HumanDetails> humanDetails;

      final Optional<DroidDetails> droidDetails;

      private volatile String $toString;

      private volatile int $hashCode;

      private volatile boolean $hashCodeMemoized;

      public Fragments(@Nullable HumanDetails humanDetails, @Nullable DroidDetails droidDetails) {
        this.humanDetails = Optional.fromNullable(humanDetails);
        this.droidDetails = Optional.fromNullable(droidDetails);
      }

      public Optional<HumanDetails> humanDetails() {
        return this.humanDetails;
      }

      public Optional<DroidDetails> droidDetails() {
        return this.droidDetails;
      }

      public ResponseFieldMarshaller marshaller() {
        return new ResponseFieldMarshaller() {
          @Override
          public void marshal(ResponseWriter writer) {
            final HumanDetails $humanDetails = humanDetails.isPresent() ? humanDetails.get() : null;
            if ($humanDetails != null) {
              $humanDetails.marshaller().marshal(writer);
            }
            final DroidDetails $droidDetails = droidDetails.isPresent() ? droidDetails.get() : null;
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
          return this.humanDetails.equals(that.humanDetails)
           && this.droidDetails.equals(that.droidDetails);
        }
        return false;
      }

      @Override
      public int hashCode() {
        if (!$hashCodeMemoized) {
          int h = 1;
          h *= 1000003;
          h ^= humanDetails.hashCode();
          h *= 1000003;
          h ^= droidDetails.hashCode();
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

    public static final class Mapper implements ResponseFieldMapper<AsHuman> {
      final Fragments.Mapper fragmentsFieldMapper = new Fragments.Mapper();

      @Override
      public AsHuman map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String name = reader.readString($responseFields[1]);
        final Double height = reader.readDouble($responseFields[2]);
        final Fragments fragments = reader.readConditional((ResponseField.ConditionalTypeField) $responseFields[3], new ResponseReader.ConditionalTypeReader<Fragments>() {
          @Override
          public Fragments read(String conditionalType, ResponseReader reader) {
            return fragmentsFieldMapper.map(reader, conditionalType);
          }
        });
        return new AsHuman(__typename, name, height, fragments);
      }
    }
  }

  public static class AsDroid {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forString("name", "name", null, false),
      ResponseField.forString("primaryFunction", "primaryFunction", null, true),
      ResponseField.forFragment("__typename", "__typename", Arrays.asList("Human",
      "Droid"))
    };

    final @Nonnull String __typename;

    final @Nonnull String name;

    final Optional<String> primaryFunction;

    private final @Nonnull Fragments fragments;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsDroid(@Nonnull String __typename, @Nonnull String name,
        @Nullable String primaryFunction, @Nonnull Fragments fragments) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
      this.__typename = __typename;
      if (name == null) {
        throw new NullPointerException("name can't be null");
      }
      this.name = name;
      this.primaryFunction = Optional.fromNullable(primaryFunction);
      if (fragments == null) {
        throw new NullPointerException("fragments can't be null");
      }
      this.fragments = fragments;
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * What others call this droid
     */
    public @Nonnull String name() {
      return this.name;
    }

    /**
     * This droid's primary function
     */
    public Optional<String> primaryFunction() {
      return this.primaryFunction;
    }

    public @Nonnull Fragments fragments() {
      return this.fragments;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], name);
          writer.writeString($responseFields[2], primaryFunction.isPresent() ? primaryFunction.get() : null);
          fragments.marshaller().marshal(writer);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "AsDroid{"
          + "__typename=" + __typename + ", "
          + "name=" + name + ", "
          + "primaryFunction=" + primaryFunction + ", "
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
      if (o instanceof AsDroid) {
        AsDroid that = (AsDroid) o;
        return this.__typename.equals(that.__typename)
         && this.name.equals(that.name)
         && this.primaryFunction.equals(that.primaryFunction)
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
        h ^= name.hashCode();
        h *= 1000003;
        h ^= primaryFunction.hashCode();
        h *= 1000003;
        h ^= fragments.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static class Fragments {
      final Optional<HumanDetails> humanDetails;

      final Optional<DroidDetails> droidDetails;

      private volatile String $toString;

      private volatile int $hashCode;

      private volatile boolean $hashCodeMemoized;

      public Fragments(@Nullable HumanDetails humanDetails, @Nullable DroidDetails droidDetails) {
        this.humanDetails = Optional.fromNullable(humanDetails);
        this.droidDetails = Optional.fromNullable(droidDetails);
      }

      public Optional<HumanDetails> humanDetails() {
        return this.humanDetails;
      }

      public Optional<DroidDetails> droidDetails() {
        return this.droidDetails;
      }

      public ResponseFieldMarshaller marshaller() {
        return new ResponseFieldMarshaller() {
          @Override
          public void marshal(ResponseWriter writer) {
            final HumanDetails $humanDetails = humanDetails.isPresent() ? humanDetails.get() : null;
            if ($humanDetails != null) {
              $humanDetails.marshaller().marshal(writer);
            }
            final DroidDetails $droidDetails = droidDetails.isPresent() ? droidDetails.get() : null;
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
          return this.humanDetails.equals(that.humanDetails)
           && this.droidDetails.equals(that.droidDetails);
        }
        return false;
      }

      @Override
      public int hashCode() {
        if (!$hashCodeMemoized) {
          int h = 1;
          h *= 1000003;
          h ^= humanDetails.hashCode();
          h *= 1000003;
          h ^= droidDetails.hashCode();
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

    public static final class Mapper implements ResponseFieldMapper<AsDroid> {
      final Fragments.Mapper fragmentsFieldMapper = new Fragments.Mapper();

      @Override
      public AsDroid map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String name = reader.readString($responseFields[1]);
        final String primaryFunction = reader.readString($responseFields[2]);
        final Fragments fragments = reader.readConditional((ResponseField.ConditionalTypeField) $responseFields[3], new ResponseReader.ConditionalTypeReader<Fragments>() {
          @Override
          public Fragments read(String conditionalType, ResponseReader reader) {
            return fragmentsFieldMapper.map(reader, conditionalType);
          }
        });
        return new AsDroid(__typename, name, primaryFunction, fragments);
      }
    }
  }

  public static class Luke {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forInlineFragment("__typename", "__typename", Arrays.asList("Human")),
      ResponseField.forInlineFragment("__typename", "__typename", Arrays.asList("Droid")),
      ResponseField.forFragment("__typename", "__typename", Arrays.asList("Human",
      "Droid"))
    };

    final @Nonnull String __typename;

    final Optional<AsHuman1> asHuman;

    final Optional<AsDroid1> asDroid;

    private final @Nonnull Fragments fragments;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Luke(@Nonnull String __typename, @Nullable AsHuman1 asHuman, @Nullable AsDroid1 asDroid,
        @Nonnull Fragments fragments) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
      this.__typename = __typename;
      this.asHuman = Optional.fromNullable(asHuman);
      this.asDroid = Optional.fromNullable(asDroid);
      if (fragments == null) {
        throw new NullPointerException("fragments can't be null");
      }
      this.fragments = fragments;
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    public Optional<AsHuman1> asHuman() {
      return this.asHuman;
    }

    public Optional<AsDroid1> asDroid() {
      return this.asDroid;
    }

    public @Nonnull Fragments fragments() {
      return this.fragments;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          final AsHuman1 $asHuman = asHuman.isPresent() ? asHuman.get() : null;
          if ($asHuman != null) {
            $asHuman.marshaller().marshal(writer);
          }
          final AsDroid1 $asDroid = asDroid.isPresent() ? asDroid.get() : null;
          if ($asDroid != null) {
            $asDroid.marshaller().marshal(writer);
          }
          fragments.marshaller().marshal(writer);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Luke{"
          + "__typename=" + __typename + ", "
          + "asHuman=" + asHuman + ", "
          + "asDroid=" + asDroid + ", "
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
         && this.asHuman.equals(that.asHuman)
         && this.asDroid.equals(that.asDroid)
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
        h ^= asHuman.hashCode();
        h *= 1000003;
        h ^= asDroid.hashCode();
        h *= 1000003;
        h ^= fragments.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static class Fragments {
      final Optional<HumanDetails> humanDetails;

      final Optional<DroidDetails> droidDetails;

      private volatile String $toString;

      private volatile int $hashCode;

      private volatile boolean $hashCodeMemoized;

      public Fragments(@Nullable HumanDetails humanDetails, @Nullable DroidDetails droidDetails) {
        this.humanDetails = Optional.fromNullable(humanDetails);
        this.droidDetails = Optional.fromNullable(droidDetails);
      }

      public Optional<HumanDetails> humanDetails() {
        return this.humanDetails;
      }

      public Optional<DroidDetails> droidDetails() {
        return this.droidDetails;
      }

      public ResponseFieldMarshaller marshaller() {
        return new ResponseFieldMarshaller() {
          @Override
          public void marshal(ResponseWriter writer) {
            final HumanDetails $humanDetails = humanDetails.isPresent() ? humanDetails.get() : null;
            if ($humanDetails != null) {
              $humanDetails.marshaller().marshal(writer);
            }
            final DroidDetails $droidDetails = droidDetails.isPresent() ? droidDetails.get() : null;
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
          return this.humanDetails.equals(that.humanDetails)
           && this.droidDetails.equals(that.droidDetails);
        }
        return false;
      }

      @Override
      public int hashCode() {
        if (!$hashCodeMemoized) {
          int h = 1;
          h *= 1000003;
          h ^= humanDetails.hashCode();
          h *= 1000003;
          h ^= droidDetails.hashCode();
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
      final AsHuman1.Mapper asHuman1FieldMapper = new AsHuman1.Mapper();

      final AsDroid1.Mapper asDroid1FieldMapper = new AsDroid1.Mapper();

      final Fragments.Mapper fragmentsFieldMapper = new Fragments.Mapper();

      @Override
      public Luke map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final AsHuman1 asHuman = reader.readConditional((ResponseField.ConditionalTypeField) $responseFields[1], new ResponseReader.ConditionalTypeReader<AsHuman1>() {
          @Override
          public AsHuman1 read(String conditionalType, ResponseReader reader) {
            return asHuman1FieldMapper.map(reader);
          }
        });
        final AsDroid1 asDroid = reader.readConditional((ResponseField.ConditionalTypeField) $responseFields[2], new ResponseReader.ConditionalTypeReader<AsDroid1>() {
          @Override
          public AsDroid1 read(String conditionalType, ResponseReader reader) {
            return asDroid1FieldMapper.map(reader);
          }
        });
        final Fragments fragments = reader.readConditional((ResponseField.ConditionalTypeField) $responseFields[3], new ResponseReader.ConditionalTypeReader<Fragments>() {
          @Override
          public Fragments read(String conditionalType, ResponseReader reader) {
            return fragmentsFieldMapper.map(reader, conditionalType);
          }
        });
        return new Luke(__typename, asHuman, asDroid, fragments);
      }
    }
  }

  public static class AsHuman1 {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forString("name", "name", null, false),
      ResponseField.forDouble("height", "height", null, true),
      ResponseField.forFragment("__typename", "__typename", Arrays.asList("Human",
      "Droid"))
    };

    final @Nonnull String __typename;

    final @Nonnull String name;

    final Optional<Double> height;

    private final @Nonnull Fragments fragments;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsHuman1(@Nonnull String __typename, @Nonnull String name, @Nullable Double height,
        @Nonnull Fragments fragments) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
      this.__typename = __typename;
      if (name == null) {
        throw new NullPointerException("name can't be null");
      }
      this.name = name;
      this.height = Optional.fromNullable(height);
      if (fragments == null) {
        throw new NullPointerException("fragments can't be null");
      }
      this.fragments = fragments;
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * What this human calls themselves
     */
    public @Nonnull String name() {
      return this.name;
    }

    /**
     * Height in the preferred unit, default is meters
     */
    public Optional<Double> height() {
      return this.height;
    }

    public @Nonnull Fragments fragments() {
      return this.fragments;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], name);
          writer.writeDouble($responseFields[2], height.isPresent() ? height.get() : null);
          fragments.marshaller().marshal(writer);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "AsHuman1{"
          + "__typename=" + __typename + ", "
          + "name=" + name + ", "
          + "height=" + height + ", "
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
      if (o instanceof AsHuman1) {
        AsHuman1 that = (AsHuman1) o;
        return this.__typename.equals(that.__typename)
         && this.name.equals(that.name)
         && this.height.equals(that.height)
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
        h ^= name.hashCode();
        h *= 1000003;
        h ^= height.hashCode();
        h *= 1000003;
        h ^= fragments.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static class Fragments {
      final Optional<HumanDetails> humanDetails;

      final Optional<DroidDetails> droidDetails;

      private volatile String $toString;

      private volatile int $hashCode;

      private volatile boolean $hashCodeMemoized;

      public Fragments(@Nullable HumanDetails humanDetails, @Nullable DroidDetails droidDetails) {
        this.humanDetails = Optional.fromNullable(humanDetails);
        this.droidDetails = Optional.fromNullable(droidDetails);
      }

      public Optional<HumanDetails> humanDetails() {
        return this.humanDetails;
      }

      public Optional<DroidDetails> droidDetails() {
        return this.droidDetails;
      }

      public ResponseFieldMarshaller marshaller() {
        return new ResponseFieldMarshaller() {
          @Override
          public void marshal(ResponseWriter writer) {
            final HumanDetails $humanDetails = humanDetails.isPresent() ? humanDetails.get() : null;
            if ($humanDetails != null) {
              $humanDetails.marshaller().marshal(writer);
            }
            final DroidDetails $droidDetails = droidDetails.isPresent() ? droidDetails.get() : null;
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
          return this.humanDetails.equals(that.humanDetails)
           && this.droidDetails.equals(that.droidDetails);
        }
        return false;
      }

      @Override
      public int hashCode() {
        if (!$hashCodeMemoized) {
          int h = 1;
          h *= 1000003;
          h ^= humanDetails.hashCode();
          h *= 1000003;
          h ^= droidDetails.hashCode();
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

    public static final class Mapper implements ResponseFieldMapper<AsHuman1> {
      final Fragments.Mapper fragmentsFieldMapper = new Fragments.Mapper();

      @Override
      public AsHuman1 map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String name = reader.readString($responseFields[1]);
        final Double height = reader.readDouble($responseFields[2]);
        final Fragments fragments = reader.readConditional((ResponseField.ConditionalTypeField) $responseFields[3], new ResponseReader.ConditionalTypeReader<Fragments>() {
          @Override
          public Fragments read(String conditionalType, ResponseReader reader) {
            return fragmentsFieldMapper.map(reader, conditionalType);
          }
        });
        return new AsHuman1(__typename, name, height, fragments);
      }
    }
  }

  public static class AsDroid1 {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forString("name", "name", null, false),
      ResponseField.forString("primaryFunction", "primaryFunction", null, true),
      ResponseField.forFragment("__typename", "__typename", Arrays.asList("Human",
      "Droid"))
    };

    final @Nonnull String __typename;

    final @Nonnull String name;

    final Optional<String> primaryFunction;

    private final @Nonnull Fragments fragments;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AsDroid1(@Nonnull String __typename, @Nonnull String name,
        @Nullable String primaryFunction, @Nonnull Fragments fragments) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
      this.__typename = __typename;
      if (name == null) {
        throw new NullPointerException("name can't be null");
      }
      this.name = name;
      this.primaryFunction = Optional.fromNullable(primaryFunction);
      if (fragments == null) {
        throw new NullPointerException("fragments can't be null");
      }
      this.fragments = fragments;
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * What others call this droid
     */
    public @Nonnull String name() {
      return this.name;
    }

    /**
     * This droid's primary function
     */
    public Optional<String> primaryFunction() {
      return this.primaryFunction;
    }

    public @Nonnull Fragments fragments() {
      return this.fragments;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], name);
          writer.writeString($responseFields[2], primaryFunction.isPresent() ? primaryFunction.get() : null);
          fragments.marshaller().marshal(writer);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "AsDroid1{"
          + "__typename=" + __typename + ", "
          + "name=" + name + ", "
          + "primaryFunction=" + primaryFunction + ", "
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
      if (o instanceof AsDroid1) {
        AsDroid1 that = (AsDroid1) o;
        return this.__typename.equals(that.__typename)
         && this.name.equals(that.name)
         && this.primaryFunction.equals(that.primaryFunction)
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
        h ^= name.hashCode();
        h *= 1000003;
        h ^= primaryFunction.hashCode();
        h *= 1000003;
        h ^= fragments.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static class Fragments {
      final Optional<HumanDetails> humanDetails;

      final Optional<DroidDetails> droidDetails;

      private volatile String $toString;

      private volatile int $hashCode;

      private volatile boolean $hashCodeMemoized;

      public Fragments(@Nullable HumanDetails humanDetails, @Nullable DroidDetails droidDetails) {
        this.humanDetails = Optional.fromNullable(humanDetails);
        this.droidDetails = Optional.fromNullable(droidDetails);
      }

      public Optional<HumanDetails> humanDetails() {
        return this.humanDetails;
      }

      public Optional<DroidDetails> droidDetails() {
        return this.droidDetails;
      }

      public ResponseFieldMarshaller marshaller() {
        return new ResponseFieldMarshaller() {
          @Override
          public void marshal(ResponseWriter writer) {
            final HumanDetails $humanDetails = humanDetails.isPresent() ? humanDetails.get() : null;
            if ($humanDetails != null) {
              $humanDetails.marshaller().marshal(writer);
            }
            final DroidDetails $droidDetails = droidDetails.isPresent() ? droidDetails.get() : null;
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
          return this.humanDetails.equals(that.humanDetails)
           && this.droidDetails.equals(that.droidDetails);
        }
        return false;
      }

      @Override
      public int hashCode() {
        if (!$hashCodeMemoized) {
          int h = 1;
          h *= 1000003;
          h ^= humanDetails.hashCode();
          h *= 1000003;
          h ^= droidDetails.hashCode();
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

    public static final class Mapper implements ResponseFieldMapper<AsDroid1> {
      final Fragments.Mapper fragmentsFieldMapper = new Fragments.Mapper();

      @Override
      public AsDroid1 map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String name = reader.readString($responseFields[1]);
        final String primaryFunction = reader.readString($responseFields[2]);
        final Fragments fragments = reader.readConditional((ResponseField.ConditionalTypeField) $responseFields[3], new ResponseReader.ConditionalTypeReader<Fragments>() {
          @Override
          public Fragments read(String conditionalType, ResponseReader reader) {
            return fragmentsFieldMapper.map(reader, conditionalType);
          }
        });
        return new AsDroid1(__typename, name, primaryFunction, fragments);
      }
    }
  }
}
