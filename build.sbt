import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings


val appName = "self-assessment-assist"

val silencerVersion = "1.7.12"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    majorVersion                     := 0,
    scalaVersion                     := "2.13.8",
    libraryDependencies              ++= AppDependencies.compile ++ AppDependencies.test,

    // Use the silencer plugin to suppress warnings
    scalacOptions += "-P:silencer:pathFilters=routes",

    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    )

    //
    // NOTE : This is how you would disable the HTML test reporter
    //        and thereafter get rid of the flexmark-java library above
    //
    // Test / testOptions := Seq(
    //   Tests.Argument(TestFrameworks.ScalaTest, "-oD")
    // )
  )
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
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

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  parallelExecution            := false,
  fork                         := true,
  javaOptions                  ++= Seq(
    "-Dconfig.resource=test.application.conf"
  )
)