package com.w47s0n.core.state

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import com.w47s0n.core.message.Startup.GoToNextField
import com.w47s0n.core.update.Update

class CreateConnectionTest extends AnyFlatSpec with Matchers {

  "ConnectionForm" should "have an empty error field on creation" in {
    val form = Startup.ConnectionForm.empty
    form.errors shouldBe None
  }

  it should "cycle through all fields when GoToNextField is dispatched" in {
    val initialState = State.empty
    initialState.startup.createConnection.form.activeField shouldBe 0

    val stateAfter1stCycle = Update.update(GoToNextField, initialState, None).state
    stateAfter1stCycle.startup.createConnection.form.activeField shouldBe 1

    val stateAfter2ndCycle = Update.update(GoToNextField, stateAfter1stCycle, None).state
    stateAfter2ndCycle.startup.createConnection.form.activeField shouldBe 2

    val stateAfter3rdCycle = Update.update(GoToNextField, stateAfter2ndCycle, None).state
    stateAfter3rdCycle.startup.createConnection.form.activeField shouldBe 3

    val stateAfter4thCycle = Update.update(GoToNextField, stateAfter3rdCycle, None).state
    stateAfter4thCycle.startup.createConnection.form.activeField shouldBe 0
  }

  it should "update the form state when text is entered" in {
    val initialState = State.empty
    val newName = "My Test Connection"
    val updatedState = Update.update(com.w47s0n.core.message.Startup.UpdateTextField(newName), initialState, None).state
    updatedState.startup.createConnection.form.connectionName shouldBe newName

    val stateWithPortActive = Update.update(GoToNextField, Update.update(GoToNextField, Update.update(GoToNextField, initialState, None).state, None).state, None).state
    stateWithPortActive.startup.createConnection.form.activeField shouldBe 3

    val newPort = "5432"
    val finalState = Update.update(com.w47s0n.core.message.Startup.UpdateTextField(newPort), stateWithPortActive, None).state
    finalState.startup.createConnection.form.port.toString shouldBe newPort
  }

  it should "populate the errors field when a non-numeric port is entered" in {
    val initialState = State.empty
    val stateWithPortActive = Update.update(GoToNextField, Update.update(GoToNextField, Update.update(GoToNextField, initialState, None).state, None).state, None).state
    stateWithPortActive.startup.createConnection.form.activeField shouldBe 3

    val invalidPort = "abc"
    val finalState = Update.update(com.w47s0n.core.message.Startup.UpdateTextField(invalidPort), stateWithPortActive, None).state
    finalState.startup.createConnection.form.errors shouldBe Some(Startup.ValidationError(None, Some("Port must be a number"), None))
  }

  it should "save connection to memory when SaveConnection is dispatched with valid form" in {
    val initialState = State.empty
    val initialConnectionsCount = initialState.startup.connections.size

    // Enter create connection mode and fill in the form
    val stateInCreateMode = Update.update(com.w47s0n.core.message.Startup.EnterCreateConnection, initialState, None).state
    stateInCreateMode.startup.isAtCreateConnection shouldBe true

    // Set connection name
    val stateWithName = Update.update(com.w47s0n.core.message.Startup.UpdateTextField("Test Connection"), stateInCreateMode, None).state
    stateWithName.startup.createConnection.form.connectionName shouldBe "Test Connection"

    // Move to host field and set host
    val stateAtHost = Update.update(GoToNextField, Update.update(GoToNextField, stateWithName, None).state, None).state
    stateAtHost.startup.createConnection.form.activeField shouldBe 2
    val stateWithHost = Update.update(com.w47s0n.core.message.Startup.UpdateTextField("myhost.local"), stateAtHost, None).state
    stateWithHost.startup.createConnection.form.host shouldBe "myhost.local"

    // Save the connection
    val finalState = Update.update(com.w47s0n.core.message.Startup.SaveConnection, stateWithHost, None).state

    // Should return to main screen
    finalState.startup.isAtCreateConnection shouldBe false

    // Connection should be added to the list
    finalState.startup.connections.size shouldBe (initialConnectionsCount + 1)
    finalState.startup.connections.last.name shouldBe "Test Connection"
  }

