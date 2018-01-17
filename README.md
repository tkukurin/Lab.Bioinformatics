# Mapping long reads to large reference databases

This is a project for the bioinformatics course on FER (http://www.fer.unizg.hr/en/course/bio).

Paper describing the implemented algorithm can be found
[here](https://www.biorxiv.org/content/biorxiv/early/2017/01/27/103812.full.pdf), and
its C++ implementation is [on GitHub](https://github.com/marbl/MashMap). The C++ implementation
seems to differ a bit from the paper description as the authors have improved the algorithm
over time.

## Installation
The dependencies for this program are all bundled in `./pom.xml` and therefore will automatically
be downloaded; you only need to have Maven installed on you machine. Running `mvn package` from
project root should be enough to install the program under `./target`.

## Running the program
The program expects two parameters, reference and query read in FASTA file format
(provided FASTA files should not contain any comments).

You can run the program by issuing the command:
```
java -jar ./target/bioinf-1.0-SNAPSHOT.jar [reference.fa] [query.fa]
```

If everything goes well, read data should be output to `./query.fa-out.txt` (note that the FASTA
file name is suffixed by `out.txt`.

## Testing
If you would like to run a sample test of the functionality, under `./helpers` there are two
simple bash scripts; you can first generate queries using `simulate.sh` like so:
```
./helpers/simulate.sh read
```

This will create sample queries under `./genomes/clostridium/queries`. You can test the program
(after it's been built using `mvn package`) on every one of them by running:
```
./helpers/run.sh
```

This will produce outputs in the root directory.

