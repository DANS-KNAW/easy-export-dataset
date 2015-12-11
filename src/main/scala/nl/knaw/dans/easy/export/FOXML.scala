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

import scala.xml.transform.{RewriteRule, RuleTransformer}
import scala.xml.{Elem, Node, NodeSeq}

object FOXML {

  /** Keep inline datastreams that do not contain dataset-related fedora-ids.
    *
    * Rationale:
    * - sids are not downloaded when downloading EASY_FILE_METADATA.
    * - EMD contains discipline.id for audiences which are not dataset related
    *   these IDs should be identical between EASY-fedora instances
    *   unless a release that changed audiences was not applied everywhere
    */
  val plainCopy = Seq("DC", "EMD", "AMD", "PRSQL", "DMD")

  private val rule = new RewriteRule {
    override def transform(n: Node): NodeSeq = n match {
      case Elem("foxml", "datastream", _, _, _*) if !plainCopy.contains(n \@ "ID") =>
        NodeSeq.Empty
      case Elem("foxml", "contentDigest", _, _, _*) =>
        NodeSeq.Empty
      case Elem("dc", "identifier", _, _, _*) if hasDatasetNamespace(n) =>
        NodeSeq.Empty
      case Elem("foxml", "digitalObject", attrs, scope, children @ _*) =>
        Elem("foxml", "digitalObject", attrs.remove("PID"), scope, minimizeEmpty=false, children: _*)
      case _ => n
    }
  }

  private val transformer = new RuleTransformer(rule)

  private def hasDatasetNamespace(n: Node): Boolean =
    Seq("easy-dataset", "easy-file", "easy-folder")
      .contains(n.text.replaceAll(":.*", ""))

  def strip(foXml: Elem): String =
    transformer.transform(foXml).head.toString()
}
