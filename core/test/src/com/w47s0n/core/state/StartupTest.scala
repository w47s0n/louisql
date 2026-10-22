package com.w47s0n.core.state

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class StartupTest extends AnyFlatSpec with Matchers {

  "Startup.empty" should "start with no connections until they are loaded from disk" in {
    val startup = Startup.empty

    startup.isAtCreateConnection shouldBe false
    startup.selectedIndex shouldBe 0
    startup.connections shouldBe empty
  }

  "CreateConnection" should "cycle between fields and update error message" in {
    val cc   = Startup.CreateConnection("test", "url")
    val form = cc.form
    form.activeField shouldBe 0
    form.cycleField.activeField shouldBe 1
    form.cycleField.cycleField.activeField shouldBe 2
  }

  // it should "check if form is complete" in {
  //   val incomplete = Startup.CreateConnection("", "").form
  //   incomplete.isComplete shouldBe false
  //
  //   val complete = Startup.CreateConnection("name", "url").form
  //   complete.isComplete shouldBe true
  // }

  "Startup" should "select next connection" in {
    val startup = Fixtures.startup
    val next    = startup.selectNext

    next.selectedIndex shouldBe 1
  }

  it should "wrap around when selecting next at end" in {
    val startup = Fixtures.startup.copy(selectedIndex = 2)
    val next    = startup.selectNext

    next.selectedIndex shouldBe 0
  }

  it should "select previous connection" in {
    val startup = Fixtures.startup.copy(selectedIndex = 1)
    val prev    = startup.selectPrev

    prev.selectedIndex shouldBe 0
  }

  it should "wrap around when selecting previous at start" in {
    val startup = Fixtures.startup.copy(selectedIndex = 0)
    val prev    = startup.selectPrev

    prev.selectedIndex shouldBe 2
  }

  it should "delete the selected connection and keep the index" in {
    val deleted = Fixtures.startup.copy(selectedIndex = 1).deleteSelected

    deleted.connections.map(_.name) shouldBe List("localhost", "staging")
    deleted.selectedIndex shouldBe 1
  }

  it should "clamp the index when deleting the last connection" in {
    val deleted = Fixtures.startup.copy(selectedIndex = 2).deleteSelected

    deleted.connections.map(_.name) shouldBe List("localhost", "production")
    deleted.selectedIndex shouldBe 1
  }

  it should "reset the index to 0 when deleting the only connection" in {
    val single  = Fixtures.startup.copy(connections = Fixtures.startup.connections.take(1))
    val deleted = single.deleteSelected

    deleted.connections shouldBe empty
    deleted.selectedIndex shouldBe 0
  }

  it should "do nothing when deleting from an empty list" in {
    val noConnections = Fixtures.startup.copy(connections = Nil)

    noConnections.deleteSelected shouldBe noConnections
  }
}
