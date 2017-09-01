/**
 * Copyright (C) 2015 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.export

import java.io.File

import com.yourmediashelf.fedora.client.FedoraCredentials

import scala.util.Try

case class Settings(datasetId: String,
                    sdoSet: File,
                    fedora: FedoraProvider) {
}

object Settings {
  def apply(conf: Conf
           ): Try[Settings] = {
    FedoraProvider(createCredentials(conf))
      .map(new Settings(conf.datasetId(), conf.sdoSet(), _))
  }

  def createCredentials(conf: Conf
                       ): FedoraCredentials {def toString: String} = {
    new FedoraCredentials(
      conf.fedora(),
      conf.user(),
      conf.password()
    ) {
      override def toString = s"FedoraCredentials (${conf.fedora()}, ${conf.user()}, ...)"
    }
  }
}
