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

  // Actions
  case object Confirm extends ZMsg
  case object Cancel extends ZMsg
  case object Delete extends ZMsg

  // Text input
  case class TextInput(char: Char) extends ZMsg
  case object Backspace extends ZMsg

  // Hotkeys (direct action shortcuts)
  case class Hotkey(char: Char) extends ZMsg

  // Application
  case object Quit extends ZMsg
}
