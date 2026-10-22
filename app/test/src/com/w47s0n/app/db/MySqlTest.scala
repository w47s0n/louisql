package com.w47s0n.app.db

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import cats.effect.unsafe.implicits.global

import com.w47s0n.core.effect.QueryResult
import com.w47s0n.app.Effects

// Talks to a real MySQL. Only runs when SCALASQL_MYSQL=user:password@host:port is set, e.g.
//   podman run -d -p 13306:3306 -e MYSQL_ROOT_PASSWORD=root mysql:8.4
//   SCALASQL_MYSQL=root:root@127.0.0.1:13306 mill app.test
class MySqlTest extends AnyFlatSpec with Matchers {

  private val target = sys.env.get("SCALASQL_MYSQL").map { s =>
    val Array(user, rest) = s.split(":", 2)
    val Array(pw, hostPort) = rest.split("@", 2)
    val Array(host, port) = hostPort.split(":")
    MySql.Target(host, port.toInt, user, Some(pw), None)
  }

  private def run(sql: String, params: String*) = MySql.query(target.get, sql, params).unsafeRunSync()

  "MySql.query" should "return rows, NULLs, update counts and errors" in {
    assume(target.isDefined, "SCALASQL_MYSQL not set")
    run("DROP DATABASE IF EXISTS scalasql_it")
    run("CREATE DATABASE scalasql_it")
    run("CREATE TABLE scalasql_it.users (id BIGINT PRIMARY KEY, name VARCHAR(50) NULL)")
    run("INSERT INTO scalasql_it.users VALUES (1, 'ada'), (2, NULL)") shouldBe a[QueryResult.Affected]
    run("UPDATE scalasql_it.users SET name = 'bob' WHERE id = 2") match {
      case QueryResult.Affected(n, _) => n shouldBe 1
      case other                      => fail(other.toString)
    }

    run("SELECT id, name FROM scalasql_it.users ORDER BY id") match {
      case QueryResult.Rows(cols, rows, truncated, _) =>
        cols shouldBe Vector("id", "name")
        rows shouldBe Vector(Vector("1", "ada"), Vector("2", "bob"))
        truncated shouldBe false
      case other => fail(other.toString)
    }
    run("SELECT table_name FROM information_schema.tables WHERE table_schema = ?", "scalasql_it")
      .asInstanceOf[QueryResult.Rows].rows shouldBe Vector(Vector("users"))
    run("SELECT NULL AS n").asInstanceOf[QueryResult.Rows].rows shouldBe Vector(Vector("NULL"))
    run("DROP DATABASE scalasql_it")
  }

  it should "stop at the row limit and say so" in {
    assume(target.isDefined, "SCALASQL_MYSQL not set")
    val many = run("SELECT 1 FROM information_schema.columns a, information_schema.columns b LIMIT 1500")
    many match {
      case QueryResult.Rows(_, rows, truncated, _) => (rows.size, truncated) shouldBe (MySql.RowLimit, true)
      case other                                   => fail(other.toString)
    }
  }

  "Effects.describe" should "turn a decorated driver error into one plain line" in {
    assume(target.isDefined, "SCALASQL_MYSQL not set")
    val msg = Effects.describe(scala.util.Try(run("SELEC nope")).failed.get)
    msg should startWith("You have an error in your SQL syntax")
    msg should endWith("(MySQL 1064)")
    msg should not include "🔥"
    msg should not include "\n"
    msg should not include "\u001b"
  }
}
