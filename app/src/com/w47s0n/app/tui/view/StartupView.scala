package com.w47s0n.app.tui.view

import layoutz._
import com.w47s0n.core.state.Startup
import com.w47s0n.core.state.Startup.ConnectionForm
import Theme._

object StartupView {

  private val ListWidth   = 32
  private val DetailWidth = Width - ListWidth - 2
  private val BodyHeight  = 10

  def apply(state: Startup, notice: Option[String] = None): Element =
    if (state.isAtCreateConnection) CreateConnectionView(state)
    else
      layout(
        header("Connections"),
        tightRow(
          pane(listPane(state), ListWidth, BodyHeight),
          pane(Nil, 2, BodyHeight),
          pane(detailPane(state), DetailWidth, BodyHeight),
        ),
        statusBar(
          left = notice.fold(Seq(Seg(" ● ", Accent), Seg(s"${state.connections.size} saved", Muted)))(n => Seq(Seg(s" $n", Danger))),
          hints = Seq("↑↓" -> "Choose", "n" -> "New", "e" -> "Edit", "d" -> "Delete", "q" -> "Quit"),
          primary = Option.when(state.connections.nonEmpty)("Connect" -> "⏎"),
        ),
      )

  private def listPane(state: Startup): Seq[Element] =
    if (state.connections.isEmpty) Seq(line(Seq(Seg(" No connections", Muted)), w = ListWidth))
    else
      sectionLabel("Connections", state.connections.size, ListWidth) +: line(Nil, w = ListWidth) +:
        state.connections.zipWithIndex.map { (conn, i) =>
          val engine = ConnectionForm.fromConnection(conn).db.toString.toLowerCase
          if (i == state.selectedIndex)
            line(Seq(Seg("▌", Accent), Seg(s" ${fit(conn.name, 20)}", bold = true), Seg(engine, Muted)), Some(SelBg), ListWidth)
          else
            line(Seq(Seg(s"  ${fit(conn.name, 20)}"), Seg(engine, Faint)), w = ListWidth)
        }

  private def detailPane(state: Startup): Seq[Element] =
    state.selectedConnection.fold(Seq(line(Seq(Seg("Press n to add one", Muted)), w = DetailWidth))) { conn =>
      val form = ConnectionForm.fromConnection(conn)
      def orDash(s: String) = if (s.isEmpty) "—" else s
      Seq(
        line(Seq(Seg(conn.name, bold = true)), w = DetailWidth),
        line(Seq(Seg(conn.url.value, Muted)), w = DetailWidth),
        line(Nil, w = DetailWidth),
        detail("Engine", form.db.toString, DetailWidth),
        detail("Host", form.host, DetailWidth),
        detail("Port", form.port.toString, DetailWidth),
        detail("User", orDash(form.user), DetailWidth),
        detail("Database", orDash(form.database), DetailWidth),
      )
    }
}
