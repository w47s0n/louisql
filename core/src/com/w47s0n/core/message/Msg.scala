package com.w47s0n.core.message 

sealed trait Msg

object Msg {
  object NoOp extends Msg
}

sealed trait Startup extends Msg

object Startup {
  case object EnterCreateConnection extends Startup
  case object CancelCreateConnection extends Startup
  case object CycleCreateConnectionField extends Startup
  case object AddConnection extends Startup
  case class CreateConnectionCharInput(char: Char) extends Startup
  case object CreateConnectionBackspace extends Startup
  case class EditConnection(index: Int) extends Startup
  case class DeleteConnection(index: Int) extends Startup
  case object SelectNextConnection extends Startup
  case object SelectPrevConnection extends Startup
  case object QuitApp extends Startup
}


