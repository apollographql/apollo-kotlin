// AUTO-GENERATED FILE. DO NOT MODIFY.
//
// This class was automatically generated by Apollo GraphQL plugin from the GraphQL queries it found.
// It should not be modified by hand.
//
package com.example.hero_with_review;

import com.apollographql.apollo.api.InputFieldMarshaller;
import com.apollographql.apollo.api.InputFieldWriter;
import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ResponseWriter;
import com.apollographql.apollo.api.ScalarTypeAdapters;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.api.internal.SimpleOperationResponseParser;
import com.apollographql.apollo.api.internal.UnmodifiableMapBuilder;
import com.apollographql.apollo.api.internal.Utils;
import com.apollographql.apollo.internal.QueryDocumentMinifier;
import com.example.hero_with_review.type.Episode;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import okio.BufferedSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TestQuery implements Mutation<TestQuery.Data, Optional<TestQuery.Data>, TestQuery.Variables> {
  public static final String OPERATION_ID = "df7f6bf82724eedee5118f165075b5de1a2b3a06d0390126bf7932dc8df3f082";

  public static final String QUERY_DOCUMENT = QueryDocumentMinifier.minify(
    "mutation TestQuery($ep: Episode!) {\n"
        + "  createReview(episode: $ep, review: {stars: 5, listOfEnums: [JEDI, EMPIRE, NEWHOPE], listOfStringNonOptional: [\"1\", \"2\", \"3\"], favoriteColor: {red: 1, blue: 1}}) {\n"
        + "    __typename\n"
        + "    stars\n"
        + "    commentary\n"
        + "  }\n"
        + "}"
  );

  public static final OperationName OPERATION_NAME = new OperationName() {
    @Override
    public String name() {
      return "TestQuery";
    }
  };

  private final TestQuery.Variables variables;

  public TestQuery(@NotNull Episode ep) {
    Utils.checkNotNull(ep, "ep == null");
    variables = new TestQuery.Variables(ep);
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
  public TestQuery.Variables variables() {
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

  @Override
  @NotNull
  public Response<Optional<TestQuery.Data>> parse(@NotNull final BufferedSource source,
      @NotNull final ScalarTypeAdapters scalarTypeAdapters) throws IOException {
    return SimpleOperationResponseParser.parse(source, this, scalarTypeAdapters);
  }

  public static final class Builder {
    private @NotNull Episode ep;

    Builder() {
    }

    public Builder ep(@NotNull Episode ep) {
      this.ep = ep;
      return this;
    }

    public TestQuery build() {
      Utils.checkNotNull(ep, "ep == null");
      return new TestQuery(ep);
    }
  }

  public static final class Variables extends Operation.Variables {
    private final @NotNull Episode ep;

    private final transient Map<String, Object> valueMap = new LinkedHashMap<>();

    Variables(@NotNull Episode ep) {
      this.ep = ep;
      this.valueMap.put("ep", ep);
    }

    public @NotNull Episode ep() {
      return ep;
    }

    @Override
    public Map<String, Object> valueMap() {
      return Collections.unmodifiableMap(valueMap);
    }

    @Override
    public InputFieldMarshaller marshaller() {
      return new InputFieldMarshaller() {
        @Override
        public void marshal(InputFieldWriter writer) throws IOException {
          writer.writeString("ep", ep.rawValue());
        }
      };
    }
  }

  public static class Data implements Operation.Data {
    static final ResponseField[] $responseFields = {
      ResponseField.forObject("createReview", "createReview", new UnmodifiableMapBuilder<String, Object>(2)
      .put("episode", new UnmodifiableMapBuilder<String, Object>(2)
        .put("kind", "Variable")
        .put("variableName", "ep")
        .build())
      .put("review", new UnmodifiableMapBuilder<String, Object>(4)
        .put("stars", "5")
        .put("listOfEnums", "[JEDI, EMPIRE, NEWHOPE]")
        .put("listOfStringNonOptional", "[1, 2, 3]")
        .put("favoriteColor", new UnmodifiableMapBuilder<String, Object>(2)
          .put("red", "1")
          .put("blue", "1.0")
          .build())
        .build())
      .build(), true, Collections.<ResponseField.Condition>emptyList())
    };

    final Optional<CreateReview> createReview;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

    public Data(@Nullable CreateReview createReview) {
      this.createReview = Optional.fromNullable(createReview);
    }

    public Optional<CreateReview> createReview() {
      return this.createReview;
    }

    @SuppressWarnings("unchecked")
    public ResponseFieldMarshaller marshaller() {
      return new ResponseFieldMarshaller() {
        @Override
        public void marshal(ResponseWriter writer) {
          writer.writeObject($responseFields[0], createReview.isPresent() ? createReview.get().marshaller() : null);
        }
      };
    }

    @Override
    public String toString() {
      if ($toString == null) {
        $toString = "Data{"
          + "createReview=" + createReview
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
        return this.createReview.equals(that.createReview);
      }
      return false;
    }

    @Override
    public int hashCode() {
      if (!$hashCodeMemoized) {
        int h = 1;
        h *= 1000003;
        h ^= createReview.hashCode();
        $hashCode = h;
        $hashCodeMemoized = true;
      }
      return $hashCode;
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final CreateReview.Mapper createReviewFieldMapper = new CreateReview.Mapper();

      @Override
      public Data map(ResponseReader reader) {
        final CreateReview createReview = reader.readObject($responseFields[0], new ResponseReader.ObjectReader<CreateReview>() {
          @Override
          public CreateReview read(ResponseReader reader) {
            return createReviewFieldMapper.map(reader);
          }
        });
        return new Data(createReview);
      }
    }
  }

  public static class CreateReview {
    static final ResponseField[] $responseFields = {
      ResponseField.forString("__typename", "__typename", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forInt("stars", "stars", null, false, Collections.<ResponseField.Condition>emptyList()),
      ResponseField.forString("commentary", "commentary", null, true, Collections.<ResponseField.Condition>emptyList())
    };

    final @NotNull String __typename;

    final int stars;

    final Optional<String> commentary;

    private transient volatile String $toString;

    private transient volatile int $hashCode;

    private transient volatile boolean $hashCodeMemoized;

    public CreateReview(@NotNull String __typename, int stars, @Nullable String commentary) {
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

    @SuppressWarnings("unchecked")
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
        $toString = "CreateReview{"
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
      if (o instanceof CreateReview) {
        CreateReview that = (CreateReview) o;
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

    public static final class Mapper implements ResponseFieldMapper<CreateReview> {
      @Override
      public CreateReview map(ResponseReader reader) {
        final String __typename = reader.readString($responseFields[0]);
        final int stars = reader.readInt($responseFields[1]);
        final String commentary = reader.readString($responseFields[2]);
        return new CreateReview(__typename, stars, commentary);
      }
    }
  }
}
