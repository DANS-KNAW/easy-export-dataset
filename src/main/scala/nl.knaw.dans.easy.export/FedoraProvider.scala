/*
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

import java.io.InputStream

import com.yourmediashelf.fedora.client.request.{ FedoraRequest, RiSearch }
import com.yourmediashelf.fedora.client.{ FedoraClient, FedoraCredentials }
import org.apache.commons.io.IOUtils
import resource._

import scala.util.{ Failure, Try }

class FedoraProvider(client: FedoraClient) {

  def getSubordinates(datasetId: String): Try[Seq[String]] = {
    search(
      s"""
         |PREFIX dans: <http://dans.knaw.nl/ontologies/relations#>
         |SELECT ?s WHERE {?s dans:isSubordinateTo <info:fedora/$datasetId> . }
         |""".stripMargin)
      .map(_.tail.map(_.split("/").last))
  }

  private def search(query: String): Try[Seq[String]] = {
    val riSearch = new RiSearch(query).lang("sparql").format("csv")
    managed(riSearch.execute(client))
      .flatMap(response => managed(response.getEntityInputStream))
      .map(is => new String(IOUtils.toByteArray(is)).split("\n").toSeq)
      .tried
      .recoverWith {
        case t: Throwable =>
          Failure(new Exception(s"$this, query '$query' failed, cause: ${ t.getMessage }", t))
      }
  }

  def disseminateDatastream(objectId: String, streamId: String): ManagedResource[InputStream] = {
    managed(FedoraClient.getDatastreamDissemination(objectId, streamId).execute(client))
      .flatMap(response => managed(response.getEntityInputStream))
  }

  def getFoXml(objectId: String): ManagedResource[InputStream] = {
    managed(FedoraClient.getObjectXML(objectId).execute(client))
      .flatMap(response => managed(response.getEntityInputStream))
  }
}

object FedoraProvider {
  def apply(credentials: FedoraCredentials): Try[FedoraProvider] = {
    Try {
      val fedoraClient = new FedoraClient(credentials)
      FedoraRequest.setDefaultClient(fedoraClient)
      new FedoraProvider(fedoraClient)
    }.recoverWith {
      case t: Throwable =>
        Failure(new Exception(s"could not set default fedora client with $credentials, cause: ${ t.getMessage }", t))
    }
  }
}
