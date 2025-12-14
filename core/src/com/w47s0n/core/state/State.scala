package com.w47s0n.core.state 

import com.w47s0n.core.state.Startup

case class State(
  startup: Startup
)

object State {
 val empty: State = State(Startup.empty)
}
