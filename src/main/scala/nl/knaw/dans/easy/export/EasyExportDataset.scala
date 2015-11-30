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

import com.yourmediashelf.fedora.generated.access.DatastreamType
import nl.knaw.dans.easy.stage.lib.JSON.createFileCfg
import org.slf4j.LoggerFactory

import scala.util.Try

object EasyExportDataset {

  private val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    if (Conf.defaultOptions.isFailure)
      log.warn(s"No defaults : ${Conf.defaultOptions.failed.get.getMessage}")
    (for {
      s <- Settings(Conf(args))
      ids <- run(s)
      _ = log.info(s"STAGED ${ids.mkString(", ")}")
    } yield ())
      .recover { case t: Throwable =>
        log.error("STAGING FAILED", t)
      }
  }

  def run(implicit s: Settings): Try[Seq[String]] = {
    log.info(s.toString)
    val callExportObject = (id: String) => exportObject(id)
    for {
      _      <- Try(s.sdoSet.mkdirs())
      subIds <- s.fedora.getSubordinates(s.datasetId).map(RichSeq(_))
      allIds  = s.datasetId +: subIds
      _      <- exportObject(s.datasetId) // TODO rename sdoDir to dataset
      _      <- subIds.foreachUntilFailure(callExportObject)
    } yield allIds
  }

  def exportObject(objectId: String
                  )(implicit s: Settings): Try[Unit] = {
    val sdoDir = toSdoDir(objectId)
    val callExportDatastream = (dst: DatastreamType) => exportDatastream(objectId, sdoDir, dst)
    val callWriteFileJSON = (dst: DatastreamType) => writeFileJSON(objectId, sdoDir, dst)
    log.info(s"exporting $objectId to $sdoDir")
    for {
      _              <- Try(sdoDir.mkdir())
      allDatastreams <- s.fedora.getDatastreams(objectId)
      mostDatastreams = RichSeq(allDatastreams.filter(_.getDsid != "RELS-EXT"))
      _              <- mostDatastreams.foreachUntilFailure(callExportDatastream)
      files           = RichSeq(allDatastreams.filter(_.getDsid == "EASY_FILE"))
      _              <- files.foreachUntilFailure(callWriteFileJSON)
      // TODO fo.xml, cfg.json for dataset, folders and DownLoadHistory
    } yield ()
  }

  def exportDatastream(objectId:String,
                       sdoDir: File,
                       dst: DatastreamType
                      )(implicit s: Settings): Try[DatastreamType]= {
    val exportFile = new File(sdoDir, dst.getDsid)
    log.info(s"exporting datastream to $exportFile (${dst.getLabel}, ${dst.getMimeType})")
    for {
      is <- s.fedora.disseminateDatastream(objectId, dst.getDsid)
      _ <- writeAndClose(is, exportFile)
    } yield dst
  }

  def writeFileJSON(objectId:String,
                    sdoDir: File,
                    dst: DatastreamType
                   )(implicit s: Settings): Try[Unit] = {
    for {
      parentId <- getParentId(objectId)
      parentSDO = toSdoDir(parentId).toString
      location = new File(sdoDir, "EASY_FILE").toString
      content = createFileCfg(location, dst.getMimeType, ("parentSDO", parentSDO))
      _      <- write(new File(sdoDir, "cfg.json"), content.getBytes)
    } yield ()
  }

  def getParentId(objectId: String
                 )(implicit s: Settings): Try[String] = {
    for {
      inputStream <- s.fedora.disseminateDatastream(objectId, "RELS-EXT")
      objectXML   <- readXmlAndClose(inputStream)
    } yield (objectXML \ "Description" \ "isMemberOf").head
      .attribute("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "resource").head.text
  }

  def toSdoDir(objectId: String)(implicit s: Settings): File =
    new File(s.sdoSet, objectId.replaceAll("[^0-9a-zA-Z]", "_"))
}
