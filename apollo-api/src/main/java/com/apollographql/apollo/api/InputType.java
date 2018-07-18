package com.apollographql.apollo.api;

import org.jetbrains.annotations.NotNull;

public interface InputType {
  @NotNull InputFieldMarshaller marshaller();
}
