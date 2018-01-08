/**
 * Copyright (C) 2015 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.export

import java.nio.file.Paths

import com.yourmediashelf.fedora.client.FedoraCredentials
import nl.knaw.dans.lib.error._

object Command extends App {

  val configuration = Configuration(Paths.get(System.getProperty("app.home")))
  val clo = new CommandLineOptions(args, configuration)

  FedoraProvider(new FedoraCredentials(
    configuration.properties.getString("default.fcrepo-server"),
    configuration.properties.getString("default.fcrepo-username"),
    configuration.properties.getString("default.fcrepo-password")
  ))
    .map(Settings(clo.datasetId(), clo.sdoSet(), _))
    .flatMap(implicit settings => EasyExportDataset.run)
    .doIfSuccess(ids => println(s"OK: Completed successfully. Exported: ${ ids.mkString(", ") }"))
    .doIfFailure { case e => println(s"FAILED: ${ e.getMessage }") }
}
