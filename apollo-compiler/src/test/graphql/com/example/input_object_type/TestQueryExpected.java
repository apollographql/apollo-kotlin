package com.example.input_object_type;

import com.apollostack.api.graphql.Mutation;
import com.apollostack.api.graphql.Operation;
import com.example.input_object_type.type.Episode;
import com.example.input_object_type.type.ReviewInput;
import java.lang.IllegalStateException;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TestQuery implements Mutation<TestQuery.Variables> {
  public static final String OPERATION_DEFINITION = "mutation TestQuery($ep: Episode!, $review: ReviewInput!) {\n"
      + "  createReview(episode: $ep, review: $review) {\n"
      + "    stars\n"
      + "    commentary\n"
      + "  }\n"
      + "}";

  public static final String QUERY_DOCUMENT = OPERATION_DEFINITION;

  private final TestQuery.Variables variables;

  public TestQuery(TestQuery.Variables variables) {
    this.variables = variables;
  }

  @Override
  public String queryDocument() {
    return QUERY_DOCUMENT;
  }

  @Override
  public TestQuery.Variables variables() {
    return variables;
  }

  public static final class Variables extends Operation.Variables {
    private final @Nonnull Episode ep;

    private final @Nonnull ReviewInput review;

    Variables(@Nonnull Episode ep, @Nonnull ReviewInput review) {
      this.ep = ep;
      this.review = review;
    }

    public @Nonnull Episode ep() {
      return ep;
    }

    public @Nonnull ReviewInput review() {
      return review;
    }

    public static Builder builder() {
      return new Builder();
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

      public Variables build() {
        if (ep == null) throw new IllegalStateException("ep can't be null");
        if (review == null) throw new IllegalStateException("review can't be null");
        return new Variables(ep, review);
      }
    }
  }

  public interface Data extends Operation.Data {
    @Nullable CreateReview createReview();

    interface CreateReview {
      int stars();

      @Nullable String commentary();
    }
  }
}
