package com.w47s0n.core.message 

import com.w47s0n.core.state.Startup.ConnectionForm.Database

sealed trait Msg

object Msg {
  object NoOp extends Msg
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
  case class EditConnection(index: Int) extends Startup
  case class DeleteConnection(index: Int) extends Startup
  case object SelectNextConnection extends Startup
  case object SelectPrevConnection extends Startup
  case object QuitApp extends Startup
}


