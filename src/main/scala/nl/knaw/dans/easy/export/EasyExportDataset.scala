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

import java.io.File

import nl.knaw.dans.easy.export.EasyExportDataset._
import nl.knaw.dans.easy.export.FOXML.{warnForUserIds, strip}
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}
import scala.xml.Node

class EasyExportDataset(s: Settings) {

  def run(): Try[Seq[String]] = {
    log.info(s.toString)
    for {
      _      <- Try(s.sdoSet.mkdirs())
      subIds <- s.fedora.getSubordinates(s.datasetId)
      allIds  = s.datasetId +: subIds
      _      <- subIds.foreachUntilFailure(exportObject(_,allIds))
      foXML  <- exportObject(s.datasetId, allIds)
    } yield allIds
  }

  private def exportObject(objectId: String,
                           allIds: Seq[String]
                          ): Try[Node] = {
    val sdoDir = new File(s.sdoSet,toSdoName(objectId))
    log.info(s"exporting $objectId to $sdoDir")
    for {
      foXmlInputStream   <- s.fedora.getFoXml(objectId)
      foXml              <- foXmlInputStream.readXmlAndClose
      datastreams        <- Try((foXml \ "datastream").theSeq.filter(download))
      relsExtXml         <- getRelsExt(foXml)
      jsonContent        <- JSON(sdoDir, datastreams, relsExtXml , placeHoldersFor = allIds)
      _                  <- Try(sdoDir.mkdir())
      _                  <- datastreams.foreachUntilFailure(exportDatastream(objectId, sdoDir, _))
      _                  <- verifyFedoraIds(jsonContent, allIds, "cfg.json")
      _                  <- new File(sdoDir, "cfg.json").safeWrite(jsonContent)
      foxmlContent        = strip(foXml)
      _                  <- verifyFedoraIds(foxmlContent, allIds, "fo.xml")
      _                  <- new File(sdoDir, "fo.xml").safeWrite(foxmlContent)
      _                   = warnForUserIds(foXml)
    } yield foXml
  }

  private def exportDatastream(objectId:String,
                               sdoDir: File,
                               ds: Node
                              ): Try[Unit]= {
    for {
      dsID       <- Try(ds \@ "ID")
      exportFile = new File(sdoDir, dsID)
      _          = log.info(s"exporting datastream $dsID to $exportFile")
      is         <- s.fedora.disseminateDatastream(objectId, dsID)
      _          <- is.copyAndClose(exportFile)
    // TODO histories of versionable datastreams such as (additional) licenses?
    } yield ()
  }

  /** On the flight verification of a proper implementation.
    * Errors might occur when trying to export something else than a dataset.
    * A proper implementation means none of the downloaded ids in any fo.xml or cfg.json
    * Not quoted IDs are accepted, as for example a relation with
    * "oai:easy.dans.knaw.nl:easy-dataset:300".
    * */
  def verifyFedoraIds(content: String, allIds: Seq[String], fileName :String): Try[Unit] =
    allIds.foreachUntilFailure(id =>
      if (content.contains(s">$id<") || content.contains(s""""$id""""))
        Failure(new Exception(s"$fileName contains a downloaded ID $id\n$content"))
      else Success(Unit)
    )

  private def getRelsExt(foXml: Node) = Try((
    (foXml \ "datastream").theSeq.filter(n => n \@ "ID" == "RELS-EXT"
    ).last \ "datastreamVersion" \ "xmlContent"
  ).last.descendant.filter(_.label=="RDF").last)

  private val skipDownload = Set("RELS-EXT", "AUDIT") ++ FOXML.downloadInFoxml
  private def download(datastream: Node): Boolean = {

    val datastreamID = datastream.attribute("ID").get.head.text
    // N.B: in theory .get and .head are not safe, in practice that means invalid RELS-EXT
    // this method is a filter argument, calling the filter is made honest with a try

    if (skipDownload.contains(datastreamID))
      false
    else { // skip datastreams that are references to external storage
      val cl = (datastream \ "datastreamVersion") \ "contentLocation"
      if (cl.isEmpty) true // inline datastream
      else (cl.last \@ "TYPE") == "INTERNAL_ID"
    }
  }
}

object EasyExportDataset {

  private val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    if (Conf.defaultOptions.isFailure)
      log.warn(s"No defaults : ${Conf.defaultOptions.failed.get.getMessage}")
    (for {
      s <- Settings(Conf(args))
      ids <- new EasyExportDataset(s)run()
      _ = log.info(s"STAGED ${ids.mkString(", ")}")
    } yield ()).recover { case t: Throwable =>
        log.error("STAGING FAILED", t)
      }
  }
}
