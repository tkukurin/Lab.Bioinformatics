#!/bin/bash
# simulate reads using WGSIM.
# @param 1: output filename

DIRECTORY=$(dirname $0)
BASEDIR="$DIRECTORY/../genomes/clostridium"
QUERIES="$BASEDIR/queries"
#i=5
#LOC="${QUERIES}/$1-${i}-1.fq.fa"
#Q="$BASEDIR/_clostridium_cellulosi.DG5.dna.toplevel.fa"

REFERENCE="$BASEDIR/_clostridium_cellulosi.DG5.dna.toplevel.fa"

for query in $(ls ${QUERIES} | grep .fa$); do
  echo "Mapping $query..."
  ${DIRECTORY}../target/bioinf-1.0-SNAPSHOT.jar ${REFERENCE} ${query}
done

