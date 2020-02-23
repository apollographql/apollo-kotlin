package com.apollographql.apollo.api;

import com.apollographql.apollo.api.internal.InputFieldMarshaller;
import org.jetbrains.annotations.NotNull;

public interface InputType {
  @NotNull InputFieldMarshaller marshaller();
}
