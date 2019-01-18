package com.apollographql.apollo.api;

import java.io.File;
import java.net.URLConnection;

import okhttp3.MediaType;


public class GraphqlUpload {
    public File file;

    public GraphqlUpload(File f) {
        file = f;
    }

    public MediaType getMimetype() {
        return MediaType.parse(URLConnection.guessContentTypeFromName(file.getName()));
    }
}
