package com.w47s0n.app.tui

import layoutz._
import com.w47s0n.app.Effects
import com.w47s0n.core.effect.Effect
import com.w47s0n.core.state.{Screen, State}
import com.w47s0n.core.message.Msg
import com.w47s0n.core.update.Update
import com.w47s0n.app.tui.view.{StartupView, WorkspaceView}
import com.w47s0n.app.tui.message.{KeyMapper, MsgTranslator}
import com.w47s0n.core.message.Startup.QuitApp

// ========== Layoutz Presentation Layer ==========
// Uses core module for business logic (Model, Msg, Update); runs its Effects as Cmds

object Layoutz extends LayoutzApp[State, Msg] {

  // ========== Initialization ==========
  def init: (State, Cmd[Msg]) =
    (State.empty.copy(viewport = Effects.terminalSize()), Effects.run(Effect.LoadConnections))

  // ========== Update (delegates to core) ==========

  def update(msg: Msg, state: State): (State, Cmd[Msg]) =
    msg match {
      case QuitApp =>
        (state, Cmd.exit)
      case _ =>
        val result = Update.update(msg, state)
        val cmd = result.effects match {
          case Nil      => Cmd.none
          case e :: Nil => Effects.run(e)
          case many     => Cmd.batch(many.map(Effects.run)*)
        }
        (result.state, cmd)
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
    state.screen match {
      case Screen.Workspace => WorkspaceView(state)
      case Screen.Startup   => StartupView(state.startup, state.notice)
    }
}
