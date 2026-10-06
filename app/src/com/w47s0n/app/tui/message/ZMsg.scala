package com.w47s0n.app.tui.message

// ========== ZMsg (Layoutz Message) ==========
// TUI-specific messages that capture UI events (key presses, etc.)
// These are translated to core Msg based on the current application context

sealed trait ZMsg

object ZMsg {
  // Navigation
  case object NavUp extends ZMsg
  case object NavDown extends ZMsg
  case object NavLeft extends ZMsg
  case object NavRight extends ZMsg

  object VimKey {
    def unapply(msg: ZMsg): Option[NavUp.type | NavDown.type] = msg match {
      case CharInput('j') => Some(NavDown)
      case CharInput('k') => Some(NavUp)
      case _ => None
    }
  }

  // Actions
  case object Confirm extends ZMsg
  case object Cancel extends ZMsg
  case object Delete extends ZMsg

  case class CharInput(char: Char) extends ZMsg // this can be converted to HostKey/typing
  case object Backspace extends ZMsg

  // Hotkeys (direct action shortcuts)
  // case class Hotkey(char: Char) extends ZMsg

  // Application
  case object Save extends ZMsg
  case object Test extends ZMsg
  case object Connect extends ZMsg
}
