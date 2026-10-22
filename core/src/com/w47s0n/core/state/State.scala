package com.w47s0n.core.state

import com.w47s0n.core.effect.Effect

enum Screen {
  case Startup, Workspace
}

// Asks for a connection's password, then replays the query that needed it
case class PasswordPrompt(connection: String, label: String, input: String, pending: Effect.Query)

case class State(
  startup: Startup,
  screen: Screen = Screen.Startup,
  workspace: Workspace = Workspace(),
  passwords: Map[String, String] = Map.empty, // session only, never saved
  prompt: Option[PasswordPrompt] = None,
  notice: Option[String] = None,
  viewport: (Int, Int) = (120, 40),
)

object State {
 val empty: State = State(Startup.empty)
}
