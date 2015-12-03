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

  private val rule = new RewriteRule {
    override def transform(n: Node): NodeSeq = n match {
      case Elem("foxml", "datastream", _, _, _*) =>
        NodeSeq.Empty
      case Elem("foxml", "digitalObject", attrs, scope, children @ _*) =>
        Elem("foxml", "digitalObject", attrs.remove("PID"), scope, minimizeEmpty=false, children: _*)
      case _ => n
    }
  }
  private val transformer = new RuleTransformer(rule)

  def strip(foXml: Elem): String =
    transformer.transform(foXml).head.toString()
}
