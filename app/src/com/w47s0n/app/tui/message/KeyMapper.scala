package com.w47s0n.app.tui.message

import layoutz.*

// ========== KeyMapper ==========
// Maps layoutz keyboard events to ZMsg (TUI-specific messages)
// This is the first layer of translation: KeyEvent -> ZMsg

object KeyMapper {

  def map(key: layoutz.Key): Option[ZMsg] = key match {

    // Arrow navigation
    case ArrowUpKey    => Some(ZMsg.NavUp)
    case ArrowDownKey  => Some(ZMsg.NavDown)
    case ArrowLeftKey  => Some(ZMsg.NavLeft)
    case ArrowRightKey => Some(ZMsg.NavRight)

    // Action keys
    case EnterKey     => Some(ZMsg.Confirm)
    case EscapeKey    => Some(ZMsg.Cancel)
    case BackspaceKey => Some(ZMsg.Backspace)
    case DeleteKey    => Some(ZMsg.Delete)
    case TabKey       => Some(ZMsg.NavDown) // Tab typically moves to next field

    // Character keys - distinguish between hotkeys and text input based on context
    // The context-aware translation happens in MsgTranslator
    case CharKey(c) if c == 'q' || c == 'Q' => Some(ZMsg.Quit)
    case CharKey(c) => Some(ZMsg.Hotkey(c)) // Preserve original case for text input

    case _ => None
  }
}
