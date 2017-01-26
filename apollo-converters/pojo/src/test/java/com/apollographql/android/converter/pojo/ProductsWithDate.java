package com.apollographql.android.converter.pojo;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Date;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;

@Generated("Apollo GraphQL")
public final class ProductsWithDate implements Query<Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query ProductsWithDate {\n"
      + "  shop {\n"
      + "    products(first: 10) {\n"
      + "      edges {\n"
      + "        node {\n"
      + "          title\n"
      + "          createdAt\n"
      + "        }\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private final Variables variables;

  public ProductsWithDate() {
    this.variables = Operation.EMPTY_VARIABLES;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public Variables variables() {
    return variables;
  }

  public static class Data implements Operation.Data {
    private static final ResponseFieldMapper<Data> MAPPER = new ResponseFieldMapper<Data>() {
      private final Field[] FIELDS = {
        Field.forObject("shop", "shop", null, false, new Field.ObjectReader<Shop>() {
          @Override public Shop read(final ResponseReader reader) throws IOException {
            return new Shop(reader);
          }
        })
      };

      @Override
      public void map(final ResponseReader reader, final Data instance) throws IOException {
        reader.read(new ResponseReader.ValueHandler() {
          @Override
          public void handle(final int fieldIndex, final Object value) throws IOException {
            switch (fieldIndex) {
              case 0: {
                instance.shop = (Shop) value;
                break;
              }
            }
          }
        }, FIELDS);
      }
    };

    private @Nonnull Shop shop;

    public Data(ResponseReader reader) throws IOException {
      MAPPER.map(reader, this);
    }

    public @Nonnull Shop shop() {
      return this.shop;
    }

    public static class Shop {
      private static final ResponseFieldMapper<Shop> MAPPER = new ResponseFieldMapper<Shop>() {
        private final Field[] FIELDS = {
          Field.forObject("products", "products", null, false, new Field.ObjectReader<Product>() {
            @Override public Product read(final ResponseReader reader) throws IOException {
              return new Product(reader);
            }
          })
        };

        @Override
        public void map(final ResponseReader reader, final Shop instance) throws IOException {
          reader.read(new ResponseReader.ValueHandler() {
            @Override
            public void handle(final int fieldIndex, final Object value) throws IOException {
              switch (fieldIndex) {
                case 0: {
                  instance.products = (Product) value;
                  break;
                }
              }
            }
          }, FIELDS);
        }
      };

      private @Nonnull Product products;

      public Shop(ResponseReader reader) throws IOException {
        MAPPER.map(reader, this);
      }

      public @Nonnull Product products() {
        return this.products;
      }

      public static class Product {
        private static final ResponseFieldMapper<Product> MAPPER = new ResponseFieldMapper<Product>() {
          private final Field[] FIELDS = {
            Field.forList("edges", "edges", null, false, new Field.ObjectReader<Edge>() {
              @Override public Edge read(final ResponseReader reader) throws IOException {
                return new Edge(reader);
              }
            })
          };

          @Override
          public void map(final ResponseReader reader, final Product instance) throws IOException {
            reader.read(new ResponseReader.ValueHandler() {
              @Override
              public void handle(final int fieldIndex, final Object value) throws IOException {
                switch (fieldIndex) {
                  case 0: {
                    instance.edges = (List<? extends Edge>) value;
                    break;
                  }
                }
              }
            }, FIELDS);
          }
        };

        private @Nonnull List<? extends Edge> edges;

        public Product(ResponseReader reader) throws IOException {
          MAPPER.map(reader, this);
        }

        public @Nonnull List<? extends Edge> edges() {
          return this.edges;
        }

        public static class Edge {
          private static final ResponseFieldMapper<Edge> MAPPER = new ResponseFieldMapper<Edge>() {
            private final Field[] FIELDS = {
              Field.forObject("node", "node", null, false, new Field.ObjectReader<Node>() {
                @Override public Node read(final ResponseReader reader) throws IOException {
                  return new Node(reader);
                }
              })
            };

            @Override
            public void map(final ResponseReader reader, final Edge instance) throws IOException {
              reader.read(new ResponseReader.ValueHandler() {
                @Override
                public void handle(final int fieldIndex, final Object value) throws IOException {
                  switch (fieldIndex) {
                    case 0: {
                      instance.node = (Node) value;
                      break;
                    }
                  }
                }
              }, FIELDS);
            }
          };

          private @Nonnull Node node;

          public Edge(ResponseReader reader) throws IOException {
            MAPPER.map(reader, this);
          }

          public @Nonnull Node node() {
            return this.node;
          }

          public static class Node {
            private static final ResponseFieldMapper<Node> MAPPER = new ResponseFieldMapper<Node>() {
              private final Field[] FIELDS = {
                Field.forString("title", "title", null, false),
                Field.forCustomType("createdAt", "createdAt", null, false, CustomType.DATETIME)
              };

              @Override
              public void map(final ResponseReader reader, final Node instance) throws IOException {
                reader.read(new ResponseReader.ValueHandler() {
                  @Override
                  public void handle(final int fieldIndex, final Object value) throws IOException {
                    switch (fieldIndex) {
                      case 0: {
                        instance.title = (String) value;
                        break;
                      }
                      case 1: {
                        instance.createdAt = (Date) value;
                        break;
                      }
                    }
                  }
                }, FIELDS);
              }
            };

            private @Nonnull String title;

            private @Nonnull Date createdAt;

            public Node(ResponseReader reader) throws IOException {
              MAPPER.map(reader, this);
            }

            public @Nonnull String title() {
              return this.title;
            }

            public @Nonnull Date createdAt() {
              return this.createdAt;
            }
          }
        }
      }
    }
  }
}
