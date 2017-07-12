package com.example.fragment_in_fragment;

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
import com.apollographql.apollo.api.internal.UnmodifiableMapBuilder;
import com.example.fragment_in_fragment.fragment.StarshipFragment;
import java.lang.NullPointerException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class AllStarships implements Query<AllStarships.Data, Optional<AllStarships.Data>, Operation.Variables> {
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
   + StarshipFragment.FRAGMENT_DEFINITION;

  private static final OperationName OPERATION_NAME = new OperationName() {
    @Override
    public String name() {
      return "AllStarships";
    }
  };

  private final Operation.Variables variables;

  public AllStarships() {
    this.variables = Operation.EMPTY_VARIABLES;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public Optional<AllStarships.Data> wrapData(AllStarships.Data data) {
    return Optional.fromNullable(data);
  }

  @Override
  public Operation.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<AllStarships.Data> responseFieldMapper() {
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

    public AllStarships build() {
      return new AllStarships();
    }
  }

  public static class Data implements Operation.Data {
    static final ResponseField[] $responseFields = {
      ResponseField.forObject("allStarships", "allStarships", new UnmodifiableMapBuilder<String, Object>(1)
        .put("first", "7.0")
      .build(), true)
    };

    final Optional<AllStarships1> allStarships;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Data(@Nullable AllStarships1 allStarships) {
      this.allStarships = Optional.fromNullable(allStarships);
    }

    public Optional<AllStarships1> allStarships() {
      return this.allStarships;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeObject($responseFields[0], allStarships.isPresent() ? allStarships.get().marshaller() : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Data{"
          + "allStarships=" + allStarships
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
        return this.allStarships.equals(that.allStarships);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= allStarships.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final AllStarships1.Mapper allStarships1FieldMapper = new AllStarships1.Mapper();

      @Override
      public Data map(ResponseReader reader) {
        final AllStarships1 allStarships = reader.readObject($responseFields[0], new ResponseReader.ObjectReader<AllStarships1>() {
          @Override
          public AllStarships1 read(ResponseReader reader) {
            return allStarships1FieldMapper.map(reader);
          }
        });
        return new Data(allStarships);
      }
    }
  }

  public static class AllStarships1 {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forObjectList("edges", "edges", null, true)
    };

    final @Nonnull String __typename;

    final Optional<List<Edge>> edges;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public AllStarships1(@Nonnull String __typename, @Nullable List<Edge> edges) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
      this.__typename = __typename;
      this.edges = Optional.fromNullable(edges);
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * A list of edges.
     */
    public Optional<List<Edge>> edges() {
      return this.edges;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeList($responseFields[1], edges.isPresent() ? new ResponseWriter.ListWriter() {
            @Override
            public void write(ResponseWriter.ListItemWriter listItemWriter) {
              for (Edge $item : edges.get()) {
                listItemWriter.writeObject($item.marshaller());
              }
            }
          } : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "AllStarships1{"
          + "__typename=" + __typename + ", "
          + "edges=" + edges
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof AllStarships1) {
        AllStarships1 that = (AllStarships1) o;
        return this.__typename.equals(that.__typename)
         && this.edges.equals(that.edges);
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
        h ^= edges.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<AllStarships1> {
      final Edge.Mapper edgeFieldMapper = new Edge.Mapper();

      @Override
      public AllStarships1 map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final List<Edge> edges = reader.readList($responseFields[1], new ResponseReader.ListReader<Edge>() {
          @Override
          public Edge read(ResponseReader.ListItemReader reader) {
            return reader.readObject(new ResponseReader.ObjectReader<Edge>() {
              @Override
              public Edge read(ResponseReader reader) {
                return edgeFieldMapper.map(reader);
              }
            });
          }
        });
        return new AllStarships1(__typename, edges);
      }
    }
  }

  public static class Edge {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forObject("node", "node", null, true)
    };

    final @Nonnull String __typename;

    final Optional<Node> node;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Edge(@Nonnull String __typename, @Nullable Node node) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
      this.__typename = __typename;
      this.node = Optional.fromNullable(node);
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * The item at the end of the edge
     */
    public Optional<Node> node() {
      return this.node;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeObject($responseFields[1], node.isPresent() ? node.get().marshaller() : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Edge{"
          + "__typename=" + __typename + ", "
          + "node=" + node
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Edge) {
        Edge that = (Edge) o;
        return this.__typename.equals(that.__typename)
         && this.node.equals(that.node);
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
        h ^= node.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Edge> {
      final Node.Mapper nodeFieldMapper = new Node.Mapper();

      @Override
      public Edge map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final Node node = reader.readObject($responseFields[1], new ResponseReader.ObjectReader<Node>() {
          @Override
          public Node read(ResponseReader reader) {
            return nodeFieldMapper.map(reader);
          }
        });
        return new Edge(__typename, node);
      }
    }
  }

  public static class Node {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forString("id", "id", null, false),
      ResponseField.forString("name", "name", null, true),
      ResponseField.forObject("pilotConnection", "pilotConnection", null, true),
      ResponseField.forFragment("__typename", "__typename", Arrays.asList("Starship"))
    };

    final @Nonnull String __typename;

    final @Nonnull String id;

    final Optional<String> name;

    final Optional<PilotConnection> pilotConnection;

    private final @Nonnull Fragments fragments;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Node(@Nonnull String __typename, @Nonnull String id, @Nullable String name,
        @Nullable PilotConnection pilotConnection, @Nonnull Fragments fragments) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
      this.__typename = __typename;
      if (id == null) {
        throw new NullPointerException("id can't be null");
      }
      this.id = id;
      this.name = Optional.fromNullable(name);
      this.pilotConnection = Optional.fromNullable(pilotConnection);
      if (fragments == null) {
        throw new NullPointerException("fragments can't be null");
      }
      this.fragments = fragments;
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * The ID of an object
     */
    public @Nonnull String id() {
      return this.id;
    }

    /**
     * The name of this starship. The common name, such as "Death Star".
     */
    public Optional<String> name() {
      return this.name;
    }

    public Optional<PilotConnection> pilotConnection() {
      return this.pilotConnection;
    }

    public @Nonnull Fragments fragments() {
      return this.fragments;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], id);
          writer.writeString($responseFields[2], name.isPresent() ? name.get() : null);
          writer.writeObject($responseFields[3], pilotConnection.isPresent() ? pilotConnection.get().marshaller() : null);
          fragments.marshaller().marshal(writer);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Node{"
          + "__typename=" + __typename + ", "
          + "id=" + id + ", "
          + "name=" + name + ", "
          + "pilotConnection=" + pilotConnection + ", "
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
      if (o instanceof Node) {
        Node that = (Node) o;
        return this.__typename.equals(that.__typename)
         && this.id.equals(that.id)
         && this.name.equals(that.name)
         && this.pilotConnection.equals(that.pilotConnection)
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
        h ^= id.hashCode();
        h *= 1000003;
        h ^= name.hashCode();
        h *= 1000003;
        h ^= pilotConnection.hashCode();
        h *= 1000003;
        h ^= fragments.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static class Fragments {
      final @Nonnull StarshipFragment starshipFragment;

      private volatile String $toString;

      private volatile int $hashCode;

      private volatile boolean $hashCodeMemoized;

      public Fragments(@Nonnull StarshipFragment starshipFragment) {
        if (starshipFragment == null) {
          throw new NullPointerException("starshipFragment can't be null");
        }
        this.starshipFragment = starshipFragment;
      }

      public @Nonnull StarshipFragment starshipFragment() {
        return this.starshipFragment;
      }

      public ResponseFieldMarshaller marshaller() {
        return new ResponseFieldMarshaller() {
          @Override
          public void marshal(ResponseWriter writer) {
            final StarshipFragment $starshipFragment = starshipFragment;
            if ($starshipFragment != null) {
              $starshipFragment.marshaller().marshal(writer);
            }
          }
        };
      }

      @Override
      public String toString() {
        if ($toString == null) {
          $toString = "Fragments{"
            + "starshipFragment=" + starshipFragment
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
          return this.starshipFragment.equals(that.starshipFragment);
        }
        return false;
      }

      @Override
      public int hashCode() {
        if (!$hashCodeMemoized) {
          int h = 1;
          h *= 1000003;
          h ^= starshipFragment.hashCode();
          $hashCode = h;
          $hashCodeMemoized = true;
        }
        return $hashCode;
      }

      public static final class Mapper implements FragmentResponseFieldMapper<Fragments> {
        final StarshipFragment.Mapper starshipFragmentFieldMapper = new StarshipFragment.Mapper();

        @Override
        public @Nonnull Fragments map(ResponseReader reader, @Nonnull String conditionalType) {
          StarshipFragment starshipFragment = null;
          if (StarshipFragment.POSSIBLE_TYPES.contains(conditionalType)) {
            starshipFragment = starshipFragmentFieldMapper.map(reader);
          }
          return new Fragments(starshipFragment);
        }
      }
    }

    public static final class Mapper implements ResponseFieldMapper<Node> {
      final PilotConnection.Mapper pilotConnectionFieldMapper = new PilotConnection.Mapper();

      final Fragments.Mapper fragmentsFieldMapper = new Fragments.Mapper();

      @Override
      public Node map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String id = reader.readString($responseFields[1]);
        final String name = reader.readString($responseFields[2]);
        final PilotConnection pilotConnection = reader.readObject($responseFields[3], new ResponseReader.ObjectReader<PilotConnection>() {
          @Override
          public PilotConnection read(ResponseReader reader) {
            return pilotConnectionFieldMapper.map(reader);
          }
        });
        final Fragments fragments = reader.readConditional((ResponseField.ConditionalTypeField) $responseFields[4], new ResponseReader.ConditionalTypeReader<Fragments>() {
          @Override
          public Fragments read(String conditionalType, ResponseReader reader) {
            return fragmentsFieldMapper.map(reader, conditionalType);
          }
        });
        return new Node(__typename, id, name, pilotConnection, fragments);
      }
    }
  }

  public static class PilotConnection {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forObjectList("edges", "edges", null, true)
    };

    final @Nonnull String __typename;

    final Optional<List<Edge1>> edges;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public PilotConnection(@Nonnull String __typename, @Nullable List<Edge1> edges) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
      this.__typename = __typename;
      this.edges = Optional.fromNullable(edges);
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * A list of edges.
     */
    public Optional<List<Edge1>> edges() {
      return this.edges;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeList($responseFields[1], edges.isPresent() ? new ResponseWriter.ListWriter() {
            @Override
            public void write(ResponseWriter.ListItemWriter listItemWriter) {
              for (Edge1 $item : edges.get()) {
                listItemWriter.writeObject($item.marshaller());
              }
            }
          } : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "PilotConnection{"
          + "__typename=" + __typename + ", "
          + "edges=" + edges
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof PilotConnection) {
        PilotConnection that = (PilotConnection) o;
        return this.__typename.equals(that.__typename)
         && this.edges.equals(that.edges);
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
        h ^= edges.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<PilotConnection> {
      final Edge1.Mapper edge1FieldMapper = new Edge1.Mapper();

      @Override
      public PilotConnection map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final List<Edge1> edges = reader.readList($responseFields[1], new ResponseReader.ListReader<Edge1>() {
          @Override
          public Edge1 read(ResponseReader.ListItemReader reader) {
            return reader.readObject(new ResponseReader.ObjectReader<Edge1>() {
              @Override
              public Edge1 read(ResponseReader reader) {
                return edge1FieldMapper.map(reader);
              }
            });
          }
        });
        return new PilotConnection(__typename, edges);
      }
    }
  }

  public static class Edge1 {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forObject("node", "node", null, true)
    };

    final @Nonnull String __typename;

    final Optional<Node1> node;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Edge1(@Nonnull String __typename, @Nullable Node1 node) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
      this.__typename = __typename;
      this.node = Optional.fromNullable(node);
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * The item at the end of the edge
     */
    public Optional<Node1> node() {
      return this.node;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeObject($responseFields[1], node.isPresent() ? node.get().marshaller() : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Edge1{"
          + "__typename=" + __typename + ", "
          + "node=" + node
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Edge1) {
        Edge1 that = (Edge1) o;
        return this.__typename.equals(that.__typename)
         && this.node.equals(that.node);
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
        h ^= node.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Edge1> {
      final Node1.Mapper node1FieldMapper = new Node1.Mapper();

      @Override
      public Edge1 map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final Node1 node = reader.readObject($responseFields[1], new ResponseReader.ObjectReader<Node1>() {
          @Override
          public Node1 read(ResponseReader reader) {
            return node1FieldMapper.map(reader);
          }
        });
        return new Edge1(__typename, node);
      }
    }
  }

  public static class Node1 {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false),
      ResponseField.forString("name", "name", null, true)
    };

    final @Nonnull String __typename;

    final Optional<String> name;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Node1(@Nonnull String __typename, @Nullable String name) {
      if (__typename == null) {
        throw new NullPointerException("__typename can't be null");
      }
      this.__typename = __typename;
      this.name = Optional.fromNullable(name);
    }

    public @Nonnull String __typename() {
      return this.__typename;
    }

    /**
     * The name of this person.
     */
    public Optional<String> name() {
      return this.name;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeString($responseFields[1], name.isPresent() ? name.get() : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Node1{"
          + "__typename=" + __typename + ", "
          + "name=" + name
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Node1) {
        Node1 that = (Node1) o;
        return this.__typename.equals(that.__typename)
         && this.name.equals(that.name);
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
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Node1> {
      @Override
      public Node1 map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final String name = reader.readString($responseFields[1]);
        return new Node1(__typename, name);
      }
    }
  }
}
