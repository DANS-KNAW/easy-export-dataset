easy-export-dataset
===================
[![Build Status](https://travis-ci.org/DANS-KNAW/easy-export-dataset.svg?branch=master)](https://travis-ci.org/DANS-KNAW/easy-export-dataset)

Export an EASY dataset to a Staged Digital Object set.


SYNOPSIS
--------

    easy-export-dataset <dataset-pid> <staged-digital-object-set>


DESCRIPTION
-----------

Exports an EASY dataset from one repository to a [Staged Digital Object set] which can be [imported] into another EASY Fedora Commons 3.x Repository. 

All the digital objects belonging to the dataset are 
exported, including: the dataset, all file and folder items, download history and jump-off pages. If `dataset-pid`
is not present in the Fedora repository or `stage-digital-object-set` cannot be created (e.g., it already exists)
the program terminates with an error.

Objects belonging to the dataset are selectied via the relation `isSubordinateTo`.
For each digital object the last version of each (managed) datastream, a `fo.xml` and `cfg.json` file are downloaded.
The `fo.xml` file includes the inline datastreams except `RELS_EXT`
which is exported into to the "relations"-map in the file `cfg.json` ([Digital Object Configuration]).
Fedora PIDs in this file that reference downloaded objects are replaced by the appropriate SDO-name.

Checksums and PIDs of downloaded objects are removed from the downloaded `fo.xml`.
For that purpose the following components are removed:

* Any element who's content equals the id of one of the downloaded objects,
* The attribute PID in the element `<foxml:digitalObject>`
* The attribute DIGEST in the element `<foxml:contentDigest>`


ARGUMENTS
---------

     -h, --help      Show help message
     -v, --version   Show version of this program
    
    trailing arguments:
     dataset-pid (required)                 The id of a dataset in the fedora repository
     staged-digital-object-set (required)   The resulting Staged Digital Object directory that will be created.


INSTALLATION AND CONFIGURATION
------------------------------
Currently this project is built as an RPM package for RHEL7/CentOS7 and later. The RPM will install the binaries to
`/opt/dans.knaw.nl/easy-export-dataset` and the configuration files to `/etc/opt/dans.knaw.nl/easy-export-dataset` 

To install the module on systems that do not support RPM, you can copy and unarchive the tarball to the target host.
You will have to take care of placing the files in the correct locations for your system yourself. For instructions
on building the tarball, see next section.


BUILDING FROM SOURCE
--------------------

Prerequisites:

* Java 8 or higher
* Maven 3.3.3 or higher
* RPM
 
Steps:

        git clone https://github.com/DANS-KNAW/easy-export-dataset.git
        cd easy-export-dataset
        mvn install
        
If the `rpm` executable is found at `/usr/local/bin/rpm`, the build profile that includes the RPM 
packaging will be activated. If `rpm` is available, but at a different path, then activate it by using
Maven's `-P` switch: `mvn -Pprm install`.

Alternatively, to build the tarball execute:

    mvn clean install assembly:single

[Staged Digital Object set]: https://github.com/DANS-KNAW/easy-ingest#staged-digital-object-set
[Digital Object Configuration]: https://github.com/DANS-KNAW/easy-ingest#digital-object-configuration-file
[EASY Metadata]: https://easy.dans.knaw.nl/schemas/md/emd/2013/11/emd.xsd
[DCTERMS format]: http://dublincore.org/documents/dcmi-terms/#terms-format
[MIME Type]: https://en.wikipedia.org/wiki/MIME
[imported]: https://github.com/DANS-KNAW/easy-export-dataset/wiki
