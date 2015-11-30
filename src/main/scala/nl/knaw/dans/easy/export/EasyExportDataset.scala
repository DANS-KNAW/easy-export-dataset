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
      _      <- exportObject(s.datasetId)
      _      <- subIds.foreachUntilFailure(callExportObject)
    } yield allIds
  }

  def exportObject(objectId: String
                  )(implicit s: Settings): Try[Unit] = {
    val sdoDir = new File(s.sdoSet, objectId.replaceAll("[^0-9a-zA-Z]", "_"))
    val callExportDatastream = (dst: DatastreamType) => exportDatastream(objectId, sdoDir, dst)
    log.info(s"exporting $objectId to $sdoDir")
    for {
      _              <- Try(sdoDir.mkdir())
      allDatastreams <- s.fedora.getDatastreams(objectId)
      mostDatastreams = RichSeq(allDatastreams.filter(_.getDsid != "RELS-EXT"))
      _              <- mostDatastreams.foreachUntilFailure(callExportDatastream)
      // TODO fo.xml / cfg.json
    } yield ()
  }

  def exportDatastream(objectId:String,
                       sdoDir: File,
                       dst: DatastreamType
                      )(implicit s: Settings): Try[Unit]= {
    val exportFile = new File(sdoDir, dst.getDsid)
    log.info(s"exporting datastream to $exportFile (${dst.getLabel}, ${dst.getMimeType})")
    for {
      is <- s.fedora.disseminateDatastream(objectId, dst.getDsid)
      _ <- writeAndClose(is, exportFile)
    } yield()
  }
}
