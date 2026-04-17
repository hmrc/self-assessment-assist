import sbt.*

object AppDependencies {

  val bootStrapPlayVersion = "10.7.0"
  val hmrcMongoVersion = "2.12.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30"         % bootStrapPlayVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-work-item-repo-play-30" % hmrcMongoVersion,
    "org.typelevel"     %% "cats-core"                         % "2.13.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatestplus" %% "scalacheck-1-18"        % "3.2.19.0",
    "uk.gov.hmrc"              %% "bootstrap-test-play-30"  % bootStrapPlayVersion,
    "uk.gov.hmrc.mongo"        %% "hmrc-mongo-test-play-30" % hmrcMongoVersion,
    "org.scalamock"            %% "scalamock"               % "7.5.5"
  ).map(_ % Test)

  val itDependencies: Seq[ModuleID] = Seq(
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.21.1",
    "io.swagger.parser.v3"         % "swagger-parser-v3"     % "2.1.39"
  ).map(_ % Test)

}
