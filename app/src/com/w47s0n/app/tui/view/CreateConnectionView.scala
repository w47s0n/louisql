package com.w47s0n.app.tui.view

import layoutz._
import layoutz.Element
import com.w47s0n.core.state.Startup.CreateConnection

object CreateConnectionView {

  private object Colors {
    val Hotkey = Color.BrightYellow
    val Button = Color.White
  }

  def apply(state: CreateConnection): Element = {
    layout(
      br,
      textInput("Name", state.form.nameInput, placeholder = "Enter connection name", active = state.form.activeField == 0),
      br,
      textInput("URL", state.form.urlInput, placeholder = "postgres://localhost:5432/mydb", active = state.form.activeField == 1),
      br(12),
      renderButtons
    )
  }

  private def renderButtons: Element = {
    val buttons = List(
      ("F1", "Save"),
      ("F2", "Test"),
      ("F3", "Connect"),
      ("Esc", "Cancel")
    )

    val buttonElements = buttons.map { case (hotkey, label) =>
      val hotkeyText = hotkey.color(Colors.Hotkey)
      val labelText = s" $label".color(Colors.Button)
      box("")(row(hotkeyText, labelText)).border(Border.Round)
    }

    row(buttonElements*)
  }
}
