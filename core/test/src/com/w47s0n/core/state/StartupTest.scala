package com.w47s0n.core.state

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import java.net.URL

class StartupTest extends AnyFlatSpec with Matchers {

  "Startup.empty" should "have default values" in {
    val startup = Startup.empty

    startup.isAtCreateConnection shouldBe false
    startup.selectedIndex shouldBe 0
    startup.connections should not be empty
  }

  "CreateConnection" should "cycle between fields" in {
    val cc = Startup.CreateConnection("test", "url")
    val form = cc.form
    form.activeField shouldBe 0
    form.cycleField.activeField shouldBe 1
    form.cycleField.cycleField.activeField shouldBe 0
  }

  it should "check if form is complete" in {
    val incomplete = Startup.CreateConnection("", "").form
    incomplete.isComplete shouldBe false

    val complete = Startup.CreateConnection("name", "url").form
    complete.isComplete shouldBe true
  }

  "Startup" should "select next connection" in {
    val startup = Startup.empty
    val next = startup.selectNext

    next.selectedIndex shouldBe 1
  }

  it should "wrap around when selecting next at end" in {
    val startup = Startup.empty.copy(selectedIndex = 2)
    val next = startup.selectNext

    next.selectedIndex shouldBe 0
  }

  it should "select previous connection" in {
    val startup = Startup.empty.copy(selectedIndex = 1)
    val prev = startup.selectPrev

    prev.selectedIndex shouldBe 0
  }

  it should "wrap around when selecting previous at start" in {
    val startup = Startup.empty.copy(selectedIndex = 0)
    val prev = startup.selectPrev

    prev.selectedIndex shouldBe 2
  }
}
