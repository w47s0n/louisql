package com.w47s0n.app.tui.view

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import com.w47s0n.core.message.Startup.{EditConnection, EnterCreateConnection, SaveConnection, UpdateTextField}
import com.w47s0n.core.state.{Startup, State}
import com.w47s0n.core.update.Update

class ViewTest extends AnyFlatSpec with Matchers {

  private def plain(state: State): Seq[String] =
    StartupView(state.startup).render.replaceAll("\u001b\\[[0-9;]*m", "").split("\n").toSeq

  private def step(state: State, msgs: com.w47s0n.core.message.Msg*): State =
    msgs.foldLeft(state)((s, m) => Update.update(m, s, ()).state)

  private val screens = Map(
    "startup"       -> State.empty,
    "last selected" -> State.empty.copy(startup = State.empty.startup.copy(selectedIndex = 2)),
    "empty"         -> State.empty.copy(startup = State.empty.startup.copy(connections = Nil)),
    "new w/ errors" -> step(State.empty, EnterCreateConnection, SaveConnection),
    "long input"    -> step(State.empty, EnterCreateConnection, UpdateTextField("x" * 200)),
    "edit"          -> step(State.empty.copy(startup = State.empty.startup.copy(selectedIndex = 1)), EditConnection),
  )

  "Every screen" should "fit in Theme.Width columns" in {
    for ((name, state) <- screens; l <- plain(state))
      withClue(s"$name: [$l]")(l.length should be <= Theme.Width)
  }

  "Startup screen" should "show the selected connection's details" in {
    val text = plain(State.empty.copy(startup = State.empty.startup.copy(selectedIndex = 1))).mkString("\n")
    text should include("prod.example.com")
    text should include("3 saved")
  }

  it should "mask the password in a URL" in {
    StartupView.masked("postgresql://bob:secret@db:5432/app") shouldBe "postgresql://bob:•••@db:5432/app"
    StartupView.masked("postgresql://db:5432/app") shouldBe "postgresql://db:5432/app"
  }

  "Connection form" should "name the connection being edited" in {
    plain(screens("edit")).head should include("Edit connection · production")
    plain(screens("new w/ errors")).mkString("\n") should include("Name is required")
  }
}
