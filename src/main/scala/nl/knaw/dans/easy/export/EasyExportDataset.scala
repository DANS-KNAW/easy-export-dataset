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

import nl.knaw.dans.easy.export.FOXML.strip
import org.slf4j.LoggerFactory

import scala.util.Try
import scala.xml.Node

object EasyExportDataset {

  private val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    if (Conf.defaultOptions.isFailure)
      log.warn(s"No defaults : ${Conf.defaultOptions.failed.get.getMessage}")
    (for {
      s <- Settings(Conf(args))
      ids <- run(s)
      _ = log.info(s"STAGED ${ids.mkString(", ")}")
    } yield ()).recover { case t: Throwable =>
        log.error("STAGING FAILED", t)
      }
  }

  def run(implicit s: Settings): Try[Seq[String]] = {
    log.info(s.toString)
    for {
      _      <- Try(s.sdoSet.mkdirs())
      subIds <- s.fedora.getSubordinates(s.datasetId)
      allIds = s.datasetId +: subIds
      _      <- exportObject(s.datasetId, allIds) // TODO rename sdoDir easy_dataset_NNN to dataset?
      _      <- subIds.foreachUntilFailure((id: String) => exportObject(id,allIds))
    } yield allIds
  }

  def exportObject(objectId: String,
                  allIds: Seq[String]
                  )(implicit s: Settings): Try[Unit] = {
    val sdoDir = new File(s.sdoSet,toSdoName(objectId))
    log.info(s"exporting $objectId to $sdoDir")
    for {
      _                  <- Try(sdoDir.mkdir())
      foXmlInputStream   <- s.fedora.getFoXml(objectId)
      foXml              <- foXmlInputStream.readXmlAndClose
      _                  <- new File(sdoDir, "fo.xml").safeWrite(strip(foXml))
      relsExtInputStream <- s.fedora.disseminateDatastream(objectId, "RELS-EXT")
      relsExtXML         <- relsExtInputStream.readXmlAndClose
      datastreams        <- Try((foXml \ "datastream").theSeq.filterNot(skip))
      jsonContent        <- JSON(sdoDir, datastreams, relsExtXML, placeHoldersFor = allIds)
      _                  <- new File(sdoDir, "cfg.json").safeWrite(jsonContent)
      _                  <- datastreams.foreachUntilFailure((ds: Node) => exportDatastream(objectId, sdoDir, ds))
    } yield ()
  }

  def skip(n: Node): Boolean = Set("RELS-EXT", "AUDIT")
    .contains(n.attribute("ID").get.head.text)// N.B: .get and .head are not safe

  def exportDatastream(objectId:String,
                       sdoDir: File,
                       ds: Node
                      )(implicit s: Settings): Try[Unit]= {
      for {
        dsID       <- Try(ds \@ "ID")
        exportFile = new File(sdoDir, dsID)
        _          = log.info(s"exporting datastream $dsID to $exportFile")
        is         <- s.fedora.disseminateDatastream(objectId, dsID)
        _          <- is.copyAndClose(exportFile)
      // TODO histories of versionable datastreams such as (additional) licenses?
      } yield ()
    }
}
