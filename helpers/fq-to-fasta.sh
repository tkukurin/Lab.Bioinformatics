#!/bin/sh
# convert from FQ to FA (expect single file name as input)
sed '/^@/!d;s//>/;N' $1 > $1.fa
