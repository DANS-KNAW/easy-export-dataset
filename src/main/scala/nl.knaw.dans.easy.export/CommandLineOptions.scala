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

import org.rogach.scallop.{ ScallopConf, ScallopOption }

class CommandLineOptions(args: Seq[String], configuration: Configuration) extends ScallopConf(args) {

  appendDefaultToDescription = true
  editBuilder(_.setHelpWidth(110))

  printedName = "easy-export-dataset"
  version(s"$printedName ${ configuration.version }")
  val description = "Export an EASY dataset to a Staged Digital Object set."
  val synopsis = s"$printedName <dataset-pid> <staged-digital-object-set>"
  banner(
    s"""
       |$description
       |
       |Usage:
       |
       |$synopsis
       |
       |Options:
       |""".stripMargin
  )

  val datasetId: ScallopOption[String] = trailArg("dataset-pid",
    descr = "The id of a dataset in the fedora repository")
  val sdoSet: ScallopOption[File] = trailArg("staged-digital-object-set",
    descr = "The resulting Staged Digital Object directory that will be created.")

  /** long option names to explicitly defined short names */
  val optionMap: Map[String, Char] = builder.opts
    .withFilter(_.requiredShortNames.nonEmpty)
    .map(opt => (opt.name, opt.requiredShortNames.head))
    .toMap

  validateFileDoesNotExist(sdoSet)

  verify()
}
