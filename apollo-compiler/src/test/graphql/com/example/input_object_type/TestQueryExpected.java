package com.example.input_object_type;

import com.apollostack.api.GraphQLMutation;
import com.apollostack.api.GraphQLOperation;
import java.lang.Override;
import java.lang.String;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TestQuery implements GraphQLMutation<TestQuery.Variables> {
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

  public static final class Variables extends GraphQLOperation.Variables {
    @Nonnull Episode ep;

    @Nonnull ReviewInput review;

    Variables() {
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
      private final Variables variables = new Variables();

      Builder() {
      }

      public Builder ep(@Nonnull Episode ep) {
        variables.ep = ep;
        return this;
      }

      public Builder review(@Nonnull ReviewInput review) {
        variables.review = review;
        return this;
      }

      public Variables build() {
        return variables;
      }
    }
  }

  public interface Data extends GraphQLOperation.Data {
    @Nullable CreateReview createReview();

    interface CreateReview {
      int stars();

      @Nullable String commentary();
    }
  }
}
