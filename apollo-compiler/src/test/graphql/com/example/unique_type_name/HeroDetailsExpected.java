package com.example.unique_type_name.fragment;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import java.io.IOException;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public interface HeroDetails {
  String FRAGMENT_DEFINITION = "fragment HeroDetails on Character {\n"
      + "  __typename\n"
      + "  name\n"
      + "  friendsConnection {\n"
      + "    totalCount\n"
      + "    edges {\n"
      + "      node {\n"
      + "        __typename\n"
      + "        name\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "}";

  String TYPE_CONDITION = "Character";

  @Nonnull String name();

  @Nonnull FriendsConnection friendsConnection();

  interface FriendsConnection {
    @Nullable Integer totalCount();

    @Nullable List<? extends Edge> edges();

    interface Edge {
      @Nullable Node node();

      interface Node {
        @Nonnull String name();

        final class Mapper implements ResponseFieldMapper<Node> {
          final Factory factory;

          final Field[] fields = {
            Field.forString("name", "name", null, false)
          };

          public Mapper(@Nonnull Factory factory) {
            this.factory = factory;
          }

          @Override
          public Node map(ResponseReader reader) throws IOException {
            final __ContentValues contentValues = new __ContentValues();
            reader.read(new ResponseReader.ValueHandler() {
              @Override
              public void handle(final int fieldIndex, final Object value) throws IOException {
                switch (fieldIndex) {
                  case 0: {
                    contentValues.name = (String) value;
                    break;
                  }
                }
              }
            }, fields);
            return factory.creator().create(contentValues.name);
          }

          static final class __ContentValues {
            String name;
          }
        }

        interface Factory {
          Creator creator();
        }

        interface Creator {
          Node create(@Nonnull String name);
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
        Creator creator();

        Node.Factory nodeFactory();
      }

      interface Creator {
        Edge create(@Nullable Node node);
      }
    }

    final class Mapper implements ResponseFieldMapper<FriendsConnection> {
      final Factory factory;

      final Field[] fields = {
        Field.forInt("totalCount", "totalCount", null, true),
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
      public FriendsConnection map(ResponseReader reader) throws IOException {
        final __ContentValues contentValues = new __ContentValues();
        reader.read(new ResponseReader.ValueHandler() {
          @Override
          public void handle(final int fieldIndex, final Object value) throws IOException {
            switch (fieldIndex) {
              case 0: {
                contentValues.totalCount = (Integer) value;
                break;
              }
              case 1: {
                contentValues.edges = (List<? extends Edge>) value;
                break;
              }
            }
          }
        }, fields);
        return factory.creator().create(contentValues.totalCount, contentValues.edges);
      }

      static final class __ContentValues {
        Integer totalCount;

        List<? extends Edge> edges;
      }
    }

    interface Factory {
      Creator creator();

      Edge.Factory edgeFactory();
    }

    interface Creator {
      FriendsConnection create(@Nullable Integer totalCount, @Nullable List<? extends Edge> edges);
    }
  }

  final class Mapper implements ResponseFieldMapper<HeroDetails> {
    final Factory factory;

    final Field[] fields = {
      Field.forString("name", "name", null, false),
      Field.forObject("friendsConnection", "friendsConnection", null, false, new Field.ObjectReader<FriendsConnection>() {
        @Override public FriendsConnection read(final ResponseReader reader) throws IOException {
          return new FriendsConnection.Mapper(factory.friendsConnectionFactory()).map(reader);
        }
      })
    };

    public Mapper(@Nonnull Factory factory) {
      this.factory = factory;
    }

    @Override
    public HeroDetails map(ResponseReader reader) throws IOException {
      final __ContentValues contentValues = new __ContentValues();
      reader.read(new ResponseReader.ValueHandler() {
        @Override
        public void handle(final int fieldIndex, final Object value) throws IOException {
          switch (fieldIndex) {
            case 0: {
              contentValues.name = (String) value;
              break;
            }
            case 1: {
              contentValues.friendsConnection = (FriendsConnection) value;
              break;
            }
          }
        }
      }, fields);
      return factory.creator().create(contentValues.name, contentValues.friendsConnection);
    }

    static final class __ContentValues {
      String name;

      FriendsConnection friendsConnection;
    }
  }

  interface Factory {
    Creator creator();

    FriendsConnection.Factory friendsConnectionFactory();
  }

  interface Creator {
    HeroDetails create(@Nonnull String name, @Nonnull FriendsConnection friendsConnection);
  }
}
