plugins {
  id("buildsrc.convention.kotlin-jvm")
  alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
  implementation(project(":kdocset-utils"))
  implementation(libs.sqliteJdbc)
  testImplementation(libs.junitJupiter)
}