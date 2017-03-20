package com.example.input_object_type;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Mutation;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import com.apollographql.android.api.graphql.internal.Optional;
import com.apollographql.android.api.graphql.util.UnmodifiableMapBuilder;
import com.apollographql.android.api.graphql.util.Utils;
import com.example.input_object_type.type.Episode;
import com.example.input_object_type.type.ReviewInput;
import java.io.IOException;
import java.lang.IllegalStateException;
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
public final class TestQuery implements Mutation<TestQuery.Data, Optional<TestQuery.Data>, TestQuery.Variables> {
  public static final String OPERATION_DEFINITION = "mutation TestQuery($ep: Episode!, $review: ReviewInput!) {\n"
      + "  createReview(episode: $ep, review: $review) {\n"
      + "    __typename\n"
      + "    stars\n"
      + "    commentary\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private final TestQuery.Variables variables;

  public TestQuery(@Nonnull Episode ep, @Nonnull ReviewInput review) {
    Utils.checkNotNull(ep, "ep == null");
    Utils.checkNotNull(review, "review == null");
    variables = new TestQuery.Variables(ep, review);
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

    public TestQuery build() {
      if (ep == null) throw new IllegalStateException("ep can't be null");
      if (review == null) throw new IllegalStateException("review can't be null");
      return new TestQuery(ep, review);
    }
  }

  public static class Data implements Operation.Data {
    private final Optional<CreateReview> createReview;

    public Data(@Nullable CreateReview createReview) {
      this.createReview = Optional.fromNullable(createReview);
    }

    public Optional<CreateReview> createReview() {
      return this.createReview;
    }

    @Override
    public String toString() {
      return "Data{"
        + "createReview=" + createReview
        + "}";
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o instanceof Data) {
        Data that = (Data) o;
        return ((this.createReview == null) ? (that.createReview == null) : this.createReview.equals(that.createReview));
      }
      return false;
    }

    @Override
    public int hashCode() {
      int h = 1;
      h *= 1000003;
      h ^= (createReview == null) ? 0 : createReview.hashCode();
      return h;
    }

    public static class CreateReview {
      private final int stars;

      private final Optional<String> commentary;

      public CreateReview(int stars, @Nullable String commentary) {
        this.stars = stars;
        this.commentary = Optional.fromNullable(commentary);
      }

      public int stars() {
        return this.stars;
      }

      public Optional<String> commentary() {
        return this.commentary;
      }

      @Override
      public String toString() {
        return "CreateReview{"
          + "stars=" + stars + ", "
          + "commentary=" + commentary
          + "}";
      }

      @Override
      public boolean equals(Object o) {
        if (o == this) {
          return true;
        }
        if (o instanceof CreateReview) {
          CreateReview that = (CreateReview) o;
          return this.stars == that.stars
           && ((this.commentary == null) ? (that.commentary == null) : this.commentary.equals(that.commentary));
        }
        return false;
      }

      @Override
      public int hashCode() {
        int h = 1;
        h *= 1000003;
        h ^= stars;
        h *= 1000003;
        h ^= (commentary == null) ? 0 : commentary.hashCode();
        return h;
      }

      public static final class Mapper implements ResponseFieldMapper<CreateReview> {
        final Field[] fields = {
          Field.forInt("stars", "stars", null, false),
          Field.forString("commentary", "commentary", null, true)
        };

        @Override
        public CreateReview map(ResponseReader reader) throws IOException {
          final int stars = reader.read(fields[0]);
          final String commentary = reader.read(fields[1]);
          return new CreateReview(stars, commentary);
        }
      }
    }

    public static final class Mapper implements ResponseFieldMapper<Data> {
      final CreateReview.Mapper createReviewFieldMapper = new CreateReview.Mapper();

      final Field[] fields = {
        Field.forObject("createReview", "createReview", new UnmodifiableMapBuilder<String, Object>(2)
          .put("review", new UnmodifiableMapBuilder<String, Object>(2)
            .put("kind", "Variable")
            .put("variableName", "review")
          .build())
          .put("episode", new UnmodifiableMapBuilder<String, Object>(2)
            .put("kind", "Variable")
            .put("variableName", "ep")
          .build())
        .build(), true, new Field.ObjectReader<CreateReview>() {
          @Override public CreateReview read(final ResponseReader reader) throws IOException {
            return createReviewFieldMapper.map(reader);
          }
        })
      };

      @Override
      public Data map(ResponseReader reader) throws IOException {
        final CreateReview createReview = reader.read(fields[0]);
        return new Data(createReview);
      }
    }
  }
}
