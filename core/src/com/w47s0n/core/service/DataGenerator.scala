package com.w47s0n.core.service

import scala.util.Random

// ========== Data Generation (Business Logic) ==========

object DataGenerator {

  // Type aliases
  type ColumnName = String
  type RowData = Map[ColumnName, String]
  type TableData = (List[RowData], List[ColumnName])

  // Constants
  object Constants {
    val MaxDisplayRows = 20
    val MinColumns = 3
    val MaxColumns = 6
    val MinRows = 5
    val MaxRows = 15
    val AllowedSpecialChars = ",.;:!?'\"()[]{}@#$%^&*-_=+<>/\\|`~"
  }

  private val random = new Random()

  private object SampleData {
    val columnNames: List[ColumnName] = List(
      "id", "name", "email", "status", "amount",
      "date", "category", "priority", "assigned_to", "duration"
    )

    val values: Map[ColumnName, List[String]] = Map(
      "id" -> (1 to 10).map(_.toString).toList,
      "name" -> List("Alice", "Bob", "Carol", "David", "Eve", "Frank", "Grace", "Henry", "Ivy", "Jack"),
      "email" -> List("alice@example.com", "bob@example.com", "carol@example.com", "david@example.com", "eve@example.com"),
      "status" -> List("active", "pending", "completed", "cancelled", "processing"),
      "amount" -> List("$100", "$250", "$500", "$1000", "$1500", "$2000", "$3500", "$5000"),
      "date" -> List("2024-01-15", "2024-02-20", "2024-03-10", "2024-04-05", "2024-05-12"),
      "category" -> List("Sales", "Marketing", "Engineering", "Support", "Finance", "HR"),
      "priority" -> List("High", "Medium", "Low", "Critical", "Normal"),
      "assigned_to" -> List("Team A", "Team B", "Team C", "Team D", "Team E"),
      "duration" -> List("1h", "2h", "30m", "45m", "1.5h", "3h", "4h", "5h")
    )
  }

  def generateRandomTable(): TableData = {
    val columns = selectRandomColumns()
    val rows = generateRows(columns)
    (rows, columns)
  }

  private def selectRandomColumns(): List[ColumnName] = {
    val numColumns = Constants.MinColumns +
      random.nextInt(Constants.MaxColumns - Constants.MinColumns + 1)
    random.shuffle(SampleData.columnNames).take(numColumns)
  }

  private def generateRows(columns: List[ColumnName]): List[RowData] = {
    val numRows = Constants.MinRows +
      random.nextInt(Constants.MaxRows - Constants.MinRows + 1)
    List.fill(numRows)(generateRow(columns))
  }

  private def generateRow(columns: List[ColumnName]): RowData =
    columns.map { col =>
      col -> selectRandomValue(col)
    }.toMap

  private def selectRandomValue(column: ColumnName): String =
    SampleData.values
      .get(column)
      .map(values => values(random.nextInt(values.length)))
      .getOrElse("N/A")
}
