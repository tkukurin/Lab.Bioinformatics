#!/bin/bash
# simulate reads using WGSIM.
# @param 1: output filename

echo $0
DIRECTORY=$(dirname $0)
BASEDIR="$DIRECTORY/../genomes/clostridium"
echo $DIRECTORY
echo $BASEDIR
$DIRECTORY/wgsim -15000 -210000 -N50 \
\
  $BASEDIR/_clostridium_cellulosi.DG5.dna.toplevel.fa \
\
  $BASEDIR/$1-1.fq \
  $BASEDIR/$1-2.fq
