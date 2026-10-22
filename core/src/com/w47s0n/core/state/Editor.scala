package com.w47s0n.core.state

// Multi-line text buffer for the query console. row/col is the cursor; col can sit
// one past the last character (end of line).
case class Editor(lines: Vector[String] = Vector(""), row: Int = 0, col: Int = 0) {

  private def line = lines(row)

  def insert(c: Char): Editor =
    copy(lines = lines.updated(row, line.patch(col, c.toString, 0)), col = col + 1)

  def newline: Editor = {
    val (before, after) = line.splitAt(col)
    copy(lines = lines.patch(row, Seq(before, after), 1), row = row + 1, col = 0)
  }

  def backspace: Editor =
    if (col > 0) copy(lines = lines.updated(row, line.patch(col - 1, "", 1)), col = col - 1)
    else if (row > 0) {
      val prev = lines(row - 1)
      copy(lines = lines.patch(row - 1, Seq(prev + line), 2), row = row - 1, col = prev.length)
    } else this

  def delete: Editor =
    if (col < line.length) copy(lines = lines.updated(row, line.patch(col, "", 1)))
    else if (row < lines.size - 1) copy(lines = lines.patch(row, Seq(line + lines(row + 1)), 2))
    else this

  def left: Editor =
    if (col > 0) copy(col = col - 1)
    else if (row > 0) copy(row = row - 1, col = lines(row - 1).length)
    else this

  def right: Editor =
    if (col < line.length) copy(col = col + 1)
    else if (row < lines.size - 1) copy(row = row + 1, col = 0)
    else this

  def up: Editor   = if (row > 0) copy(row = row - 1, col = col.min(lines(row - 1).length)) else home
  def down: Editor = if (row < lines.size - 1) copy(row = row + 1, col = col.min(lines(row + 1).length)) else end
  def home: Editor = copy(col = 0)
  def end: Editor  = copy(col = line.length)

  def text: String = lines.mkString("\n")

  // Adds text on new lines after the last one and puts the cursor at its end
  def append(s: String): Editor = {
    val base  = if (text.trim.isEmpty) Vector.empty else lines
    val added = base ++ s.split("\n", -1)
    Editor(added, added.size - 1, added.last.length)
  }

  // The ';'-separated statement the cursor is in. A cursor after a ';' on the same line
  // (spaces aside) belongs to that statement; on trailing blank lines, to the last one.
  // ponytail: ';' inside string literals splits the statement, use a real tokenizer if that bites
  def statementAt: String = {
    val all       = text
    val lineStart = lines.take(row).map(_.length + 1).sum
    val before    = line.take(col).reverse.dropWhile(c => c == ' ' || c == '\t')
    val offset    = if (before.headOption.contains(';')) lineStart + before.length - 1 else lineStart + col
    val ends      = all.indices.filter(all(_) == ';').map(_ + 1) :+ all.length
    val spans     = (0 +: ends.init).zip(ends)
    val chosen = spans.indexWhere((from, to) => offset >= from && offset < to) match {
      case -1 => spans.size - 1
      case i  => i
    }
    spans.take(chosen + 1).reverse
      .map((from, to) => all.substring(from, to).trim.stripSuffix(";").trim)
      .find(_.nonEmpty)
      .getOrElse("")
  }
}
