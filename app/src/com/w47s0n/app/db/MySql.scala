package com.w47s0n.app.db

import cats.effect.IO
import cats.syntax.all.*
import ldbc.connector.*
import ldbc.sql.ResultSet
import com.w47s0n.core.effect.QueryResult

// The one place that talks to MySQL. Everything else (tree, console) is "run SQL, get rows".
// ponytail: connection per request, keep one open per data source if latency matters
object MySql {

  case class Target(host: String, port: Int, user: String, password: Option[String], database: Option[String])

  val RowLimit = 1000

  def query(target: Target, sql: String, params: Seq[String] = Nil): IO[QueryResult] = {
    val base = MySQLDataSource.build[IO](target.host, target.port, target.user).setAllowPublicKeyRetrieval(true)
    val ds   = target.database.filter(_.nonEmpty).foldLeft(target.password.fold(base)(base.setPassword))(_.setDatabase(_))
    ds.getConnection.use { conn =>
      // Bound parameters are only used for the explorer's metadata SELECTs, so that path always reads rows.
      // (ldbc's prepared execute() leaves getUpdateCount at -1, so it couldn't report writes anyway.)
      val run =
        if (params.nonEmpty)
          conn.prepareStatement(sql).flatMap(ps => params.zipWithIndex.traverse_((p, i) => ps.setString(i + 1, p)) *> ps.executeQuery()).flatMap(rows)
        else
          conn.createStatement().flatMap { st =>
            st.execute(sql).flatMap {
              case true  => st.getResultSet().flatMap(rs => rows(rs.get))
              case false => st.getUpdateCount().map(QueryResult.Affected(_, 0))
            }
          }
      IO.monotonic.flatMap(start => run.flatMap(result => IO.monotonic.map(end => withMillis(result, (end - start).toMillis))))
    }
  }

  private def withMillis(result: QueryResult, ms: Long): QueryResult = result match {
    case r: QueryResult.Rows     => r.copy(millis = ms)
    case a: QueryResult.Affected => a.copy(millis = ms)
  }

  private def rows(rs: ResultSet[IO]): IO[QueryResult] =
    rs.getMetaData().flatMap { meta =>
      val n       = meta.getColumnCount()
      val columns = (1 to n).map(meta.getColumnLabel).toVector
      val cell    = (i: Int) => rs.getString(i).flatMap(v => rs.wasNull().map(isNull => if (isNull || v == null) "NULL" else v))
      def loop(acc: Vector[Vector[String]]): IO[(Vector[Vector[String]], Boolean)] =
        rs.next().flatMap {
          case false                       => IO.pure((acc, false))
          case true if acc.size == RowLimit => IO.pure((acc, true))
          case true                        => (1 to n).toVector.traverse(cell).flatMap(row => loop(acc :+ row))
        }
      loop(Vector.empty).map((data, truncated) => QueryResult.Rows(columns, data, truncated, 0))
    }
}
