package com.apollographql.android;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.annotation.Nullable;

import okio.ByteString;

final class Utils {
  private Utils() {
  }

  static <T> T checkNotNull(T reference, @Nullable Object errorMessage) {
    if (reference == null) {
      throw new NullPointerException(String.valueOf(errorMessage));
    }
    return reference;
  }

  static ByteString md5(ByteString source) {
    MessageDigest messageDigest;
    try {
      messageDigest = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError();
    }
    messageDigest.update(source.toByteArray());
    return ByteString.of(messageDigest.digest());
  }
}
