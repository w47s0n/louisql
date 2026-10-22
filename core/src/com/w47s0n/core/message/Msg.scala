package com.w47s0n.core.message

import com.w47s0n.core.effect.{Purpose, QueryResult}
import com.w47s0n.core.state.Startup.ConnectionForm.Database

sealed trait Msg

object Msg {
  object NoOp extends Msg

  // Answers to effects
  case class QueryDone(purpose: Purpose, result: Either[String, QueryResult]) extends Msg
  case class ConnectionsLoaded(text: Either[String, String])                 extends Msg
  case class ConnectionsSaved(result: Either[String, Unit])                  extends Msg

  // Password prompt, wherever it is open
  case class PromptInput(c: Char) extends Msg
  case object PromptBackspace     extends Msg
  case object PromptSubmit        extends Msg
  case object PromptCancel        extends Msg
}

sealed trait Startup extends Msg

object Startup {
  case object EnterCreateConnection extends Startup
  case object CancelCreateConnection extends Startup
  case object TestConnection extends Startup
  case object ConnectConnection extends Startup
  case object SaveConnection extends Startup

  sealed trait CreateConnectionForm extends Startup

  case class UpdateTextField(newValue: String) extends CreateConnectionForm
  case object GoToNextField extends CreateConnectionForm
  case class SelectDBMS(db: Database) extends CreateConnectionForm

  case object CreateConnectionBackspace extends Startup
  case object EditConnection extends Startup
  case object DeleteConnection extends Startup
  case object SelectNextConnection extends Startup
  case object SelectPrevConnection extends Startup
  case object OpenWorkspace extends Startup
  case object QuitApp extends Startup
}

sealed trait WorkspaceMsg extends Msg

object WorkspaceMsg {
  case object Close        extends WorkspaceMsg
  case object FocusNext    extends WorkspaceMsg
  case object TreeUp       extends WorkspaceMsg
  case object TreeDown     extends WorkspaceMsg
  case object TreeExpand   extends WorkspaceMsg
  case object TreeCollapse extends WorkspaceMsg
  case object TreeActivate extends WorkspaceMsg // Enter: open a table's rows, toggle anything else
  case object TreeRefresh  extends WorkspaceMsg
  case object Run          extends WorkspaceMsg
  case class Edit(key: EditKey)                   extends WorkspaceMsg
  case class ScrollResult(rows: Int, cols: Int) extends WorkspaceMsg

  enum EditKey {
    case Insert(c: Char)
    case Newline, Backspace, Delete, Left, Right, Up, Down, Home, End
  }
}
