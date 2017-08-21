package com.example.mutation_create_review_semantic_naming;

import com.apollographql.apollo.api.InputFieldMarshaller;
import com.apollographql.apollo.api.InputFieldWriter;
import com.apollographql.apollo.api.Mutation;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ResponseWriter;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.api.internal.UnmodifiableMapBuilder;
import com.apollographql.apollo.api.internal.Utils;
import com.example.mutation_create_review_semantic_naming.type.Episode;
import com.example.mutation_create_review_semantic_naming.type.ReviewInput;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class CreateReviewForEpisodeMutation implements Mutation<CreateReviewForEpisodeMutation.Data, Optional<CreateReviewForEpisodeMutation.Data>, CreateReviewForEpisodeMutation.Variables> {
  public static final String OPERATION_DEFINITION = "mutation CreateReviewForEpisode($ep: Episode!, $review: ReviewInput!) {\n"
      + "  createReview(episode: $ep, review: $review) {\n"
      + "    __typename\n"
      + "    stars\n"
      + "    commentary\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private static final OperationName OPERATION_NAME = new OperationName() {
    @Override
    public String name() {
      return "CreateReviewForEpisode";
    }
  };

  private final CreateReviewForEpisodeMutation.Variables variables;

  public CreateReviewForEpisodeMutation(@Nonnull Episode ep, @Nonnull ReviewInput review) {
    Utils.checkNotNull(ep, "ep == null");
    Utils.checkNotNull(review, "review == null");
    variables = new CreateReviewForEpisodeMutation.Variables(ep, review);
  }

  @Override
  public String operationId() {
    return "eb015fa9dd6e305a9228393e61579154ae22719f6a18df6d00b45659ee2e7f7f";
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public Optional<CreateReviewForEpisodeMutation.Data> wrapData(CreateReviewForEpisodeMutation.Data data) {
    return Optional.fromNullable(data);
  }

  @Override
  public CreateReviewForEpisodeMutation.Variables variables() {
    return variables;
  }

  @Override
  public ResponseFieldMapper<CreateReviewForEpisodeMutation.Data> responseFieldMapper() {
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
    private @Nonnull Episode ep;

    private @Nonnull ReviewInput review;

    Builder() {
    }

    public Builder ep(@Nonnull Episode ep) {
      this.ep = ep;
      return this;
    }

    public Builder review(@Nonnull ReviewInput review) {
      this.review = review;
      return this;
    }

    public CreateReviewForEpisodeMutation build() {
      Utils.checkNotNull(ep, "ep == null");
      Utils.checkNotNull(review, "review == null");
      return new CreateReviewForEpisodeMutation(ep, review);
    }
  }

  public static final class Variables extends Operation.Variables {
    private final @Nonnull Episode ep;

    private final @Nonnull ReviewInput review;

    private final transient Map<String, Object> valueMap = new LinkedHashMap<>();

    Variables(@Nonnull Episode ep, @Nonnull ReviewInput review) {
      this.ep = ep;
      this.review = review;
      this.valueMap.put("ep", ep);
      this.valueMap.put("review", review);
    }

    public @Nonnull Episode ep() {
      return ep;
    }

    public @Nonnull ReviewInput review() {
      return review;
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
          writer.writeString("ep", ep.name());
          writer.writeObject("review", review.marshaller());
        }
      };
    }
  }

  public static class Data implements Operation.Data {
    static final ResponseField[] $responseFields = {
      ResponseField.forObject("createReview", "createReview", new UnmodifiableMapBuilder<String, Object>(2)
        .put("review", new UnmodifiableMapBuilder<String, Object>(2)
          .put("kind", "Variable")
          .put("variableName", "review")
        .build())
        .put("episode", new UnmodifiableMapBuilder<String, Object>(2)
          .put("kind", "Variable")
          .put("variableName", "ep")
        .build())
      .build(), true, Collections.<ResponseField.Condition>emptyList())
    };

    final Optional<CreateReview> createReview;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public Data(@Nullable CreateReview createReview) {
      this.createReview = Optional.fromNullable(createReview);
    }

    public Optional<CreateReview> createReview() {
      return this.createReview;
    }

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

    final @Nonnull String __typename;

    final int stars;

    final Optional<String> commentary;

    private volatile String $toString;

    private volatile int $hashCode;

    private volatile boolean $hashCodeMemoized;

    public CreateReview(@Nonnull String __typename, int stars, @Nullable String commentary) {
      this.__typename = Utils.checkNotNull(__typename, "__typename == null");
      this.stars = stars;
      this.commentary = Optional.fromNullable(commentary);
    }

    public @Nonnull String __typename() {
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
