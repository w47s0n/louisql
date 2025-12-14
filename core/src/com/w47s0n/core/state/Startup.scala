package com.w47s0n.core.state

import com.w47s0n.core.state.Startup.CreateConnection
import java.net.URL
import com.w47s0n.core.state.Startup.Connection


case class Startup(
  isAtCreateConnection: Boolean,
  createConnection: CreateConnection,
  connections: List[Connection],
  selectedIndex: Int
) {
  def selectedConnection: Option[Connection] =
    connections.lift(selectedIndex)

  def selectNext: Startup =
    if (connections.isEmpty) this
    else copy(selectedIndex = (selectedIndex + 1) % connections.size)

  def selectPrev: Startup =
    if (connections.isEmpty) this
    else copy(selectedIndex = (selectedIndex - 1 + connections.size) % connections.size)
}

object Startup {
  case class CreateConnection(nameInput: String, urlInput: String, activeField: Int = 0) {
    val fieldCount = 2
    def cycleField: CreateConnection = copy(activeField = (activeField + 1) % fieldCount)
    def isComplete: Boolean = nameInput.nonEmpty && urlInput.nonEmpty
  }
  case class Connection(name: String, url: URL)

  val empty: Startup = Startup(
    isAtCreateConnection = false,
    CreateConnection("", "", 0),
    connections = List(
      Connection("localhost", new URL("http", "localhost", 5432, "mydb")),
      Connection("production", new URL("http", "prod.example.com", 5432, "proddb")),
      Connection("staging", new URL("http", "staging.example.com", 5432, "stagingdb"))
    ),
    selectedIndex = 0
  )
}


case class DbState(
  inputText: String,
  currentQuery: String,
  results: List[Map[String, String]],
  columns: List[String],
  isLoading: Boolean,
  error: Option[String],
  lastExecutedQuery: Option[String]
) {

  def appendToInput(char: Char): DbState =
    copy(inputText = inputText + char)

  def deleteLastChar: DbState =
    copy(inputText = if (inputText.nonEmpty) inputText.dropRight(1) else "")

  def executeQuery(
    query: String,
    rows: List[Map[String, String]],
    cols: List[String]
  ): DbState =
    copy(
      currentQuery = query,
      results = rows,
      columns = cols,
      isLoading = false,
      error = None,
      lastExecutedQuery = Some(query),
      inputText = ""
    )
}

object DbState {
  val empty: DbState = DbState(
    inputText = "",
    currentQuery = "",
    results = List.empty,
    columns = List.empty,
    isLoading = false,
    error = None,
    lastExecutedQuery = None
  )
}
