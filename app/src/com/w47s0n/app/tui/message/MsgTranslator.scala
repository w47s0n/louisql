package com.w47s0n.app.tui.message

import com.w47s0n.core.state.State
import com.w47s0n.core.message.Msg
import com.w47s0n.core.message.Startup as StartupMsg
import StartupMsg.QuitApp

// ========== MsgTranslator ==========
// Translates ZMsg (TUI-specific) to core Msg (business logic) based on current state
// This context-aware translation allows the same key to trigger different actions
// depending on which screen the user is on

object MsgTranslator {

  def translate(zMsg: ZMsg, state: State): Option[Msg] = {
    if (state.startup.isAtCreateConnection) {
      translateCreateConnection(zMsg, state)
    } else {
      translateStartup(zMsg, state)
    }
  }

  // ========== Startup Screen ==========
  // Hotkeys: N=New, C=Connect, E=Edit, D=Delete, Q=Quit

  private def translateStartup(zMsg: ZMsg, state: State): Option[Msg] = zMsg match {
    // Hotkeys for menu actions (case-insensitive)
    case ZMsg.Hotkey(c) if c.toLower == 'n' => Some(StartupMsg.EnterCreateConnection)
    case ZMsg.Hotkey(c) if c.toLower == 'c' => None // TODO: Connect to selected connection
    case ZMsg.Hotkey(c) if c.toLower == 'e' => None // TODO: Edit selected connection (needs index)
    case ZMsg.Hotkey(c) if c.toLower == 'd' => None // TODO: Delete selected connection (needs index)
    case ZMsg.Quit                          => Some(QuitApp)

    // Vim-style navigation: j=down, k=up (case-insensitive)
    case ZMsg.Hotkey(c) if c.toLower == 'j' => Some(StartupMsg.SelectNextConnection)
    case ZMsg.Hotkey(c) if c.toLower == 'k' => Some(StartupMsg.SelectPrevConnection)

    // Arrow key navigation
    case ZMsg.NavUp   => Some(StartupMsg.SelectPrevConnection)
    case ZMsg.NavDown => Some(StartupMsg.SelectNextConnection)

    // Confirm could connect to selected connection
    case ZMsg.Confirm => None // TODO: Connect to selected connection

    // Cancel on startup does nothing
    case ZMsg.Cancel => None

    case _ => None
  }

  // ========== Create Connection Screen ==========
  // Text input mode for name/URL fields

  private def translateCreateConnection(zMsg: ZMsg, state: State): Option[Msg] = zMsg match {
    // Cancel (Esc) returns to startup
    case ZMsg.Cancel => Some(StartupMsg.CancelCreateConnection)

    // Enter: cycle fields if incomplete, add connection if complete
    case ZMsg.Confirm =>
      if (state.startup.createConnection.form.isComplete)
        Some(StartupMsg.AddConnection)
      else
        Some(StartupMsg.CycleCreateConnectionField)

    // Text input - Hotkey chars become text input in this context
    case ZMsg.Hotkey(c)    => Some(StartupMsg.CreateConnectionCharInput(c))
    case ZMsg.TextInput(c) => Some(StartupMsg.CreateConnectionCharInput(c))
    case ZMsg.Backspace    => Some(StartupMsg.CreateConnectionBackspace)

    // Quit still works
    case ZMsg.Quit => Some(QuitApp)

    case _ => None
  }
}
