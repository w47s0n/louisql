package com.w47s0n.app.tui.view

import layoutz._
import com.w47s0n.core.state.{ConnectionUrl, Startup}

object StartupView {

  private object Colors {
    val Logo = Color.BrightWhite
    val Button = Color.BrightYellow
    val ButtonSelected = Color.BrightCyan
    val Hotkey = Color.BrightMagenta
    val ConnectionNormal = Color.White
    val ConnectionSelected = Color.BrightCyan
    val ConnectionSelectedBg = Color.Blue
  }

  private val CatLogo = Seq(
    "                                 ░██                                  ░██ ",
    "                                 ░██                                  ░██ ",
    " ░███████   ░███████   ░██████   ░██  ░██████    ░███████   ░████████ ░██ ",
    "░██        ░██    ░██       ░██  ░██       ░██  ░██        ░██    ░██ ░██ ",
    " ░███████  ░██         ░███████  ░██  ░███████   ░███████  ░██    ░██ ░██ ",
    "       ░██ ░██    ░██ ░██   ░██  ░██ ░██   ░██         ░██ ░██   ░███ ░██ ",
    " ░███████   ░███████   ░█████░██ ░██  ░█████░██  ░███████   ░█████░██ ░██ ",
    "                                                                  ░██     ",
    "                                                                  ░██     "
  )

  def apply(state: Startup): layoutz.Element = {
    val content = if (state.isAtCreateConnection) {
      box("")(
        CreateConnectionView(state.createConnection)
      ).border(Border.Double)
    } else {
      val buttons = List("New", "Connect", "Edit", "Delete", "Quit")
      box("")(
        layout(
          br,
          renderLogo,
          br,
          renderConnectionList(state),
          br,
          renderButtons(buttons),
          br
        )
      ).border(Border.Double)
    }

    layout(
      br(10),  // Add vertical spacing from top
      margin("                                          ")(content)  // Manual horizontal centering (~42 spaces)
    )
  }

  private def renderLogo: layoutz.Element = {
    val logoLines = CatLogo.map(line =>
      ("    " + line).color(Colors.Logo)
    )
    layout(logoLines*)
  }

  private def renderConnectionList(state: Startup): layoutz.Element = {
    if (state.connections.isEmpty) {
      layout("    No saved connections. Press 'n' to create one.".color(Colors.ConnectionNormal))
    } else {
      val header = "    Saved Connections:".color(Colors.Logo)
      val connectionItems = state.connections.zipWithIndex.map { case (conn, idx) =>
        val isSelected = idx == state.selectedIndex
        val prefix = if (isSelected) "  > " else "    "
        val displayText = s"$prefix${conn.name} (${conn.url.value})"

        if (isSelected) {
          displayText.color(Colors.ConnectionSelected)
        } else {
          displayText.color(Colors.ConnectionNormal)
        }
      }
      layout(
        header,
        br,
        layout(connectionItems*)
      )
    }
  }

  private def renderButtons(buttons: Seq[String]): layoutz.Element = {
    val buttonElements = buttons.zipWithIndex.map { case (label, idx) =>
      val firstChar = label.head.toString
      val restChars = label.tail
      val baseColor = Colors.Button

      // Manually build string with ANSI color codes for mixed coloring
      val hotkeyCode = "\u001b[95m\u001b[1m" // Bright magenta + bold
      val resetCode = "\u001b[0m"
      val baseColorCode = "\u001b[93m" // Bright cyan or bright yellow

      val buttonText = s"  $hotkeyCode$firstChar$resetCode$baseColorCode$restChars$resetCode  "

      box("")(buttonText).border(Border.Round)
    }

    row(buttonElements*)
  }
}
