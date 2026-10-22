package com.w47s0n.core.state

import com.w47s0n.core.effect.QueryResult

enum Kind {
  case Connection, Schema, Folder, Table, View, Column, Message
}

// One node of the explorer tree. children = None means "not loaded yet".
case class Node(
  label: String,
  kind: Kind,
  detail: String = "",
  children: Option[Vector[Node]] = None,
  expanded: Boolean = false,
  loading: Boolean = false,
) {
  def isLeaf: Boolean = kind == Kind.Column || kind == Kind.Message
}

// A visible tree row: where it sits and how deep.
case class TreeRow(path: Vector[String], depth: Int, node: Node)

object Tree {
  def flatten(roots: Vector[Node]): Vector[TreeRow] = {
    def go(nodes: Vector[Node], parent: Vector[String]): Vector[TreeRow] =
      nodes.flatMap { n =>
        val path = parent :+ n.label
        TreeRow(path, parent.size, n) +: (if (n.expanded) go(n.children.getOrElse(Vector.empty), path) else Vector.empty)
      }
    go(roots, Vector.empty)
  }

  def update(roots: Vector[Node], path: Vector[String])(f: Node => Node): Vector[Node] =
    path.headOption.fold(roots) { head =>
      roots.map { n =>
        if (n.label != head) n
        else if (path.size == 1) f(n)
        else n.copy(children = n.children.map(update(_, path.tail)(f)))
      }
    }

  def at(roots: Vector[Node], path: Vector[String]): Option[Node] =
    path.headOption.flatMap(head => roots.find(_.label == head)).flatMap { n =>
      if (path.size == 1) Some(n) else at(n.children.getOrElse(Vector.empty), path.tail)
    }
}

enum Focus {
  case Explorer, Console, Result
}

enum ResultPane {
  case Idle
  case Running(sql: String)
  case Done(result: QueryResult)
  case Failed(message: String)
}

case class Workspace(
  roots: Vector[Node] = Vector.empty,
  selected: Int = 0,
  focus: Focus = Focus.Explorer,
  editor: Editor = Editor(),
  result: ResultPane = ResultPane.Idle,
  scrollRow: Int = 0,
  scrollCol: Int = 0,
  active: Option[String] = None, // connection the console runs against
) {
  def rows: Vector[TreeRow]            = Tree.flatten(roots)
  def selectedRow: Option[TreeRow]     = rows.lift(selected)
}
