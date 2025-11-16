# kdocset-reader

Create a library for searching (via sqlite) `.docset` folder(s)
(i.e. the same docsets used in Dash & Zeal).

Generate classes in package `com.github.jglanz.kdocset.reader`

## `ZDocsetReaderManager`

`ZDocsetReaderManager` is instantiated with the path to the docsets parent folder.
The parent folder contains one or more docset folders.

`.list()` returns a list of subfolders (represented as the base name of the docset folder name, i.e. `java.docset` would
simply be represented as `java` in the list returned)
that contain valid docsets.

`.get(name)` returns a `ZDocsetReader` for the given docset name.

## `ZDocsetReader`

`ZDocsetReader` is instantiated with a docset folder path.
`.search(query, limit)` performs a search on the docset's sqlite database for the given query string.

It returns a list of `ZDocsetSearchResult` objects.

## `ZDocsetSearchResult`

Represents a single search result. Contains the following properties:

* `name`: the name of the docset entry
* `type`: the type of the docset entry (e.g. `Class`, `Function`, `Property`, etc.)
* `path`: the path to the docset entry in the docset's sqlite database to the html file contained in the `Documents`
  folder of the docset.
* `fileUrl`: a url representation of the `path` property.

## Followup

Everything in the previous task appears correct accept for the fact that that the presumed database schema is completely
incorrect. These docset sqlite databases are used by Zeal & Dash. So there is for example tables named `ZFILEPATH` &
`ZTOKEN` (as well as many more). Please fix the queries used to search,etc. An example database is
`/Users/jglanz/Library/Application Support/Dash/DocSets/C++/C++.docset/Contents/Resources/docSet.dsidx`

# IntelliJ Plugin

Create new intellij platform plugin module in this project with the name `kdocset-reader-idea-plugin`.  Only target the latest `2025.2+` IDEs.

- Add a tool window named `Docsets` that allows the user to select a docset from the list of available docsets (using `ZDocsetReaderManager`).
- Add a JCEF browser component to the tool window that displays the html file for the selected docset entry.
- Add a search field to the tool window that allows the user to search the selected docset.
- Add a settings page to the plugin that allows the user to specify the path to the docsets parent folder.
- On symbol hover and quick documentation, display a documentation link to a docset entry if the symbol is from a docset.