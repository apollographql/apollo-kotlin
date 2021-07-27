package com.apollographql.apollo3.gradle.api

import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Project

object AndroidProject {
  fun onEachVariant(project: Project, withTestVariants: Boolean = false, block: (BaseVariant) -> Unit) {
    project.applicationVariants?.all {
      block(it)
    }
    project.libraryVariants?.all {
      block(it)
    }

    if (withTestVariants) {
      project.testVariants?.all {
        block(it)
      }
      project.unitTestVariants?.all {
        block(it)
      }
    }
  }
}
