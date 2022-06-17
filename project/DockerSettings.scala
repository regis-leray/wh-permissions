import com.williamhill.bettingengine.sbt.FileSystem
import sbt.Keys.name
import sbt._
import sbtassembly.AssemblyKeys.assemblyOutputPath
import sbtassembly.AssemblyPlugin.autoImport._
import sbtdocker.DockerPlugin.autoImport._

object DockerSettings {
  val pushDockerRegistry = sys.env.getOrElse("NexusUploadRegistry", "nexus-uploads.dtc.prod.williamhill.plc")
  val pullDockerRegistry = sys.env.getOrElse("NexusDownloadRegistry", "docker-registry.prod.williamhill.plc")
  val dockerNamespace    = sys.env.getOrElse("Docker_Namespace", "platform")
  def dockerSettings(imageName: String, mainFQCN: String) = {

    List(
      docker / imageNames := Seq(
        ImageName(
          registry = Some(s"$pushDockerRegistry/$dockerNamespace"),
          repository = name.value,
          tag = Some(FileSystem.gitCommitHashes(1)),
        ),
      ),
      docker              := (docker dependsOn assembly).value,
      docker / dockerfile := mkDockerFile(imageName, (assembly / assemblyOutputPath).value, mainFQCN),
    )
  }

  private def mkDockerFile(appName: String, artifact: File, mainFullyQualifiedClassname: String): Dockerfile = {
    val artifactTargetPath = s"/app/${artifact.name}"
    val newrelicLicenseKey =
      sys.env.getOrElse("NEW_RELIC_LICENSE_KEY", "4f418044fd5c957c81dcfc25d4bb60c4abd9e5ff")
    val newrelicAppPath = "/opt/newrelic"

    new Dockerfile {
      from(s"--platform=linux/amd64 $pullDockerRegistry/betting-engine/rhel7-java-11-newrelic:0.0.9")
      workDir("/")
      env(
        "NEW_RELIC_ENABLED"     -> "true",
        "NEW_RELIC_APP_NAME"    -> appName,
        "NEW_RELIC_LICENSE_KEY" -> newrelicLicenseKey,
        "OTEL_TRACES_EXPORTER"  -> "none",
        "OTEL_METRICS_EXPORTER" -> "none",
      )
      runRaw("useradd -ms /bin/bash unityuser")
      runRaw("chown unityuser /opt/newrelic/newrelic.jar")
      user("unityuser")
      cmdRaw(
        s"java -Duser.timezone=UTC -Xms1G -Xmx2G -XX:+PrintGCDetails $$JAVA_OPTS -classpath $artifactTargetPath $mainFullyQualifiedClassname",
      )
      add(artifact, artifactTargetPath)
    }
  }
}
