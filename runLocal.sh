#!/bin/bash

########################################
############# CSCI 2951-O ##############
########################################
E_BADARGS=65
if [ $# -ne 1 ]
then
	echo "Usage: `basename $0` <input>"
	exit $E_BADARGS
fi

input=$1

# export the solver libraries into the path
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/Applications/CPLEX_Studio/cpoptimizer/bin/arm64_osx:/Applications/CPLEX_Studio2211/cpoptimizer/bin/arm-64_osx
# add the solver jar to the classpath and run
arm64java="/Applications/CPLEX_Studio2211/opl/oplide/jdk-18.0.2+9-jre/Contents/Home/bin/java"

$arm64java -Djava.library.path="/Applications/CPLEX_Studio2211/opl/bin/arm64_osx/" -cp /Applications/CPLEX_Studio2211/cpoptimizer/lib/ILOG.CP.jar:src solver.ls.Main $input
