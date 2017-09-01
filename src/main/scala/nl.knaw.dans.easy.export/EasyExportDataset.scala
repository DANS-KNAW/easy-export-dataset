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

import java.io.File

import nl.knaw.dans.easy.export.FOXML._
import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.{ Failure, Success, Try }
import scala.xml.{ Node, XML }

object EasyExportDataset extends DebugEnhancedLogging {

  def run(implicit settings: Settings): Try[Seq[String]] = {
    val result = for {
      _ <- Try(settings.sdoSet.mkdirs())
      subIds <- settings.fedora.getSubordinates(settings.datasetId)
      allIds = settings.datasetId +: subIds
      _ <- subIds.foreachUntilFailure(exportObject(_, allIds))
      _ <- exportObject(settings.datasetId, allIds)
    } yield allIds

    result
      .doIfSuccess(ids => logger.info(s"OK: Completed successfully. Ids: ${ ids.mkString(", ") }"))
      .doIfFailure { case e => logger.error(s"FAILED: ${ e.getMessage }") }
  }

  private def exportObject(objectId: String, allIds: Seq[String])(implicit settings: Settings): Try[Node] = {
    val sdoDir = new File(settings.sdoSet, toSdoName(objectId))
    logger.info(s"exporting $objectId to $sdoDir")
    for {
      foXml <- settings.fedora.getFoXml(objectId).map(XML.load).tried
      managedStreams = getManagedStreams(foXml)
      relsExtXml <- getRelsExt(foXml)
      jsonContent <- JSON(sdoDir, managedStreams, relsExtXml, placeHoldersFor = allIds)
      _ = sdoDir.mkdir()
      _ <- managedStreams.foreachUntilFailure(exportDatastream(objectId, sdoDir, _))
      _ <- verifyFedoraIds(jsonContent, allIds, "cfg.json")
      _ <- new File(sdoDir, "cfg.json").safeWrite(jsonContent)
      foxmlContent = strip(foXml, allIds)
      _ <- verifyFedoraIds(foxmlContent, allIds, "fo.xml")
      _ <- new File(sdoDir, "fo.xml").safeWrite(foxmlContent)
      _ = for (maybeUserId <- getUserIds(foXml)) logger.warn(s"fo.xml contains $maybeUserId")
    } yield foXml
  }

  private def exportDatastream(objectId: String, sdoDir: File, ds: Node)(implicit s: Settings): Try[Unit] = {
    for {
      dsID <- Try { ds \@ "ID" }
      exportFile = new File(sdoDir, dsID)
      _ = logger.info(s"exporting datastream $dsID to $exportFile")
      _ <- s.fedora.disseminateDatastream(objectId, dsID)
        .map(_.copyToFile(exportFile))
        .tried
        .flatten
    // TODO histories of versionable datastreams such as (additional) licenses?
    } yield ()
  }

  /**
   * On the flight verification of a proper implementation.
   * A proper implementation means none of the downloaded ids in any fo.xml or cfg.json
   * Not quoted IDs are accepted, as for example a relation with
   * "oai:easy.dans.knaw.nl:easy-dataset:300".
   */
  def verifyFedoraIds(content: String, allIds: Seq[String], fileName: String): Try[Unit] = {
    allIds.foreachUntilFailure {
      case id if content.contains(s">$id<") || content.contains(s""""$id"""") =>
        Failure(new Exception(s"$fileName contains a downloaded ID $id\n$content"))
      case _ => Success(())
    }
  }
}
