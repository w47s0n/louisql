package com.w47s0n.core.update

import com.softwaremill.quicklens._
import com.w47s0n.core.effect.{Effect, Purpose, QueryResult}
import com.w47s0n.core.message.WorkspaceMsg
import com.w47s0n.core.message.WorkspaceMsg.*
import com.w47s0n.core.state.*
import com.w47s0n.core.state.Startup.{Connection, ConnectionForm}
import com.w47s0n.core.update.Update.UpdateResult

// Explorer tree, query console and result grid. The tree loads lazily: expanding a
// node runs an information_schema query and builds its children from the rows.
object WorkspaceUpdate {

  // ---------- what each level of the tree asks MySQL ----------

  private def childrenQuery(row: TreeRow): Option[(String, Seq[String])] = row.node.kind match {
    case Kind.Connection =>
      Some(("SELECT schema_name FROM information_schema.schemata ORDER BY schema_name", Nil))
    case Kind.Schema =>
      Some(("SELECT table_name, table_type FROM information_schema.tables WHERE table_schema = ? ORDER BY table_name", Seq(row.path(1))))
    case Kind.Table | Kind.View =>
      Some((
        "SELECT column_name, column_type, column_key FROM information_schema.columns " +
          "WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position",
        Seq(row.path(1), row.path(3)),
      ))
    case _ => None
  }

  private[update] def childrenFrom(kind: Kind, rows: Vector[Vector[String]]): Vector[Node] = kind match {
    case Kind.Connection => rows.map(r => Node(r(0), Kind.Schema))
    case Kind.Schema =>
      val (views, tables) = rows.partition(_(1).endsWith("VIEW"))
      def folder(name: String, items: Vector[Node], open: Boolean) =
        Node(name, Kind.Folder, items.size.toString, Some(items), expanded = open)
      Vector(folder("tables", tables.map(r => Node(r(0), Kind.Table)), open = true)) ++
        Option.when(views.nonEmpty)(folder("views", views.map(r => Node(r(0), Kind.View)), open = false))
    case _ => rows.map(r => Node(r(0), Kind.Column, r(1) + (if (r(2) == "PRI") "  PK" else "")))
  }

  // ---------- queries go through the password gate ----------

  private def connection(state: State, name: String): Option[Connection] =
    state.startup.connections.find(_.name == name)

  private[update] def query(state: State, conn: Connection, sql: String, params: Seq[String], purpose: Purpose): UpdateResult = {
    val effect: Effect.Query = Effect.Query(conn, None, sql, params, purpose)
    state.passwords.get(conn.name) match {
      case Some(pw) => UpdateResult(state, List(effect.copy(password = Option(pw).filter(_.nonEmpty))))
      case None if state.prompt.isDefined => UpdateResult(cancelled(state, purpose)) // one prompt at a time
      case None =>
        val form  = ConnectionForm.fromConnection(conn)
        val label = s"${if (form.user.isEmpty) "" else form.user + "@"}${form.host}"
        UpdateResult(state.copy(prompt = Some(PasswordPrompt(conn.name, label, "", effect))))
    }
  }

  // Undo the "in progress" marks of a query that will never run
  private[update] def cancelled(state: State, purpose: Purpose): State = purpose match {
    case Purpose.Children(path) =>
      state.modify(_.workspace.roots).using(Tree.update(_, path)(_.copy(loading = false, expanded = false)))
    case Purpose.Console => state.modify(_.workspace.result).setTo(ResultPane.Idle)
    case Purpose.Test    => state.modify(_.startup.createConnection.form.testResult).setTo(None)
  }

  // ---------- opening ----------

  def open(state: State): UpdateResult =
    state.startup.selectedConnection.fold(UpdateResult(state)) { conn =>
      val existing = state.workspace.roots
      val roots = state.startup.connections.toVector.map { c =>
        existing.find(n => n.label == c.name && n.detail == c.url.value)
          .getOrElse(Node(c.name, Kind.Connection, c.url.value))
      }
      val index = Tree.flatten(roots).indexWhere(_.path == Vector(conn.name))
      val opened = state.copy(screen = Screen.Workspace, notice = None)
        .modify(_.workspace).using(_.copy(roots = roots, selected = index, focus = Focus.Explorer, active = Some(conn.name)))
      if (roots(index).expanded) UpdateResult(opened) else expand(opened)
    }

  // ---------- tree ----------

  private def select(state: State, index: Int): State = {
    val rows  = state.workspace.rows
    val clamp = index.max(0).min((rows.size - 1).max(0))
    state.modify(_.workspace).using(w => w.copy(selected = clamp, active = rows.lift(clamp).map(_.path.head).orElse(w.active)))
  }

  private def expand(state: State): UpdateResult =
    state.workspace.selectedRow.fold(UpdateResult(state)) { row =>
      val node = row.node
      if (node.isLeaf || node.loading) UpdateResult(state)
      else if (node.expanded) UpdateResult(select(state, state.workspace.selected + 1)) // step inside
      else if (node.children.isDefined)
        UpdateResult(state.modify(_.workspace.roots).using(Tree.update(_, row.path)(_.copy(expanded = true))))
      else
        (childrenQuery(row), connection(state, row.path.head)) match {
          case (Some((sql, params)), Some(conn)) =>
            val marked = state.modify(_.workspace.roots).using(Tree.update(_, row.path)(_.copy(expanded = true, loading = true)))
            query(marked, conn, sql, params, Purpose.Children(row.path))
          case _ => UpdateResult(state)
        }
    }

