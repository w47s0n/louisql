package com.w47s0n.core.state

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import com.w47s0n.core.message.Startup.GoToNextField
import com.w47s0n.core.effect.{Effect, Purpose, QueryResult}
import com.w47s0n.core.message.Msg
import com.w47s0n.core.update.Update

class CreateConnectionTest extends AnyFlatSpec with Matchers {

  "ConnectionForm" should "have an empty error field on creation" in {
    val form = Startup.ConnectionForm.empty
    form.errors shouldBe None
  }

  it should "cycle through all fields when GoToNextField is dispatched" in {
    // Name, Engine, Host, Port, User, Password, Database, then back to Name
    val visited = (1 to 7).scanLeft(Fixtures.state)((s, _) => Update.update(GoToNextField, s).state)
    visited.map(_.startup.createConnection.form.activeField) shouldBe Seq(0, 1, 2, 3, 4, 5, 6, 0)
  }

  it should "update the form state when text is entered" in {
    val initialState = Fixtures.state
    val newName = "My Test Connection"
    val updatedState = Update.update(com.w47s0n.core.message.Startup.UpdateTextField(newName), initialState).state
    updatedState.startup.createConnection.form.connectionName shouldBe newName

    val stateWithPortActive = Update.update(GoToNextField, Update.update(GoToNextField, Update.update(GoToNextField, initialState).state).state).state
    stateWithPortActive.startup.createConnection.form.activeField shouldBe 3

    val newPort = "5432"
    val finalState = Update.update(com.w47s0n.core.message.Startup.UpdateTextField(newPort), stateWithPortActive).state
    finalState.startup.createConnection.form.port.toString shouldBe newPort
  }

  it should "populate the errors field when a non-numeric port is entered" in {
    val initialState = Fixtures.state
    val stateWithPortActive = Update.update(GoToNextField, Update.update(GoToNextField, Update.update(GoToNextField, initialState).state).state).state
    stateWithPortActive.startup.createConnection.form.activeField shouldBe 3

    val invalidPort = "abc"
    val finalState = Update.update(com.w47s0n.core.message.Startup.UpdateTextField(invalidPort), stateWithPortActive).state
    finalState.startup.createConnection.form.errors shouldBe Some(Startup.ValidationError(None, Some("Port must be a number"), None))
  }

  it should "save connection to memory when SaveConnection is dispatched with valid form" in {
    val initialState = Fixtures.state
    val initialConnectionsCount = initialState.startup.connections.size

    // Enter create connection mode and fill in the form
    val stateInCreateMode = Update.update(com.w47s0n.core.message.Startup.EnterCreateConnection, initialState).state
    stateInCreateMode.startup.isAtCreateConnection shouldBe true

    // Set connection name
    val stateWithName = Update.update(com.w47s0n.core.message.Startup.UpdateTextField("Test Connection"), stateInCreateMode).state
    stateWithName.startup.createConnection.form.connectionName shouldBe "Test Connection"

    // Move to host field and set host
    val stateAtHost = Update.update(GoToNextField, Update.update(GoToNextField, stateWithName).state).state
    stateAtHost.startup.createConnection.form.activeField shouldBe 2
    val stateWithHost = Update.update(com.w47s0n.core.message.Startup.UpdateTextField("myhost.local"), stateAtHost).state
    stateWithHost.startup.createConnection.form.host shouldBe "myhost.local"

    // Save the connection
    val finalState = Update.update(com.w47s0n.core.message.Startup.SaveConnection, stateWithHost).state

    // Should return to main screen
    finalState.startup.isAtCreateConnection shouldBe false

    // Connection should be added to the list
    finalState.startup.connections.size shouldBe (initialConnectionsCount + 1)
    finalState.startup.connections.last.name shouldBe "Test Connection"
  }

  it should "show validation error when SaveConnection is dispatched with empty name" in {
    val initialState = Fixtures.state

    // Enter create connection mode
    val stateInCreateMode = Update.update(com.w47s0n.core.message.Startup.EnterCreateConnection, initialState).state

    // Move to host field and set host (but leave name empty)
    val stateAtHost = Update.update(GoToNextField, Update.update(GoToNextField, stateInCreateMode).state).state
    val stateWithHost = Update.update(com.w47s0n.core.message.Startup.UpdateTextField("myhost.local"), stateAtHost).state

    // Try to save
    val finalState = Update.update(com.w47s0n.core.message.Startup.SaveConnection, stateWithHost).state

    // Should stay on create connection screen
    finalState.startup.isAtCreateConnection shouldBe true

    // Should have validation error
    finalState.startup.createConnection.form.errors.flatMap(_.name) shouldBe Some("Name is required")
    finalState.startup.createConnection.form.errors.flatMap(_.host) shouldBe None // host is filled
  }

