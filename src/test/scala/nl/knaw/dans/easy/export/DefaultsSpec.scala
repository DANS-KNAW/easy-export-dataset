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

import scala.collection.immutable.HashMap
import org.apache.commons.configuration.PropertiesConfiguration
import org.scalatest.{FlatSpec, Matchers}

class DefaultsSpec extends FlatSpec with Matchers {

  val optionsMap = HashMap("fcrepo-user" -> 'u',
                           "fcrepo-server" -> 's',
                           "fcrepo-password" -> 'p')

  val tmpFile = new File("target/test/application.properties")
  writeAll(tmpFile, """default.fcrepo-server=http://localhost:8080/fedora
                      |default.fcrepo-user=somebody
                      |default.fcrepo-password=secret
                      | """.stripMargin)
  
  "minimal args" should "retreive all default values" in {
    val args = "easy-dataset:1 ./doesNotExist".split(" ").toSeq
    Defaults(tmpFile, optionsMap, args).get.length shouldBe 6
  }
  
  "provided options" should "retreive less defaults" in {
    val args = "-p p easy-dataset:1 ./doesNotExist".split(" ").toSeq
    Defaults(tmpFile, optionsMap, args).get shouldBe 2
  }
}
