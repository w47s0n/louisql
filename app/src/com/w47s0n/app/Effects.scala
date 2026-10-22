package com.w47s0n.app

import java.nio.file.{Files, Path, Paths}
import scala.concurrent.duration.*
import scala.util.Try
import cats.effect.unsafe.implicits.global
import layoutz.Cmd
import com.w47s0n.app.db.MySql
import com.w47s0n.core.effect.{Effect, QueryResult}
import com.w47s0n.core.message.Msg
import com.w47s0n.core.state.Startup.{Connection, ConnectionForm}
import com.w47s0n.core.state.Startup.ConnectionForm.Database

// Runs the core's Effects as layoutz Cmds (each on its own task) and turns the
// outcome back into a Msg. The only place besides MySql with side effects.
object Effects {

  private val store: Path = Paths.get(sys.props("user.home"), ".config", "scalasql", "connections.tsv")

  def run(effect: Effect): Cmd[Msg] = effect match {
    case Effect.Query(conn, password, sql, params, purpose) =>
      task(Msg.QueryDone(purpose, _))(target(conn, password).flatMap(t => attempt(query(t, sql, params))))
    case Effect.SaveConnections(text) =>
      task(Msg.ConnectionsSaved(_))(attempt { Files.createDirectories(store.getParent); Files.writeString(store, text); () })
    case Effect.LoadConnections =>
      task(Msg.ConnectionsLoaded(_))(attempt(if (Files.exists(store)) Files.readString(store) else ""))
  }

  private def task[A](toMsg: Either[String, A] => Msg)(work: => Either[String, A]): Cmd[Msg] =
    Cmd.task(work)(r => toMsg(r.flatten))

  private def query(t: MySql.Target, sql: String, params: Seq[String]): QueryResult =
    MySql.query(t, sql, params).timeout(30.seconds).unsafeRunSync()

  private def target(conn: Connection, password: Option[String]): Either[String, MySql.Target] = {
    val form = ConnectionForm.fromConnection(conn)
    if (form.db != Database.Mysql) Left(s"${form.db} is not supported yet, only MySQL")
    else Right(MySql.Target(form.host, form.port, form.user, password, Option(form.database).filter(_.nonEmpty)))
  }

  private def attempt[A](work: => A): Either[String, A] = Try(work).toEither.left.map(describe)

  // One readable line: drivers like to decorate their messages with colours and boxes
  private[app] def describe(e: Throwable): String = e match {
    case _: java.util.concurrent.TimeoutException => "Timed out after 30 s"
    case e: ldbc.connector.exception.SQLException => // getMessage is a decorated banner ending in "Class: message"
      val plain = e.getMessage.linesIterator.toSeq.lastOption.getOrElse("").stripPrefix(s"${e.getClass.getName}: ")
      if (e.getErrorCode == 0) plain else s"$plain  (MySQL ${e.getErrorCode})"
    case _ =>
      Option(e.getMessage).map(_.replaceAll("\u001b\\[[0-9;]*m", "").linesIterator.map(_.trim).filter(_.nonEmpty).mkString(" "))
        .filter(_.nonEmpty).getOrElse(e.getClass.getSimpleName)
  }

  // (columns, rows) of the controlling terminal
  // ponytail: read once at start, poll it if resizing while running matters
  def terminalSize(): (Int, Int) =
    Try {
      val p   = new ProcessBuilder("sh", "-c", "stty size < /dev/tty").start()
      val out = scala.io.Source.fromInputStream(p.getInputStream).mkString.trim
      p.waitFor()
      val Array(rows, cols) = out.split("\\s+").map(_.toInt)
      (cols, rows)
    }.getOrElse((120, 40))
}
