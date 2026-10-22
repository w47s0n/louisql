package com.w47s0n.core.state

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import com.softwaremill.quicklens.*
import com.w47s0n.core.effect.{Effect, Purpose, QueryResult}
import com.w47s0n.core.message.{Msg, Startup as StartupMsg, WorkspaceMsg}
import com.w47s0n.core.service.ConnectionStore
import com.w47s0n.core.update.Update

class WorkspaceTest extends AnyFlatSpec with Matchers {

  private def step(s: State, msg: Msg) = Update.update(msg, s)
  private def rows(cells: Vector[String]*) = QueryResult.Rows(Vector("a"), cells.toVector, truncated = false, 1)
  private def queries(r: Update.UpdateResult) = r.effects.collect { case q: Effect.Query => q }

  // Opened on "localhost" with its password already known this session
  private val opened = step(Fixtures.state.copy(passwords = Map("localhost" -> "pw")), StartupMsg.OpenWorkspace)

  "Tree" should "flatten only expanded nodes and update by path" in {
    val tree = Vector(Node("c", Kind.Connection, children = Some(Vector(Node("s", Kind.Schema))), expanded = true), Node("d", Kind.Connection))
    Tree.flatten(tree).map(r => (r.path, r.depth)) shouldBe Vector((Vector("c"), 0), (Vector("c", "s"), 1), (Vector("d"), 0))
    Tree.flatten(Tree.update(tree, Vector("c"))(_.copy(expanded = false))).size shouldBe 2
    Tree.at(tree, Vector("c", "s")).map(_.kind) shouldBe Some(Kind.Schema)
  }

  "Opening the workspace" should "list every connection and start loading the selected one's schemas" in {
    opened.state.screen shouldBe Screen.Workspace
    opened.state.workspace.roots.map(_.label) shouldBe Vector("localhost", "production", "staging")
    val q = queries(opened).head
    q.purpose shouldBe Purpose.Children(Vector("localhost"))
    q.password shouldBe Some("pw")
    opened.state.workspace.roots.head.loading shouldBe true
  }

  it should "ask for the password first when the session doesn't know it, then replay the query" in {
    val asked = step(Fixtures.state, StartupMsg.OpenWorkspace)
    asked.effects shouldBe Nil
    asked.state.prompt.map(_.label) shouldBe Some("root@localhost")

    val typed     = "secret".foldLeft(asked.state)((s, c) => step(s, Msg.PromptInput(c)).state)
    val submitted = step(typed, Msg.PromptSubmit)
    submitted.state.prompt shouldBe None
    submitted.state.passwords("localhost") shouldBe "secret"
    queries(submitted).map(q => (q.purpose, q.password)) shouldBe List((Purpose.Children(Vector("localhost")), Some("secret")))
  }

  it should "undo the loading mark when the prompt is cancelled" in {
    val cancelled = step(step(Fixtures.state, StartupMsg.OpenWorkspace).state, Msg.PromptCancel).state
    cancelled.prompt shouldBe None
    cancelled.workspace.roots.head.loading shouldBe false
    cancelled.workspace.roots.head.expanded shouldBe false
  }

  "Loaded rows" should "become schemas, then table/view folders, then columns" in {
    val schemas = step(opened.state, Msg.QueryDone(Purpose.Children(Vector("localhost")), Right(rows(Vector("app"), Vector("sys"))))).state
    schemas.workspace.rows.map(_.node.label) shouldBe Vector("localhost", "app", "sys", "production", "staging")

    val onApp   = step(schemas, WorkspaceMsg.TreeDown).state
    val loading = step(onApp, WorkspaceMsg.TreeExpand)
    queries(loading).head.params shouldBe Seq("app")

    val tables = step(loading.state, Msg.QueryDone(Purpose.Children(Vector("localhost", "app")),
      Right(rows(Vector("users", "BASE TABLE"), Vector("active_users", "VIEW"))))).state
    tables.workspace.rows.map(r => (r.node.label, r.node.detail)).slice(2, 6) shouldBe
      Vector(("tables", "1"), ("users", ""), ("views", "1"), ("sys", ""))

    val onUsers = step(step(tables, WorkspaceMsg.TreeDown).state, WorkspaceMsg.TreeDown).state
    val cols    = step(onUsers, WorkspaceMsg.TreeExpand)
    queries(cols).head.params shouldBe Seq("app", "users")
    val done = step(cols.state, Msg.QueryDone(Purpose.Children(Vector("localhost", "app", "tables", "users")),
      Right(rows(Vector("id", "bigint", "PRI"), Vector("name", "varchar(255)", ""))))).state
    done.workspace.rows.slice(4, 6).map(r => (r.node.label, r.node.detail)) shouldBe Vector(("id", "bigint  PK"), ("name", "varchar(255)"))
  }

