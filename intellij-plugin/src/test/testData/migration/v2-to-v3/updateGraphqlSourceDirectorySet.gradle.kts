apollo {
  graphqlSourceDirectorySet.srcDirs += "shared/graphql"
  graphqlSourceDirectorySet.include("**/*.graphql")
  graphqlSourceDirectorySet.exclude("**/schema.graphql")
}
