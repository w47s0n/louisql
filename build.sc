import mill._
import mill.scalalib._

trait CommonModule extends ScalaModule {
  def scalaVersion = "3.7.4"
}

// Core module: Pure ELM business logic (Model, Msg, Update)
// No UI dependencies - can be used by layoutz, Tyrian, or any ELM-based frontend
object core extends CommonModule {

  def mvnDeps = Seq(
    mvn"com.softwaremill.quicklens::quicklens::1.9.9"
  )

  object test extends ScalaTests with TestModule.ScalaTest {
    def mvnDeps = Seq(
      mvn"org.scalatest::scalatest::3.2.19"
    )
  }
}

// App module: Presentation layer using layoutz (TUI)
// Depends on core for business logic
// Future: Add Tyrian+Electron for Desktop app
object app extends CommonModule {

  def moduleDeps = Seq(core)

  def mvnDeps = Seq(
    mvn"xyz.matthieucourt::layoutz:0.5.0"
  )

  object test extends ScalaTests with TestModule.ScalaTest {
    def mvnDeps = Seq(
      mvn"org.scalatest::scalatest::3.2.19"
    )
  }
}
