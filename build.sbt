import scoverage.ScoverageKeys
import org.scalafmt.sbt.ScalafmtPlugin
import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / scalaVersion := "2.13.16"
ThisBuild / majorVersion := 0

val appName = "self-assessment-assist"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    scalafmtOnCompile               := true,
    retrieveManaged                 := true,
    scalacOptions ++= List(
      "-Xfatal-warnings",
      "-Wconf:src=routes/.*:s",
      "-feature"
    )
  )
  .settings(
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    Compile / unmanagedClasspath += baseDirectory.value / "resources"
  )
  .settings(CodeCoverageSettings.settings: _*)
  .settings(PlayKeys.playDefaultPort := 8342)

  lazy val it = project
    .enablePlugins(PlayScala)
    .dependsOn(microservice % "test->test")
    .settings(DefaultBuildSettings.itSettings() ++ ScalafmtPlugin.scalafmtConfigSettings)
    .settings(
      Test / fork := true,
      Test / javaOptions += "-Dlogger.resource=logback-test.xml")
    .settings(libraryDependencies ++= AppDependencies.itDependencies)
    .settings(scalacOptions ++= Seq("-Xfatal-warnings")
    )