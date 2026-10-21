package com.w47s0n.app.tui.view

import layoutz._
import com.w47s0n.core.state.Startup
import com.w47s0n.core.state.Startup.ConnectionForm.Database
import Theme._

object CreateConnectionView {

  private val LabelWidth = 14

  def apply(state: Startup): Element = {
    val form   = state.createConnection.form
    val errors = form.errors
    val title  = state.createConnection.editingIndex
      .flatMap(state.connections.lift)
      .fold("New connection")(c => s"Edit connection · ${c.name}")

    layout(
      header(title),
      field("Name", form.connectionName, "Enter connection name", form.activeField == 0),
      message(errors.flatMap(_.name), Danger),
      engine(form.db, form.isSelectingDb),
      message(None, Danger),
      field("Host", form.host, "localhost", form.activeField == 2),
      message(errors.flatMap(_.host), Danger),
      field("Port", form.port.toString, form.db.defaultPort.toString, form.activeField == 3),
      message(errors.flatMap(_.port), Danger),
      message(form.testResult, if (form.testResult.exists(_.startsWith("Failed"))) Danger else Ok),
      statusBar(
        left = Nil,
        hints = Seq("Tab" -> "Next", "↑↓" -> "Engine", "^T" -> "Test", "^O" -> "Connect", "Esc" -> "Cancel"),
        primary = Some("Save" -> "^S"),
      ),
    )
  }

  private def label(name: String, active: Boolean): Seg =
    if (active) Seg(fit(s"▌ $name", LabelWidth), Accent) else Seg(fit(s"  $name", LabelWidth), Muted)

  // Active row is painted like ghostty's highlighted setting; empty values show their placeholder
  private def field(name: String, value: String, placeholder: String, active: Boolean): Element = {
    val cursor = if (active) Seq(Seg("▏", Accent)) else Nil
    val shown  = if (value.isEmpty) cursor :+ Seg(placeholder, Muted) else Seg(value) +: cursor
    line(label(name, active) +: shown, Option.when(active)(SelBg))
  }

  // DBMS choice as pills: the chosen one is filled, accent when the row is active
  private def engine(db: Database, active: Boolean): Element = {
    val pills = Database.values.toSeq.flatMap { d =>
      val text = s" ${d.toString.toLowerCase} "
      val pill =
        if (d != db) Seg(text, Muted)
        else if (active) Seg(text, Ink, Some(Accent), bold = true)
        else Seg(text, Fg, Some(KeyBg))
      Seq(pill, Seg(" "))
    }
    line(label("Engine", active) +: pills, Option.when(active)(SelBg))
  }

  private def message(text: Option[String], color: Color.True): Element =
    line(text.toSeq.map(t => Seg(" " * LabelWidth + t, color)))
}
