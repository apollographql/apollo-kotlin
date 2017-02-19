package com.example.fragments_with_type_condition;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
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
public final class TestQuery implements Query<Operation.Variables> {
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
  public Operation.Variables variables() {
    return variables;
  }

  public static class Data implements Operation.Data {
    public static final Creator CREATOR = new Creator() {
      @Override
      public @Nonnull Data create(@Nullable R2 r2, @Nullable Luke luke) {
        return new Data(r2, luke);
      }
    };

    public static final Factory FACTORY = new Factory() {
      @Override
      public @Nonnull Creator creator() {
        return CREATOR;
      }

      @Override
      public @Nonnull R2.Factory r2Factory() {
        return R2.FACTORY;
      }

      @Override
      public @Nonnull Luke.Factory lukeFactory() {
        return Luke.FACTORY;
      }
    };

    private final @Nullable R2 r2;

    private final @Nullable Luke luke;

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

    public static class R2 {
      public static final Creator CREATOR = new Creator() {
        @Override
        public @Nonnull R2 create(@Nonnull Fragments fragments) {
          return new R2(fragments);
        }
      };

      public static final Factory FACTORY = new Factory() {
        @Override
        public @Nonnull Creator creator() {
          return CREATOR;
        }

        @Override
        public @Nonnull Fragments.Factory fragmentsFactory() {
          return Fragments.FACTORY;
        }
      };

      private final Fragments fragments;

      public R2(Fragments fragments) {
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
        public static final Creator CREATOR = new Creator() {
          @Override
          public @Nonnull Fragments create(@Nullable HumanDetails humanDetails,
              @Nullable DroidDetails droidDetails) {
            return new Fragments(humanDetails, droidDetails);
          }
        };

        public static final Factory FACTORY = new Factory() {
          @Override
          public @Nonnull Creator creator() {
            return CREATOR;
          }

          @Override
          public @Nonnull HumanDetails.Factory humanDetailsFactory() {
            return HumanDetails.FACTORY;
          }

          @Override
          public @Nonnull DroidDetails.Factory droidDetailsFactory() {
            return DroidDetails.FACTORY;
          }
        };

        private HumanDetails humanDetails;

        private DroidDetails droidDetails;

        public Fragments(HumanDetails humanDetails, DroidDetails droidDetails) {
          this.humanDetails = humanDetails;
          this.droidDetails = droidDetails;
        }

        public @Nullable HumanDetails humanDetails() {
          return this.humanDetails;
        }

        public @Nullable DroidDetails droidDetails() {
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

        public static final class Mapper implements ResponseFieldMapper<Fragments> {
          final Factory factory;

          String conditionalType;

          public Mapper(@Nonnull Factory factory, @Nonnull String conditionalType) {
            this.factory = factory;
            this.conditionalType = conditionalType;
          }

          @Override
          public @Nonnull Fragments map(ResponseReader reader) throws IOException {
            HumanDetails humanDetails = null;
            DroidDetails droidDetails = null;
            if (conditionalType.equals(HumanDetails.TYPE_CONDITION)) {
              humanDetails = new HumanDetails.Mapper(factory.humanDetailsFactory()).map(reader);
            }
            if (conditionalType.equals(DroidDetails.TYPE_CONDITION)) {
              droidDetails = new DroidDetails.Mapper(factory.droidDetailsFactory()).map(reader);
            }
            return factory.creator().create(humanDetails, droidDetails);
          }
        }

        public interface Factory {
          @Nonnull Creator creator();

          @Nonnull HumanDetails.Factory humanDetailsFactory();

          @Nonnull DroidDetails.Factory droidDetailsFactory();
        }

        public interface Creator {
          @Nonnull Fragments create(@Nullable HumanDetails humanDetails,
              @Nullable DroidDetails droidDetails);
        }
      }

      public static final class Mapper implements ResponseFieldMapper<R2> {
        final Factory factory;

        final Field[] fields = {
          Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<Fragments>() {
            @Override
            public Fragments read(String conditionalType, ResponseReader reader) throws
                IOException {
              return new Fragments.Mapper(factory.fragmentsFactory(), conditionalType).map(reader);
            }
          })
        };

        public Mapper(@Nonnull Factory factory) {
          this.factory = factory;
        }

        @Override
        public R2 map(ResponseReader reader) throws IOException {
          final __ContentValues contentValues = new __ContentValues();
          reader.toBufferedReader().read(new ResponseReader.ValueHandler() {
            @Override
            public void handle(final int fieldIndex, final Object value) throws IOException {
              switch (fieldIndex) {
                case 0: {
                  contentValues.fragments = (Fragments) value;
                  break;
                }
              }
            }
          }, fields);
          return factory.creator().create(contentValues.fragments);
        }

        static final class __ContentValues {
          Fragments fragments;
        }
      }

      public interface Factory {
        @Nonnull Creator creator();

        @Nonnull Fragments.Factory fragmentsFactory();
      }

      public interface Creator {
        @Nonnull R2 create(@Nonnull Fragments fragments);
      }
    }

    public static class Luke {
      public static final Creator CREATOR = new Creator() {
        @Override
        public @Nonnull Luke create(@Nonnull Fragments fragments) {
          return new Luke(fragments);
        }
      };

