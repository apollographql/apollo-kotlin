package com.apollographql.android.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import okhttp3.internal.io.FileSystem;
import okio.Sink;
import okio.Source;

class NoFileSystem implements FileSystem {
  @Override public Source source(File file) throws FileNotFoundException {
    throw new UnsupportedOperationException();
  }

  @Override public Sink sink(File file) throws FileNotFoundException {
    throw new UnsupportedOperationException();
  }

  @Override public Sink appendingSink(File file) throws FileNotFoundException {
    throw new UnsupportedOperationException();
  }

  @Override public void delete(File file) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override public boolean exists(File file) {
    throw new UnsupportedOperationException();
  }

  @Override public long size(File file) {
    throw new UnsupportedOperationException();
  }

  @Override public void rename(File from, File to) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override public void deleteContents(File directory) throws IOException {
    throw new UnsupportedOperationException();
  }
}
