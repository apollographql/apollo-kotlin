package com.apollographql.apollo.api;

import java.io.File;

public class GraphqlUpload {
    public String mimetype;
    public File file;

    public GraphqlUpload(String mimetype, File file) {
        this.mimetype = mimetype;
        this.file = file;
    }
}
