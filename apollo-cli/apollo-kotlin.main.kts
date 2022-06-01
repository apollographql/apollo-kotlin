#!/usr/bin/env kotlin

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:Repository("file://Users/mbonnin/.m2/repository")

@file:DependsOn("com.apollographql.apollo3:apollo-cli:3.3.1-SNAPSHOT")

import com.apollographql.apollo3.cli.main

main(args)