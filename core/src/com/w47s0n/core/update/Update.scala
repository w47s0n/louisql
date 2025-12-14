package com.w47s0n.core.update

import com.w47s0n.core.state.State
import com.w47s0n.core.state.Startup.{CreateConnection, Connection}
import com.w47s0n.core.service.DataGenerator
import com.softwaremill.quicklens._
import com.w47s0n.core.message.Startup
import com.w47s0n.core.message.Startup.*
import com.w47s0n.core.message.Msg
import java.net.URL

// ========== Update Logic (ELM Update) ==========
// Pure functions that handle state transitions
// No UI dependencies - can be used by any ELM-based frontend

object Update {

  // Result type for update functions
  // The Cmd type parameter allows different frontends to use their own command types
  case class UpdateResult[Cmd](state: State, cmd: Cmd)

  private def handleMenuSelect[Cmd](state: State, noCmd: Cmd): UpdateResult[Cmd] = {
    // TODO: Handle menu selection based on selectedMenuIndex
    // 0 = New, 1 = Connect, 2 = Edit, 3 = Delete, 4 = Quit
    UpdateResult(state, noCmd)
  }

  private def showCreateNewConnectionScreen[Cmd](state: State, noCmd: Cmd): UpdateResult[Cmd] = {
    val updatedState = state
      .modify(_.startup.isAtCreateConnection).setTo(true)
    UpdateResult(updatedState, noCmd)
  }

  private def cancelCreateConnection[Cmd](state: State, noCmd: Cmd): UpdateResult[Cmd] = {
    val updatedState = state
      .modify(_.startup.isAtCreateConnection).setTo(false)
    UpdateResult(updatedState, noCmd)
  }

  private def addConnection[Cmd](state: State, noCmd: Cmd): UpdateResult[Cmd] = {
    val form = state.startup.createConnection.form
    val newConnection = Connection(form.nameInput, new URL("http", form.urlInput, 5432, ""))
    val updatedState = state
      .modify(_.startup.connections).using(_ :+ newConnection)
      .modify(_.startup.isAtCreateConnection).setTo(false)
    UpdateResult(updatedState, noCmd)
  }

  private def handleCharInput[Cmd](state: State, char: Char, noCmd: Cmd): UpdateResult[Cmd] = {
    val form = state.startup.createConnection.form
    val updatedState = if (form.activeField == 0) {
      state.modify(_.startup.createConnection.form.nameInput).using(_ + char)
    } else {
      state.modify(_.startup.createConnection.form.urlInput).using(_ + char)
    }
    UpdateResult(updatedState, noCmd)
  }

  private def handleBackspace[Cmd](state: State, noCmd: Cmd): UpdateResult[Cmd] = {
    val cc = state.startup.createConnection.form
    val updatedState = if (cc.activeField == 0) {
      state.modify(_.startup.createConnection.form.nameInput).using(s => if (s.nonEmpty) s.dropRight(1) else s)
    } else {
      state.modify(_.startup.createConnection.form.urlInput).using(s => if (s.nonEmpty) s.dropRight(1) else s)
    }
    UpdateResult(updatedState, noCmd)
  }

  def update[Cmd](msg: Msg, state: State, noCmd: Cmd): UpdateResult[Cmd] = msg match {
    case startup: Startup =>
      startup match {
        case EnterCreateConnection          => showCreateNewConnectionScreen(state, noCmd)
        case CancelCreateConnection         => cancelCreateConnection(state, noCmd)
        case CycleCreateConnectionField     => UpdateResult(state.modify(_.startup.createConnection.form).using(_.cycleField), noCmd)
        case AddConnection                  => addConnection(state, noCmd)
        case CreateConnectionCharInput(c)   => handleCharInput(state, c, noCmd)
        case CreateConnectionBackspace      => handleBackspace(state, noCmd)
        case EditConnection(index)          => ???
        case DeleteConnection(index)        => ???
        case SelectNextConnection           => UpdateResult(state.modify(_.startup).using(_.selectNext), noCmd)
        case SelectPrevConnection           => UpdateResult(state.modify(_.startup).using(_.selectPrev), noCmd)
        case QuitApp                        => UpdateResult(state, noCmd) // Handled by layoutz layer
      }
    case Msg.NoOp => UpdateResult(state, noCmd)
  }
}
