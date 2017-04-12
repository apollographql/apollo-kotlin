package com.example.fragments_with_type_condition;

import com.apollographql.apollo.api.Field;
import com.apollographql.apollo.api.FragmentResponseFieldMapper;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.internal.Optional;
import com.example.fragments_with_type_condition.fragment.DroidDetails;
import com.example.fragments_with_type_condition.fragment.HumanDetails;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
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

  public static class Data implements Operation.Data {
    private final Optional<R2> r2;

    private final Optional<Luke> luke;

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

    @Override
    public String toString() {
      return "Data{"
        + "r2=" + r2 + ", "
        + "luke=" + luke
        + "}";
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
      int h = 1;
      h *= 1000003;
      h ^= (r2 == null) ? 0 : r2.hashCode();
      h *= 1000003;
      h ^= (luke == null) ? 0 : luke.hashCode();
      return h;
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final R2.Mapper r2FieldMapper = new R2.Mapper();

      final Luke.Mapper lukeFieldMapper = new Luke.Mapper();

      final Field[] fields = {
        Field.forObject("r2", "hero", null, true, new Field.ObjectReader<R2>() {
          @Override public R2 read(final ResponseReader reader) throws IOException {
            return r2FieldMapper.map(reader);
          }
        }),
        Field.forObject("luke", "hero", null, true, new Field.ObjectReader<Luke>() {
          @Override public Luke read(final ResponseReader reader) throws IOException {
            return lukeFieldMapper.map(reader);
          }
        })
      };

      @Override
      public Data map(ResponseReader reader) throws IOException {
        final R2 r2 = reader.read(fields[0]);
        final Luke luke = reader.read(fields[1]);
        return new Data(r2, luke);
      }
    }

    public static class R2 {
      private final @Nonnull Fragments fragments;

      public R2(@Nonnull Fragments fragments) {
        this.fragments = fragments;
      }

      public @Nonnull Fragments fragments() {
        return this.fragments;
      }

      @Override
      public String toString() {
        return "R2{"
          + "fragments=" + fragments
          + "}";
      }

      @Override
      public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof R2) {
          R2 that = (R2) o;
          return ((this.fragments == null) ? (that.fragments == null) : this.fragments.equals(that.fragments));
        }
        return false;
      }

      @Override
      public int hashCode() {
        int h = 1;
        h *= 1000003;
        h ^= (fragments == null) ? 0 : fragments.hashCode();
        return h;
      }

      public static class Fragments {
        private final @Nonnull HumanDetails humanDetails;

        private final @Nonnull DroidDetails droidDetails;

        public Fragments(@Nonnull HumanDetails humanDetails, @Nonnull DroidDetails droidDetails) {
          this.humanDetails = humanDetails;
          this.droidDetails = droidDetails;
        }

        public @Nonnull HumanDetails humanDetails() {
          return this.humanDetails;
        }

        public @Nonnull DroidDetails droidDetails() {
          return this.droidDetails;
        }

        @Override
        public String toString() {
          return "Fragments{"
            + "humanDetails=" + humanDetails + ", "
            + "droidDetails=" + droidDetails
            + "}";
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
          int h = 1;
          h *= 1000003;
          h ^= (humanDetails == null) ? 0 : humanDetails.hashCode();
          h *= 1000003;
          h ^= (droidDetails == null) ? 0 : droidDetails.hashCode();
          return h;
        }

        public static final class Mapper implements FragmentResponseFieldMapper<Fragments> {
          final HumanDetails.Mapper humanDetailsFieldMapper = new HumanDetails.Mapper();

          final DroidDetails.Mapper droidDetailsFieldMapper = new DroidDetails.Mapper();

          @Override
          public @Nonnull Fragments map(ResponseReader reader, @Nonnull String conditionalType)
              throws IOException {
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

        final Field[] fields = {
          Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<Fragments>() {
            @Override
            public Fragments read(String conditionalType, ResponseReader reader) throws
                IOException {
              return fragmentsFieldMapper.map(reader, conditionalType);
            }
          })
        };

        @Override
        public R2 map(ResponseReader reader) throws IOException {
          final Fragments fragments = reader.read(fields[0]);
          return new R2(fragments);
        }
      }
    }

    public static class Luke {
      private final @Nonnull Fragments fragments;

      public Luke(@Nonnull Fragments fragments) {
        this.fragments = fragments;
      }

      public @Nonnull Fragments fragments() {
        return this.fragments;
      }

      @Override
      public String toString() {
        return "Luke{"
          + "fragments=" + fragments
          + "}";
      }

      @Override
      public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof Luke) {
          Luke that = (Luke) o;
          return ((this.fragments == null) ? (that.fragments == null) : this.fragments.equals(that.fragments));
        }
        return false;
      }

      @Override
      public int hashCode() {
        int h = 1;
        h *= 1000003;
        h ^= (fragments == null) ? 0 : fragments.hashCode();
        return h;
      }

      public static class Fragments {
        private final @Nonnull HumanDetails humanDetails;

        private final @Nonnull DroidDetails droidDetails;

        public Fragments(@Nonnull HumanDetails humanDetails, @Nonnull DroidDetails droidDetails) {
          this.humanDetails = humanDetails;
          this.droidDetails = droidDetails;
        }

        public @Nonnull HumanDetails humanDetails() {
          return this.humanDetails;
        }

        public @Nonnull DroidDetails droidDetails() {
          return this.droidDetails;
        }

        @Override
        public String toString() {
          return "Fragments{"
            + "humanDetails=" + humanDetails + ", "
            + "droidDetails=" + droidDetails
            + "}";
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
          int h = 1;
          h *= 1000003;
          h ^= (humanDetails == null) ? 0 : humanDetails.hashCode();
          h *= 1000003;
          h ^= (droidDetails == null) ? 0 : droidDetails.hashCode();
          return h;
        }

        public static final class Mapper implements FragmentResponseFieldMapper<Fragments> {
          final HumanDetails.Mapper humanDetailsFieldMapper = new HumanDetails.Mapper();

          final DroidDetails.Mapper droidDetailsFieldMapper = new DroidDetails.Mapper();

          @Override
          public @Nonnull Fragments map(ResponseReader reader, @Nonnull String conditionalType)
              throws IOException {
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

        final Field[] fields = {
          Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<Fragments>() {
            @Override
            public Fragments read(String conditionalType, ResponseReader reader) throws
                IOException {
              return fragmentsFieldMapper.map(reader, conditionalType);
            }
          })
        };

        @Override
        public Luke map(ResponseReader reader) throws IOException {
          final Fragments fragments = reader.read(fields[0]);
          return new Luke(fragments);
        }
      }
    }
  }
}
