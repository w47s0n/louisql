package com.w47s0n.core.update

import com.softwaremill.quicklens._
import com.w47s0n.core.effect.{Effect, Purpose}
import com.w47s0n.core.message.{Msg, Startup, WorkspaceMsg}
import com.w47s0n.core.message.Startup.*
import com.w47s0n.core.service.ConnectionStore
import com.w47s0n.core.state.{ConnectionUrl, State}
import com.w47s0n.core.state.Startup.{Authentication, Connection, CreateConnection, ValidationError}
import com.w47s0n.core.state.Startup.ConnectionForm

// ========== Update Logic (ELM Update) ==========
// Pure functions that handle state transitions. Side effects are returned as
// Effect values for the frontend to run; their answers come back as Msgs.

object Update {

  case class UpdateResult(state: State, effects: List[Effect] = Nil)

  private def showCreateNewConnectionScreen(state: State): UpdateResult =
    UpdateResult(state.modify(_.startup.isAtCreateConnection).setTo(true))

  private def cancelCreateConnection(state: State): UpdateResult = {
    val updatedState = state
      .modify(_.startup.isAtCreateConnection)
      .setTo(false)
      .modify(_.startup.createConnection)
      .setTo(CreateConnection.empty)
    UpdateResult(updatedState)
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

  private[update] def persist(state: State): Effect =
    Effect.SaveConnections(ConnectionStore.render(state.startup.connections))

  private def saveConnection(state: State): UpdateResult = {
    val form = state.startup.createConnection.form
    validate(form) match {
      case Some(errors) => UpdateResult(withErrors(state, errors))
      case None         =>
        val editing       = state.startup.createConnection.editingIndex
        val oldName       = editing.flatMap(state.startup.connections.lift).map(_.name)
        val newConnection = Connection(form.connectionName, ConnectionUrl.fromForm(form))
        // Passwords stay in memory for the session; the saved file never sees them
        val passwords = oldName.fold(state.passwords)(state.passwords - _) ++
          Option.when(form.password.nonEmpty)(form.connectionName -> form.password)
        val updatedState = state
          .modify(_.startup.connections)
          .using(cs => editing.fold(cs :+ newConnection)(cs.updated(_, newConnection)))
          .modify(_.startup.isAtCreateConnection)
          .setTo(false)
          .modify(_.startup.createConnection)
          .setTo(CreateConnection.empty)
          .copy(passwords = passwords)
        UpdateResult(updatedState, List(persist(updatedState)))
    }
  }

  private def deleteConnection(state: State): UpdateResult =
    state.startup.selectedConnection.fold(UpdateResult(state)) { conn =>
      val updated = state.modify(_.startup).using(_.deleteSelected).copy(passwords = state.passwords - conn.name)
      UpdateResult(updated, List(persist(updated)))
    }

  private def testConnection(state: State): UpdateResult = {
    val form = state.startup.createConnection.form
    validate(form) match {
      case Some(errors) => UpdateResult(withErrors(state, errors))
      case None         =>
        val conn  = Connection(form.connectionName, ConnectionUrl.fromForm(form))
        val query = Effect.Query(conn, Option(form.password).filter(_.nonEmpty), "SELECT 1", Nil, Purpose.Test)
        val updatedState = state
          .modify(_.startup.createConnection.form)
          .using(_.copy(errors = None, testResult = Some("Testing…")))
        UpdateResult(updatedState, List(query))
    }
  }

  // Save, then open the workspace on the saved connection
  private def connectConnection(state: State): UpdateResult = {
    val editing = state.startup.createConnection.editingIndex
    val saved   = saveConnection(state)
    if (saved.state.startup.isAtCreateConnection) saved // validation failed
    else {
      val index    = editing.getOrElse(saved.state.startup.connections.size - 1)
      val selected = saved.state.modify(_.startup.selectedIndex).setTo(index)
      val opened   = WorkspaceUpdate.open(selected)
      opened.copy(effects = saved.effects ++ opened.effects)
    }
  }

  private def editConnection(state: State): UpdateResult =
    state.startup.selectedConnection.fold(UpdateResult(state)) { conn =>
      val parsed = ConnectionForm.fromConnection(conn)
      val form   = parsed.copy(auth = Some(Authentication(parsed.user, state.passwords.getOrElse(conn.name, ""))))
      val updatedState = state
        .modify(_.startup.isAtCreateConnection)
        .setTo(true)
        .modify(_.startup.createConnection)
        .setTo(CreateConnection(isActive = false, form, Some(state.startup.selectedIndex)))
      UpdateResult(updatedState)
    }

  private def handleBackspace(state: State): UpdateResult = {
    val form = state.startup.createConnection.form
    val currentValue = form.activeFieldValue
    if (currentValue.nonEmpty) {
      val newValue = currentValue.dropRight(1)
      val updatedForm = form.updateActiveField(newValue)
      UpdateResult(state.modify(_.startup.createConnection.form).setTo(updatedForm))
    } else {
      UpdateResult(state)
    }
  }

  private def createConnectionFormHandle(state: State, msg: CreateConnectionForm): UpdateResult =
    msg match {
      case GoToNextField =>
        UpdateResult(state.modify(_.startup.createConnection.form).using(_.cycleField))
      case SelectDBMS(db) =>
        UpdateResult(state.modify(_.startup.createConnection.form.db).setTo(db))
      case UpdateTextField(newValue) =>
        val form = state.startup.createConnection.form
        val updatedForm = if (form.activeField == 3 && newValue.toIntOption.isEmpty) {
          form.copy(errors = Some(form.errors.getOrElse(ValidationError(None, None, None)).copy(port = Some("Port must be a number"))))
        } else {
          form.updateActiveField(newValue).copy(errors = form.errors.map(_.copy(port = None)))
        }
        UpdateResult(state.modify(_.startup.createConnection.form).setTo(updatedForm))
    }

  private def prompt(msg: Msg, state: State): UpdateResult =
    state.prompt.fold(UpdateResult(state)) { p =>
      msg match {
        case Msg.PromptInput(c)  => UpdateResult(state.copy(prompt = Some(p.copy(input = p.input :+ c))))
        case Msg.PromptBackspace => UpdateResult(state.copy(prompt = Some(p.copy(input = p.input.dropRight(1)))))
        case Msg.PromptSubmit    =>
          val updated = state.copy(prompt = None, passwords = state.passwords + (p.connection -> p.input))
          UpdateResult(updated, List(p.pending.copy(password = Option(p.input).filter(_.nonEmpty))))
        case _ => UpdateResult(WorkspaceUpdate.cancelled(state.copy(prompt = None), p.pending.purpose))
      }
    }

  def update(msg: Msg, state: State): UpdateResult = msg match {
    case startup: Startup =>
      startup match {
        case EnterCreateConnection     => showCreateNewConnectionScreen(state)
        case CancelCreateConnection    => cancelCreateConnection(state)
        case SaveConnection            => saveConnection(state)
        case TestConnection            => testConnection(state)
        case ConnectConnection         => connectConnection(state)
        case msg: CreateConnectionForm => createConnectionFormHandle(state, msg)
        case CreateConnectionBackspace => handleBackspace(state)
        case EditConnection            => editConnection(state)
        case DeleteConnection          => deleteConnection(state)
        case SelectNextConnection      => UpdateResult(state.modify(_.startup).using(_.selectNext))
        case SelectPrevConnection      => UpdateResult(state.modify(_.startup).using(_.selectPrev))
        case OpenWorkspace             => WorkspaceUpdate.open(state)
        case QuitApp                   => UpdateResult(state) // Handled by layoutz layer
      }
    case w: WorkspaceMsg => WorkspaceUpdate.update(w, state)

    case Msg.QueryDone(Purpose.Test, result) =>
      val text = result.fold(e => s"Failed: $e", _ => "Connection OK")
      UpdateResult(state.modify(_.startup.createConnection.form.testResult).setTo(Some(text)))
    case Msg.QueryDone(purpose, result) => WorkspaceUpdate.queryDone(state, purpose, result)

    case Msg.ConnectionsLoaded(Right(text)) =>
      UpdateResult(state.modify(_.startup.connections).setTo(ConnectionStore.parse(text)))
    case Msg.ConnectionsLoaded(Left(err)) => UpdateResult(state.copy(notice = Some(s"Could not read saved connections: $err")))
    case Msg.ConnectionsSaved(Left(err))  => UpdateResult(state.copy(notice = Some(s"Could not save connections: $err")))
    case Msg.ConnectionsSaved(Right(_))   => UpdateResult(state)

    case Msg.PromptInput(_) | Msg.PromptBackspace | Msg.PromptSubmit | Msg.PromptCancel => prompt(msg, state)
    case Msg.NoOp => UpdateResult(state)
  }
}
