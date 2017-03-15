package com.example.fragment_in_fragment;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.FragmentResponseFieldMapper;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import com.apollographql.android.api.graphql.internal.Optional;
import com.apollographql.android.api.graphql.util.UnmodifiableMapBuilder;
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
public final class AllStarships implements Query<AllStarships.Data, Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query AllStarships {\n"
      + "  allStarships(first: 7) {\n"
      + "    __typename\n"
      + "    edges {\n"
      + "      __typename\n"
      + "      node {\n"
      + "        __typename\n"
      + "        ...starshipFragment\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION + "\n"
   + StarshipFragment.FRAGMENT_DEFINITION + "\n"
   + PilotFragment.FRAGMENT_DEFINITION;

  private final Operation.Variables variables;

  public AllStarships() {
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

  @Override
  public ResponseFieldMapper<? extends Operation.Data> responseFieldMapper() {
    return new Data.Mapper();
  }

  public static class Data implements Operation.Data {
    private final Optional<AllStarship> allStarships;

    public Data(@Nullable AllStarship allStarships) {
      this.allStarships = Optional.fromNullable(allStarships);
    }

    public Optional<AllStarship> allStarships() {
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
      private final Optional<List<Edge>> edges;

      public AllStarship(@Nullable List<Edge> edges) {
        this.edges = Optional.fromNullable(edges);
      }

      public Optional<List<Edge>> edges() {
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
        private final Optional<Node> node;

        public Edge(@Nullable Node node) {
          this.node = Optional.fromNullable(node);
        }

        public Optional<Node> node() {
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
            private Optional<StarshipFragment> starshipFragment;

            public Fragments(@Nullable StarshipFragment starshipFragment) {
              this.starshipFragment = Optional.fromNullable(starshipFragment);
            }

            public Optional<StarshipFragment> starshipFragment() {
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

            public static final class Mapper implements FragmentResponseFieldMapper<Fragments> {
              final StarshipFragment.Mapper starshipFragmentFieldMapper = new StarshipFragment.Mapper();

              @Override
              public @Nonnull Fragments map(ResponseReader reader, @Nonnull String conditionalType)
                  throws IOException {
                StarshipFragment starshipFragment = null;
                if (StarshipFragment.POSSIBLE_TYPES.contains(conditionalType)) {
                  starshipFragment = starshipFragmentFieldMapper.map(reader);
                }
                return new Fragments(starshipFragment);
              }
            }
          }

          public static final class Mapper implements ResponseFieldMapper<Node> {
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
            public Node map(ResponseReader reader) throws IOException {
              final Fragments fragments = reader.read(fields[0]);
              return new Node(fragments);
            }
          }
        }

        public static final class Mapper implements ResponseFieldMapper<Edge> {
          final Node.Mapper nodeFieldMapper = new Node.Mapper();

          final Field[] fields = {
            Field.forObject("node", "node", null, true, new Field.ObjectReader<Node>() {
              @Override public Node read(final ResponseReader reader) throws IOException {
                return nodeFieldMapper.map(reader);
              }
            })
          };

          @Override
          public Edge map(ResponseReader reader) throws IOException {
            final Node node = reader.read(fields[0]);
            return new Edge(node);
          }
        }
      }

      public static final class Mapper implements ResponseFieldMapper<AllStarship> {
        final Edge.Mapper edgeFieldMapper = new Edge.Mapper();

        final Field[] fields = {
          Field.forList("edges", "edges", null, true, new Field.ObjectReader<Edge>() {
            @Override public Edge read(final ResponseReader reader) throws IOException {
              return edgeFieldMapper.map(reader);
            }
          })
        };

        @Override
        public AllStarship map(ResponseReader reader) throws IOException {
          final List<Edge> edges = reader.read(fields[0]);
          return new AllStarship(edges);
        }
      }
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final AllStarship.Mapper allStarshipFieldMapper = new AllStarship.Mapper();

      final Field[] fields = {
        Field.forObject("allStarships", "allStarships", new UnmodifiableMapBuilder<String, Object>(1)
          .put("first", "7.0")
        .build(), true, new Field.ObjectReader<AllStarship>() {
          @Override public AllStarship read(final ResponseReader reader) throws IOException {
            return allStarshipFieldMapper.map(reader);
          }
        })
      };

      @Override
      public Data map(ResponseReader reader) throws IOException {
        final AllStarship allStarships = reader.read(fields[0]);
        return new Data(allStarships);
      }
    }
  }
}
