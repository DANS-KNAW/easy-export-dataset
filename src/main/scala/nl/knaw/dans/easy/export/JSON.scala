/** *****************************************************************************
  * Copyright 2015 DANS - Data Archiving and Networked Services
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
  * *****************************************************************************/
package nl.knaw.dans.easy.export

import java.io.File

import com.yourmediashelf.fedora.generated.access.DatastreamType
import org.json4s.JsonAST
import org.json4s.JsonAST.JObject
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

import scala.util.{Failure, Try}
import scala.xml.{Node, Elem}

object JSON {

  def apply(sdoDir: File,
            datastreams: Seq[DatastreamType],
            relsExt: Elem
           )(implicit s: Settings): Try[String] = Try {
    // Try because .get from options and .head from sequences are not safe

    val descriptionNode = (relsExt \ "Description").head
    val objectId = descriptionNode.attribute(rdf, "about").get.head.text.replaceAll("[^/]*/", "")
    val relations = descriptionNode.descendant.filter(_.label != "#PCDATA")
    pretty(render(
      ("namespace" -> objectId.replaceAll(":.*", "")) ~
        ("datastreams" -> datastreams.map(convertDatastream(_, sdoDir))) ~
        ("relations" -> relations.map(convertRelation))
    ))
  }.recoverWith { case t: Throwable =>
    Failure(new Exception(s"invalid RELS-EXT for ${sdoDir.getName}? ${t.getMessage}", t))
  }

  // TODO is this exhaustive? Rather invert the test to be safe.
  private val needsPlaceHolder = Seq[String]("easy-dataset", "easy-file", "easy-folder", "easy-dlh")

  private val rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"

  private def convertRelation(n: Node)(implicit s: Settings): JsonAST.JObject = {
    val isLiteral = {
      val a = n.attribute(rdf, "parseType")
      a.isDefined && a.get.text == "Literal"
    }
    if (isLiteral)
      ("predicate" -> s"${n.namespace}${n.label}") ~
        ("object" -> n.text) ~
        ("isLiteral" -> true)
    else {
      val resource = n.attribute(rdf, "resource").get.head.text
      val objectID = resource.replaceAll("[^/]*/", "")
      val objectNameSpace = objectID.replaceAll(":.*", "")
      if (needsPlaceHolder.contains(objectNameSpace))
        ("predicate" -> s"${n.namespace}${n.label}") ~
          ("objectSDO" -> toSdoDir(objectID).getName)
      else
        ("predicate" -> s"${n.namespace}${n.label}") ~
          ("object" -> resource)
    }
  }

  def convertDatastream(ds: DatastreamType, sdoDir: File): JObject = {
    // TODO what about files stored outside fedora?
    // TODO read the controlgroup from fedora
    if (ds.getDsid == "EASY_FILE")
      ("dsLocation" -> new File(sdoDir, ds.getDsid).toString) ~
        ("dsID" -> ds.getDsid) ~
        ("label" -> ds.getLabel) ~
        ("mimeType" -> ds.getMimeType) ~
        ("controlGroup" -> "R")
    else
      ("contentFile" -> new File(sdoDir, ds.getDsid).toString) ~
        ("dsID" -> ds.getDsid) ~
        ("label" -> ds.getLabel) ~
        ("mimeType" -> ds.getMimeType) ~
        ("controlGroup" -> (if (ds.getDsid == "ADDITIONAL_LICENSE") "M" else "X"))
  }
}