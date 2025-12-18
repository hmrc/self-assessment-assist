import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / scalaVersion := "3.4.3"
ThisBuild / majorVersion := 0
ThisBuild / scalacOptions ++= Seq(
  "-Werror",
  "-Wconf:msg=Flag.*repeatedly:s"
)
ThisBuild / scalacOptions += "-nowarn" // Added help suppress warnings in migration. Must be removed when changes shown are complete
ThisBuild / scalafmtOnCompile := true

val appName = "self-assessment-assist"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    scalacOptions ++= Seq(
      "-feature",
      "-Wconf:src=routes/.*:s"
    )
  )
  .settings(
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    Compile / unmanagedClasspath += baseDirectory.value / "resources"
  )
  .settings(CodeCoverageSettings.settings)
  .settings(PlayKeys.playDefaultPort := 8342)

  lazy val it = project
    .enablePlugins(PlayScala)
    .dependsOn(microservice % "test->test")
    .settings(DefaultBuildSettings.itSettings())
    .settings(
      Test / fork := true,
      Test / javaOptions += "-Dlogger.resource=logback-test.xml"
    )
    .settings(libraryDependencies ++= AppDependencies.itDependencies)