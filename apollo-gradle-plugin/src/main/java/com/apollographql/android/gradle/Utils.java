package com.apollographql.android.gradle;

import java.io.File;

public class Utils {
  public static String capitalize(String s) {
    return s.substring(0, 1).toUpperCase() + s.substring(1);
  }

  public static void deleteDirectory(File directory) {
    if (directory.exists()){
      File[] files = directory.listFiles();
      if (files != null){
        for (File file : files) {
          if (file.isDirectory()) {
            deleteDirectory(file);
          } else {
            file.delete();
          }
        }
      }
    }
    directory.delete();
  }
}
