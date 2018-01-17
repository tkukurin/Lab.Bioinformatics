#!/bin/bash
# run program on simulated reads

DIRECTORY=$(dirname $0)
BASEDIR="$DIRECTORY/.."

DATADIR="$BASEDIR/genomes/clostridium"
QUERIES="$DATADIR/queries"

REFERENCE="$DATADIR/_clostridium_cellulosi.DG5.dna.toplevel.fa"

MASHDIR="${BASEDIR}/mashout"
mkdir -p "${MASHDIR}"

for query in $(ls ${QUERIES} | grep .fa$); do
  echo "Mapping $query..."
  ${DIRECTORY}/mashmap -s "${REFERENCE}" -q "$QUERIES/${query}" -o "${MASHDIR}/$query"
done

