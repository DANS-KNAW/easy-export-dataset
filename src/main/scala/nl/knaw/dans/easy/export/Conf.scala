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
import java.net.URL

import org.rogach.scallop._
import org.slf4j.LoggerFactory

import scala.util.Success

class Conf private (args: Seq[String]) extends ScallopConf(args) {
  val log = LoggerFactory.getLogger(getClass)

  printedName = "easy-export-dataset"
  version(s"$printedName v${Version()}")
  banner(s"""
            |Export an EASY dataset to a Staged Digital Object set.
            |
            |Usage:
            |
            | $printedName <dataset-pid> <staged-digital-object-set>
            |
            |Options:
            |""".stripMargin)

  val shouldNotExist = singleArgConverter[File](conv = {f =>
    if (new File(f).exists()) {
      log.error(s"$f allready exists")
      throw new IllegalArgumentException()
    } else {
      val parent = new File(f).getParentFile
      if (!parent.isDirectory) {
        log.error(s"$parent is not an existing directory")
        throw new IllegalArgumentException()
      }
    }
    new File(f)
  })

  val fedora = opt[URL]("fcrepo-server", required = true, short= 'f',
    descr = "URL of Fedora Commons Repository Server to connect to ")
  val user = opt[String]("fcrepo-user", required = true, short = 'u',
    descr = "User to connect to fcrepo-server")
  val password = opt[String]("fcrepo-password", required = true, short = 'p',
    descr = "Password for fcrepo-user")

  val datasetId = trailArg[String](
    name = "dataset-pid",
    descr = "The id of a dataset in the fedora repository",
    required = true)
  val sdoSet = trailArg[File](
    name = "staged-digital-object-set",
    descr = "The resulting Staged Digital Object directory that will be created.",
    required = true)(shouldNotExist)

  /** long option names to explicitly defined short names */
  val optionMap = builder.opts
    .withFilter(opt => opt.requiredShortNames.nonEmpty)
    .map(opt => (opt.name, opt.requiredShortNames.head)).toMap
}

object Conf {

  private val log = LoggerFactory.getLogger(getClass)

  def apply (args: Array[String] = Array[String]()): Conf =
    new Conf(getDefaults(args) ++ args)

  private def getDefaults(args: Array[String]): Seq[String] = {
    val validArgs = "-f http://localhost:8080/fedora -u u -p p id ./doesNotExist".split(" ")
    val optionMap = new Conf(validArgs).optionMap // using apply here would cause stack overflow!
    val propsFile = new File(System.getProperty("app.home", ""), "cfg/application.properties")
    log.info(s"reading defaults from ${propsFile.getAbsolutePath}")
    Defaults(propsFile, optionMap, args)
      .recoverWith { case t: Throwable =>
        log.warn(s"ignored defaults in ${propsFile.getAbsolutePath} : ${t.getMessage}")
        Success(Seq[String]())
      }.get
  }
}
