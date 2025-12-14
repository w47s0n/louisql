package com.w47s0n.app.tui

import layoutz._
import com.w47s0n.core.state.State
import com.w47s0n.core.state.DbState
import com.w47s0n.core.message.{Msg}
import com.w47s0n.core.update.Update
import com.w47s0n.core.service.DataGenerator
import com.w47s0n.app.tui.view.StartupView
import com.w47s0n.app.tui.message.{ZMsg, KeyMapper, MsgTranslator}
import com.w47s0n.core.message.Startup.QuitApp
import scala.util.Try

// ========== Layoutz Presentation Layer ==========
// Uses core module for business logic (Model, Msg, Update)

object Layoutz extends LayoutzApp[State, Msg] {

  def sideEffect(task: => Unit): Cmd[Msg] = {
    val eitherTask: () => Either[String, String] = () => Try(task).toEither.map(_ => "whatever").swap.map(_ => "whatever")
    Cmd.perform(eitherTask, _ => Msg.NoOp)
  }

  // ========== Initialization ==========
  def init: (State, Cmd[Msg]) =
    (State.empty, Cmd.none)

  // ========== Update (delegates to core) ==========

  def update(msg: Msg, state: State): (State, Cmd[Msg]) =  {
    val systemExitCmd = sideEffect(System.exit(0))
      msg match {
        case QuitApp =>
          System.exit(0)
          (state, systemExitCmd)
        case _ =>
          val result = Update.update(msg, state, Cmd.none)
          (result.state, result.cmd)
      }
  }

  // ========== Subscriptions ==========
  // Uses ZMsg translation layer: KeyEvent -> ZMsg -> Msg
  // This allows context-aware key handling (same key = different action per screen)

  def subscriptions(state: State): Sub[Msg] =
    Sub.onKeyPress { key =>
      KeyMapper.map(key).flatMap(zMsg => MsgTranslator.translate(zMsg, state))
    }

  // ========== View ==========

  def view(state: State): layoutz.Element =
    StartupView(state.startup)
}