      public static final Factory FACTORY = new Factory() {
        @Override
        public @Nonnull Creator creator() {
          return CREATOR;
        }

        @Override
        public @Nonnull Fragments.Factory fragmentsFactory() {
          return Fragments.FACTORY;
        }
      };

      private final Fragments fragments;

      public Luke(Fragments fragments) {
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
        public static final Creator CREATOR = new Creator() {
          @Override
          public @Nonnull Fragments create(@Nullable HumanDetails humanDetails,
              @Nullable DroidDetails droidDetails) {
            return new Fragments(humanDetails, droidDetails);
          }
        };

        public static final Factory FACTORY = new Factory() {
          @Override
          public @Nonnull Creator creator() {
            return CREATOR;
          }

          @Override
          public @Nonnull HumanDetails.Factory humanDetailsFactory() {
            return HumanDetails.FACTORY;
          }

          @Override
          public @Nonnull DroidDetails.Factory droidDetailsFactory() {
            return DroidDetails.FACTORY;
          }
        };

        private HumanDetails humanDetails;

        private DroidDetails droidDetails;

        public Fragments(HumanDetails humanDetails, DroidDetails droidDetails) {
          this.humanDetails = humanDetails;
          this.droidDetails = droidDetails;
        }

        public @Nullable HumanDetails humanDetails() {
          return this.humanDetails;
        }

        public @Nullable DroidDetails droidDetails() {
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

        public static final class Mapper implements ResponseFieldMapper<Fragments> {
          final Factory factory;

          String conditionalType;

          public Mapper(@Nonnull Factory factory, @Nonnull String conditionalType) {
            this.factory = factory;
            this.conditionalType = conditionalType;
          }

          @Override
          public @Nonnull Fragments map(ResponseReader reader) throws IOException {
            HumanDetails humanDetails = null;
            DroidDetails droidDetails = null;
            if (conditionalType.equals(HumanDetails.TYPE_CONDITION)) {
              humanDetails = new HumanDetails.Mapper(factory.humanDetailsFactory()).map(reader);
            }
            if (conditionalType.equals(DroidDetails.TYPE_CONDITION)) {
              droidDetails = new DroidDetails.Mapper(factory.droidDetailsFactory()).map(reader);
            }
            return factory.creator().create(humanDetails, droidDetails);
          }
        }

        public interface Factory {
          @Nonnull Creator creator();

          @Nonnull HumanDetails.Factory humanDetailsFactory();

          @Nonnull DroidDetails.Factory droidDetailsFactory();
        }

        public interface Creator {
          @Nonnull Fragments create(@Nullable HumanDetails humanDetails,
              @Nullable DroidDetails droidDetails);
        }
      }

      public static final class Mapper implements ResponseFieldMapper<Luke> {
        final Factory factory;

        final Field[] fields = {
          Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<Fragments>() {
            @Override
            public Fragments read(String conditionalType, ResponseReader reader) throws
                IOException {
              return new Fragments.Mapper(factory.fragmentsFactory(), conditionalType).map(reader);
            }
          })
        };

        public Mapper(@Nonnull Factory factory) {
          this.factory = factory;
        }

        @Override
        public Luke map(ResponseReader reader) throws IOException {
          final __ContentValues contentValues = new __ContentValues();
          reader.toBufferedReader().read(new ResponseReader.ValueHandler() {
            @Override
            public void handle(final int fieldIndex, final Object value) throws IOException {
              switch (fieldIndex) {
                case 0: {
                  contentValues.fragments = (Fragments) value;
                  break;
                }
              }
            }
          }, fields);
          return factory.creator().create(contentValues.fragments);
        }

        static final class __ContentValues {
          Fragments fragments;
        }
      }

      public interface Factory {
        @Nonnull Creator creator();

        @Nonnull Fragments.Factory fragmentsFactory();
      }

      public interface Creator {
        @Nonnull Luke create(@Nonnull Fragments fragments);
      }
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final Factory factory;

      final Field[] fields = {
        Field.forObject("r2", "hero", null, true, new Field.ObjectReader<R2>() {
          @Override public R2 read(final ResponseReader reader) throws IOException {
            return new R2.Mapper(factory.r2Factory()).map(reader);
          }
        }),
        Field.forObject("luke", "hero", null, true, new Field.ObjectReader<Luke>() {
          @Override public Luke read(final ResponseReader reader) throws IOException {
            return new Luke.Mapper(factory.lukeFactory()).map(reader);
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
                contentValues.r2 = (R2) value;
                break;
              }
              case 1: {
                contentValues.luke = (Luke) value;
                break;
              }
            }
          }
        }, fields);
        return factory.creator().create(contentValues.r2, contentValues.luke);
      }

      static final class __ContentValues {
        R2 r2;

        Luke luke;
      }
    }

    public interface Factory {
      @Nonnull Creator creator();

      @Nonnull R2.Factory r2Factory();

      @Nonnull Luke.Factory lukeFactory();
    }

    public interface Creator {
      @Nonnull Data create(@Nullable R2 r2, @Nullable Luke luke);
    }
  }
}