  it should "show a load error as a message row" in {
    val failed = step(opened.state, Msg.QueryDone(Purpose.Children(Vector("localhost")), Left("Access denied"))).state
    failed.workspace.rows(1).node shouldBe Node("Access denied", Kind.Message)
  }

  "Collapse" should "fold an open node, or jump to the parent from a closed one" in {
    val schemas = step(opened.state, Msg.QueryDone(Purpose.Children(Vector("localhost")), Right(rows(Vector("app"))))).state
    val onApp   = step(schemas, WorkspaceMsg.TreeDown).state
    step(onApp, WorkspaceMsg.TreeCollapse).state.workspace.selected shouldBe 0
    step(schemas, WorkspaceMsg.TreeCollapse).state.workspace.rows.size shouldBe 3
  }

  "Enter on a table" should "append a SELECT to the console and run it" in {
    val tree = opened.state.modify(_.workspace.roots).setTo(Vector(Node("localhost", Kind.Connection, expanded = true, children = Some(Vector(
      Node("app", Kind.Schema, expanded = true, children = Some(Vector(
        Node("tables", Kind.Folder, expanded = true, children = Some(Vector(Node("users", Kind.Table))))))))))))
    val onUsers = tree.modify(_.workspace.selected).setTo(3)
    val ran     = step(onUsers, WorkspaceMsg.TreeActivate)
    ran.state.workspace.editor.text shouldBe "SELECT * FROM `app`.`users` LIMIT 100;"
    queries(ran).map(q => (q.sql, q.purpose)) shouldBe List(("SELECT * FROM `app`.`users` LIMIT 100", Purpose.Console))
    ran.state.workspace.result shouldBe ResultPane.Running("SELECT * FROM `app`.`users` LIMIT 100")
  }

  "Run" should "send the statement under the cursor and show the answer" in {
    val typed = "select 1;\nselect 2".foldLeft(opened.state) { (s, c) =>
      val key = if (c == '\n') WorkspaceMsg.EditKey.Newline else WorkspaceMsg.EditKey.Insert(c)
      step(s, WorkspaceMsg.Edit(key)).state
    }
    val running = step(typed, WorkspaceMsg.Run)
    queries(running).map(_.sql) shouldBe List("select 2")

    val answer = rows(Vector("2"))
    val done   = step(running.state, Msg.QueryDone(Purpose.Console, Right(answer))).state
    done.workspace.result shouldBe ResultPane.Done(answer)
    step(running.state, Msg.QueryDone(Purpose.Console, Left("boom"))).state.workspace.result shouldBe ResultPane.Failed("boom")
  }

  "Result scroll" should "stay inside the rows and columns" in {
    val grid = QueryResult.Rows(Vector("a", "b"), Vector.fill(5)(Vector("1", "2")), truncated = false, 1)
    val done = opened.state.modify(_.workspace.result).setTo(ResultPane.Done(grid))
    val far  = step(done, WorkspaceMsg.ScrollResult(100, 100)).state.workspace
    (far.scrollRow, far.scrollCol) shouldBe (4, 1)
    val back = step(done, WorkspaceMsg.ScrollResult(-3, -3)).state.workspace
    (back.scrollRow, back.scrollCol) shouldBe (0, 0)
  }

  "Saving and deleting" should "persist connections without passwords" in {
    val deleted = step(Fixtures.state.copy(passwords = Map("localhost" -> "pw")), StartupMsg.DeleteConnection)
    deleted.state.passwords shouldBe Map.empty
    deleted.effects shouldBe List(Effect.SaveConnections(ConnectionStore.render(Fixtures.connections.tail)))
  }

  "ConnectionStore" should "round-trip connections and skip junk lines" in {
    ConnectionStore.parse(ConnectionStore.render(Fixtures.connections)) shouldBe Fixtures.connections
    ConnectionStore.parse("no tab here\n\tmissing name\n") shouldBe Nil
    ConnectionStore.render(Fixtures.connections) should not include "pw"
  }
}
