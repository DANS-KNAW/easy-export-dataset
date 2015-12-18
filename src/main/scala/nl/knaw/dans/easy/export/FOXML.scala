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

import org.slf4j.LoggerFactory

import scala.util.Try
import scala.xml.transform.{RewriteRule, RuleTransformer}
import scala.xml.{Elem, Node, NodeSeq}

object FOXML {

  private val log = LoggerFactory.getLogger(getClass)

  /** Keep inline datastreams that do not contain dataset-related fedora-ids.
    *
    * Rationale:
    * - sids are not downloaded when downloading EASY_FILE_METADATA.
    * - EMD contains discipline.id for audiences which are not dataset related
    *   these IDs should be identical between EASY-fedora instances
    *   unless a release that changed audiences was not applied everywhere
    */
  val downloadInFoxml = Seq("DC", "EMD", "AMD", "PRSQL", "DMD", "EASY_FILE_METADATA", "EASY_ITEM_CONTAINER_MD")

  private val rule = new RewriteRule {
    override def transform(n: Node): NodeSeq = n match {

      // skip fedora IDs
      case Elem("foxml", "datastream", _, _, _*) if !downloadInFoxml.contains(n \@ "ID") =>
        NodeSeq.Empty
      case Elem("dc", "identifier", _, _, _*) if hasDatasetNamespace(n) =>
        NodeSeq.Empty
      case Elem(_, "sid", _, _, _*) if hasDatasetNamespace(n) =>
        NodeSeq.Empty
      case Elem("foxml", "digitalObject", attrs, scope, children @ _*) =>
        Elem("foxml", "digitalObject", attrs.remove("PID"), scope, minimizeEmpty=false, children: _*)

      // skip obsolete content of FILE_ITEM_METADATA with fedora ids, they might not have been cleaned up
      case Elem(_, "parentSid", _, _, _*) =>
        NodeSeq.Empty
      case Elem(_, "datasetSid", _, _, _*) =>
        NodeSeq.Empty

      // skip cheksum as we might have altered the content in the cases above
      case Elem("foxml", "contentDigest", attrs, scope, children @ _*) =>
        Elem("foxml", "contentDigest", attrs.remove("DIGEST"), scope, minimizeEmpty=false, children: _*)

      case _ => n
    }
  }

  private val transformer = new RuleTransformer(rule)

  private def hasDatasetNamespace(n: Node): Boolean =
    Seq("easy-dataset", "easy-file", "easy-folder", "dans-jumpoff", "easy-dlh")
      .contains(n.text.replaceAll(":.*", ""))

  def strip(foXml: Elem): String =
    transformer.transform(foXml).head.toString()


  def warnForUserIds(foXml: Elem): Unit = Try {
    for {maybeString <- foXml.descendant_or_self
      .map(node => toUserMsg(node))
      .filter(_.isDefined).sortBy(_.get).distinct
    } log.warn(s"fo.xml contains ${maybeString.get}")
  }

  /** labels of XML elements that contain user IDs, e.g: <depositorId>someone</depositorId> */
  private val userLabels = Set("user-id", "depositorId", "doneById", "requesterId")

  private def toUserMsg(node: Node): Option[String] = {
    if (userLabels.contains(node.label))
      Some(s"${node.label}: ${node.text}")
    else if (isOwnerProperty(node))
      Some(s"property ownerId: ${node \@ "VALUE"}")
    else None
  }

  /** Checks for a node like <xxx:property NAME="yyy#ownerId" VALUE="zzz"/> */
  private def isOwnerProperty(node: Node): Boolean =
    node.label == "property" && (node \@ "NAME").endsWith("#ownerId")
}
