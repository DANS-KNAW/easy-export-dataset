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

import com.yourmediashelf.fedora.generated.access.DatastreamType
import org.scalatest.{FlatSpec, Matchers}

class JsonSpec extends FlatSpec with Matchers {

  implicit val settings = Settings(Conf.dummyInstance).get

  "file in folder" should "produce proper relations" in {
    val relsExt =
      <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <rdf:Description rdf:about="info:fedora/easy-file:10">
          <isMemberOf xmlns="http://dans.knaw.nl/ontologies/relations#"
                      rdf:resource="info:fedora/easy-folder:3"></isMemberOf>
          <isSubordinateTo xmlns="http://dans.knaw.nl/ontologies/relations#"
                           rdf:resource="info:fedora/easy-dataset:1"></isSubordinateTo>
          <hasModel xmlns="info:fedora/fedora-system:def/model#"
                    rdf:resource="info:fedora/easy-model:EDM1FILE"></hasModel>
          <hasModel xmlns="info:fedora/fedora-system:def/model#"
                    rdf:resource="info:fedora/dans-container-item-v1"></hasModel>
        </rdf:Description>
      </rdf:RDF>


    JSON(toSdoDir("easy-file:10"), Seq[DatastreamType](), relsExt).get shouldBe
      """{
        |  "namespace":"easy-file",
        |  "datastreams":[],
        |  "relations":[{
        |    "predicate":"http://dans.knaw.nl/ontologies/relations#isMemberOf",
        |    "objectSDO":"./DirThatDoesNotExist/easy_folder_3"
        |  },{
        |    "predicate":"http://dans.knaw.nl/ontologies/relations#isSubordinateTo",
        |    "objectSDO":"./DirThatDoesNotExist/easy_dataset_1"
        |  },{
        |    "predicate":"info:fedora/fedora-system:def/model#hasModel",
        |    "object":"info:fedora/easy-model:EDM1FILE"
        |  },{
        |    "predicate":"info:fedora/fedora-system:def/model#hasModel",
        |    "object":"info:fedora/dans-container-item-v1"
        |  }]
        |}""".stripMargin
  }

  "dataset" should "produce proper datastreams" in {
    val relsExt =
      <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
        <rdf:Description rdf:about="info:fedora/easy-dataset:1">
        </rdf:Description>
      </rdf:RDF>

    val dataStreams = Seq(
      ("DC", "view content Dublin Core Record for this object", "text/xml"),
      ("PRSQL", "view content Permission request sequences for this dataset", "text/xml"),
      ("EASY_ITEM_CONTAINER_MD", "view content Metadata for this item container", "text/xml"),
      ("EMD", " view content Descriptive metadata for this dataset", "text/xml"),
      //("RELS-EXT", "view content rels-ext", "text/xml"),
      ("AMD", "Administrative metadata for this dataset", "text/xml")
    ).map { case (dsId, label, mime) => val ds = new DatastreamType()
      ds.setDsid(dsId)
      ds.setLabel(label)
      ds.setMimeType(mime)
      ds
    }

    JSON(toSdoDir("easy-dataset:1"), dataStreams, relsExt).get shouldBe
      """{
        |  "namespace":"easy-dataset",
        |  "datastreams":[{
        |    "contentFile":"./DirThatDoesNotExist/easy_dataset_1/DC",
        |    "dsID":"DC",
        |    "label":"view content Dublin Core Record for this object",
        |    "mimeType":"text/xml",
        |    "controlGroup":"X"
        |  },{
        |    "contentFile":"./DirThatDoesNotExist/easy_dataset_1/PRSQL",
        |    "dsID":"PRSQL",
        |    "label":"view content Permission request sequences for this dataset",
        |    "mimeType":"text/xml",
        |    "controlGroup":"X"
        |  },{
        |    "contentFile":"./DirThatDoesNotExist/easy_dataset_1/EASY_ITEM_CONTAINER_MD",
        |    "dsID":"EASY_ITEM_CONTAINER_MD",
        |    "label":"view content Metadata for this item container",
        |    "mimeType":"text/xml",
        |    "controlGroup":"X"
        |  },{
        |    "contentFile":"./DirThatDoesNotExist/easy_dataset_1/EMD",
        |    "dsID":"EMD",
        |    "label":" view content Descriptive metadata for this dataset",
        |    "mimeType":"text/xml",
        |    "controlGroup":"X"
        |  },{
        |    "contentFile":"./DirThatDoesNotExist/easy_dataset_1/AMD",
        |    "dsID":"AMD",
        |    "label":"Administrative metadata for this dataset",
        |    "mimeType":"text/xml",
        |    "controlGroup":"X"
        |  }],
        |  "relations":[]
        |}""".stripMargin
  }
}
