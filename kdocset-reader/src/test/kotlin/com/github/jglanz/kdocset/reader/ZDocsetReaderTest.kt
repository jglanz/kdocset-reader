package com.github.jglanz.kdocset.reader

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class ZDocsetReaderTest {

  private fun prepareTempDocset(): Path {
    val tempDir = Files.createTempDirectory("kdocset-test-")
    // Build the expected docset layout
    val contents = tempDir.resolve("Contents")
    val resources = contents.resolve("Resources")
    val documents = resources.resolve("Documents")
    Files.createDirectories(documents)

    // Copy the test dsidx into place as docSet.dsidx
    val dsidxUrl = checkNotNull(javaClass.getResource("/C++docset.dsidx")) { "Missing test resource C++docset.dsidx" }
    val dsidxPath = Path.of(dsidxUrl.toURI())
    val targetDb = resources.resolve("docSet.dsidx")
    Files.createDirectories(targetDb.parent)
    Files.copy(dsidxPath, targetDb, StandardCopyOption.REPLACE_EXISTING)

    return tempDir
  }

  @Test
  fun searchStdPrint() {
    val docsetRoot = prepareTempDocset()
    val reader = ZDocsetReader(docsetRoot)

    val results = reader.search("std::print", limit = 50)

    assertFalse(results.isEmpty(), "Expected results for std::print")
    assertTrue(
      results.any { it.name.contains("std::print", ignoreCase = true) },
      "At least one result should contain 'std::print' in the name"
    )
    // Basic sanity checks about fileUrl composition
    assertTrue(results.all { it.fileUrl.startsWith("file:") }, "fileUrl should be a file: URL")
  }

  @Test
  fun searchStdVector() {
    val docsetRoot = prepareTempDocset()
    val reader = ZDocsetReader(docsetRoot)

    val results = reader.search("std::vector", limit = 50)

    assertFalse(results.isEmpty(), "Expected results for std::vector")
    assertTrue(
      results.any { it.name.contains("std::vector", ignoreCase = true) },
      "At least one result should contain 'std::vector' in the name"
    )
    assertTrue(results.all { it.fileUrl.startsWith("file:") }, "fileUrl should be a file: URL")
  }
}
