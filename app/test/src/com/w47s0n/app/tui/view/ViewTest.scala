package com.w47s0n.app.tui.view

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import com.w47s0n.core.effect.{Purpose, QueryResult}
import com.w47s0n.core.message.Msg
import com.w47s0n.core.message.Startup.{EditConnection, EnterCreateConnection, OpenWorkspace, SaveConnection, UpdateTextField}
import com.w47s0n.core.message.WorkspaceMsg
import com.w47s0n.core.state.{ConnectionUrl, ResultPane, State}
import com.w47s0n.core.state.Startup.Connection
import com.w47s0n.app.tui.Layoutz

class ViewTest extends AnyFlatSpec with Matchers {

  private def plain(state: State): Seq[String] =
    Layoutz.view(state).render.replaceAll("\u001b\\[[0-9;]*m", "").split("\n").toSeq

  private def step(state: State, msgs: Msg*): State =
    msgs.foldLeft(state)((s, m) => Layoutz.update(m, s)._1)

  private val saved = State.empty.copy(startup = State.empty.startup.copy(connections = List(
    Connection("localhost", ConnectionUrl("mysql://root@localhost:3306/app")),
    Connection("production", ConnectionUrl("mysql://app@prod.example.com:3306/proddb")),
  )))

  private val rows = QueryResult.Rows(
    Vector("id", "name", "email", "a_very_long_column_name_that_goes_on_and_on"),
    Vector.tabulate(60)(i => Vector(i.toString, s"user $i", if (i % 3 == 0) "NULL" else s"u$i@example.com", "x" * 80)),
    truncated = true, 12,
  )

  // Workspace with a loaded tree, some console text and a result grid
  private def workspace(w: Int, h: Int) = {
    val opened = step(saved.copy(viewport = (w, h), passwords = Map("localhost" -> "")), OpenWorkspace)
    val loaded = step(opened, Msg.QueryDone(Purpose.Children(Vector("localhost")),
      Right(QueryResult.Rows(Vector("s"), Vector(Vector("app"), Vector("sys")), truncated = false, 1))))
    val typed = "select * from users;".foldLeft(loaded)((s, c) => step(s, WorkspaceMsg.Edit(WorkspaceMsg.EditKey.Insert(c))))
    step(typed, Msg.QueryDone(Purpose.Console, Right(rows)), WorkspaceMsg.FocusNext, WorkspaceMsg.FocusNext)
  }

  private val screens: Map[String, (State, Int, Int)] = Map(
    "startup"       -> (saved, 96, 99),
    "empty"         -> (State.empty, 96, 99),
    "new w/ errors" -> (step(saved, EnterCreateConnection, SaveConnection), 96, 99),
    "long input"    -> (step(saved, EnterCreateConnection, UpdateTextField("x" * 200)), 96, 99),
    "edit"          -> (step(saved.copy(startup = saved.startup.copy(selectedIndex = 1)), EditConnection), 96, 99),
    "ws 120x40"     -> (workspace(120, 40), 120, 40),
    "ws 80x24"      -> (workspace(80, 24), 80, 24),
    "ws error"      -> (workspace(80, 24).copy(workspace = workspace(80, 24).workspace.copy(result = ResultPane.Failed("e" * 500))), 80, 24),
    "ws prompt"     -> (step(saved.copy(viewport = (80, 24)), OpenWorkspace), 80, 24),
  )

  "Every screen" should "fit the terminal it is drawn for" in {
    for ((name, (state, w, h)) <- screens) {
      val lines = plain(state)
      withClue(s"$name height")(lines.size should be < h) // layoutz needs the last line free
      for (l <- lines) withClue(s"$name: [$l]")(l.length should be <= w)
    }
  }

  "Startup screen" should "show the selected connection's details" in {
    val text = plain(saved.copy(startup = saved.startup.copy(selectedIndex = 1))).mkString("\n")
    text should include("prod.example.com")
    text should include("proddb")
    text should include("2 saved")
  }

  "Connection form" should "name the connection being edited and mask the password" in {
    plain(screens("edit")._1).head should include("Edit connection · production")
    plain(screens("new w/ errors")._1).mkString("\n") should include("Name is required")
    val typedPassword = step(saved, EnterCreateConnection) match {
      case s => (1 to 5).foldLeft(s)((st, _) => step(st, com.w47s0n.core.message.Startup.GoToNextField))
    }
    val withPw = step(typedPassword, UpdateTextField("hunter2"))
    plain(withPw).mkString("\n") should (include("•••••••") and not include "hunter2")
  }

  "Workspace" should "show the tree, the console text and the result grid" in {
    val text = plain(workspace(120, 40)).mkString("\n")
    text should include("▾ localhost")
    text should include("▸ app")
    text should include("select * from users;")
    text should include("60+ rows · 12 ms")
    text should include("user 0")
  }

  it should "ask for a missing password in a bar above the status line" in {
    plain(screens("ws prompt")._1).mkString("\n") should include("Password for root@localhost")
  }
}