  it should "show validation error when SaveConnection is dispatched with empty name" in {
    val initialState = State.empty

    // Enter create connection mode
    val stateInCreateMode = Update.update(com.w47s0n.core.message.Startup.EnterCreateConnection, initialState, None).state

    // Move to host field and set host (but leave name empty)
    val stateAtHost = Update.update(GoToNextField, Update.update(GoToNextField, stateInCreateMode, None).state, None).state
    val stateWithHost = Update.update(com.w47s0n.core.message.Startup.UpdateTextField("myhost.local"), stateAtHost, None).state

    // Try to save
    val finalState = Update.update(com.w47s0n.core.message.Startup.SaveConnection, stateWithHost, None).state

    // Should stay on create connection screen
    finalState.startup.isAtCreateConnection shouldBe true

    // Should have validation error
    finalState.startup.createConnection.form.errors.flatMap(_.name) shouldBe Some("Name is required")
    finalState.startup.createConnection.form.errors.flatMap(_.host) shouldBe None // host is filled
  }

  it should "remove the last character when backspace is pressed" in {
    val initialState = State.empty

    // Set connection name
    val stateWithName = Update.update(com.w47s0n.core.message.Startup.UpdateTextField("Hello"), initialState, None).state
    stateWithName.startup.createConnection.form.connectionName shouldBe "Hello"

    // Press backspace
    val stateAfterBackspace = Update.update(com.w47s0n.core.message.Startup.CreateConnectionBackspace, stateWithName, None).state
    stateAfterBackspace.startup.createConnection.form.connectionName shouldBe "Hell"

    // Press backspace again
    val stateAfterBackspace2 = Update.update(com.w47s0n.core.message.Startup.CreateConnectionBackspace, stateAfterBackspace, None).state
    stateAfterBackspace2.startup.createConnection.form.connectionName shouldBe "Hel"
  }

  it should "do nothing when backspace is pressed on empty field" in {
    val initialState = State.empty
    initialState.startup.createConnection.form.connectionName shouldBe ""

    // Press backspace on empty field
    val stateAfterBackspace = Update.update(com.w47s0n.core.message.Startup.CreateConnectionBackspace, initialState, None).state
    stateAfterBackspace.startup.createConnection.form.connectionName shouldBe ""
  }

  // Name + host filled, still on create screen
  private def filledForm: State = {
    val inCreate = Update.update(com.w47s0n.core.message.Startup.EnterCreateConnection, State.empty, None).state
    val named    = Update.update(com.w47s0n.core.message.Startup.UpdateTextField("dev"), inCreate, None).state
    val atHost   = Update.update(GoToNextField, Update.update(GoToNextField, named, None).state, None).state
    Update.update(com.w47s0n.core.message.Startup.UpdateTextField("db.local"), atHost, None).state
  }

  it should "set a test result when TestConnection is dispatched with a valid form" in {
    val finalState = Update.update(com.w47s0n.core.message.Startup.TestConnection, filledForm, None).state
    finalState.startup.isAtCreateConnection shouldBe true
    finalState.startup.createConnection.form.testResult shouldBe Some("Connection OK")
    finalState.startup.connections shouldBe State.empty.startup.connections
  }

  it should "show validation errors and no test result when TestConnection is dispatched with empty form" in {
    val inCreate   = Update.update(com.w47s0n.core.message.Startup.EnterCreateConnection, State.empty, None).state
    val finalState = Update.update(com.w47s0n.core.message.Startup.TestConnection, inCreate, None).state
    finalState.startup.createConnection.form.errors shouldBe Some(Startup.ValidationError(Some("Host is required"), None, None, Some("Name is required")))
    finalState.startup.createConnection.form.testResult shouldBe None
  }

  it should "save and select the new connection when ConnectConnection is dispatched" in {
    val finalState = Update.update(com.w47s0n.core.message.Startup.ConnectConnection, filledForm, None).state
    finalState.startup.isAtCreateConnection shouldBe false
    finalState.startup.selectedConnection.map(_.name) shouldBe Some("dev")
  }

  it should "stay on the form when ConnectConnection is dispatched with invalid form" in {
    val inCreate   = Update.update(com.w47s0n.core.message.Startup.EnterCreateConnection, State.empty, None).state
    val finalState = Update.update(com.w47s0n.core.message.Startup.ConnectConnection, inCreate, None).state
    finalState.startup.isAtCreateConnection shouldBe true
    finalState.startup.selectedIndex shouldBe 0
  }

  it should "discard the form and return to startup when CancelCreateConnection is dispatched" in {
    val finalState = Update.update(com.w47s0n.core.message.Startup.CancelCreateConnection, filledForm, None).state
    finalState.startup.isAtCreateConnection shouldBe false
    finalState.startup.createConnection.form shouldBe Startup.ConnectionForm.empty
    finalState.startup.connections shouldBe State.empty.startup.connections
  }
}
