import sbt.Keys.libraryDependencies
import sbt._
import play.core.PlayVersion

object AppDependencies {

  private val bootstrapVersion = "10.1.0"
  private val hmrcMongoVersion = "2.7.0"

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30"       % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"              % hmrcMongoVersion,
    "org.apache.pekko"  %% "pekko-stream"                    % PlayVersion.pekkoVersion,
    "org.apache.pekko"  %% "pekko-connectors-json-streaming" % "1.0.2"
  )

  val test = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % bootstrapVersion % Test,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % hmrcMongoVersion % Test
  )

  val it = Seq.empty
}
