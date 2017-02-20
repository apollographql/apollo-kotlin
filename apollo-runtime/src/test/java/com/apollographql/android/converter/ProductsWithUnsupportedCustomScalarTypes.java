package com.apollographql.android.converter;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Query;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import com.apollographql.android.converter.type.CustomType;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;

public final class ProductsWithUnsupportedCustomScalarTypes implements Query<Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query ProductsWithDate {\n"
      + "  shop {\n"
      + "    products(first: 10) {\n"
      + "      edges {\n"
      + "        node {\n"
      + "          title\n"
      + "          unsupportedCustomScalarTypeNumber\n"
      + "          unsupportedCustomScalarTypeBool\n"
      + "          unsupportedCustomScalarTypeString\n"
      + "        }\n"
      + "      }\n"
      + "    }\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private final Variables variables;

  public ProductsWithUnsupportedCustomScalarTypes() {
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
    public static final Creator CREATOR = new Creator() {
      @Override
      public Data create(@Nonnull Shop shop) {
        return new Data(shop);
      }
    };

    public static final Factory FACTORY = new Factory() {
      @Override
      public Creator creator() {
        return CREATOR;
      }

      @Override
      public Shop.Factory shopFactory() {
        return Shop.FACTORY;
      }
    };

    private @Nonnull Shop shop;

    public Data(@Nonnull Shop shop) {
      this.shop = shop;
    }

    public @Nonnull Shop shop() {
      return this.shop;
    }

    public static class Shop {
      public static final Creator CREATOR = new Creator() {
        @Override
        public Shop create(@Nonnull Product products) {
          return new Shop(products);
        }
      };

      public static final Factory FACTORY = new Factory() {
        @Override
        public Creator creator() {
          return CREATOR;
        }

        @Override
        public Product.Factory productFactory() {
          return Product.FACTORY;
        }
      };

      private @Nonnull Product products;

      public Shop(@Nonnull Product products) {
        this.products = products;
      }

      public @Nonnull Product products() {
        return this.products;
      }

      public static class Product {
        public static final Creator CREATOR = new Creator() {
          @Override
          public Product create(@Nonnull List<? extends Edge> edges) {
            return new Product(edges);
          }
        };

        public static final Factory FACTORY = new Factory() {
          @Override
          public Creator creator() {
            return CREATOR;
          }

          @Override
          public Edge.Factory edgeFactory() {
            return Edge.FACTORY;
          }
        };

        private @Nonnull List<? extends Edge> edges;

        public Product(@Nonnull List<? extends Edge> edges) {
          this.edges = edges;
        }

        public @Nonnull List<? extends Edge> edges() {
          return this.edges;
        }

        public static class Edge {
          public static final Creator CREATOR = new Creator() {
            @Override
            public Edge create(@Nonnull Node node) {
              return new Edge(node);
            }
          };

          public static final Factory FACTORY = new Factory() {
            @Override
            public Creator creator() {
              return CREATOR;
            }

            @Override
            public Node.Factory nodeFactory() {
              return Node.FACTORY;
            }
          };

          private @Nonnull Node node;

          public Edge(@Nonnull Node node) {
            this.node = node;
          }

          public @Nonnull Node node() {
            return this.node;
          }

          public static class Node {
            public static final Creator CREATOR = new Creator() {
              @Override
              public Node create(@Nonnull String title, @Nonnull Object unsupportedCustomScalarTypeNumber,
                @Nonnull Object unsupportedCustomScalarTypeBool, @Nonnull Object unsupportedCustomScalarTypeString) {
                return new Node(title, unsupportedCustomScalarTypeNumber, unsupportedCustomScalarTypeBool,
                    unsupportedCustomScalarTypeString);
              }
            };

            public static final Factory FACTORY = new Factory() {
              @Override
              public Creator creator() {
                return CREATOR;
              }
            };

            private @Nonnull String title;

            private @Nonnull Object unsupportedCustomScalarTypeNumber;

            private @Nonnull Object unsupportedCustomScalarTypeBool;

            private @Nonnull Object unsupportedCustomScalarTypeString;

            public Node(@Nonnull String title, @Nonnull Object unsupportedCustomScalarTypeNumber,
                @Nonnull Object unsupportedCustomScalarTypeBool, @Nonnull Object unsupportedCustomScalarTypeString) {
              this.title = title;
              this.unsupportedCustomScalarTypeNumber = unsupportedCustomScalarTypeNumber;
              this.unsupportedCustomScalarTypeBool = unsupportedCustomScalarTypeBool;
              this.unsupportedCustomScalarTypeString = unsupportedCustomScalarTypeString;
            }

            public @Nonnull String title() {
              return this.title;
            }

            public @Nonnull Object unsupportedCustomScalarTypeNumber() {
              return unsupportedCustomScalarTypeNumber;
            }

            public @Nonnull Object unsupportedCustomScalarTypeBool() {
              return unsupportedCustomScalarTypeBool;
            }

            public @Nonnull Object unsupportedCustomScalarTypeString() {
              return unsupportedCustomScalarTypeString;
            }

            public interface Factory {
              Creator creator();
            }

            public interface Creator {
              Node create(@Nonnull String title, @Nonnull Object unsupportedCustomScalarTypeNumber,
                @Nonnull Object unsupportedCustomScalarTypeBool, @Nonnull Object unsupportedCustomScalarTypeString);
            }

            public static final class Mapper implements ResponseFieldMapper<Node> {
              final Factory factory;

              final Field[] fields = {
                Field.forString("title", "title", null, false),
                  Field.forCustomType("unsupportedCustomScalarTypeNumber", "unsupportedCustomScalarTypeNumber", null,
                      false, CustomType.UNSUPPORTEDCUSTOMSCALARTYPENUMBER),
                  Field.forCustomType("unsupportedCustomScalarTypeBool", "unsupportedCustomScalarTypeBool", null, false,
                      CustomType.UNSUPPORTEDCUSTOMSCALARTYPEBOOL),
                  Field.forCustomType("unsupportedCustomScalarTypeString", "unsupportedCustomScalarTypeString", null,
                      false, CustomType.UNSUPPORTEDCUSTOMSCALARTYPESTRING)
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
                        contentValues.title = (String) value;
                        break;
                      }
                      case 1: {
                        contentValues.unsupportedCustomScalarTypeNumber = (Object) value;
                        break;
                      }
                      case 2: {
                        contentValues.unsupportedCustomScalarTypeBool = (Object) value;
                        break;
                      }
                      case 3: {
                        contentValues.unsupportedCustomScalarTypeString = (Object) value;
                        break;
                      }
                    }
                  }
                }, fields);
                return factory.creator().create(contentValues.title, contentValues.unsupportedCustomScalarTypeNumber,
                    contentValues.unsupportedCustomScalarTypeBool, contentValues.unsupportedCustomScalarTypeString);
              }

              static final class __ContentValues {
                String title;

                Object unsupportedCustomScalarTypeNumber;

                Object unsupportedCustomScalarTypeBool;

                Object unsupportedCustomScalarTypeString;
              }
            }
          }

          public interface Factory {
            Creator creator();

            Node.Factory nodeFactory();
          }

          public interface Creator {
            Edge create(@Nonnull Node node);
          }

          public static final class Mapper implements ResponseFieldMapper<Edge> {
            final Factory factory;

            final Field[] fields = {
              Field.forObject("node", "node", null, false, new Field.ObjectReader<Node>() {
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
        }

        public interface Factory {
          Creator creator();

          Edge.Factory edgeFactory();
        }

        public interface Creator {
          Product create(@Nonnull List<? extends Edge> edges);
        }

        public static final class Mapper implements ResponseFieldMapper<Product> {
          final Factory factory;

          final Field[] fields = {
            Field.forList("edges", "edges", null, false, new Field.ObjectReader<Edge>() {
              @Override public Edge read(final ResponseReader reader) throws IOException {
                return new Edge.Mapper(factory.edgeFactory()).map(reader);
              }
            })
          };

          public Mapper(@Nonnull Factory factory) {
            this.factory = factory;
          }

          @Override
          public Product map(ResponseReader reader) throws IOException {
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
      }

      public interface Factory {
        Creator creator();

        Product.Factory productFactory();
      }

      public interface Creator {
        Shop create(@Nonnull Product products);
      }

      public static final class Mapper implements ResponseFieldMapper<Shop> {
        final Factory factory;

        final Field[] fields = {
          Field.forObject("products", "products", null, false, new Field.ObjectReader<Product>() {
            @Override public Product read(final ResponseReader reader) throws IOException {
              return new Product.Mapper(factory.productFactory()).map(reader);
            }
          })
        };

        public Mapper(@Nonnull Factory factory) {
          this.factory = factory;
        }

        @Override
        public Shop map(ResponseReader reader) throws IOException {
          final __ContentValues contentValues = new __ContentValues();
          reader.read(new ResponseReader.ValueHandler() {
            @Override
            public void handle(final int fieldIndex, final Object value) throws IOException {
              switch (fieldIndex) {
                case 0: {
                  contentValues.products = (Product) value;
                  break;
                }
              }
            }
          }, fields);
          return factory.creator().create(contentValues.products);
        }

        static final class __ContentValues {
          Product products;
        }
      }
    }

    public interface Factory {
      Creator creator();

      Shop.Factory shopFactory();
    }

    public interface Creator {
      Data create(@Nonnull Shop shop);
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final Factory factory;

      final Field[] fields = {
        Field.forObject("shop", "shop", null, false, new Field.ObjectReader<Shop>() {
          @Override public Shop read(final ResponseReader reader) throws IOException {
            return new Shop.Mapper(factory.shopFactory()).map(reader);
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
                contentValues.shop = (Shop) value;
                break;
              }
            }
          }
        }, fields);
        return factory.creator().create(contentValues.shop);
      }

      static final class __ContentValues {
        Shop shop;
      }
    }
  }
}
