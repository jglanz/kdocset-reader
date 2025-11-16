@file:Suppress("UnstableApiUsage")

import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

plugins {
  id("org.jetbrains.intellij.platform.settings") version "2.10.4"
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
  repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
  
  repositories {
    mavenCentral()
    intellijPlatform {
      defaultRepositories()
    }
  }
}

include(":kdocset-reader")
include(":kdocset-utils")
include(":kdocset-reader-idea-plugin")

rootProject.name = "kdocset-reader-project"