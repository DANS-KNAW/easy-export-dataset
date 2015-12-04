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

import java.io.File

import org.scalatest.{FlatSpec, Matchers}

import scala.xml.Node

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

    JSON(
      new File (settings.sdoSet, toSdoName("easy-file:10")),
      Seq[Node](),
      relsExt,
      placeHoldersFor = Seq("easy-folder:3", "easy-dataset:1")
    ).get shouldBe
      """{
        |  "namespace":"easy-file",
        |  "datastreams":[],
        |  "relations":[{
        |    "predicate":"http://dans.knaw.nl/ontologies/relations#isMemberOf",
        |    "objectSDO":"easy_folder_3"
        |  },{
        |    "predicate":"http://dans.knaw.nl/ontologies/relations#isSubordinateTo",
        |    "objectSDO":"easy_dataset_1"
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

    val foXml =
      <foxml:digitalObject VERSION="1.1" PID="easy-dataset:1"
                           xmlns:foxml="info:fedora/fedora-system:def/foxml#"
                           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                           xsi:schemaLocation="info:fedora/fedora-system:def/foxml# http://www.fedora.info/definitions/1/0/foxml1-1.xsd">
        <foxml:datastream ID="DC" STATE="A" CONTROL_GROUP="X" VERSIONABLE="false">
          <foxml:datastreamVersion ID="DC.2" LABEL="Dublin Core Record for this object"
                                   CREATED="2015-05-29T15:24:55.122Z" MIMETYPE="text/xml"
                                   FORMAT_URI="http://www.openarchives.org/OAI/2.0/oai_dc/" SIZE="4089">
          </foxml:datastreamVersion>
        </foxml:datastream>
        <foxml:datastream ID="PRSQL" STATE="A" CONTROL_GROUP="X" VERSIONABLE="false">
          <foxml:datastreamVersion ID="PRSQL.3" LABEL="Permission request sequences for this dataset"
                                   CREATED="2015-08-08T23:44:19.487Z" MIMETYPE="text/xml" SIZE="150">
            <foxml:contentDigest TYPE="SHA-1" DIGEST="dbbbe9f7d96717b9ed383b7204fd93f410baef29"/>
          </foxml:datastreamVersion>
        </foxml:datastream>
        <foxml:datastream ID="EASY_ITEM_CONTAINER_MD" STATE="A" CONTROL_GROUP="X" VERSIONABLE="false">
          <foxml:datastreamVersion ID="EASY_ITEM_CONTAINER_MD.5" LABEL="Metadata for this item container"
                                   CREATED="2015-08-08T23:44:19.553Z" MIMETYPE="text/xml"
                                   FORMAT_URI="http://easy.dans.knaw.nl/easy/item-container-md/" SIZE="156">
            <foxml:contentDigest TYPE="SHA-1" DIGEST="c40570d1cfa0bf9ab45d5d483ca6bb23b8620e98"/>
          </foxml:datastreamVersion>
        </foxml:datastream>
        <foxml:datastream ID="EMD" STATE="A" CONTROL_GROUP="X" VERSIONABLE="false">
          <foxml:datastreamVersion ID="EMD.9" LABEL="Descriptive metadata for this dataset"
                                   CREATED="2015-08-28T13:09:53.619Z" MIMETYPE="text/xml"
                                   FORMAT_URI="http://easy.dans.knaw.nl/easy/easymetadata/" SIZE="5676">
            <foxml:contentDigest TYPE="SHA-1" DIGEST="b26519a65bf9fddf1a5885154f36a26850a110c1"/>
          </foxml:datastreamVersion>
        </foxml:datastream>
        <foxml:datastream ID="AMD" STATE="A" CONTROL_GROUP="X" VERSIONABLE="false">
          <foxml:datastreamVersion ID="AMD.7" LABEL="Administrative metadata for this dataset"
                                   CREATED="2015-08-28T13:09:53.755Z" MIMETYPE="text/xml" SIZE="3381">
            <foxml:contentDigest TYPE="SHA-1" DIGEST="72ceb7b04bb362a319d65a029ba0881e014cc8e8"/>
          </foxml:datastreamVersion>
        </foxml:datastream>
      </foxml:digitalObject>

    JSON(
      new File(settings.sdoSet,toSdoName("easy-dataset:1")),
      foXml \ "datastream",
      relsExt,
      placeHoldersFor = Seq()
    ).get shouldBe
      """{
        |  "namespace":"easy-dataset",
        |  "datastreams":[{
        |    "contentFile":"DC",
        |    "dsID":"DC",
        |    "label":"Dublin Core Record for this object",
        |    "mimeType":"text/xml",
        |    "controlGroup":"X"
        |  },{
        |    "contentFile":"PRSQL",
        |    "dsID":"PRSQL",
        |    "label":"Permission request sequences for this dataset",
        |    "mimeType":"text/xml",
        |    "controlGroup":"X"
        |  },{
        |    "contentFile":"EASY_ITEM_CONTAINER_MD",
        |    "dsID":"EASY_ITEM_CONTAINER_MD",
        |    "label":"Metadata for this item container",
        |    "mimeType":"text/xml",
        |    "controlGroup":"X"
        |  },{
        |    "contentFile":"EMD",
        |    "dsID":"EMD",
        |    "label":"Descriptive metadata for this dataset",
        |    "mimeType":"text/xml",
        |    "controlGroup":"X"
        |  },{
        |    "contentFile":"AMD",
        |    "dsID":"AMD",
        |    "label":"Administrative metadata for this dataset",
        |    "mimeType":"text/xml",
        |    "controlGroup":"X"
        |  }],
        |  "relations":[]
        |}""".stripMargin
  }
}
