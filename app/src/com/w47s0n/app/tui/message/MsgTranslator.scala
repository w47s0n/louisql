package com.w47s0n.app.tui.message

import com.w47s0n.core.state.{Focus, Screen, State}
import com.w47s0n.core.message.{Msg, WorkspaceMsg}
import com.w47s0n.core.message.Startup as StartupMsg
import com.w47s0n.core.message.WorkspaceMsg.EditKey
import StartupMsg.QuitApp
import com.w47s0n.core.state.Startup.ConnectionForm.Database

// ========== MsgTranslator ==========
// Translates ZMsg (TUI-specific) to core Msg (business logic) based on current state
// This context-aware translation allows the same key to trigger different actions
// depending on which screen the user is on

object MsgTranslator {

  def translate(zMsg: ZMsg, state: State): Option[Msg] =
    if (state.prompt.isDefined) translatePrompt(zMsg) // a prompt takes every key
    else state.screen match {
      case Screen.Workspace => translateWorkspace(zMsg, state)
      case Screen.Startup if state.startup.isAtCreateConnection => translateCreateConnection(zMsg, state) // includes typing
      case Screen.Startup => translateStartup(zMsg, state)
    }

  // ========== Startup Screen ==========
  // Hotkeys: N=New, C=Connect, E=Edit, D=Delete, Q=Quit

  private def translateStartup(zMsg: ZMsg, state: State): Option[Msg] = zMsg match {
    // Hotkeys for menu actions (case-insensitive)
    case ZMsg.CharInput(c) if c.toLower == 'n' => Some(StartupMsg.EnterCreateConnection)
    case ZMsg.CharInput(c) if c.toLower == 'c' => Some(StartupMsg.OpenWorkspace)
    case ZMsg.CharInput(c) if c.toLower == 'e' => Some(StartupMsg.EditConnection)
    case ZMsg.CharInput(c) if c.toLower == 'd' => Some(StartupMsg.DeleteConnection)
    case ZMsg.CharInput(c) if c.toLower == 'q' => Some(QuitApp)

    // Vim-style navigation: j=down, k=up (case-insensitive)
    case ZMsg.CharInput(c) if c.toLower == 'j' => Some(StartupMsg.SelectNextConnection)
    case ZMsg.CharInput(c) if c.toLower == 'k' => Some(StartupMsg.SelectPrevConnection)

    // Arrow key navigation
    case ZMsg.NavUp   => Some(StartupMsg.SelectPrevConnection)
    case ZMsg.NavDown => Some(StartupMsg.SelectNextConnection)

    case ZMsg.Confirm => Some(StartupMsg.OpenWorkspace)

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
    case ZMsg.Confirm | ZMsg.Tab => Some(StartupMsg.GoToNextField)

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

  // ========== Password prompt ==========

  private def translatePrompt(zMsg: ZMsg): Option[Msg] = zMsg match {
    case ZMsg.CharInput(c) => Some(Msg.PromptInput(c))
    case ZMsg.Backspace    => Some(Msg.PromptBackspace)
    case ZMsg.Confirm      => Some(Msg.PromptSubmit)
    case ZMsg.Cancel       => Some(Msg.PromptCancel)
    case _                 => None
  }

  // ========== Workspace ==========
  // Tab, Esc and ^R work everywhere; the rest depends on the focused pane

  private def translateWorkspace(zMsg: ZMsg, state: State): Option[Msg] = zMsg match {
    case ZMsg.Tab    => Some(WorkspaceMsg.FocusNext)
    case ZMsg.Cancel => Some(WorkspaceMsg.Close)
    case ZMsg.Run    => Some(WorkspaceMsg.Run)
    case _ =>
      state.workspace.focus match {
        case Focus.Explorer => translateExplorer(zMsg)
        case Focus.Console  => translateConsole(zMsg)
        case Focus.Result   => translateResult(zMsg, page = (state.viewport._2 / 2).max(1))
      }
  }

  private def translateExplorer(zMsg: ZMsg): Option[Msg] = zMsg match {
    case ZMsg.NavUp | ZMsg.CharInput('k')    => Some(WorkspaceMsg.TreeUp)
    case ZMsg.NavDown | ZMsg.CharInput('j')  => Some(WorkspaceMsg.TreeDown)
    case ZMsg.NavRight | ZMsg.CharInput('l') => Some(WorkspaceMsg.TreeExpand)
    case ZMsg.NavLeft | ZMsg.CharInput('h')  => Some(WorkspaceMsg.TreeCollapse)
    case ZMsg.Confirm                        => Some(WorkspaceMsg.TreeActivate)
    case ZMsg.CharInput('r')                 => Some(WorkspaceMsg.TreeRefresh)
    case _                                   => None
  }

  private def translateConsole(zMsg: ZMsg): Option[Msg] = (zMsg match {
    case ZMsg.CharInput(c) => Some(EditKey.Insert(c))
    case ZMsg.Confirm      => Some(EditKey.Newline)
    case ZMsg.Backspace    => Some(EditKey.Backspace)
    case ZMsg.Delete       => Some(EditKey.Delete)
    case ZMsg.NavLeft      => Some(EditKey.Left)
    case ZMsg.NavRight     => Some(EditKey.Right)
    case ZMsg.NavUp        => Some(EditKey.Up)
    case ZMsg.NavDown      => Some(EditKey.Down)
    case ZMsg.Home         => Some(EditKey.Home)
    case ZMsg.End          => Some(EditKey.End)
    case _                 => None
  }).map(WorkspaceMsg.Edit(_))

  private def translateResult(zMsg: ZMsg, page: Int): Option[Msg] = zMsg match {
    case ZMsg.NavUp | ZMsg.CharInput('k')    => Some(WorkspaceMsg.ScrollResult(-1, 0))
    case ZMsg.NavDown | ZMsg.CharInput('j')  => Some(WorkspaceMsg.ScrollResult(1, 0))
    case ZMsg.PageUp                         => Some(WorkspaceMsg.ScrollResult(-page, 0))
    case ZMsg.PageDown                       => Some(WorkspaceMsg.ScrollResult(page, 0))
    case ZMsg.NavLeft | ZMsg.CharInput('h')  => Some(WorkspaceMsg.ScrollResult(0, -1))
    case ZMsg.NavRight | ZMsg.CharInput('l') => Some(WorkspaceMsg.ScrollResult(0, 1))
    case _                                   => None
  }
}
