package com.w47s0n.app.tui.view

import layoutz._

// Palette and building blocks shared by every screen, after ghostty-config's chrome:
// one-line header, thin rules, filled selection, keycap status bar.
// ponytail: fixed dark palette, derive it from the terminal's colours if light terminals matter
object Theme {
  val Width = 96

  val Fg     = Color.True(0xd8, 0xd8, 0xe0)
  val Muted  = Color.True(0x7c, 0x7c, 0x85)
  val Faint  = Color.True(0x44, 0x44, 0x4a)
  val Accent = Color.True(0x7a, 0xa2, 0xf7)
  val SelBg  = Color.True(0x2f, 0x3a, 0x52)
  val KeyBg  = Color.True(0x2a, 0x2a, 0x31)
  val Danger = Color.True(0xf7, 0x76, 0x8e)
  val Ok     = Color.True(0x9e, 0xce, 0x6a)
  val Ink    = Color.True(0x10, 0x10, 0x14) // text on an accent fill

  // A run of text with its own colours. Each segment is coloured on its own, so an
  // inner reset never punches a hole in a painted row.
  case class Seg(text: String, fg: Color.True = Fg, bg: Option[Color.True] = None, bold: Boolean = false)

  def fit(s: String, w: Int): String = s.take(w).padTo(w, ' ')

  // ponytail: raw SGR because layoutz 0.8.0's colorBg maps true colours to basic ANSI codes
  private def sgr(s: Seg, rowBg: Option[Color.True]): String = {
    val codes = Seq(s"38;2;${s.fg.r};${s.fg.g};${s.fg.b}") ++
      s.bg.orElse(rowBg).map(c => s"48;2;${c.r};${c.g};${c.b}") ++
      Option.when(s.bold)("1")
    codes.map(c => s"\u001b[${c}m").mkString + s.text + "\u001b[0m"
  }

  // One line of exactly w cells, cut or padded; bg paints every segment that has no colour of its own.
  def line(segs: Seq[Seg], bg: Option[Color.True] = None, w: Int = Width): Element = {
    val fitted = segs.foldLeft((Vector.empty[Seg], w)) { case ((acc, room), s) =>
      (acc :+ s.copy(text = s.text.take(room)), (room - s.text.length).max(0))
    }._1.filter(_.text.nonEmpty)
    val used = fitted.map(_.text.length).sum
    val all  = if (used < w) fitted :+ Seg(" " * (w - used)) else fitted
    Text(all.map(sgr(_, bg)).mkString)
  }

  def rule(w: Int = Width): Element = line(Seq(Seg("─" * w, Faint)), w = w)

  def header(location: String, w: Int = Width): Element = layout(
    line(Seq(Seg(" scalasql", bold = true), Seg(s"  ·  $location", Muted)), w = w),
    rule(w),
    line(Nil, w = w),
  )

  // Exactly h lines of w cells, so panes placed side by side stay aligned
  def pane(lines: Seq[Element], w: Int, h: Int): Element =
    layout((lines.take(h) ++ Seq.fill(h - lines.size)(line(Nil, w = w)))*)

  // Uppercase label with a right-aligned count, e.g. "CONNECTIONS        3"
  def sectionLabel(name: String, count: Int, w: Int): Element = {
    val n = count.toString
    line(Seq(Seg(fit(s" ${name.toUpperCase}", w - n.length - 1), Muted), Seg(n, Muted)), w = w)
  }

  def detail(label: String, value: String, w: Int): Element =
    line(Seq(Seg(fit(label, 10), Muted), Seg(value)), w = w)

  def keycap(key: String): Seg = Seg(s" $key ", Fg, Some(KeyBg))

  def statusBar(left: Seq[Seg], hints: Seq[(String, String)], primary: Option[(String, String)] = None, w: Int = Width): Element = {
    val hintSegs    = hints.flatMap((key, label) => Seq(keycap(key), Seg(s" $label  ", Muted)))
    val primarySegs = primary.toSeq.map((label, key) => Seg(s" $label $key ", Ink, Some(Accent), bold = true))
    val right       = hintSegs ++ primarySegs
    val gap         = w - (left ++ right).map(_.text.length).sum
    layout(rule(w), line((left :+ Seg(" " * gap.max(1))) ++ right, w = w))
  }
}
