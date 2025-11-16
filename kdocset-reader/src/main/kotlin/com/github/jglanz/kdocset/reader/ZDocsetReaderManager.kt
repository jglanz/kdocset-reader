package com.github.jglanz.kdocset.reader

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Optional
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class ZDocsetReaderManager(parentFolder: String) {
  private val parentPath: Path = Paths.get(parentFolder)
  
  init {
    require(Files.exists(parentPath) && parentPath.isDirectory()) {
      "Parent folder '$parentFolder' does not exist or is not a directory"
    }
  }
  
  fun list(): List<String> {
    if (!Files.isDirectory(parentPath)) return emptyList()
    val results = mutableListOf<String>()
    Files.newDirectoryStream(parentPath).use { stream ->
      for (child in stream) {
        if (resolveDocsetPath(child).isPresent) {
          results += child.fileName.toString()
        }
      }
    }
    return results.sorted()
  }
  
  fun get(name: String): ZDocsetReader {
    val docsetPath = resolveDocsetPath(parentPath.resolve(name))
    if (docsetPath.isEmpty) throw IOException("Docset '$name' not found or invalid")
    
    return ZDocsetReader(docsetPath.get())
  }
  
  private fun resolveDocsetPath(dir: Path): Optional<Path> {
    if (!Files.isDirectory(dir)) return Optional.empty()
    var docsetDir = dir
    if (!docsetDir.name.endsWith(".docset")) {
      docsetDir = docsetDir.resolve(dir.name + ".docset")
      if (!Files.isDirectory(docsetDir)) return Optional.empty()
    }
    val contents = docsetDir.resolve("Contents")
    val resources = contents.resolve("Resources")
    val db = resources.resolve("docSet.dsidx")
    val docs = resources.resolve("Documents")
    return if (Files.isRegularFile(db) && Files.isDirectory(docs)) Optional.of(
      docsetDir
    )
    else Optional.empty()
  }
  
  
}
