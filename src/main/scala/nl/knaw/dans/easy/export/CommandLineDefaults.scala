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

import org.apache.commons.configuration.PropertiesConfiguration

import scala.collection.JavaConverters._
import scala.util.Try

class CommandLineDefaults private(shortToLongKeys: Map[Char,String],
                                  defaultKeyValuePairs: Map[String, String]
                                 ) {
  def getOmittedOptions(specifiedArgs: Seq[String]
                       ): Array[String] =
    if (specifiedArgs.contains("--help") || specifiedArgs.contains("--version"))
                             Array[String]()
    else {
      val longArgs = specifiedArgs.filter(_.matches("--.*")).map(_.replaceFirst("--",""))
      val shortArgs = specifiedArgs.filter(_.matches("-[a-zA-Z].*")).map(_.charAt(1))
      val allArgs = longArgs ++ shortArgs.map(shortToLongKeys.getOrElse(_,"?"))

      val strings = defaultKeyValuePairs.keys.filter(!allArgs.contains(_)).toArray.map {
        key => Array(s"--$key", defaultKeyValuePairs.get(key).get)
      }
      strings.flatten
    }
}

object CommandLineDefaults {

  val applPropsFile = new File(System.getProperty("app.home", ""), "cfg/application.properties")

  def apply(longToShortKeys: Map[String, Char]
           ): Try[CommandLineDefaults] =
    Try(new CommandLineDefaults(
      invert(longToShortKeys),
      getDefaultKeyValuePairs(applPropsFile)
    ))

  def apply(propertiesFile: File,
            longToShortKeys: Map[String, Char]
           ): Try[CommandLineDefaults] =
    Try(new CommandLineDefaults(
      invert(longToShortKeys),
      getDefaultKeyValuePairs(propertiesFile)
    ))

  private def getDefaultKeyValuePairs(propertiesFile: File
                                     ): Map[String, String] = {

    if (!propertiesFile.isFile || !propertiesFile.canRead)
      throw new Exception(s"${propertiesFile.getAbsolutePath} is not a readable file")

    val properties = new PropertiesConfiguration(propertiesFile) // may also throw something
    properties.getKeys.asScala.toList
      .filter(_.startsWith("default."))
      .map(key => key.replaceFirst("default.", "") -> properties.getString(key)).toMap
  }
}
