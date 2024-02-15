import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, integrationTestSettings}


val appName = "self-assessment-assist"

// val silencerVersion = "1.7.12"

lazy val ItTest = config("it") extend Test

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    majorVersion                     := 0,
    scalaVersion                     := "2.13.12",
    libraryDependencies              ++= AppDependencies.compile ++ AppDependencies.test(),

    // Use the silencer plugin to suppress warnings
    scalacOptions += "-Wconf:cat=unused-imports&src=routes/.*:s",

   // libraryDependencies ++= Seq(
   //   compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
   //   "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
   // )

    //
    // NOTE : This is how you would disable the HTML test reporter
    //        and thereafter get rid of the flexmark-java library above
    //
    // Test / testOptions := Seq(
    //   Tests.Argument(TestFrameworks.ScalaTest, "-oD")
    // )
  )
  .settings(inConfig(Test)(testSettings))
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(PlayKeys.playDefaultPort := 8342)
  .settings(CodeCoverageSettings.settings: _*)
  .settings(
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*components.*;.*repositories.*;" +
      ".*BuildInfo.*;.*javascript.*;.*Routes.*;.*config.*;.*TimeProvider;.*GuiceInjector;" +
      ".*ControllerConfiguration;.*testonly.*;",
    ScoverageKeys.coverageMinimumStmtTotal := 80)
  .settings(Compile / unmanagedResourceDirectories += baseDirectory.value / "resources")
  .configs(ItTest)
  .settings(
    inConfig(ItTest)(Defaults.itSettings ++ headerSettings(ItTest) ++ automateHeaderSettings(ItTest)),
    ItTest / fork := true,
    ItTest / unmanagedSourceDirectories := Seq((ItTest / baseDirectory).value / "it"),
    ItTest / unmanagedClasspath += baseDirectory.value / "resources",
    Runtime / unmanagedClasspath += baseDirectory.value / "resources",
    ItTest / parallelExecution := false,
    addTestReportOption(ItTest, "int-test-reports"))


lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  parallelExecution            := false,
  fork                         := true,
  javaOptions                  ++= Seq(
    "-Dconfig.resource=test.application.conf"
  )
)
