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
  val downloadInFoxml = Seq("DC", "EMD", "AMD", "PRSQL", "DMD")

  /** labels of XML elements that contain user IDs, e.g: <depositorId>someone</depositorId> */
  val userLabels = Set("user-id", "depositorId", "doneById", "requesterId")

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
      case Elem("foxml", "contentDigest", attrs, scope, children @ _*) =>
        Elem("foxml", "contentDigest", attrs.remove("DIGEST"), scope, minimizeEmpty=false, children: _*)

      // warnings for user ids's
      case Elem("foxml", "property", _, _, _*) =>
        if ((n \@ "NAME").contains("ownerId"))
          log.warn(s"fo.xml contains property ownerId: ${n \@ "VALUE"}")
        n
      case _ =>
        if (userLabels.contains(n.label))
          log.warn(s"fo.xml contains ${n.label}: ${n.text}")
        n
    }
  }

  private val transformer = new RuleTransformer(rule)

  private def hasDatasetNamespace(n: Node): Boolean =
    Seq("easy-dataset", "easy-file", "easy-folder", "dans-jumpoff")
      .contains(n.text.replaceAll(":.*", ""))

  def strip(foXml: Elem): String =
    transformer.transform(foXml).head.toString()
}
