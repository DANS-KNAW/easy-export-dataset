easy-export-dataset
===================

Export an EASY dataset to a BagIt directory.


SYNOPSIS
--------

    easy-export-dataset <dataset-pid> <export-bag-directory>
    
 
DESCRIPTION
-----------

Exports an EASY dataset to a bag directory, i.e. a directory conforming to the [BagIt] format (referred to as `$BAG`
in the following). If `$BAG` already exists the program terminates with an error message. 

The dataset files are contained in the bag's `$BAG/data` directory. The dataset metadata is put 
in `$BAG/metadata/easymetadata.xml` in [EASY Metadata] format. The file metadata is stored in `$BAG/metadata/files.xml`
which has a document element called `<files>` and below that one `<file>` element for each file. Each `<file>` element
has one nested [DCTERMS format] element with the [MIME Type] of the file.


ARGUMENTS
---------

<!-- Paste here from command line -->




INSTALLATION AND CONFIGURATION
------------------------------

### Installation steps:

1. Unzip the tarball to a directory of your choice, e.g. /opt/
2. A new directory called easy-export-dataset-<version> will be created (referred to as `$APPHOME` in the following)
3. Create a symbolic link to `$APPHOME/bin/easy-export-dataset` at `/usr/bin/easy-export-dataset` (or at some other
   location that is on the `PATH`. 

 
### Configuration:

Configuration settings must be specified in `$APPHOME/cfg/application.properties`. These include the connection 
settings for Fedora and for the File-system RDB.


BUILDING FROM SOURCE
--------------------

Prerequisites:

* Java 8 or higher
* Maven 3.3.3 or higher
 
Steps:

        git clone https://github.com/DANS-KNAW/easy-export-dataset.git
        cd easy-export-dataset
        mvn install
  

[BagIt]: https://tools.ietf.org/html/draft-kunze-bagit-11
[EASY Metadata]: https://easy.dans.knaw.nl/schemas/md/emd/2013/11/emd.xsd
[DCTERMS format]: http://dublincore.org/documents/dcmi-terms/#terms-format
[MIME Type]: https://en.wikipedia.org/wiki/MIME