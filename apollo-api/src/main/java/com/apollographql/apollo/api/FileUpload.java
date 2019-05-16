package com.apollographql.apollo.api;

import java.io.File;

public final class FileUpload {
    public final String mimetype;
    public final File file;

    public FileUpload(String mimetype, File file) {
        this.mimetype = mimetype;
        this.file = file;
    }
}
