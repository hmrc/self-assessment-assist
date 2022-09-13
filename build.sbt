import sbt.Keys.testOptions
import sbt.Test
import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "self-assessment-assist"

val silencerVersion = "1.7.7"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    majorVersion                     := 0,
    scalaVersion                     := "2.12.15",
    libraryDependencies              ++= AppDependencies.compile ++ AppDependencies.test,

    // Use the silencer plugin to suppress warnings
    scalacOptions += "-P:silencer:pathFilters=routes",

    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full,
      "org.scalatestplus" %% "mockito-4-5" % "3.2.12.0" % Test,

      // NOTE: See https://github.com/vsch/flexmark-java to understand that
      //       versions 0.62.2 or below, Java 8 or above, Java 9+ compatible.
      //       while for Versions 0.64.0 or above, Java 11 or above.
      "com.vladsch.flexmark" % "flexmark-profile-pegdown" % "0.62.2" % Test
    )

    //
    // NOTE : This is how you would disable the HTML test reporter
    //        and thereafter get rid of the flexmark-java library above
    //
    // Test / testOptions := Seq(
    //   Tests.Argument(TestFrameworks.ScalaTest, "-oD")
    // )
  )
  .settings(publishingSettings: _*)
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(PlayKeys.playDefaultPort := 8242)
  .settings(CodeCoverageSettings.settings: _*)
  .settings(Compile / unmanagedResourceDirectories += baseDirectory.value / "resources")
