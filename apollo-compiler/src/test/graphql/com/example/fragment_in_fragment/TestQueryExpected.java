package com.example.fragment_in_fragment;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import com.example.fragment_in_fragment.fragment.PilotFragment;
import com.example.fragment_in_fragment.fragment.StarshipFragment;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query AllStarships {\n"
      + "  allStarships(first: 7) {\n"
      + "    edges {\n"
      + "      node {\n"
      + "        ...starshipFragment\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION + "\n"
   + StarshipFragment.FRAGMENT_DEFINITION + "\n"
   + PilotFragment.FRAGMENT_DEFINITION;

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
    @Nullable AllStarship allStarships();

    interface AllStarship {
      @Nullable List<? extends Edge> edges();

      interface Edge {
        @Nullable Node node();

        interface Node {
          @Nonnull Fragments fragments();

          interface Fragments {
            @Nullable StarshipFragment starshipFragment();

            final class Mapper implements ResponseFieldMapper<Fragments> {
              final Factory factory;

              String conditionalType;

              public Mapper(@Nonnull Factory factory, @Nonnull String conditionalType) {
                this.factory = factory;
                this.conditionalType = conditionalType;
              }

              @Override
              public @Nonnull Fragments map(ResponseReader reader) throws IOException {
                StarshipFragment starshipFragment = null;
                if (conditionalType.equals(StarshipFragment.TYPE_CONDITION)) {
                  starshipFragment = new StarshipFragment.Mapper(factory.starshipFragmentFactory()).map(reader);
                }
                return factory.creator().create(starshipFragment);
              }
            }

            interface Factory {
              @Nonnull Creator creator();

              @Nonnull StarshipFragment.Factory starshipFragmentFactory();
            }

            interface Creator {
              @Nonnull Fragments create(@Nullable StarshipFragment starshipFragment);
            }
          }

          final class Mapper implements ResponseFieldMapper<Node> {
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
            public Node map(ResponseReader reader) throws IOException {
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
            @Nonnull Node create(@Nonnull Fragments fragments);
          }
        }

        final class Mapper implements ResponseFieldMapper<Edge> {
          final Factory factory;

          final Field[] fields = {
            Field.forObject("node", "node", null, true, new Field.ObjectReader<Node>() {
              @Override public Node read(final ResponseReader reader) throws IOException {
                return new Node.Mapper(factory.nodeFactory()).map(reader);
              }
            })
          };

          public Mapper(@Nonnull Factory factory) {
            this.factory = factory;
          }

          @Override
          public Edge map(ResponseReader reader) throws IOException {
            final __ContentValues contentValues = new __ContentValues();
            reader.read(new ResponseReader.ValueHandler() {
              @Override
              public void handle(final int fieldIndex, final Object value) throws IOException {
                switch (fieldIndex) {
                  case 0: {
                    contentValues.node = (Node) value;
                    break;
                  }
                }
              }
            }, fields);
            return factory.creator().create(contentValues.node);
          }

          static final class __ContentValues {
            Node node;
          }
        }

        interface Factory {
          @Nonnull Creator creator();

          @Nonnull Node.Factory nodeFactory();
        }

        interface Creator {
          @Nonnull Edge create(@Nullable Node node);
        }
      }

      final class Mapper implements ResponseFieldMapper<AllStarship> {
        final Factory factory;

        final Field[] fields = {
          Field.forList("edges", "edges", null, true, new Field.ObjectReader<Edge>() {
            @Override public Edge read(final ResponseReader reader) throws IOException {
              return new Edge.Mapper(factory.edgeFactory()).map(reader);
            }
          })
        };

        public Mapper(@Nonnull Factory factory) {
          this.factory = factory;
        }

        @Override
        public AllStarship map(ResponseReader reader) throws IOException {
          final __ContentValues contentValues = new __ContentValues();
          reader.read(new ResponseReader.ValueHandler() {
            @Override
            public void handle(final int fieldIndex, final Object value) throws IOException {
              switch (fieldIndex) {
                case 0: {
                  contentValues.edges = (List<? extends Edge>) value;
                  break;
                }
              }
            }
          }, fields);
          return factory.creator().create(contentValues.edges);
        }

        static final class __ContentValues {
          List<? extends Edge> edges;
        }
      }

      interface Factory {
        @Nonnull Creator creator();

        @Nonnull Edge.Factory edgeFactory();
      }

      interface Creator {
        @Nonnull AllStarship create(@Nullable List<? extends Edge> edges);
      }
    }

    final class Mapper implements ResponseFieldMapper<Data> {
      final Factory factory;

      final Field[] fields = {
        Field.forObject("allStarships", "allStarships", null, true, new Field.ObjectReader<AllStarship>() {
          @Override public AllStarship read(final ResponseReader reader) throws IOException {
            return new AllStarship.Mapper(factory.allStarshipFactory()).map(reader);
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
                contentValues.allStarships = (AllStarship) value;
                break;
              }
            }
          }
        }, fields);
        return factory.creator().create(contentValues.allStarships);
      }

      static final class __ContentValues {
        AllStarship allStarships;
      }
    }

    interface Factory {
      @Nonnull Creator creator();

      @Nonnull AllStarship.Factory allStarshipFactory();
    }

    interface Creator {
      @Nonnull Data create(@Nullable AllStarship allStarships);
    }
  }
}
