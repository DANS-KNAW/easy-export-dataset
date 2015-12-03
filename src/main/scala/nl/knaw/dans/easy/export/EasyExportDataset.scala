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
    val callExportObject = (id: String) => exportObject(id)
    for {
      _      <- Try(s.sdoSet.mkdirs())
      subIds <- s.fedora.getSubordinates(s.datasetId).map(RichSeq(_))
      _      <- exportObject(s.datasetId) // TODO rename sdoDir easy_dataset_NNN to dataset?
      _      <- subIds.foreachUntilFailure(callExportObject)
    } yield s.datasetId +: subIds
  }

  def exportObject(objectId: String
                  )(implicit s: Settings): Try[Unit] = {
    val sdoDir = toSdoDir(objectId)
    val callExportDatastream = (ds: Node) => exportDatastream(objectId, sdoDir, ds)
    log.info(s"exporting $objectId to $sdoDir")
    for {
      _                  <- Try(sdoDir.mkdir())
      foXmlInputStream   <- s.fedora.getFoXml(objectId)
      foXml              <- readXmlAndClose(foXmlInputStream) // TODO replace fedora IDs with SDOs
      _                  <- write(foXml.toString().getBytes, new File(sdoDir, "fo.xml"))
      datastreams        <- Try(RichSeq((foXml \ "datastream").theSeq.filterNot(skip)))
      relsExtInputStream <- s.fedora.disseminateDatastream(objectId, "RELS-EXT")
      relsExtXML         <- readXmlAndClose(relsExtInputStream)
      jsonContent        <- JSON(sdoDir, datastreams, relsExtXML)
      _                  <- write(jsonContent.getBytes, new File(sdoDir, "cfg.json"))
      _                  <- datastreams.foreachUntilFailure(callExportDatastream)
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
        _          <- copyAndClose(is, exportFile)
      // TODO histories of versionable datastreams such as (additional) licenses
      } yield ()
    }
}