  private def collapse(state: State): UpdateResult =
    state.workspace.selectedRow.fold(UpdateResult(state)) { row =>
      if (row.node.expanded)
        UpdateResult(state.modify(_.workspace.roots).using(Tree.update(_, row.path)(_.copy(expanded = false))))
      else UpdateResult(select(state, state.workspace.rows.indexWhere(_.path == row.path.init)))
    }

  private def activate(state: State): UpdateResult =
    state.workspace.selectedRow.fold(UpdateResult(state)) { row =>
      row.node.kind match {
        case Kind.Table | Kind.View =>
          def quote(id: String) = "`" + id.replace("`", "``") + "`"
          val sql = s"SELECT * FROM ${quote(row.path(1))}.${quote(row.path(3))} LIMIT 100;"
          run(state.modify(_.workspace.editor).using(_.append(sql)))
        case _ if row.node.expanded => collapse(state)
        case _                      => expand(state)
      }
    }

  // Forget a node's children and load them again (a leaf refreshes its parent)
  private def refresh(state: State): UpdateResult =
    state.workspace.selectedRow.fold(UpdateResult(state)) { row =>
      val target = if (row.node.isLeaf) row.path.init else row.path
      val reset  = state.modify(_.workspace.roots).using(Tree.update(_, target)(_.copy(children = None, expanded = false, loading = false)))
      expand(select(reset, reset.workspace.rows.indexWhere(_.path == target)))
    }

  // ---------- console ----------

  private def run(state: State): UpdateResult = {
    val sql = state.workspace.editor.statementAt
    state.workspace.active.flatMap(connection(state, _)) match {
      case _ if sql.isEmpty => UpdateResult(state)
      case None             => UpdateResult(state.copy(notice = Some("Pick a connection in the explorer first")))
      case Some(conn) =>
        val running = state.modify(_.workspace.result).setTo(ResultPane.Running(sql)).copy(notice = None)
        query(running, conn, sql, Nil, Purpose.Console)
    }
  }

  private def edit(editor: Editor, key: EditKey): Editor = key match {
    case EditKey.Insert(c) => editor.insert(c)
    case EditKey.Newline   => editor.newline
    case EditKey.Backspace => editor.backspace
    case EditKey.Delete    => editor.delete
    case EditKey.Left      => editor.left
    case EditKey.Right     => editor.right
    case EditKey.Up        => editor.up
    case EditKey.Down      => editor.down
    case EditKey.Home      => editor.home
    case EditKey.End       => editor.end
  }

  private def scroll(state: State, dr: Int, dc: Int): State = state.workspace.result match {
    case ResultPane.Done(QueryResult.Rows(cols, rows, _, _)) =>
      state.modify(_.workspace).using(w =>
        w.copy(
          scrollRow = (w.scrollRow + dr).min(rows.size - 1).max(0),
          scrollCol = (w.scrollCol + dc).min(cols.size - 1).max(0),
        )
      )
    case _ => state
  }

  // ---------- answers ----------

  def queryDone(state: State, purpose: Purpose, result: Either[String, QueryResult]): UpdateResult = purpose match {
    case Purpose.Children(path) =>
      val children = result match {
        case Right(QueryResult.Rows(_, rows, _, _)) =>
          Tree.at(state.workspace.roots, path).map(n => childrenFrom(n.kind, rows)).getOrElse(Vector.empty)
        case Right(_)  => Vector.empty
        case Left(err) => Vector(Node(err, Kind.Message))
      }
      UpdateResult(state.modify(_.workspace.roots).using(Tree.update(_, path)(_.copy(children = Some(children), loading = false))))
    case Purpose.Console =>
      val pane = result.fold(ResultPane.Failed(_), ResultPane.Done(_))
      UpdateResult(state.modify(_.workspace).using(_.copy(result = pane, scrollRow = 0, scrollCol = 0)))
    case Purpose.Test => UpdateResult(state) // handled by Update
  }

  def update(msg: WorkspaceMsg, state: State): UpdateResult = msg match {
    case Close        => UpdateResult(state.copy(screen = Screen.Startup))
    case FocusNext    => UpdateResult(state.modify(_.workspace.focus).using(f => Focus.fromOrdinal((f.ordinal + 1) % Focus.values.length)))
    case TreeUp       => UpdateResult(select(state, state.workspace.selected - 1))
    case TreeDown     => UpdateResult(select(state, state.workspace.selected + 1))
    case TreeExpand   => expand(state)
    case TreeCollapse => collapse(state)
    case TreeActivate => activate(state)
    case TreeRefresh  => refresh(state)
    case Run          => run(state)
    case Edit(key)    => UpdateResult(state.modify(_.workspace.editor).using(edit(_, key)))
    case ScrollResult(dr, dc) => UpdateResult(scroll(state, dr, dc))
  }
}
