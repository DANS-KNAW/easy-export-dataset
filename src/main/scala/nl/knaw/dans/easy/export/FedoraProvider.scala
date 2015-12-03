/*******************************************************************************
  * Copyright 2015 DANS - Data Archiving and Networked Services
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  ******************************************************************************/

package nl.knaw.dans.easy.export

import java.io.InputStream

import com.yourmediashelf.fedora.client.FedoraClient._
import com.yourmediashelf.fedora.client.request.{FedoraRequest, RiSearch}
import com.yourmediashelf.fedora.client.{FedoraClient, FedoraCredentials}

import scala.util.{Failure, Try}

case class FedoraProvider private() {

  def getSubordinates(datasetId : String) : Try[Seq[String]] =
    search( s"""
               |PREFIX dans: <http://dans.knaw.nl/ontologies/relations#>
               |SELECT ?s WHERE {?s dans:isSubordinateTo <info:fedora/$datasetId> . }
               |""".stripMargin).map(_.tail.map(_.split("/").last))

  private def search(query: String
                    ): Try[Seq[String]] =
    (for {
      response <- Try {new RiSearch(query).lang("sparql").format("csv").execute()}
      is        = response.getEntityInputStream
      lines    <- is.readAndClose.map(new String(_).split("\n").toSeq)
    } yield lines)
      .recoverWith { case t: Throwable =>
        Failure(new Exception(s"$this, query '$query' failed, cause: ${t.getMessage}", t))
      }

  def disseminateDatastream(objectId: String,
                            streamId: String
                           ): Try[InputStream] =
    Try(getDatastreamDissemination(objectId, streamId).execute())
      .map(_.getEntityInputStream)
      .recoverWith { case t: Throwable =>
        Failure(new Exception(s"$this, could not get datastream $streamId of $objectId, cause: ${t.getMessage}", t))
      }

  def getFoXml(objectId: String
              ): Try[InputStream] =
    Try(getObjectXML(objectId).execute())
      .map(_.getEntityInputStream)
      .recoverWith { case t: Throwable =>
        Failure(new Exception(s"$this, could not get fo.xml of $objectId, cause: ${t.getMessage}", t))
      }
}

object FedoraProvider {
  def apply (credentials: FedoraCredentials): Try[FedoraProvider] =
    Try {
      val fedoraClient = new FedoraClient(credentials) {
        override def toString = s"${super.toString} with $credentials"
      }
      FedoraRequest.setDefaultClient(fedoraClient)
      new FedoraProvider() {
        override def toString = s"${super.toString} with $credentials"
      }
    }.recoverWith{case t: Throwable =>
      Failure(new Exception(s"could not set default fedora client with $credentials, cause: ${t.getMessage}",t))
    }
}
