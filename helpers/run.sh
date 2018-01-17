#!/bin/bash
# run program on simulated reads

DIRECTORY=$(dirname $0)
BASEDIR="$DIRECTORY/.."

DATADIR="$BASEDIR/genomes/clostridium"
QUERIES="$DATADIR/queries"

REFERENCE="$DATADIR/_clostridium_cellulosi.DG5.dna.toplevel.fa"

for query in $(ls ${QUERIES} | grep .fa$); do
  echo "Mapping $query..."
  java -jar "${BASEDIR}/target/bioinf-1.0-SNAPSHOT.jar" "${REFERENCE}" "$QUERIES/${query}"
done

