package com.w47s0n.core.state

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class EditorTest extends AnyFlatSpec with Matchers {

  private def typed(s: String): Editor = s.foldLeft(Editor())((e, c) => if (c == '\n') e.newline else e.insert(c))

  "Editor" should "type, break lines and track the cursor" in {
    val e = typed("ab\ncd")
    e.lines shouldBe Vector("ab", "cd")
    (e.row, e.col) shouldBe (1, 2)
  }

  it should "join lines on backspace at line start and delete at line end" in {
    val atStart = typed("ab\ncd").home
    atStart.backspace shouldBe Editor(Vector("abcd"), 0, 2)
    typed("ab\ncd").up.end.delete shouldBe Editor(Vector("abcd"), 0, 2)
  }

  it should "not move past the edges" in {
    Editor().left.up.backspace shouldBe Editor()
    typed("ab").right.down.delete.col shouldBe 2
  }

  it should "clamp the column when moving to a shorter line" in {
    val e = typed("a\nlonger").up
    (e.row, e.col) shouldBe (0, 1)
  }

  it should "append on new lines, replacing an empty buffer" in {
    Editor().append("SELECT 1;") shouldBe Editor(Vector("SELECT 1;"), 0, 9)
    typed("x").append("y").lines shouldBe Vector("x", "y")
  }

  "statementAt" should "pick the statement under the cursor" in {
    val e = typed("select 1;\nselect 2;\nselect 3")
    e.statementAt shouldBe "select 3"
    e.up.statementAt shouldBe "select 2"
    e.up.up.home.statementAt shouldBe "select 1"
  }

  it should "give a cursor right after ';' to that statement, and the next line's start to the next" in {
    val e = typed("select 1;\nselect 2;")
    e.up.end.statementAt shouldBe "select 1"
    e.home.statementAt shouldBe "select 2"
  }

  it should "use the last statement from trailing blank lines, and nothing from an empty buffer" in {
    typed("select 1;\n\n").statementAt shouldBe "select 1"
    Editor().statementAt shouldBe ""
  }
}
