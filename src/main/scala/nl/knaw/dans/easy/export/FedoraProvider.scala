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
import com.yourmediashelf.fedora.generated.management.DatastreamProfile

import scala.collection.JavaConversions._
import scala.io.Source.fromInputStream
import scala.util.{Success, Try}

case class FedoraProvider(credentials: FedoraCredentials) {

  FedoraRequest.setDefaultClient(
    new FedoraClient(credentials) {
      override def toString = s"${super.toString} ($credentials)"
    }
  )
  override def toString = s"${super.toString} ($credentials)"

  def getSubordinates(datasetId : String) : Try[Seq[String]] =
    search( s"""
               |PREFIX dans: <http://dans.knaw.nl/ontologies/relations#>
               |SELECT ?s WHERE {?s dans:isSubordinateTo <info:fedora/$datasetId> . }
               |""".stripMargin).map(_.tail.map(_.split("/").last))

  private def search(query: String): Try[Seq[String]] =
    for {
      response <- Try {new RiSearch(query).lang("sparql").format("csv").execute()}
      is        = response.getEntityInputStream
      lines    <- streamToLines(is)
    } yield lines

  def disseminateDatastream(objectId: String,
                            streamId: String
                           ): Try[String] =
    for {
      response <- Try {getDatastreamDissemination(objectId, streamId).execute()}
      is        = response.getEntityInputStream
      content  <- streamToString(is)
    } yield content

  def getDatastreamProfiles(objectId: String
                           ): Try[Seq[DatastreamProfile]] =
    for {
      response <- Try{getDatastreams(objectId).execute}
      profiles <- Try{response.getDatastreamProfiles.toSeq}
    } yield profiles

  private def streamToLines(inputStream: InputStream
                            ): Try[Seq[String]] = {
    try{
      Success(fromInputStream(inputStream).getLines.toSeq)
    }finally {
      // TODO IOUtils.closeQuietly(inputStream)
      // causes "java.io.IOException: stream is closed" in foreachUntilFailure
      // but no one magically closes the stream
      // not very important as long as we export only one dataset
    }
  }

  private def streamToString(inputStream: InputStream
                            ): Try[String] = {
    try{
      Success(fromInputStream(inputStream).mkString)
    }finally {
      // wrapped in a try to not stumble over future 'enhancements' that do close at EOF
      Try(inputStream.close())
    }
  }
}
