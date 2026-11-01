package com.w47s0n.app.tui

import com.w47s0n.app.Version

object Main {
  // Re-runs the installer into the directory of the louisql found on PATH (installer default otherwise)
  private val UpdateScript =
    """p=$(command -v louisql) && export LOUISQL_INSTALL_DIR="$(dirname "$p")"
      |curl -fsSL https://louisql.w47s0n.com/install.sh | sh""".stripMargin

  def main(args: Array[String]): Unit = args.toList match {
    case List("--version") => println(s"louisql ${Version.value}")
    case List("update")    => sys.exit(new ProcessBuilder("sh", "-c", UpdateScript).inheritIO().start().waitFor())
    case _                 => Layoutz.run(alignment = layoutz.Alignment.Center)
  }
}
