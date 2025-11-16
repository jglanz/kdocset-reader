import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
  id("org.jetbrains.intellij.platform")
  id("buildsrc.convention.kotlin-jvm")
  alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
  implementation(project(":kdocset-reader"))
  implementation(libs.sqliteJdbc)
  intellijPlatform {
    
    // create(IntelliJPlatformType.IntellijIdeaUltimate, version)
    // create(IntelliJPlatformType.IntellijIdeaCommunity, "2025.2")
    create(IntelliJPlatformType.CLion, libs.versions.intellijTargetVersion.get())
  }
}

intellijPlatform {
  pluginConfiguration {
    ideaVersion {
      sinceBuild.set("252")
    }
  }
}


kotlin {
  jvmToolchain(21)
}
