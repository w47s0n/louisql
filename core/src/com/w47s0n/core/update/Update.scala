package com.w47s0n.core.update

import com.softwaremill.quicklens._
import com.w47s0n.core.message.Msg
import com.w47s0n.core.message.Startup
import com.w47s0n.core.message.Startup.*
import com.w47s0n.core.service.ConnectionTester
import com.w47s0n.core.state.{ConnectionUrl, State}
import com.w47s0n.core.state.Startup.{Connection, ValidationError}
import com.w47s0n.core.state.Startup.ConnectionForm

// ========== Update Logic (ELM Update) ==========
// Pure functions that handle state transitions
// No UI dependencies - can be used by any ELM-based frontend

object Update {

  // Result type for update functions
  // The Cmd type parameter allows different frontends to use their own command types
  case class UpdateResult[Cmd](state: State, cmd: Cmd)

  private def handleMenuSelect[Cmd](state: State, noCmd: Cmd): UpdateResult[Cmd] =
    // TODO: Handle menu selection based on selectedMenuIndex
    // 0 = New, 1 = Connect, 2 = Edit, 3 = Delete, 4 = Quit
    UpdateResult(state, noCmd)

  private def showCreateNewConnectionScreen[Cmd](state: State, noCmd: Cmd): UpdateResult[Cmd] = {
    val updatedState = state
      .modify(_.startup.isAtCreateConnection)
      .setTo(true)
    UpdateResult(updatedState, noCmd)
  }

  private def cancelCreateConnection[Cmd](state: State, noCmd: Cmd): UpdateResult[Cmd] = {
    val updatedState = state
      .modify(_.startup.isAtCreateConnection)
      .setTo(false)
      .modify(_.startup.createConnection.form)
      .setTo(ConnectionForm.empty)
    UpdateResult(updatedState, noCmd)
  }

  // Required-field checks shared by Save/Test/Connect; keeps any existing port error
  private def validate(form: ConnectionForm): Option[ValidationError] = {
    val nameError = Option.when(form.connectionName.trim.isEmpty)("Name is required")
    val hostError = Option.when(form.host.trim.isEmpty)("Host is required")
    val portError = form.errors.flatMap(_.port)
    Option.when(nameError.isDefined || hostError.isDefined || portError.isDefined)(
      ValidationError(hostError, portError, None, nameError)
    )
  }

  private def withErrors(state: State, errors: ValidationError): State =
    state.modify(_.startup.createConnection.form.errors).setTo(Some(errors))

  private def saveConnection[Cmd](state: State, noCmd: Cmd): UpdateResult[Cmd] = {
    val form = state.startup.createConnection.form
    validate(form) match {
      case Some(errors) => UpdateResult(withErrors(state, errors), noCmd)
      case None         =>
        // ponytail: in-memory only, persist to disk when connections must survive restarts
        val newConnection = Connection(form.connectionName, ConnectionUrl.fromForm(form))
        val updatedState  = state
          .modify(_.startup.connections)
          .using(_ :+ newConnection)
          .modify(_.startup.isAtCreateConnection)
          .setTo(false)
          .modify(_.startup.createConnection.form)
          .setTo(ConnectionForm.empty)
        UpdateResult(updatedState, noCmd)
    }
  }

  private def testConnection[Cmd](state: State, noCmd: Cmd): UpdateResult[Cmd] = {
    val form = state.startup.createConnection.form
    validate(form) match {
      case Some(errors) => UpdateResult(withErrors(state, errors), noCmd)
      case None         =>
        val result = ConnectionTester.test(ConnectionUrl.fromForm(form)).fold(err => s"Failed: $err", _ => "Connection OK")
        val updatedState = state
          .modify(_.startup.createConnection.form)
          .using(_.copy(errors = None, testResult = Some(result)))
        UpdateResult(updatedState, noCmd)
    }
  }

  // Save, then select the new connection. No query screen exists yet, so connecting stops here.
  private def connectConnection[Cmd](state: State, noCmd: Cmd): UpdateResult[Cmd] = {
    val saved = saveConnection(state, noCmd)
    if (saved.state.startup.isAtCreateConnection) saved // validation failed
    else saved.copy(state = saved.state.modify(_.startup.selectedIndex).setTo(saved.state.startup.connections.size - 1))
  }

  private def handleBackspace[Cmd](state: State, noCmd: Cmd): UpdateResult[Cmd] = {
    val form = state.startup.createConnection.form
    val currentValue = form.activeFieldValue
    if (currentValue.nonEmpty) {
      val newValue = currentValue.dropRight(1)
      val updatedForm = form.updateActiveField(newValue)
      UpdateResult(state.modify(_.startup.createConnection.form).setTo(updatedForm), noCmd)
    } else {
      UpdateResult(state, noCmd)
    }
  }

  private def createConnectionFormHandle[Cmd](state: State, msg: CreateConnectionForm, noCmd: Cmd): UpdateResult[Cmd] =
    msg match {
      case GoToNextField =>
        UpdateResult(state.modify(_.startup.createConnection.form).using(_.cycleField), noCmd)
      case SelectDBMS(db) =>
        UpdateResult(state.modify(_.startup.createConnection.form.db).setTo(db), noCmd)
      case UpdateTextField(newValue) =>
        val form = state.startup.createConnection.form
        val updatedForm = if (form.activeField == 3 && newValue.toIntOption.isEmpty) {
          form.copy(errors = Some(form.errors.getOrElse(ValidationError(None, None, None)).copy(port = Some("Port must be a number"))))
        } else {
          form.updateActiveField(newValue).copy(errors = form.errors.map(_.copy(port = None)))
        }
        UpdateResult(state.modify(_.startup.createConnection.form).setTo(updatedForm), noCmd)
    }

  def update[Cmd](msg: Msg, state: State, noCmd: Cmd): UpdateResult[Cmd] = msg match {
    case startup: Startup =>
      startup match {
        case EnterCreateConnection     => showCreateNewConnectionScreen(state, noCmd)
        case CancelCreateConnection    => cancelCreateConnection(state, noCmd)
        case SaveConnection            => saveConnection(state, noCmd)
        case TestConnection            => testConnection(state, noCmd)
        case ConnectConnection         => connectConnection(state, noCmd)
        case msg: CreateConnectionForm => createConnectionFormHandle(state, msg, noCmd)
        case CreateConnectionBackspace => handleBackspace(state, noCmd)
        case EditConnection(index)     => ???
        case DeleteConnection(index)   => ???
        case SelectNextConnection      => UpdateResult(state.modify(_.startup).using(_.selectNext), noCmd)
        case SelectPrevConnection      => UpdateResult(state.modify(_.startup).using(_.selectPrev), noCmd)
        case QuitApp                   => UpdateResult(state, noCmd) // Handled by layoutz layer
      }
    case Msg.NoOp => UpdateResult(state, noCmd)
  }
}
