/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.export

import java.io.File

import scala.collection.immutable.HashMap
import org.apache.commons.configuration.PropertiesConfiguration
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable

class CommanLineDefaultsSpec extends FlatSpec with Matchers {

  val optionsMap = HashMap("fcrepo-user" -> 'u',
                           "fcrepo-server" -> 's',
                           "fcrepo-password" -> 'p')

  val tmpFile = new File("target/test/application.properties")
  tmpFile.getParentFile.mkdirs()
  tmpFile.safeWrite(
    """default.fcrepo-server=http://localhost:8080/fedora
      |default.fcrepo-user=somebody
      |default.fcrepo-password=secret
      | """.stripMargin
  )

  "minimal args" should "retreive all default values" in {
    val args = Seq[String]()
    CommandLineDefaults(tmpFile, optionsMap).get
      .getOmittedOptions(args).mkString(" ") shouldBe
      "--fcrepo-server http://localhost:8080/fedora --fcrepo-user somebody --fcrepo-password secret"
  }

  "provided options" should "retreive less defaults" in {
    val args = "-pp -u u --fcrepo-server s".split(" ")
    CommandLineDefaults(tmpFile, optionsMap).get
      .getOmittedOptions(args).mkString(" ") shouldBe ""
  }

  "a short key identical to the start of a long key" should "not cause confusion" in {
    val optionsMap = HashMap(
      "fcrepo-user" -> 'u',
      "fcrepo-server" -> 's',
      "fcrepo-password" -> 'f'
    )
    val args = "-uu --fcrepo-server s".split(" ")
    CommandLineDefaults(tmpFile, optionsMap).get
      .getOmittedOptions(args).mkString(" ") shouldBe "--fcrepo-password secret"
  }
}
