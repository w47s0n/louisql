package com.w47s0n.app.tui.view

import layoutz._
import layoutz.Element
import com.w47s0n.core.state.Startup.CreateConnection
import com.w47s0n.core.state.Startup.ConnectionForm

object CreateConnectionView {

  private object Colors {
    val Hotkey = Color.BrightYellow
    val Button = Color.White
    val Error  = Color.Red
    val Info   = Color.Green
  }

  private def error(msg: Option[String]): Element =
    msg.fold(br)(m => s"  $m".color(Colors.Error))

  def apply(state: CreateConnection): Element = {
    val db     = state.form.db
    val errors = state.form.errors
    layout(
      br,
      textInput("Name", state.form.connectionName, placeholder = "Enter connection name", active = state.form.activeField == 0),
      error(errors.flatMap(_.name)),
      singleChoice("DBMS", ConnectionForm.Database.values.map(_.toString), db.ordinal, state.form.activeField == 1),
      br,
      textInput("host", state.form.host, placeholder = "localhost", active = state.form.activeField == 2),
      error(errors.flatMap(_.host)),
      textInput("port", state.form.port.toString, placeholder = db.defaultPort.toString, active = state.form.activeField == 3),
      error(errors.flatMap(_.port)),
      state.form.testResult.fold(br)(r => s"  $r".color(Colors.Info)),
      br(10),
      buttons
    )
  }

  private val buttons: Element = {
    val buttons = List(
      ("^S", "Save"),
      ("^T", "Test"),
      ("^O", "Connect"),
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
