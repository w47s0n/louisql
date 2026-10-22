package com.w47s0n.app.tui.message

import layoutz.Key

// ========== KeyMapper ==========
// Maps layoutz keyboard events to ZMsg (TUI-specific messages)
// This is the first layer of translation: KeyEvent -> ZMsg

object KeyMapper {

  def map(key: Key): Option[ZMsg] = key match {

    // Arrow navigation
    case Key.Up    => Some(ZMsg.NavUp)
    case Key.Down  => Some(ZMsg.NavDown)
    case Key.Left  => Some(ZMsg.NavLeft)
    case Key.Right => Some(ZMsg.NavRight)

    // Action keys
    case Key.Enter     => Some(ZMsg.Confirm)
    case Key.Escape    => Some(ZMsg.Cancel)
    case Key.Backspace => Some(ZMsg.Backspace)
    case Key.Delete    => Some(ZMsg.Delete)
    case Key.Tab       => Some(ZMsg.Tab)
    case Key.Home      => Some(ZMsg.Home)
    case Key.End       => Some(ZMsg.End)
    case Key.PageUp    => Some(ZMsg.PageUp)
    case Key.PageDown  => Some(ZMsg.PageDown)

    // Form actions (layoutz doesn't parse F-keys, so Ctrl combos instead)
    case Key.Ctrl('S') => Some(ZMsg.Save)
    case Key.Ctrl('T') => Some(ZMsg.Test)
    case Key.Ctrl('O') => Some(ZMsg.Connect)
    case Key.Ctrl('R') => Some(ZMsg.Run)

    // Character keys - 'q' is a hotkey on startup but text in the form,
    // so the context-aware translation happens in MsgTranslator
    case Key.Char(c) => Some(ZMsg.CharInput(c)) // Preserve original case for text input

    case _ => None
  }
}
