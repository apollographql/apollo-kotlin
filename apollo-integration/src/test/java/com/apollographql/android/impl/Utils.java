package com.apollographql.android.impl;

import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStreamReader;

public final class Utils {


  private Utils() {
  }

  public static String readFileToString(final Class contextClass,
      final String streamIdentifier) throws IOException {

    InputStreamReader inputStreamReader = null;
    try {
      inputStreamReader = new InputStreamReader(contextClass.getResourceAsStream(streamIdentifier));
      return CharStreams.toString(inputStreamReader);
    } catch (IOException e) {
      throw new IOException();
    }
    finally {
      if (inputStreamReader != null) {
        inputStreamReader.close();
      }
    }
  }


}
