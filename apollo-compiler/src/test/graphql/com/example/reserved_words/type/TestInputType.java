package com.example.reserved_words.type;

import com.apollographql.apollo.api.Input;
import com.apollographql.apollo.api.InputFieldMarshaller;
import com.apollographql.apollo.api.InputFieldWriter;
import com.apollographql.apollo.api.internal.Utils;
import java.io.IOException;
import java.lang.Boolean;
import java.lang.Override;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Generated("Apollo GraphQL")
public final class TestInputType {
  private final Input<Boolean> private_;

  TestInputType(Input<Boolean> private_) {
    this.private_ = private_;
  }

  public @Nullable Boolean private_() {
    return this.private_.value;
  }

  public static Builder builder() {
    return new Builder();
  }

  public InputFieldMarshaller marshaller() {
    return new InputFieldMarshaller() {
      @Override
      public void marshal(InputFieldWriter writer) throws IOException {
        if (private_.defined) {
          writer.writeBoolean("private_", private_.value);
        }
      }
    };
  }

  public static final class Builder {
    private Input<Boolean> private_ = Input.absent();

    Builder() {
    }

    public Builder private_(@Nullable Boolean private_) {
      this.private_ = Input.fromNullable(private_);
      return this;
    }

    public Builder private_Input(@Nonnull Input<Boolean> private_) {
      this.private_ = Utils.checkNotNull(private_, "private_ == null");
      return this;
    }

    public TestInputType build() {
      return new TestInputType(private_);
    }
  }
}
