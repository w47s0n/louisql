package com.w47s0n.core.state

import com.softwaremill.quicklens.*
import com.w47s0n.core.state.Startup.Authentication
import com.w47s0n.core.state.Startup.Connection
import com.w47s0n.core.state.Startup.ConnectionForm.Database
import com.w47s0n.core.state.Startup.CreateConnection

opaque type ConnectionUrl = String

object ConnectionUrl {
  def apply(value: String): ConnectionUrl = value

  def fromForm(form: Startup.ConnectionForm): ConnectionUrl = {
    val host = if (form.host.isEmpty) "localhost" else form.host
    form.db.makeUrl(host, form.port, form.connectionName, form.auth)
  }

  extension (url: ConnectionUrl) {
    def value: String = url
  }
}

case class Startup(
  isAtCreateConnection: Boolean,
  createConnection: CreateConnection,
  connections: List[Connection],
  selectedIndex: Int,
) {
  def selectedConnection: Option[Connection] =
    connections.lift(selectedIndex)

  def selectNext: Startup =
    if (connections.isEmpty) this
    else copy(selectedIndex = (selectedIndex + 1) % connections.size)

  def selectPrev: Startup =
    if (connections.isEmpty) this
    else copy(selectedIndex = (selectedIndex - 1 + connections.size) % connections.size)
}

object Startup {
  object ConnectionForm {
    val empty:ConnectionForm = ConnectionForm(Database.Postgres)
    def apply(db: Database): ConnectionForm = {
      ConnectionForm("", db, "", db.defaultPort, Some(Authentication("", "")), 0, None)
    }
    enum Database(val protocol: String, val defaultPort: Int) {
      def makeUrl(host: String, port: Int, name: String, auth: Option[Authentication]): String = auth
        .map(auth => s"$protocol://${auth.user}:${auth.password}@$host:$port/$name")
        .getOrElse(s"$protocol://$host:$port/$name")

      case Postgres extends Database("postgresql", 5432)
      case Mysql    extends Database("mysql", 3307)
    }
  }

  case class ValidationError(host: Option[String], port: Option[String], user: Option[String], name: Option[String] = None)
  case class Authentication(user: String, password: String)
  case class ConnectionForm(
    connectionName: String,
    db: Database,
    host: String,
    port: Int,
    auth: Option[Authentication],
    activeField: Int,
    errors: Option[ValidationError],
    testResult: Option[String] = None,
  ) {

    def isSelectingDb: Boolean = activeField == 1

    def updateActiveField(v: String): ConnectionForm =
      activeField match {
        case 0 => copy(connectionName = v)
        case 2 => this.modify(_.host).setTo(v)
        case 3 => this.modify(_.port).setTo(v.toIntOption.getOrElse(port))
        case _ => this
      }

    def activeFieldValue: String =
      activeField match { // is this specifically neeeded by TUI ? if yes, this should be removed
        case 0 => connectionName
        case 2 => host
        case 3 => port.toString
        case _ => ""
      }

    def cycleField: ConnectionForm = {
      val totalFields = 4
      // TODO: validation
      this.modify(_.activeField).using(current => (current + 1) % totalFields)
    }
  }
  case class CreateConnection(isActive: Boolean, form: ConnectionForm)
  object CreateConnection {
    val empty: CreateConnection                            = CreateConnection("", "")
    def apply(name: String, url: String): CreateConnection =
      CreateConnection(isActive = false, ConnectionForm.empty)
  }
  case class Connection(name: String, url: ConnectionUrl)

  val empty: Startup = Startup(
    isAtCreateConnection = false,
    CreateConnection.empty,
    connections = List(
      Connection("localhost", ConnectionUrl("postgresql://localhost:5432/mydb")),
      Connection("production", ConnectionUrl("postgresql://prod.example.com:5432/proddb")),
      Connection("staging", ConnectionUrl("postgresql://staging.example.com:5432/stagingdb")),
    ),
    selectedIndex = 0,
  )
}
