package com.w47s0n.core.service

import com.w47s0n.core.state.ConnectionUrl
import com.w47s0n.core.state.Startup.Connection

// Saved connections file: one "name<TAB>url" per line. URLs never carry passwords.
object ConnectionStore {
  def render(connections: List[Connection]): String =
    connections.map(c => s"${c.name.replace('\t', ' ')}\t${c.url.value}").mkString("", "\n", "\n")

  def parse(text: String): List[Connection] =
    text.linesIterator.toList.flatMap { l =>
      l.split("\t", 2) match {
        case Array(name, url) if name.nonEmpty => Some(Connection(name, ConnectionUrl(url)))
        case _                                 => None
      }
    }
}
