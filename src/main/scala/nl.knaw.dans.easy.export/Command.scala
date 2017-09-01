package nl.knaw.dans.easy.export

import com.yourmediashelf.fedora.client.FedoraCredentials
import nl.knaw.dans.lib.error._

object Command extends App {

  val configuration = Configuration()
  val clo = new CommandLineOptions(args, configuration)

  FedoraProvider(new FedoraCredentials(
    configuration.properties.getString("default.fcrepo-server"),
    configuration.properties.getString("default.fcrepo-username"),
    configuration.properties.getString("default.fcrepo-password")
  ))
    .map(Settings(clo.datasetId(), clo.sdoSet(), _))
    .flatMap(implicit settings => EasyExportDataset.run)
    .doIfSuccess(ids => println(s"OK: Completed successfully. Ids: ${ ids.mkString(", ") }"))
    .doIfFailure { case e => println(s"FAILED: ${ e.getMessage }") }
}
