package com.w47s0n.app.tui.message

import com.w47s0n.core.state.State
import com.w47s0n.core.message.Msg
import com.w47s0n.core.message.Startup as StartupMsg
import StartupMsg.QuitApp
import com.w47s0n.core.state.Startup.ConnectionForm.Database

// ========== MsgTranslator ==========
// Translates ZMsg (TUI-specific) to core Msg (business logic) based on current state
// This context-aware translation allows the same key to trigger different actions
// depending on which screen the user is on

object MsgTranslator {

  def translate(zMsg: ZMsg, state: State): Option[Msg] = {
    if (state.startup.isAtCreateConnection) {
      //handle differently, because this case includes typing handling
      translateCreateConnection(zMsg, state)
    } else {
      translateStartup(zMsg, state)
    }
  }

  // ========== Startup Screen ==========
  // Hotkeys: N=New, C=Connect, E=Edit, D=Delete, Q=Quit

  private def translateStartup(zMsg: ZMsg, state: State): Option[Msg] = zMsg match {
    // Hotkeys for menu actions (case-insensitive)
    case ZMsg.CharInput(c) if c.toLower == 'n' => Some(StartupMsg.EnterCreateConnection)
    case ZMsg.CharInput(c) if c.toLower == 'c' => None // TODO: Connect to selected connection
    case ZMsg.CharInput(c) if c.toLower == 'e' => Some(StartupMsg.EditConnection)
    case ZMsg.CharInput(c) if c.toLower == 'd' => Some(StartupMsg.DeleteConnection)
    case ZMsg.CharInput(c) if c.toLower == 'q' => Some(QuitApp)

    // Vim-style navigation: j=down, k=up (case-insensitive)
    case ZMsg.CharInput(c) if c.toLower == 'j' => Some(StartupMsg.SelectNextConnection)
    case ZMsg.CharInput(c) if c.toLower == 'k' => Some(StartupMsg.SelectPrevConnection)

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

  private def navigateDatabase(state: State, isUp: Boolean): Option[Msg] = {
    val currentIndex = state.startup.createConnection.form.db.ordinal
    val newIndex = if (isUp) Math.max(0, currentIndex - 1) else Math.min(Database.values.size - 1, currentIndex + 1)
    Some(StartupMsg.SelectDBMS(Database.fromOrdinal(newIndex)))
  }

  private def translateCreateConnection(zMsg: ZMsg, state: State): Option[Msg] = zMsg match {
    case ZMsg.Confirm => Some(StartupMsg.GoToNextField)

    case ZMsg.NavUp if state.startup.createConnection.form.isSelectingDb =>
      navigateDatabase(state, isUp = true)

    case ZMsg.NavDown if state.startup.createConnection.form.isSelectingDb =>
      navigateDatabase(state, isUp = false)

    case ZMsg.VimKey(nav) if state.startup.createConnection.form.isSelectingDb =>
      navigateDatabase(state, isUp = nav == ZMsg.NavUp)

    case ZMsg.CharInput(c) =>
      val activeValue = state.startup.createConnection.form.activeFieldValue
      Some(StartupMsg.UpdateTextField(activeValue :+ c))

    case ZMsg.Backspace    => Some(StartupMsg.CreateConnectionBackspace)

    // Form actions
    case ZMsg.Save    => Some(StartupMsg.SaveConnection)
    case ZMsg.Test    => Some(StartupMsg.TestConnection)
    case ZMsg.Connect => Some(StartupMsg.ConnectConnection)

    // Cancel (Esc) - return to main screen
    case ZMsg.Cancel => Some(StartupMsg.CancelCreateConnection)

    case _ => None
  }
}
