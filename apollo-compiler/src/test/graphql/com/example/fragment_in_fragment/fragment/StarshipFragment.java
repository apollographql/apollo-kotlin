package com.example.fragment_in_fragment.fragment;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public class StarshipFragment {
  public static final String FRAGMENT_DEFINITION = "fragment starshipFragment on Starship {\n"
      + "  __typename\n"
      + "  id\n"
      + "  name\n"
      + "  pilotConnection {\n"
      + "    __typename\n"
      + "    edges {\n"
      + "      __typename\n"
      + "      node {\n"
      + "        __typename\n"
      + "        ...pilotFragment\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "}";

  public static final String TYPE_CONDITION = "Starship";

  private final @Nonnull String id;

  private final @Nullable String name;

  private final @Nullable PilotConnection pilotConnection;

  public StarshipFragment(@Nonnull String id, @Nullable String name,
      @Nullable PilotConnection pilotConnection) {
    this.id = id;
    this.name = name;
    this.pilotConnection = pilotConnection;
  }

  public @Nonnull String id() {
    return this.id;
  }

  public @Nullable String name() {
    return this.name;
  }

  public @Nullable PilotConnection pilotConnection() {
    return this.pilotConnection;
  }

  @Override
  public String toString() {
    return "StarshipFragment{"
      + "id=" + id + ", "
      + "name=" + name + ", "
      + "pilotConnection=" + pilotConnection
      + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof StarshipFragment) {
      StarshipFragment that = (StarshipFragment) o;
      return ((this.id == null) ? (that.id == null) : this.id.equals(that.id))
       && ((this.name == null) ? (that.name == null) : this.name.equals(that.name))
       && ((this.pilotConnection == null) ? (that.pilotConnection == null) : this.pilotConnection.equals(that.pilotConnection));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = 1;
    h *= 1000003;
    h ^= (id == null) ? 0 : id.hashCode();
    h *= 1000003;
    h ^= (name == null) ? 0 : name.hashCode();
    h *= 1000003;
    h ^= (pilotConnection == null) ? 0 : pilotConnection.hashCode();
    return h;
  }

  public static class PilotConnection {
    private final @Nullable List<Edge> edges;

    public PilotConnection(@Nullable List<Edge> edges) {
      this.edges = edges;
    }

    public @Nullable List<Edge> edges() {
      return this.edges;
    }

    @Override
    public String toString() {
      return "PilotConnection{"
        + "edges=" + edges
        + "}";
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof PilotConnection) {
        PilotConnection that = (PilotConnection) o;
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
          private PilotFragment pilotFragment;

          public Fragments(PilotFragment pilotFragment) {
            this.pilotFragment = pilotFragment;
          }

          public @Nullable PilotFragment pilotFragment() {
            return this.pilotFragment;
          }

          @Override
          public String toString() {
            return "Fragments{"
              + "pilotFragment=" + pilotFragment
              + "}";
          }

          @Override
          public boolean equals(Object o) {
            if (o == this) {
              return true;
            }
            if (o instanceof Fragments) {
              Fragments that = (Fragments) o;
              return ((this.pilotFragment == null) ? (that.pilotFragment == null) : this.pilotFragment.equals(that.pilotFragment));
            }
            return false;
          }

          @Override
          public int hashCode() {
            int h = 1;
            h *= 1000003;
            h ^= (pilotFragment == null) ? 0 : pilotFragment.hashCode();
            return h;
          }

          public static final class Mapper implements ResponseFieldMapper<Fragments> {
            private final String conditionalType;

            public Mapper(@Nonnull String conditionalType) {
              this.conditionalType = conditionalType;
            }

            @Override
            public @Nonnull Fragments map(ResponseReader reader) throws IOException {
              PilotFragment pilotFragment = null;
              if (conditionalType.equals(PilotFragment.TYPE_CONDITION)) {
                pilotFragment = new PilotFragment.Mapper().map(reader);
              }
              return new Fragments(pilotFragment);
            }
          }
        }

        public static final class Mapper implements ResponseFieldMapper<Node> {
          final Field[] fields = {
            Field.forConditionalType("__typename", "__typename", new Field.ConditionalTypeReader<Fragments>() {
              @Override
              public Fragments read(String conditionalType, ResponseReader reader) throws
                  IOException {
                return new Fragments.Mapper(conditionalType).map(reader);
              }
            })
          };

          @Override
          public Node map(ResponseReader reader) throws IOException {
            final __ContentValues contentValues = new __ContentValues();
            reader.read(new ResponseReader.ValueHandler() {
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
            return new Node(contentValues.fragments);
          }

          static final class __ContentValues {
            Fragments fragments;
          }
        }
      }

      public static final class Mapper implements ResponseFieldMapper<Edge> {
        final Field[] fields = {
          Field.forObject("node", "node", null, true, new Field.ObjectReader<Node>() {
            @Override public Node read(final ResponseReader reader) throws IOException {
              return new Node.Mapper().map(reader);
            }
          })
        };

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
          return new Edge(contentValues.node);
        }

        static final class __ContentValues {
          Node node;
        }
      }
    }

    public static final class Mapper implements ResponseFieldMapper<PilotConnection> {
      final Field[] fields = {
        Field.forList("edges", "edges", null, true, new Field.ObjectReader<Edge>() {
          @Override public Edge read(final ResponseReader reader) throws IOException {
            return new Edge.Mapper().map(reader);
          }
        })
      };

      @Override
      public PilotConnection map(ResponseReader reader) throws IOException {
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
        return new PilotConnection(contentValues.edges);
      }

      static final class __ContentValues {
        List<Edge> edges;
      }
    }
  }

  public static final class Mapper implements ResponseFieldMapper<StarshipFragment> {
    final Field[] fields = {
      Field.forString("id", "id", null, false),
      Field.forString("name", "name", null, true),
      Field.forObject("pilotConnection", "pilotConnection", null, true, new Field.ObjectReader<PilotConnection>() {
        @Override public PilotConnection read(final ResponseReader reader) throws IOException {
          return new PilotConnection.Mapper().map(reader);
        }
      })
    };

    @Override
    public StarshipFragment map(ResponseReader reader) throws IOException {
      final __ContentValues contentValues = new __ContentValues();
      reader.read(new ResponseReader.ValueHandler() {
        @Override
        public void handle(final int fieldIndex, final Object value) throws IOException {
          switch (fieldIndex) {
            case 0: {
              contentValues.id = (String) value;
              break;
            }
            case 1: {
              contentValues.name = (String) value;
              break;
            }
            case 2: {
              contentValues.pilotConnection = (PilotConnection) value;
              break;
            }
          }
        }
      }, fields);
      return new StarshipFragment(contentValues.id, contentValues.name, contentValues.pilotConnection);
    }

    static final class __ContentValues {
      String id;

      String name;

      PilotConnection pilotConnection;
    }
  }
}
