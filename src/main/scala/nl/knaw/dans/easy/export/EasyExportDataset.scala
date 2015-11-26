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

  def main(args: Array[String]): Unit =
    run(Settings(Conf(args))).recover { case t: Throwable => log.error("staging failed", t) }

  def run(implicit s: Settings): Try[Unit] = {
    val function = (id: String) => exportObject(id)
    log.info(s.toString)
    for {
      _   <- Try(s.sdoSet.mkdir())
      _   <- exportObject(s.datasetId)
      ids <- s.fedora.getSubordinates(s.datasetId)
      _   <- foreachUntilFailure(ids, function)
    } yield ()
  }

  def exportObject(objectId: String
                  )(implicit s: Settings): Try[Unit] = {
    val sdoDir = new File(s.sdoSet, s"$objectId".replaceAll("[^0-9a-zA-Z]", "_"))
    val function = (dsp: DatastreamType) => exportDatastream(objectId, sdoDir, dsp.getDsid)
    log.info(s"exporting $objectId to $sdoDir")
    for {
      _           <- Try(sdoDir.mkdir())
      datastreams <- s.fedora.getDatastreams(objectId)
      _           <- foreachUntilFailure(datastreams.filter(_.getDsid != "RELS-EXT"), function)
      // TODO fo.xml / cfg.json
    } yield ()
  }

  def exportDatastream(objectId:String,
                       sdoDir: File,
                       datastreamID: String
                      )(implicit s: Settings): Try[Unit]= {
    val exportFile = new File(sdoDir, datastreamID)
    log.info(s"exporting datastream to $exportFile")
    for {
      content <- s.fedora.disseminateDatastream(objectId,datastreamID)
      _       <- writeAll(exportFile,content)
    } yield ()
  }
}
