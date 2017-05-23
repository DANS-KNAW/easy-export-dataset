/**
 * Copyright (C) 2015 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
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
package nl.knaw.dans.easy

import java.io.{InputStream, FileOutputStream, File}

import org.apache.commons.io.IOUtils

import scala.util.{Success, Failure, Try}
import scala.xml._

package object export {

  def invert[T1, T2](m: Map[T1, T2]): Map[T2, T1] =
    m.map { case (key, value) => (value, key) }

  def toSdoName(objectId: String): String =
    objectId.replaceAll("[^0-9a-zA-Z]", "_")

  implicit class RichNode(n: Node) {
    def headOfAttr(namespace: String, attrName: String): Option[String] =
      n.attribute(namespace, attrName).flatMap(xs => xs.headOption.map(_.text))
  }

  implicit class RichFile (left: File) {
    def safeWrite(content: String): Try[Unit] = {
      val os = new FileOutputStream(left)
      try {
        IOUtils.write(content,os)
        Success(Unit)
      }finally{
        IOUtils.closeQuietly(os)
      }
    }
  }

  implicit class RichInputStream (left: InputStream){

    def copyAndClose(f: File): Try[Unit] =
      try{
        val out = new FileOutputStream(f)
        try{
          IOUtils.copyLarge(left,out)
          Success(Unit)
        } finally {
          IOUtils.closeQuietly(out)
        }
      } finally {
        IOUtils.closeQuietly(left)
      }

    def readXmlAndClose: Success[Elem] =
      try{
        Success(XML.load(left))
      } finally {
        IOUtils.closeQuietly(left)
      }

    def readAndClose: Success[Array[Byte]] = {
      try{
        Success(IOUtils.toByteArray(left))
      }finally {
        IOUtils.closeQuietly(left)
      }
    }
  }

  implicit class RichSeq[T](left: Seq[T]) {

    def foreachUntilFailure[S](f: T => Try[S]): Try[Unit] = {
      left.foreach { x =>
        f(x).recover { case t: Throwable => return Failure(t) }
      }
      Success(Unit)
    }
  }
}
