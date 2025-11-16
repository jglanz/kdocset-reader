package com.github.jglanz.kdocset.reader

import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString

class ZDocsetReader(private val docsetDir: Path) {
  private val contentsDir: Path = docsetDir.resolve("Contents")
  private val resourcesDir: Path = contentsDir.resolve("Resources")
  private val dbPath: Path = resourcesDir.resolve("docSet.dsidx")
  private val documentsDir: Path = resourcesDir.resolve("Documents")
  
  init {
    if (!Files.isRegularFile(dbPath) || !Files.isDirectory(documentsDir)) {
      throw IOException("Invalid docset at ${docsetDir.toAbsolutePath()} - missing database or Documents directory")
    }
  }
  
  fun search(query: String, limit: Int = 50): List<ZDocsetSearchResult> {
    if (query.isBlank()) return emptyList()
    val effectiveLimit = if (limit <= 0) 50 else limit
    val results = mutableListOf<ZDocsetSearchResult>()
    Class.forName("org.sqlite.JDBC")
    val jdbcUrl = "jdbc:sqlite:${dbPath.absolutePathString()}"
    
    DriverManager.getConnection(jdbcUrl).use { conn ->
      configureConnection(conn)
      if (tableExists(
          conn,
          "searchIndex"
        )
      ) { // Legacy/simple schema as used by some docsets
        val sql = buildString {
          append("SELECT name, type, path FROM searchIndex ")
          append("WHERE name LIKE ? ESCAPE '\\' ")
          append("ORDER BY (LOWER(name) = LOWER(?)) DESC, LENGTH(name) ASC ")
          append("LIMIT ?")
        }
        conn.prepareStatement(sql).use { ps ->
          val pattern = "%${escapeLike(query)}%"
          ps.setString(1, pattern)
          ps.setString(2, query)
          ps.setInt(3, effectiveLimit)
          ps.executeQuery().use { rs ->
            while (rs.next()) {
              val name = rs.getString(1)
              val type = rs.getString(2)
              val raw_path = rs.getString(3)
              val anchorIndex = raw_path.indexOf('#')
              val (path, anchor) = if (anchorIndex == -1) Pair(raw_path, "")
              else Pair(
                raw_path.substring(0, anchorIndex),
                raw_path.substring(anchorIndex)
              )
              
              
              val fileUrl = documentsDir.resolve(path)
                .normalize()
                .toUri()
                .let { baseUri ->
                  if (anchor.isBlank()) baseUri.toString() else "$baseUri$anchor"
                }
              results += ZDocsetSearchResult(
                name = name,
                type = type,
                path = path,
                fileUrl = fileUrl
              )
            }
          }
        }
      } else if (tableExists(conn, "ZTOKEN") && tableExists(
          conn,
          "ZFILEPATH"
        )
      ) { // Dash/Zeal CoreData-based schema
        val tokenCols = getColumns(conn, "ZTOKEN")
        val filePathCols = getColumns(conn, "ZFILEPATH")
        val tokenTypeCols = if (tableExists(
            conn,
            "ZTOKENTYPE"
          )
        ) getColumns(conn, "ZTOKENTYPE") else emptySet()
        
        val nameCol = listOf(
          "ZTOKENNAME",
          "ZNAME",
          "NAME"
        ).firstOrNull { it in tokenCols }
          ?: throw IllegalStateException("Could not find token name column in ZTOKEN table")
        val pathCol = listOf(
          "ZPATH",
          "ZFILEPATH",
          "PATH"
        ).firstOrNull { it in filePathCols }
          ?: throw IllegalStateException("Could not find path column in ZFILEPATH table")
        val tokenTypeFkCol = listOf(
          "ZTOKENTYPE",
          "ZTYPE"
        ).firstOrNull { it in tokenCols } // Foreign key from ZTOKEN to ZFILEPATH primary key can vary across docsets
        val fileFkCol = listOf(
          "ZFILE",
          "ZFILEPATH",
          "ZFILEID",
          "ZPATHID",
          "ZPATH"
        ).firstOrNull { it in tokenCols }
        val anchorCol = listOf(
          "ZANCHOR",
          "ZANCHORID",
          "ANCHOR"
        ).firstOrNull { it in tokenCols }
        
        val typeNameCol = listOf(
          "ZTYPENAME",
          "ZNAME",
          "NAME"
        ).firstOrNull { it in tokenTypeCols }
        
        // Build SELECT parts
        val typeExpr = when {
          tokenTypeFkCol != null && tableExists(
            conn,
            "ZTOKENTYPE"
          ) && typeNameCol != null -> "tt.$typeNameCol"
          tokenTypeFkCol != null -> "CAST(t.$tokenTypeFkCol AS TEXT)"
          else -> "''"
        }
        val anchorExpr = if (anchorCol != null) "COALESCE('#' || t.$anchorCol, '')" else "''"
        val pathExpr = if (fileFkCol != null) "fp.$pathCol || $anchorExpr" else anchorExpr
        
        val sql = buildString {
          append("SELECT t.$nameCol AS name, $typeExpr AS type, $pathExpr AS path ")
          append("FROM ZTOKEN t ")
          if (fileFkCol != null) {
            append("LEFT JOIN ZFILEPATH fp ON fp.Z_PK = t.$fileFkCol ")
          }
          if (tokenTypeFkCol != null && tableExists(
              conn,
              "ZTOKENTYPE"
            ) && typeNameCol != null
          ) {
            append("LEFT JOIN ZTOKENTYPE tt ON tt.Z_PK = t.$tokenTypeFkCol ")
          }
          append("WHERE t.$nameCol LIKE ? ESCAPE '\\' ")
          append("ORDER BY (LOWER(t.$nameCol) = LOWER(?)) DESC, LENGTH(t.$nameCol) ASC ")
          append("LIMIT ?")
        }
        
        conn.prepareStatement(sql).use { ps ->
          val pattern = "%${escapeLike(query)}%"
          ps.setString(1, pattern)
          ps.setString(2, query)
          ps.setInt(3, effectiveLimit)
          ps.executeQuery().use { rs ->
            while (rs.next()) {
              val name = rs.getString(1)
              val type = rs.getString(2)
              val path = rs.getString(3)
              val filePath = documentsDir.resolve(path).normalize()
              val fileUrl = filePath.toUri().toString()
              results += ZDocsetSearchResult(
                name = name,
                type = type,
                path = path,
                fileUrl = fileUrl
              )
            }
          }
        }
      } else { // Unknown schema
        throw IOException("Unsupported docset index schema: expected searchIndex or ZTOKEN/ZFILEPATH tables in ${dbPath}")
      }
    }
    
    return results
  }
  
