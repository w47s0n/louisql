package com.w47s0n.app.tui.view

import layoutz._
import com.w47s0n.core.effect.QueryResult
import com.w47s0n.core.state.*
import Theme._

// Explorer tree on the left; query console over the result grid on the right.
// Uses the whole terminal, unlike the 96-column connection screens.
object WorkspaceView {

  private val MaxCell = 30

  def apply(state: State): Element = {
    val ws       = state.workspace
    val w        = (state.viewport._1 - 2).max(60)
    val h        = (state.viewport._2 - 1).max(16)
    val bodyH    = h - 3 - 2 - state.prompt.size // header, status bar, prompt
    val leftW    = (w * 3 / 10).max(26)
    val rightW   = w - leftW - 3
    val consoleH = (bodyH * 2 / 5).max(4)
    val resultH  = bodyH - consoleH - 1

    val body = tightRow(
      pane(explorer(ws, leftW, bodyH), leftW, bodyH),
      pane(Seq.fill(bodyH)(line(Seq(Seg(" │ ", Faint)), w = 3)), 3, bodyH),
      layout(
        pane(console(ws, rightW, consoleH), rightW, consoleH),
        rule(rightW),
        pane(result(ws, rightW, resultH), rightW, resultH),
      ),
    )
    val status = statusBar(
      left = state.notice.fold(Seq(Seg(" ● ", Accent), Seg(ws.active.getOrElse("—"), Muted)))(n => Seq(Seg(s" $n", Danger))),
      hints = hints(ws.focus),
      w = w,
    )
    layout((Seq(header(s"console · ${ws.active.getOrElse("no connection")}", w), body) ++ state.prompt.map(promptBar(_, w)) :+ status)*)
  }

  private def title(name: String, focused: Boolean, extra: Seq[Seg], w: Int): Element =
    line(Seg(s" $name", if (focused) Accent else Muted, bold = focused) +: extra, w = w)

  // ---------- explorer ----------

  private def explorer(ws: Workspace, w: Int, h: Int): Seq[Element] = {
    val focused = ws.focus == Focus.Explorer
    val rows    = ws.rows
    val visible = h - 1
    val offset  = (ws.selected - visible + 1).max(0) // ponytail: keeps the selection in view, not centred
    title("EXPLORER", focused, Nil, w) +: rows.slice(offset, offset + visible).zipWithIndex.map { (row, i) =>
      val n     = row.node
      val glyph = if (n.isLeaf) "  " else if (n.loading) "… " else if (n.expanded) "▾ " else "▸ "
      val (fg, bold) = n.kind match {
        case Kind.Connection => (Fg, true)
        case Kind.Folder     => (Muted, false)
        case Kind.Message    => (Danger, false)
        case _               => (Fg, false)
      }
      val detail = if (n.kind == Kind.Connection) "" else n.detail
      val segs   = Seq(Seg(" " + "  " * row.depth + glyph, Faint), Seg(n.label, fg, bold = bold), Seg(s"  $detail", Faint))
      val bg     = Option.when(offset + i == ws.selected)(if (focused) SelBg else KeyBg)
      line(segs, bg, w)
    }
  }

  // ---------- console ----------

  private def console(ws: Workspace, w: Int, h: Int): Seq[Element] = {
    val focused = ws.focus == Focus.Console
    val ed      = ws.editor
    val visible = h - 1
    val top     = (ed.row - visible + 1).max(0)
    val textW   = w - 5
    val left    = (ed.col - textW + 1).max(0) // ponytail: one horizontal offset for every line
    val hint    = Seq(Seg("   ^R runs the statement under the cursor", Faint))
    title("CONSOLE", focused, hint, w) +: ed.lines.slice(top, top + visible).zipWithIndex.map { (text, i) =>
      val r      = top + i
      val gutter = Seg(f"${r + 1}%3d  ", if (r == ed.row) Muted else Faint)
      val shown  = text.drop(left)
      if (focused && r == ed.row) {
        val c = ed.col - left
        line(Seq(gutter, Seg(shown.take(c)), Seg(shown.lift(c).fold(" ")(_.toString), Ink, Some(Accent)), Seg(shown.drop(c + 1))), w = w)
      } else line(Seq(gutter, Seg(shown)), w = w)
    }
  }

  // ---------- result ----------

  private def clean(s: String) = s.map(c => if (c == '\n' || c == '\r' || c == '\t') ' ' else c)

  private def result(ws: Workspace, w: Int, h: Int): Seq[Element] = {
    val focused = ws.focus == Focus.Result
    def head(extra: Seg*) = title("RESULT", focused, extra, w)
    ws.result match {
      case ResultPane.Idle => Seq(head(Seg("   nothing run yet", Faint)))
      case ResultPane.Running(sql) => Seq(head(Seg("   running…", Accent)), line(Seq(Seg(s" ${clean(sql)}", Faint)), w = w))
      case ResultPane.Failed(msg) =>
        head(Seg("   error", Danger)) +: clean(msg).grouped(w - 2).toSeq.map(part => line(Seq(Seg(s" $part", Danger)), w = w))
      case ResultPane.Done(QueryResult.Affected(n, ms)) => Seq(head(Seg(s"   $n rows affected · $ms ms", Muted)))
      case ResultPane.Done(QueryResult.Rows(cols, rows, truncated, ms)) =>
        val count   = s"${rows.size}${if (truncated) "+" else ""} rows · $ms ms"
        val from    = if (ws.scrollCol > 0) s" · from column ${ws.scrollCol + 1}" else ""
        val visible = h - 2
        val top     = (ws.scrollRow - visible + 1).max(0)
        val window  = rows.slice(top, top + visible)
        val shown   = cols.indices.drop(ws.scrollCol)
        val widths  = shown.map(c => (window.map(r => clean(r(c)).length) :+ cols(c).length).max.min(MaxCell))
        def cells(values: Int => String, style: (String, Int) => Seg) =
          shown.zip(widths).flatMap((c, cw) => Seq(Seg(" "), style(fit(clean(values(c)), cw), c), Seg(" ")))
        val header = line(cells(cols, (t, _) => Seg(t, Fg, bold = true)), Some(KeyBg), w)
        val body = window.zipWithIndex.map { (r, i) =>
          val selected = focused && top + i == ws.scrollRow
          line(cells(r, (t, c) => Seg(t, if (r(c) == "NULL") Faint else Fg)), Option.when(selected)(SelBg), w)
        }
        Seq(head(Seg(s"   $count$from", Muted)), header) ++ body
    }
  }

  // ---------- chrome ----------

  private def promptBar(p: PasswordPrompt, w: Int): Element =
    line(
      Seq(Seg(" Password for ", Muted), Seg(p.label, bold = true), Seg("  "), Seg("•" * p.input.length), Seg("▏", Accent),
        Seg("    ⏎ connect · Esc cancel", Muted)),
      Some(SelBg), w,
    )

  private def hints(focus: Focus): Seq[(String, String)] = focus match {
    case Focus.Explorer => Seq("↑↓" -> "Move", "←→" -> "Fold", "⏎" -> "Top 100", "r" -> "Refresh", "Tab" -> "Pane", "Esc" -> "Back")
    case Focus.Console  => Seq("^R" -> "Run", "Tab" -> "Pane", "Esc" -> "Back")
    case Focus.Result   => Seq("↑↓" -> "Rows", "PgUp PgDn" -> "Page", "←→" -> "Columns", "Tab" -> "Pane", "Esc" -> "Back")
  }
}
