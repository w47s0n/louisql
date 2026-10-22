package com.w47s0n.core.state

import com.w47s0n.core.state.Startup.Connection

// Three saved MySQL connections, like a user's file would hold
object Fixtures {
  val connections: List[Connection] = List(
    Connection("localhost", ConnectionUrl("mysql://root@localhost:3306/mydb")),
    Connection("production", ConnectionUrl("mysql://app@prod.example.com:3306/proddb")),
    Connection("staging", ConnectionUrl("mysql://app@staging.example.com:3306/stagingdb")),
  )
  val startup: Startup = Startup.empty.copy(connections = connections)
  val state: State     = State.empty.copy(startup = startup)
}
