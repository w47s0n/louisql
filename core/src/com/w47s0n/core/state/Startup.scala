package com.w47s0n.core.state

import com.softwaremill.quicklens.*
import java.net.URI
import scala.util.Try
import com.w47s0n.core.state.Startup.Authentication
import com.w47s0n.core.state.Startup.Connection
import com.w47s0n.core.state.Startup.ConnectionForm.Database
import com.w47s0n.core.state.Startup.CreateConnection

opaque type ConnectionUrl = String

object ConnectionUrl {
  def apply(value: String): ConnectionUrl = value

  def fromForm(form: Startup.ConnectionForm): ConnectionUrl = {
    val host = if (form.host.isEmpty) "localhost" else form.host
    form.db.makeUrl(host, form.port, form.database, form.auth.map(_.user).getOrElse(""))
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

  def deleteSelected: Startup = {
    val remaining = connections.patch(selectedIndex, Nil, 1)
    copy(connections = remaining, selectedIndex = selectedIndex.min((remaining.size - 1).max(0)))
  }
}

object Startup {
  object ConnectionForm {
    val empty:ConnectionForm = ConnectionForm(Database.Mysql)
    def apply(db: Database): ConnectionForm = {
      ConnectionForm("", db, "", db.defaultPort, Some(Authentication("", "")), 0, None)
    }

    // Inverse of Database.makeUrl; falls back to a name-only form if the URL doesn't parse.
    // The password is never in the URL, so it comes back empty.
    def fromConnection(c: Connection): ConnectionForm =
      Try(URI(c.url.value)).toOption
        .flatMap(uri => Database.values.find(_.protocol == uri.getScheme).map(uri -> _))
        .fold(empty.copy(connectionName = c.name)) { case (uri, db) =>
          ConnectionForm(db).copy(
            connectionName = c.name,
            host = Option(uri.getHost).getOrElse(""),
            port = if (uri.getPort == -1) db.defaultPort else uri.getPort,
            auth = Option(uri.getUserInfo).map(_.split(":", 2)).map(p => Authentication(p(0), p.lift(1).getOrElse(""))),
            database = Option(uri.getPath).getOrElse("").stripPrefix("/"),
          )
        }

    enum Database(val protocol: String, val defaultPort: Int) {
      // Never carries the password: URLs are shown on screen and saved to disk
      def makeUrl(host: String, port: Int, database: String, user: String): String = {
        val userPart = if (user.isEmpty) "" else s"$user@"
        s"$protocol://$userPart$host:$port/$database"
      }

      case Postgres extends Database("postgresql", 5432)
      case Mysql    extends Database("mysql", 3306)
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
    database: String = "",
  ) {
    def user: String     = auth.map(_.user).getOrElse("")
    def password: String = auth.map(_.password).getOrElse("")

    private def withAuth(f: Authentication => Authentication): ConnectionForm =
      copy(auth = Some(f(auth.getOrElse(Authentication("", "")))))

    def isSelectingDb: Boolean = activeField == 1

    def updateActiveField(v: String): ConnectionForm =
      activeField match {
        case 0 => copy(connectionName = v)
        case 2 => this.modify(_.host).setTo(v)
        case 3 => this.modify(_.port).setTo(if (v.isEmpty) 0 else v.toIntOption.getOrElse(port)) // 0 = cleared
        case 4 => withAuth(_.copy(user = v))
        case 5 => withAuth(_.copy(password = v))
        case 6 => copy(database = v)
        case _ => this
      }

    def activeFieldValue: String =
      activeField match { // is this specifically neeeded by TUI ? if yes, this should be removed
        case 0 => connectionName
        case 2 => host
        case 3 => port.toString
        case 4 => user
        case 5 => password
        case 6 => database
        case _ => ""
      }

    def cycleField: ConnectionForm = {
      val totalFields = 7
      // TODO: validation
      this.modify(_.activeField).using(current => (current + 1) % totalFields)
    }
  }
  case class CreateConnection(isActive: Boolean, form: ConnectionForm, editingIndex: Option[Int] = None)
  object CreateConnection {
    val empty: CreateConnection                            = CreateConnection("", "")
    def apply(name: String, url: String): CreateConnection =
      CreateConnection(isActive = false, ConnectionForm.empty)
  }
  case class Connection(name: String, url: ConnectionUrl)

  val empty: Startup = Startup(
    isAtCreateConnection = false,
    CreateConnection.empty,
    connections = Nil, // loaded from disk at start
    selectedIndex = 0,
  )
}
