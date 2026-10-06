package com.w47s0n.core.service

import com.w47s0n.core.state.ConnectionUrl

object ConnectionTester {
  // ponytail: placeholder that always succeeds, real ping once a JDBC driver is added
  def test(url: ConnectionUrl): Either[String, Unit] = Right(())
}
