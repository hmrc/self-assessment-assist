import sbt._

object AppDependencies {

  val bootstrapPlayVersion = "8.1.0"


  val compile = Seq(
    "uk.gov.hmrc"   %% "bootstrap-backend-play-28" % bootstrapPlayVersion,
    "org.typelevel" %% "cats-core"                 % "2.7.0",
    "com.chuusai"   %% "shapeless"                 % "2.4.0-M1",
    "commons-codec"  % "commons-codec"             % "1.15"
  )

  def test(scope: String = "test, it"): Seq[sbt.ModuleID] = Seq(
    "uk.gov.hmrc"              %% "bootstrap-test-play-28" % bootstrapPlayVersion   % scope,
    "de.leanovate.play-mockws" %% "play-mockws"            % "2.8.1"    % scope,
    "org.scalamock"            %% "scalamock"              % "5.2.0"    % scope,
    "com.miguno.akka"          %% "akka-mock-scheduler"    % "0.5.5"    % scope,
    "org.scalatestplus"        %% "mockito-4-5"            % "3.2.12.0" % scope,
    "org.wiremock"              % "wiremock"               % "3.0.4"    % scope,
  // NOTE: See https://github.com/vsch/flexmark-java to understand that
    //       versions 0.62.2 or below, Java 8 or above, Java 9+ compatible.
    //       while for Versions 0.64.0 or above, Java 11 or above.
    "com.vladsch.flexmark" % "flexmark-profile-pegdown" % "0.62.2" % scope
  )

}
