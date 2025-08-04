import sbt._

object AppDependencies {

  val bootStrapPlayVersion = "9.18.0"
  val hmrcMongoVersion = "2.7.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30"         % bootStrapPlayVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-work-item-repo-play-30" % hmrcMongoVersion,
    "org.typelevel"     %% "cats-core"                         % "2.13.0",
    "com.chuusai"       %% "shapeless"                         % "2.4.0-M1",
    "commons-codec"      % "commons-codec"                     % "1.18.0",
    "joda-time"          % "joda-time"                         % "2.14.0"
  )

  val test: Seq[sbt.ModuleID] = Seq(
    "uk.gov.hmrc"              %% "bootstrap-test-play-30"  % bootStrapPlayVersion,
    "uk.gov.hmrc.mongo"        %% "hmrc-mongo-test-play-30" % hmrcMongoVersion,
    "de.leanovate.play-mockws" %% "play-mockws-3-0"         % "3.0.8",
    "org.scalamock"            %% "scalamock"               % "7.3.3",
    "com.miguno.akka"          %% "akka-mock-scheduler"     % "0.5.5",
    "org.scalatestplus"        %% "mockito-4-5"             % "3.2.12.0",
    // NOTE: See https://github.com/vsch/flexmark-java to understand that
    //       versions 0.62.2 or below, Java 8 or above, Java 9+ compatible.
    //       while for Versions 0.64.0 or above, Java 11 or above.
    "com.vladsch.flexmark" % "flexmark-profile-pegdown" % "0.64.8"
  ).map(_ % Test)

  val itDependencies: Seq[ModuleID] = Seq(
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.19.1",
    "io.swagger.parser.v3"         % "swagger-parser-v3"     % "2.1.30"
  ).map(_ % Test)

}
