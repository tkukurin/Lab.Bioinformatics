#!/bin/bash
# simulate reads using WGSIM.
# @param 1: output filename

DIRECTORY=$(dirname $0)
BASEDIR="$DIRECTORY/../genomes/clostridium"
mkdir -p ${BASEDIR}/queries

READ_PAIRS=50
SEED=42

BASE_ERROR_RATES=(0.005 0.01 0.05 0.075 0.075 0.1 0.1 0.15 0.15 0.15)
MUTATION_RATES=(0 0 0.01 0.02 0.05 0.05 0.1 0.1 0.1 0.1)
INDEL_RATES=(0 0 0 0 0.025 0.025 0.05 0.05 0.1 0.1)
READ_LENGTHS=(5000 5000 10000 15000 25000 50000 75000 100000 100000 150000)

for i in {0..9}; do
  READ_LENGTH=${READ_LENGTHS[$i]}
  MUTATION_RATE=${MUTATION_RATES[$i]}
  ERROR_RATE=${BASE_ERROR_RATES[$i]}
  INDEL_RATE=${INDEL_RATES[$i]}

  LOC_1="${BASEDIR}/queries/$1-${i}-1.fq"
  LOC_2="${BASEDIR}/queries/$1-${i}-2.fq"

  ${DIRECTORY}/wgsim -1${READ_LENGTH} -2${READ_LENGTH} \
    -r${MUTATION_RATE} \
    -e${ERROR_RATE} \
    -R${INDEL_RATE} \
    -N${READ_PAIRS} \
    -S${SEED} \
   \
    ${BASEDIR}/_clostridium_cellulosi.DG5.dna.toplevel.fa \
   \
    ${LOC_1} \
    ${LOC_2}


  sed '/^@/!d;s//>/;N' ${LOC_1} > ${LOC_1}.fa
done
