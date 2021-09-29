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

import scala.util.Try
import scala.xml.transform.{ RewriteRule, RuleTransformer }
import scala.xml.{ Elem, Node, NodeSeq }

object FOXML {

  private def stripRule(downloadedIds: Seq[String]) = {
    new RewriteRule() {
      override def transform(n: Node): NodeSeq = n match {
        // skip downloaded streams handled by cfg.json
        case Elem("foxml", "datastream", _, _, _*)
          if (n \@ "ID") == "RELS-EXT" || (n \@ "CONTROL_GROUP") == "M" =>
          NodeSeq.Empty
        // skip fedora IDs
        case _ if downloadedIds.contains(n.text) =>
          NodeSeq.Empty
        case Elem("foxml", "digitalObject", attrs, scope, children @ _*) =>
          Elem("foxml", "digitalObject", attrs.remove("PID"), scope, minimizeEmpty = false, children: _*)
        // skip cheksum as we might have altered the content in the cases above
        case Elem("foxml", "contentDigest", attrs, scope, children @ _*) =>
          Elem("foxml", "contentDigest", attrs.remove("DIGEST"), scope, minimizeEmpty = false, children: _*)
        case _ => n
      }
    }
  }

  def strip(foXml: Node, ids: Seq[String]): String = {
    new RuleTransformer(stripRule(ids)).transform(foXml).head.toString()
  }

  def getManagedStreams(foXml: Node): Seq[Node] = {
    (foXml \ "datastream").theSeq.filter(_ \@ "CONTROL_GROUP" == "M")
  }

  def getRelsExt(foXml: Node) = Try {
    val xs = (foXml \ "datastream")
      .theSeq
      .filter(n => n \@ "ID" == "RELS-EXT")
      .last

    (xs \ "datastreamVersion" \ "xmlContent")
      .last
      .descendant
      .filter(_.label == "RDF")
      .last
  }

  def getUserIds(foXml: Node): List[Option[String]] = {
    foXml.descendant_or_self
      .map(node => toUserMsg(node))
      .filter(_.isDefined)
      .sortBy(_.get).distinct
  }

  /** labels of XML elements that contain user IDs, e.g: <depositorId>someone</depositorId> */
  private val userLabels = Set("user-id", "depositorId", "doneById", "requesterId")

  private def toUserMsg(node: Node): Option[String] = {
    if (userLabels.contains(node.label)) Some(s"${ node.label }: ${ node.text }")
    else if (isOwnerProperty(node)) Some(s"property ownerId: ${ node \@ "VALUE" }")
    else None
  }

  /** Checks for a node like <xxx:property NAME="yyy#ownerId" VALUE="zzz"/> */
  private def isOwnerProperty(node: Node): Boolean = {
    node.label == "property" && (node \@ "NAME").endsWith("#ownerId")
  }
}
