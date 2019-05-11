package com.apollographql.apollo.api;

import java.io.File;

public class FileUpload {
    public String mimetype;
    public File file;

    public FileUpload(String mimetype, File file) {
        this.mimetype = mimetype;
        this.file = file;
    }
}
