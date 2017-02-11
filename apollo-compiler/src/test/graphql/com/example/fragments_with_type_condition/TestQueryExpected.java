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

  public interface Data extends Operation.Data {
    @Nullable R2 r2();

    @Nullable Luke luke();

    interface R2 {
      @Nonnull Fragments fragments();

      interface Fragments {
        @Nullable HumanDetails humanDetails();

        @Nullable DroidDetails droidDetails();

        final class Mapper implements ResponseFieldMapper<Fragments> {
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

        interface Factory {
          @Nonnull Creator creator();

          @Nonnull HumanDetails.Factory humanDetailsFactory();

          @Nonnull DroidDetails.Factory droidDetailsFactory();
        }

        interface Creator {
          @Nonnull Fragments create(@Nullable HumanDetails humanDetails,
              @Nullable DroidDetails droidDetails);
        }
      }

      final class Mapper implements ResponseFieldMapper<R2> {
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

      interface Factory {
        @Nonnull Creator creator();

        @Nonnull Fragments.Factory fragmentsFactory();
      }

      interface Creator {
        @Nonnull R2 create(@Nonnull Fragments fragments);
      }
    }

    interface Luke {
      @Nonnull Fragments fragments();

      interface Fragments {
        @Nullable HumanDetails humanDetails();

        @Nullable DroidDetails droidDetails();

        final class Mapper implements ResponseFieldMapper<Fragments> {
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

        interface Factory {
          @Nonnull Creator creator();

          @Nonnull HumanDetails.Factory humanDetailsFactory();

          @Nonnull DroidDetails.Factory droidDetailsFactory();
        }

        interface Creator {
          @Nonnull Fragments create(@Nullable HumanDetails humanDetails,
              @Nullable DroidDetails droidDetails);
        }
      }

      final class Mapper implements ResponseFieldMapper<Luke> {
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

      interface Factory {
        @Nonnull Creator creator();

        @Nonnull Fragments.Factory fragmentsFactory();
      }

      interface Creator {
        @Nonnull Luke create(@Nonnull Fragments fragments);
      }
    }

    final class Mapper implements ResponseFieldMapper<Data> {
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

    interface Factory {
      @Nonnull Creator creator();

      @Nonnull R2.Factory r2Factory();

      @Nonnull Luke.Factory lukeFactory();
    }

    interface Creator {
      @Nonnull Data create(@Nullable R2 r2, @Nullable Luke luke);
    }
  }
}
