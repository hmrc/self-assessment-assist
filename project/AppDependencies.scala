import sbt._

object AppDependencies {

  val bootStrapPlayVersion = "9.13.0"
  val hmrcMongoVersion = "2.5.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30"         % bootStrapPlayVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-work-item-repo-play-30" % hmrcMongoVersion,
    "org.typelevel"     %% "cats-core"                         % "2.13.0",
    "com.chuusai"       %% "shapeless"                         % "2.4.0-M1",
    "commons-codec"      % "commons-codec"                     % "1.18.0",
    "joda-time"          % "joda-time"                         % "2.14.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"              %% "bootstrap-test-play-30"  % bootStrapPlayVersion % "test, it",
    "uk.gov.hmrc.mongo"        %% "hmrc-mongo-test-play-30" % hmrcMongoVersion     % "test, it",
    "de.leanovate.play-mockws" %% "play-mockws-3-0"         % "3.0.8"              % "test, it",
    "org.scalamock"            %% "scalamock"               % "7.3.3"              % "test, it",
    "com.miguno.akka"          %% "akka-mock-scheduler"     % "0.5.5"              % "test, it",
    "org.scalatestplus"        %% "mockito-4-5"             % "3.2.12.0"           % "test, it",
    // NOTE: See https://github.com/vsch/flexmark-java to understand that
    //       versions 0.62.2 or below, Java 8 or above, Java 9+ compatible.
    //       while for Versions 0.64.0 or above, Java 11 or above.
    "com.vladsch.flexmark" % "flexmark-profile-pegdown" % "0.64.8" % "test, it"
  )

}
