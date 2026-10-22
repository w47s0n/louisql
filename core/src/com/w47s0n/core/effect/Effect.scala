package com.w47s0n.core.effect

import com.w47s0n.core.state.Startup.Connection

// What a query sends back. Cells are already strings: the grid only ever shows text.
enum QueryResult {
  case Rows(columns: Vector[String], rows: Vector[Vector[String]], truncated: Boolean, millis: Long)
  case Affected(count: Int, millis: Long)
}

// Who asked for a query, so the answer lands in the right place.
enum Purpose {
  case Console
  case Children(path: Vector[String])
  case Test
}

// Side effects the core asks for. The frontend runs them and answers with a Msg;
// the core itself never touches the network or the disk.
enum Effect {
  case Query(conn: Connection, password: Option[String], sql: String, params: Seq[String], purpose: Purpose)
  case SaveConnections(text: String)
  case LoadConnections
}
