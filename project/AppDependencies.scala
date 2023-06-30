import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % "7.12.0",
    "org.typelevel"           %% "cats-core"                  % "2.7.0",
    "com.chuusai"             %% "shapeless"                 % "2.4.0-M1",
    "commons-codec"           % "commons-codec"               % "1.15"
  )

  val test = Seq(
    "uk.gov.hmrc"              %% "bootstrap-test-play-28"     % "7.12.0"             % "test, it",
    "de.leanovate.play-mockws" %% "play-mockws"                % "2.8.1"              % "test, it",
    "org.scalamock"            %% "scalamock"                  % "5.2.0"              % "test, it",
    "com.miguno.akka"          %% "akka-mock-scheduler"        % "0.5.5"              % "test, it",
    "org.scalatestplus"        %% "mockito-4-5"                % "3.2.12.0"           % "test, it",
  // NOTE: See https://github.com/vsch/flexmark-java to understand that
  //       versions 0.62.2 or below, Java 8 or above, Java 9+ compatible.
  //       while for Versions 0.64.0 or above, Java 11 or above.
  "com.vladsch.flexmark"        % "flexmark-profile-pegdown"    % "0.62.2"             % "test, it"
  )
}
