val ScalaVersion = "2.11.12"
val SparkVersion = "2.4.0"
val GCloudVersion = "1.80.0"

val sparkCore =  "org.apache.spark" %% "spark-core" % SparkVersion
val sparkSql =  "org.apache.spark" %% "spark-sql" % SparkVersion
val gcloudBQ = "com.google.cloud" % "google-cloud-bigquery" % GCloudVersion
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.5"

version in ThisBuild := "0.2.0"

lazy val commonSettings = Seq(
  organization := "com.github.tharwaninitin"
  , scalaVersion := ScalaVersion
)

lazy val etljobsSettings = Seq(
  name := "etljobs"
  , libraryDependencies ++= Seq( 
    sparkCore % Provided, sparkSql % Provided,  // For Spark jobs in SparkSteps
    gcloudBQ % Provided,                        // For using Big-query java API in BQLoadStep
    scalaTest % Test
  )
)

lazy val examplesSettings = Seq(
  name := "examples"
  , libraryDependencies ++= Seq(
    sparkCore, sparkSql,  // For Spark jobs in SparkSteps
    gcloudBQ,             // For using Big-query java API in BQLoadStep
    scalaTest % Test
  )
)

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .aggregate(etljobs, examples)

lazy val etljobs = (project in file("etljobs"))
  .settings(commonSettings,etljobsSettings)
  .settings(
    initialCommands := "import etljobs._"
  )

lazy val examples = (project in file("examples"))
  .settings(commonSettings,examplesSettings)
  .dependsOn(etljobs)

