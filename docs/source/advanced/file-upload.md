---
title: File upload 
---

Apollo Android supports file uploading over [graphql-multipart-request-spec](https://github.com/jaydenseric/graphql-multipart-request-spec).

You need to define this mapping in your build.gradle file.

```gradle
apollo {
  customTypeMapping = [
    "Upload" : "com.apollographql.apollo.api.FileUpload"
  ]
}
```

**Note** You don't need to register custom type adapter for `FileUpload`.

In this example, the GraphQL schema uses custom scalar type named `Upload` for file upload. 
Change it to match your GraphQL schema.

Create graphql mutation.

```
mutation SingleUpload($file: Upload!) {
  singleUpload(file: $file) {
    id
    path
    filename
    mimetype
  }
}
``` 

Call your mutation with mimetype and a valid `File`.

```java
  mutationSingle = SingleUploadMutation.builder()
        .file(new FileUpload("image/jpg", new File("/my/image.jpg")))
        .build();
```
