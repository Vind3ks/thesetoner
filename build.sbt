name := "thesetoner"

scalaVersion := "2.13.16"

// Keep stdin attached so console commands (say/join/admin add ...) work under `sbt run`
run / fork := true
run / connectInput := true
outputStrategy := Some(StdoutOutput)

// `sbt assembly` builds one runnable jar, used by the Docker image
assembly / mainClass := Some("bot")
assembly / assemblyJarName := "thesetoner.jar"
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "versions", _ @ _*) => MergeStrategy.first
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case PathList("META-INF", _ @ _*) => MergeStrategy.discard
  case "module-info.class" => MergeStrategy.discard
  case _ => MergeStrategy.first
}
