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

  public static class Data implements Operation.Data {
    public static final Creator CREATOR = new Creator() {
      @Override
      public @Nonnull Data create(@Nullable AllStarship allStarships) {
        return new Data(allStarships);
      }
    };

    public static final Factory FACTORY = new Factory() {
      @Override
      public @Nonnull Creator creator() {
        return CREATOR;
      }

      @Override
      public @Nonnull AllStarship.Factory allStarshipFactory() {
        return AllStarship.FACTORY;
      }
    };

    private final @Nullable AllStarship allStarships;

    public Data(@Nullable AllStarship allStarships) {
      this.allStarships = allStarships;
    }

    public @Nullable AllStarship allStarships() {
      return this.allStarships;
    }

    @Override
    public String toString() {
      return "Data{"
        + "allStarships=" + allStarships
        + "}";
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Data) {
        Data that = (Data) o;
        return ((this.allStarships == null) ? (that.allStarships == null) : this.allStarships.equals(that.allStarships));
      }
      return false;
    }

    @Override
    public int hashCode() {
      int h = 1;
      h *= 1000003;
      h ^= (allStarships == null) ? 0 : allStarships.hashCode();
      return h;
    }

    public static class AllStarship {
      public static final Creator CREATOR = new Creator() {
        @Override
        public @Nonnull AllStarship create(@Nullable List<Edge> edges) {
          return new AllStarship(edges);
        }
      };

      public static final Factory FACTORY = new Factory() {
        @Override
        public @Nonnull Creator creator() {
          return CREATOR;
        }

        @Override
        public @Nonnull Edge.Factory edgeFactory() {
          return Edge.FACTORY;
        }
      };

      private final @Nullable List<Edge> edges;

      public AllStarship(@Nullable List<Edge> edges) {
        this.edges = edges;
      }

      public @Nullable List<Edge> edges() {
        return this.edges;
      }

      @Override
      public String toString() {
        return "AllStarship{"
          + "edges=" + edges
          + "}";
      }

      @Override
      public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof AllStarship) {
          AllStarship that = (AllStarship) o;
          return ((this.edges == null) ? (that.edges == null) : this.edges.equals(that.edges));
        }
        return false;
      }

      @Override
      public int hashCode() {
        int h = 1;
        h *= 1000003;
        h ^= (edges == null) ? 0 : edges.hashCode();
        return h;
      }

      public static class Edge {
        public static final Creator CREATOR = new Creator() {
          @Override
          public @Nonnull Edge create(@Nullable Node node) {
            return new Edge(node);
          }
        };

        public static final Factory FACTORY = new Factory() {
          @Override
          public @Nonnull Creator creator() {
            return CREATOR;
          }

          @Override
          public @Nonnull Node.Factory nodeFactory() {
            return Node.FACTORY;
          }
        };

        private final @Nullable Node node;

        public Edge(@Nullable Node node) {
          this.node = node;
        }

        public @Nullable Node node() {
          return this.node;
        }

        @Override
        public String toString() {
          return "Edge{"
            + "node=" + node
            + "}";
        }

        @Override
        public boolean equals(Object o) {
          if (o == this) {
            return true;
          }
          if (o instanceof Edge) {
            Edge that = (Edge) o;
            return ((this.node == null) ? (that.node == null) : this.node.equals(that.node));
          }
          return false;
        }

        @Override
        public int hashCode() {
          int h = 1;
          h *= 1000003;
          h ^= (node == null) ? 0 : node.hashCode();
          return h;
        }

        public static class Node {
          public static final Creator CREATOR = new Creator() {
            @Override
            public @Nonnull Node create(@Nonnull Fragments fragments) {
              return new Node(fragments);
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

          public Node(Fragments fragments) {
            this.fragments = fragments;
          }

          public @Nonnull Fragments fragments() {
            return this.fragments;
          }

          @Override
          public String toString() {
            return "Node{"
              + "fragments=" + fragments
              + "}";
          }

          @Override
          public boolean equals(Object o) {
            if (o == this) {
              return true;
            }
            if (o instanceof Node) {
              Node that = (Node) o;
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
              public @Nonnull Fragments create(@Nullable StarshipFragment starshipFragment) {
                return new Fragments(starshipFragment);
              }
            };

            public static final Factory FACTORY = new Factory() {
              @Override
              public @Nonnull Creator creator() {
                return CREATOR;
              }

              @Override
              public @Nonnull StarshipFragment.Factory starshipFragmentFactory() {
                return StarshipFragment.FACTORY;
              }
            };

            private StarshipFragment starshipFragment;

            public Fragments(StarshipFragment starshipFragment) {
              this.starshipFragment = starshipFragment;
            }

            public @Nullable StarshipFragment starshipFragment() {
              return this.starshipFragment;
            }

            @Override
            public String toString() {
              return "Fragments{"
                + "starshipFragment=" + starshipFragment
                + "}";
            }

            @Override
            public boolean equals(Object o) {
              if (o == this) {
                return true;
              }
              if (o instanceof Fragments) {
                Fragments that = (Fragments) o;
                return ((this.starshipFragment == null) ? (that.starshipFragment == null) : this.starshipFragment.equals(that.starshipFragment));
              }
              return false;
            }

            @Override
            public int hashCode() {
              int h = 1;
              h *= 1000003;
              h ^= (starshipFragment == null) ? 0 : starshipFragment.hashCode();
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
                StarshipFragment starshipFragment = null;
                if (conditionalType.equals(StarshipFragment.TYPE_CONDITION)) {
                  starshipFragment = new StarshipFragment.Mapper(factory.starshipFragmentFactory()).map(reader);
                }
                return factory.creator().create(starshipFragment);
              }
            }

            public interface Factory {
              @Nonnull Creator creator();

              @Nonnull StarshipFragment.Factory starshipFragmentFactory();
            }

            public interface Creator {
              @Nonnull Fragments create(@Nullable StarshipFragment starshipFragment);
            }
          }

          public static final class Mapper implements ResponseFieldMapper<Node> {
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

          public interface Factory {
            @Nonnull Creator creator();

            @Nonnull Fragments.Factory fragmentsFactory();
          }

          public interface Creator {
            @Nonnull Node create(@Nonnull Fragments fragments);
          }
        }

        public static final class Mapper implements ResponseFieldMapper<Edge> {
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

        public interface Factory {
          @Nonnull Creator creator();

          @Nonnull Node.Factory nodeFactory();
        }

        public interface Creator {
          @Nonnull Edge create(@Nullable Node node);
        }
      }

      public static final class Mapper implements ResponseFieldMapper<AllStarship> {
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
                  contentValues.edges = (List<Edge>) value;
                  break;
                }
              }
            }
          }, fields);
          return factory.creator().create(contentValues.edges);
        }

        static final class __ContentValues {
          List<Edge> edges;
        }
      }

      public interface Factory {
        @Nonnull Creator creator();

        @Nonnull Edge.Factory edgeFactory();
      }

      public interface Creator {
        @Nonnull AllStarship create(@Nullable List<Edge> edges);
      }
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
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

    public interface Factory {
      @Nonnull Creator creator();

      @Nonnull AllStarship.Factory allStarshipFactory();
    }

    public interface Creator {
      @Nonnull Data create(@Nullable AllStarship allStarships);
    }
  }
}
