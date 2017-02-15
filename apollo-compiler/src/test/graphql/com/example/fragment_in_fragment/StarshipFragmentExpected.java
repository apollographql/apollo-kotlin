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
public interface StarshipFragment {
  String FRAGMENT_DEFINITION = "fragment starshipFragment on Starship {\n"
      + "  id\n"
      + "  name\n"
      + "  pilotConnection {\n"
      + "    edges {\n"
      + "      node {\n"
      + "        ...pilotFragment\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "}";

  String TYPE_CONDITION = "Starship";

  @Nonnull String id();

  @Nullable String name();

  @Nullable PilotConnection pilotConnection();

  interface PilotConnection {
    @Nullable List<Edge> edges();

    interface Edge {
      @Nullable Node node();

      interface Node {
        @Nonnull Fragments fragments();

        interface Fragments {
          @Nullable PilotFragment pilotFragment();

          final class Mapper implements ResponseFieldMapper<Fragments> {
            final Factory factory;

            String conditionalType;

            public Mapper(@Nonnull Factory factory, @Nonnull String conditionalType) {
              this.factory = factory;
              this.conditionalType = conditionalType;
            }

            @Override
            public @Nonnull Fragments map(ResponseReader reader) throws IOException {
              PilotFragment pilotFragment = null;
              if (conditionalType.equals(PilotFragment.TYPE_CONDITION)) {
                pilotFragment = new PilotFragment.Mapper(factory.pilotFragmentFactory()).map(reader);
              }
              return factory.creator().create(pilotFragment);
            }
          }

          interface Factory {
            @Nonnull Creator creator();

            @Nonnull PilotFragment.Factory pilotFragmentFactory();
          }

          interface Creator {
            @Nonnull Fragments create(@Nullable PilotFragment pilotFragment);
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

    final class Mapper implements ResponseFieldMapper<PilotConnection> {
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
        return factory.creator().create(contentValues.edges);
      }

      static final class __ContentValues {
        List<Edge> edges;
      }
    }

    interface Factory {
      @Nonnull Creator creator();

      @Nonnull Edge.Factory edgeFactory();
    }

    interface Creator {
      @Nonnull PilotConnection create(@Nullable List<Edge> edges);
    }
  }

  final class Mapper implements ResponseFieldMapper<StarshipFragment> {
    final Factory factory;

    final Field[] fields = {
      Field.forString("id", "id", null, false),
      Field.forString("name", "name", null, true),
      Field.forObject("pilotConnection", "pilotConnection", null, true, new Field.ObjectReader<PilotConnection>() {
        @Override public PilotConnection read(final ResponseReader reader) throws IOException {
          return new PilotConnection.Mapper(factory.pilotConnectionFactory()).map(reader);
        }
      })
    };

    public Mapper(@Nonnull Factory factory) {
      this.factory = factory;
    }

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
      return factory.creator().create(contentValues.id, contentValues.name, contentValues.pilotConnection);
    }

    static final class __ContentValues {
      String id;

      String name;

      PilotConnection pilotConnection;
    }
  }

  interface Factory {
    @Nonnull Creator creator();

    @Nonnull PilotConnection.Factory pilotConnectionFactory();
  }

  interface Creator {
    @Nonnull StarshipFragment create(@Nonnull String id, @Nullable String name,
        @Nullable PilotConnection pilotConnection);
  }
}
