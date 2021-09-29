/*
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

import org.json4s.JsonAST
import org.json4s.JsonAST.JObject
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

import scala.util.{ Failure, Success, Try }
import scala.xml.Node

object JSON {

  /**
   * Creates a JSON structure while replacing some fedora ids with SDO directories on the flight.
   * 'Some' being objects of an EASY-dataset.
   *
   * @param sdoDir      a directory for the staged digital object
   * @param datastreams subsection of foXML
   * @param relsExt     the content of the RESL-EXT datastream
   * @return a string representation of a json object
   */
  def apply(sdoDir: File, datastreams: Seq[Node], relsExt: Node, placeHoldersFor: Seq[String]): Try[String] = {
    (relsExt \ "Description").headOption
      .flatMap(descriptionNode => {
        descriptionNode.headOfAttr(rdf, "about")
          .map(id => ("namespace" -> id.replaceAll("[^/]*/", "").replaceAll(":.*", "")) ~
            ("datastreams" -> datastreams.map(convertDatastream(_, sdoDir))) ~
            ("relations" -> descriptionNode.descendant
              .withFilter(_.label != "#PCDATA")
              .map(convertRelation(_, placeHoldersFor))))
          .map(pretty _ compose render)
      })
      .map(Success(_))
      .getOrElse(Failure(new Exception(s"invalid RELS-EXT or fo.xml for ${ sdoDir.getName }?")))
  }

  private val rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"

  private def convertRelation(n: Node, placeHoldersFor: Seq[String]): JsonAST.JObject = {
    n.headOfAttr(rdf, "resource")
      .map(r => {
        val objectID = r.replaceAll("[^/]*/", "")
        if (placeHoldersFor.contains(objectID))
          ("predicate" -> s"${ n.namespace }${ n.label }") ~ ("objectSDO" -> toSdoName(objectID))
        else
          ("predicate" -> s"${ n.namespace }${ n.label }") ~ ("object" -> r)
      })
      .getOrElse {
        ("predicate" -> s"${ n.namespace }${ n.label }") ~
          ("object" -> n.text) ~
          ("isLiteral" -> true)
      }
  }

  private def convertDatastream(ds: Node, sdoDir: File): JObject = {
    val datastreamID = ds \@ "ID"
    val datastreamVersion = (ds \ "datastreamVersion").last

    (ds \@ "CONTROL_GROUP", datastreamVersion.attribute("foxml", "contentDigest")) match {
      case (cg @ "R", _) =>
        ("dsLocation" -> new File(sdoDir, datastreamID).toString) ~
          ("dsID" -> datastreamID) ~
          ("mimeType" -> datastreamVersion \@ "MIMETYPE") ~
          ("controlGroup" -> cg)
      case (cg, None) =>
        ("contentFile" -> datastreamID) ~
          ("dsID" -> datastreamID) ~
          ("label" -> datastreamVersion \@ "LABEL") ~ // TODO ignored by fedora API: leave more in foxml, but what/how?
          ("mimeType" -> datastreamVersion \@ "MIMETYPE") ~
          ("controlGroup" -> cg)
      case (cg, Some(ns)) =>
        ("contentFile" -> datastreamID) ~
          ("dsID" -> datastreamID) ~
          ("label" -> datastreamVersion \@ "LABEL") ~ // TODO idem
          ("mimeType" -> datastreamVersion \@ "MIMETYPE") ~
          ("controlGroup" -> cg) ~
          ("checksumType" -> ns.text) ~
          ("checksum" -> datastreamVersion \@ "DIGEST")
    }
  }
}