  it should "remove the last character when backspace is pressed" in {
    val initialState = Fixtures.state

    // Set connection name
    val stateWithName = Update.update(com.w47s0n.core.message.Startup.UpdateTextField("Hello"), initialState).state
    stateWithName.startup.createConnection.form.connectionName shouldBe "Hello"

    // Press backspace
    val stateAfterBackspace = Update.update(com.w47s0n.core.message.Startup.CreateConnectionBackspace, stateWithName).state
    stateAfterBackspace.startup.createConnection.form.connectionName shouldBe "Hell"

    // Press backspace again
    val stateAfterBackspace2 = Update.update(com.w47s0n.core.message.Startup.CreateConnectionBackspace, stateAfterBackspace).state
    stateAfterBackspace2.startup.createConnection.form.connectionName shouldBe "Hel"
  }

  it should "do nothing when backspace is pressed on empty field" in {
    val initialState = Fixtures.state
    initialState.startup.createConnection.form.connectionName shouldBe ""

    // Press backspace on empty field
    val stateAfterBackspace = Update.update(com.w47s0n.core.message.Startup.CreateConnectionBackspace, initialState).state
    stateAfterBackspace.startup.createConnection.form.connectionName shouldBe ""
  }

  // Name + host filled, still on create screen
  private def filledForm: State = {
    val inCreate = Update.update(com.w47s0n.core.message.Startup.EnterCreateConnection, Fixtures.state).state
    val named    = Update.update(com.w47s0n.core.message.Startup.UpdateTextField("dev"), inCreate).state
    val atHost   = Update.update(GoToNextField, Update.update(GoToNextField, named).state).state
    Update.update(com.w47s0n.core.message.Startup.UpdateTextField("db.local"), atHost).state
  }

  it should "ask for a real test query and show its outcome" in {
    val testing = Update.update(com.w47s0n.core.message.Startup.TestConnection, filledForm)
    testing.state.startup.createConnection.form.testResult shouldBe Some("Testing…")
    testing.effects.collect { case q: Effect.Query => (q.sql, q.purpose) } shouldBe List(("SELECT 1", Purpose.Test))
    testing.state.startup.connections shouldBe Fixtures.state.startup.connections

    val ok = Update.update(Msg.QueryDone(Purpose.Test, Right(QueryResult.Affected(0, 1))), testing.state).state
    ok.startup.createConnection.form.testResult shouldBe Some("Connection OK")
    val failed = Update.update(Msg.QueryDone(Purpose.Test, Left("Access denied")), testing.state).state
    failed.startup.createConnection.form.testResult shouldBe Some("Failed: Access denied")
  }

  it should "show validation errors and no test result when TestConnection is dispatched with empty form" in {
    val inCreate   = Update.update(com.w47s0n.core.message.Startup.EnterCreateConnection, Fixtures.state).state
    val finalState = Update.update(com.w47s0n.core.message.Startup.TestConnection, inCreate).state
    finalState.startup.createConnection.form.errors shouldBe Some(Startup.ValidationError(Some("Host is required"), None, None, Some("Name is required")))
    finalState.startup.createConnection.form.testResult shouldBe None
  }

  it should "save and select the new connection when ConnectConnection is dispatched" in {
    val finalState = Update.update(com.w47s0n.core.message.Startup.ConnectConnection, filledForm).state
    finalState.startup.isAtCreateConnection shouldBe false
    finalState.startup.selectedConnection.map(_.name) shouldBe Some("dev")
  }

  it should "stay on the form when ConnectConnection is dispatched with invalid form" in {
    val inCreate   = Update.update(com.w47s0n.core.message.Startup.EnterCreateConnection, Fixtures.state).state
    val finalState = Update.update(com.w47s0n.core.message.Startup.ConnectConnection, inCreate).state
    finalState.startup.isAtCreateConnection shouldBe true
    finalState.startup.selectedIndex shouldBe 0
  }

