/**
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
package nl.knaw.dans.easy

import java.io.{ File, InputStream }

import org.apache.commons.io.IOUtils
import resource.Using

import scala.util.{ Failure, Success, Try }
import scala.xml._

package object export {

  def invert[T1, T2](m: Map[T1, T2]): Map[T2, T1] = m.map { case (key, value) => (value, key) }

  def toSdoName(objectId: String): String = objectId.replaceAll("[^0-9a-zA-Z]", "_")

  implicit class RichNode(n: Node) {
    def headOfAttr(namespace: String, attrName: String): Option[String] = {
      n.attribute(namespace, attrName).flatMap(_.headOption.map(_.text))
    }
  }

  implicit class RichFile(file: File) {
    def safeWrite(content: String): Try[Unit] = {
      Using.fileOutputStream(file)
        .map(IOUtils.write(content, _))
        .tried
    }
  }

  implicit class RichInputStream(inputStream: InputStream) {
    def copyToFile(file: File): Try[Unit] = {
      Using.fileOutputStream(file)
        .map(IOUtils.copyLarge(inputStream, _))
        .tried
        .map(_ => ())
    }
  }

  implicit class RichSeq[T](sequence: Seq[T]) {
    def foreachUntilFailure[S](f: T => Try[S]): Try[Unit] = {
      for (l <- sequence) {
        f(l).recover { case t => return Failure(t) }
      }
      Success(())
    }
  }
}