  private fun configureConnection(conn: Connection) { // Enable immutable mode if supported (safe; ignored by older SQLite versions)
    try {
      conn.createStatement().use { st ->
        st.execute("PRAGMA query_only = ON")
        st.execute("PRAGMA case_sensitive_like = OFF")
      }
    } catch (_: Exception) { // ignore pragma failures
    }
  }
  
  private fun escapeLike(input: String): String { // Escape %, _, and \ for SQL LIKE with ESCAPE '\'
    val sb = StringBuilder(input.length)
    for (ch in input) {
      when (ch) {
        '%', '_', '\\' -> sb.append('\\').append(ch)
        else -> sb.append(ch)
      }
    }
    return sb.toString()
  }
  
  private fun tableExists(conn: Connection, table: String): Boolean {
    conn.prepareStatement("SELECT 1 FROM sqlite_master WHERE type='table' AND name=? LIMIT 1")
      .use { ps ->
        ps.setString(1, table)
        ps.executeQuery().use { rs -> return rs.next() }
      }
  }
  
  private fun getColumns(conn: Connection, table: String): Set<String> {
    conn.prepareStatement("PRAGMA table_info($table)").use { ps ->
      ps.executeQuery().use { rs ->
        val cols = mutableSetOf<String>()
        while (rs.next()) {
          cols += rs.getString(2) // name column
        }
        return cols
      }
    }
  }
}
