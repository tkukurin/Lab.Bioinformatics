# Mapping long reads to large reference databases

This is a project for the bioinformatics course on FER (http://www.fer.unizg.hr/en/course/bio).

Paper describing the implemented algorithm can be found
[here](https://www.biorxiv.org/content/biorxiv/early/2017/01/27/103812.full.pdf), and
the original C++ implementation is [on GitHub](https://github.com/marbl/MashMap).

## Installation
The project has a dependency on [https://github.com/yeastrc/java-fasta-utils](FASTA Utils), which
can be found in the `./lib` directory. In order to install the program, first run from the project
root:

```
mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file \
  -Dfile=./lib/fasta-utils-1.1.jar \
  -DgroupId=org.yeastrc.fasta \
  -Dversion=1.1 \
  -Dpackaging=jar \
  -DartifactId=fasta-utils \
  -DlocalRepositoryPath=./lib
```

After that, the library should be installed in your local maven repository under `./lib`. You
can then run `mvn package`, which should install the program in `./target`. The JAR file expects
two parameters, reference and query read in FASTA file format (without comments).

You can run the program by issuing the command:
```
java -jar ./target/bioinf-1.0-SNAPSHOT.jar [reference.fa] [query.fa]
```

If everything goes well, read data should be output to `out.txt`.

