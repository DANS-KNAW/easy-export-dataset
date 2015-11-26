import java.io.FileInputStream

import nl.knaw.dans.easy.export._

import scala.io.Source._
import java.io.File

import scala.util.Try

new java.io.File(".").getAbsolutePath
val file = new File("/Users/jokep/git/service/easy/easy-export-dataset/src/test/scala/nl/knaw/dans/easy/export/tmp.sc")
val is = new FileInputStream(file)
fromInputStream(is).mkString
is.close()
//is.read()


val inputStream = fromInputStream(new FileInputStream(file))
val triedXs: Try[Seq[String]] = Try(inputStream.getLines.toSeq)
foreachUntilFailure(triedXs, (line: String) => Try(println(line)))
inputStream.close
inputStream.mkString
