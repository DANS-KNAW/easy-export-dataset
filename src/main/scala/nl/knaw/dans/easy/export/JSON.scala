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

import org.json4s.JsonAST
import org.json4s.JsonAST.JObject
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

import scala.util.{Failure, Try}
import scala.xml.{Elem, Node}

object JSON {

  /** Creates a JSON structure while replacing some fedora ids with SDO directories on the flight.
    * 'Some' being objects of an EASY-dataset.
    *
    * @param sdoDir a directory for the staged digital object
    * @param datastreams subsection of foXML
    * @param relsExt the content of the RESL-EXT datastream
    * @param s configuration required to construct an sdoDir from a fedora-id
    * @return a string representation of a json object
    */
  def apply(sdoDir: File,
            datastreams: Seq[Node],
            relsExt: Node,
            placeHoldersFor: Seq[String]
           )(implicit s: Settings): Try[String] = Try {

    val descriptionNode = (relsExt \ "Description").head
    val objectId = descriptionNode.attribute(rdf, "about").get.head.text.replaceAll("[^/]*/", "")
    val relations = descriptionNode.descendant.filter(_.label != "#PCDATA")
    pretty(render(
      ("namespace" -> objectId.replaceAll(":.*", "")) ~
        ("datastreams" -> datastreams.map(convertDatastream(_, sdoDir))) ~
        ("relations" -> relations.map(convertRelation(_,placeHoldersFor)))
    ))
  }.recoverWith { case t: Throwable =>
    Failure(new Exception(s"invalid RELS-EXT or fo.xml for ${sdoDir.getName}? ${t.getMessage}", t))
  }

  private val rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"

  private def convertRelation(n: Node,
                              placeHoldersFor: Seq[String]
                             )(implicit s: Settings): JsonAST.JObject = {
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
      if (placeHoldersFor.contains(objectID)) {
        ("predicate" -> s"${n.namespace}${n.label}") ~ ("objectSDO" -> toSdoName(objectID))
      }
      else
        ("predicate" -> s"${n.namespace}${n.label}") ~ ("object" -> resource)
    }
  }

  def convertDatastream(ds: Node, sdoDir: File): JObject = {
    val datastreamID = ds \@ "ID"
    val datastreamVersion = (ds \ "datastreamVersion").last
    val controlGroup = ds \@ "CONTROL_GROUP"
    val checksumType = datastreamVersion.attribute("foxml", "contentDigest")
    if (controlGroup == "R")
      ("dsLocation" -> new File(sdoDir, datastreamID).toString) ~
        ("dsID" -> datastreamID) ~
        ("mimeType" -> datastreamVersion \@ "MIMETYPE") ~
        ("controlGroup" -> controlGroup)
    else if (checksumType.isEmpty)
        ("contentFile" -> datastreamID) ~
          ("dsID" -> datastreamID) ~
          ("label" -> datastreamVersion \@ "LABEL") ~ // TODO ignored by fedora API: leave more in foxml, but what/how?
          ("mimeType" -> datastreamVersion \@ "MIMETYPE") ~
          ("controlGroup" -> controlGroup)
    else
      ("contentFile" -> datastreamID) ~
        ("dsID" -> datastreamID) ~
        ("label" -> datastreamVersion \@ "LABEL") ~ // TODO idem
        ("mimeType" -> datastreamVersion \@ "MIMETYPE") ~
        ("controlGroup" -> controlGroup) ~
        ("checksumType" -> checksumType.head.text) ~
        ("checksum" -> datastreamVersion \@ "DIGEST")
  }
}