  it should "discard the form and return to startup when CancelCreateConnection is dispatched" in {
    val finalState = Update.update(com.w47s0n.core.message.Startup.CancelCreateConnection, filledForm).state
    finalState.startup.isAtCreateConnection shouldBe false
    finalState.startup.createConnection.form shouldBe Startup.ConnectionForm.empty
    finalState.startup.connections shouldBe Fixtures.state.startup.connections
  }

  "ConnectionForm.fromConnection" should "fill the form from a connection URL" in {
    val conn = Startup.Connection("dev", ConnectionUrl("mysql://bob:s3:cret@db.local:3310/app"))
    val form = Startup.ConnectionForm.fromConnection(conn)
    form.connectionName shouldBe "dev"
    form.db shouldBe Startup.ConnectionForm.Database.Mysql
    form.host shouldBe "db.local"
    form.port shouldBe 3310
    form.auth shouldBe Some(Startup.Authentication("bob", "s3:cret"))
    form.database shouldBe "app"
  }

  it should "fall back to a name-only form for an unparseable URL" in {
    val form = Startup.ConnectionForm.fromConnection(Startup.Connection("bad", ConnectionUrl("not a url")))
    form shouldBe Startup.ConnectionForm.empty.copy(connectionName = "bad")
  }

  "EditConnection" should "open the form prefilled from the selected connection" in {
    val state = Update.update(com.w47s0n.core.message.Startup.EditConnection, Fixtures.state).state
    state.startup.isAtCreateConnection shouldBe true
    state.startup.createConnection.editingIndex shouldBe Some(0)
    state.startup.createConnection.form.connectionName shouldBe "localhost"
    state.startup.createConnection.form.host shouldBe "localhost"
    state.startup.createConnection.form.port shouldBe 3306
    state.startup.createConnection.form.user shouldBe "root"
    state.startup.createConnection.form.database shouldBe "mydb"
  }

  it should "do nothing when there are no connections" in {
    val noConnections = Fixtures.state.copy(startup = Fixtures.state.startup.copy(connections = Nil))
    Update.update(com.w47s0n.core.message.Startup.EditConnection, noConnections).state shouldBe noConnections
  }

  it should "replace the connection in place on save instead of appending" in {
    val selected = Fixtures.state.copy(startup = Fixtures.state.startup.copy(selectedIndex = 1))
    val editing  = Update.update(com.w47s0n.core.message.Startup.EditConnection, selected).state
    val renamed  = editing.copy(startup = editing.startup.copy(createConnection =
      editing.startup.createConnection.copy(form = editing.startup.createConnection.form.copy(connectionName = "prod"))))
    val saved = Update.update(com.w47s0n.core.message.Startup.SaveConnection, renamed).state
    saved.startup.connections.map(_.name) shouldBe List("localhost", "prod", "staging")
    saved.startup.createConnection shouldBe Startup.CreateConnection.empty
  }

  it should "keep the edited connection selected on ConnectConnection" in {
    val selected = Fixtures.state.copy(startup = Fixtures.state.startup.copy(selectedIndex = 1))
    val editing  = Update.update(com.w47s0n.core.message.Startup.EditConnection, selected).state
    val finalState = Update.update(com.w47s0n.core.message.Startup.ConnectConnection, editing).state
    finalState.startup.connections.size shouldBe 3
    finalState.startup.selectedIndex shouldBe 1
  }

  it should "clear the editing index on cancel" in {
    val editing = Update.update(com.w47s0n.core.message.Startup.EditConnection, Fixtures.state).state
    val cancelled = Update.update(com.w47s0n.core.message.Startup.CancelCreateConnection, editing).state
    cancelled.startup.createConnection.editingIndex shouldBe None
  }

  it should "let backspace clear the port so a new one can be typed" in {
    val atPort  = (1 to 3).foldLeft(Fixtures.state)((s, _) => Update.update(GoToNextField, s).state)
    val cleared = (1 to 4).foldLeft(atPort)((s, _) => Update.update(com.w47s0n.core.message.Startup.CreateConnectionBackspace, s).state)
    cleared.startup.createConnection.form.port shouldBe 0
    val typed = "13306".foldLeft(cleared) { (s, c) =>
      Update.update(com.w47s0n.core.message.Startup.UpdateTextField(s.startup.createConnection.form.activeFieldValue :+ c), s).state
    }
    typed.startup.createConnection.form.port shouldBe 13306
  }
}
