package com.example.arguments_hardcoded;

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
import com.apollographql.apollo.api.internal.Utils;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.List;
import javax.annotation.Generated;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Generated("Apollo GraphQL")
public final class TestQuery implements Query<TestQuery.Data, Optional<TestQuery.Data>, Operation.Variables> {
  public static final String OPERATION_DEFINITION = "query TestQuery {\n"
      + "  reviews(episode: JEDI, starsInt: 10, starsFloat: 9.9) {\n"
      + "    __typename\n"
      + "    stars\n"
      + "    commentary\n"
      + "  }\n"
      + "}";

  public static final String OPERATION_ID = "a6746506c2fb20405972e7e76920eec4aa2e5e02dc429cfbd1585a4f1787b0d9";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  public static final OperationName OPERATION_NAME = new OperationName() {
    @Override
    public String name() {
      return "TestQuery";
    }
  };

  private final Operation.Variables variables;

  public TestQuery() {
    this.variables = Operation.EMPTY_VARIABLES;
  }

  @Override
  public String operationId() {
    return OPERATION_ID;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public Optional<TestQuery.Data> wrapData(TestQuery.Data data) {
    return Optional.fromNullable(data);
  }

  @Override
  public Operation.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<TestQuery.Data> responseFieldMapper() {
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

    public TestQuery build() {
      return new TestQuery();
    }
  }

  public static class Data implements Operation.Data {
    static final ResponseField[] $responseFields = {
      ResponseField.forList("reviews", "reviews", new UnmodifiableMapBuilder<String, Object>(3)
      .put("episode", "JEDI")
      .put("starsInt", 10)
      .put("starsFloat", 9.9f)
      .build(), true, Collections.<ResponseField.Condition>emptyList())
    };

    final Optional<List<Review>> reviews;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Data(@Nullable List<Review> reviews) {
      this.reviews = Optional.fromNullable(reviews);
    }

    public Optional<List<Review>> reviews() {
      return this.reviews;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeList($responseFields[0], reviews.isPresent() ? reviews.get() : null, new ResponseWriter.ListWriter() {
            @Override
            public void write(Object value, ResponseWriter.ListItemWriter listItemWriter) {
              listItemWriter.writeObject(((Review) value).marshaller());
            }
          });
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Data{"
          + "reviews=" + reviews
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
        return this.reviews.equals(that.reviews);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= reviews.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final Review.Mapper reviewFieldMapper = new Review.Mapper();

      @Override
      public Data map(ResponseReader reader) {
        final List<Review> reviews = reader.readList($responseFields[0], new ResponseReader.ListReader<Review>() {
          @Override
          public Review read(ResponseReader.ListItemReader listItemReader) {
            return listItemReader.readObject(new ResponseReader.ObjectReader<Review>() {
              @Override
              public Review read(ResponseReader reader) {
                return reviewFieldMapper.map(reader);
              }
            });
          }
        });
        return new Data(reviews);
      }
    }
  }

  public static class Review {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("stars", "stars", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("commentary", "commentary", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final int stars;

    final Optional<String> commentary;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Review(@NotNull String __typename, int stars, @Nullable String commentary) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.stars = stars;
      this.commentary = Optional.fromNullable(commentary);
    }

    public @NotNull String __typename() {
      return this.__typename;
    }

    /**
     * The number of stars this review gave, 1-5
     */
    public int stars() {
      return this.stars;
    }

    /**
     * Comment about the movie
     */
    public Optional<String> commentary() {
      return this.commentary;
    }

    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeString($responseFields[0], __typename);
          writer.writeInt($responseFields[1], stars);
          writer.writeString($responseFields[2], commentary.isPresent() ? commentary.get() : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Review{"
          + "__typename=" + __typename + ", "
          + "stars=" + stars + ", "
          + "commentary=" + commentary
          + "}";
      }
      return $toString;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Review) {
        Review that = (Review) o;
        return this.__typename.equals(that.__typename)
         && this.stars == that.stars
         && this.commentary.equals(that.commentary);
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
        h ^= stars;
        h *= 1000003;
        h ^= commentary.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Review> {
      @Override
      public Review map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final int stars = reader.readInt($responseFields[1]);
        final String commentary = reader.readString($responseFields[2]);
        return new Review(__typename, stars, commentary);
      }
    }
  }
}
