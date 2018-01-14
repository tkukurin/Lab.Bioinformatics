#!/bin/sh
# simulate reads using WGSIM.
# @param 1: output filename

BASEDIR='../genomes/clostridium'
./wgsim -15000 -210000 -N50 \

  # input
  $BASEDIR/_clostridium_cellulosi.DG5.dna.toplevel.fa \

  # outputs
  $BASEDIR/$1-1.fq \
  $BASEDIR/$1-2.fq
