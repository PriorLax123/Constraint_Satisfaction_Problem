Jackson Mishuk
 
  **NOTE: The code seems to be fully consistent with your examples, except for the xword2 with large dictionary examples. 
  				I am assuming this has to do with the arbitrary nature of mrv and mrv+deg when they are both the same in ordering
  
  The file Search.java contains all of the necessary code.

  This program allows you to give an input file with a puzzle layout and word bank The program will find a solution for the puzzle using the words given if there is one.  
  
  Command line arguments include: -d(File location of dictionary)*, -p(File location of puzzle)*, -v(Verbosity Level), -vs or --variable-selection(Variable selection, 
  -vo or --value-order(Value order), -lfc or --limited-forward-check(Limited forward check)
  **NOTE: All command line arguments marked with star are required**
  
  EXAMPLE OF CMD ARGUEMENT: java Solve.java -d ../a02-data/dictionary-large.txt -p ../a02-data/xword01.txt -v 2 -vs mrv --value-order lcv
  
  -d*:String reference for dictionary file
  
  -p*:String reference for puzzle file
  
  -v :The amount of information about the search that you want to display(Higher the number the more information that will display){0(Default), 1, 2}
  
	-vs or --variable-selection :Allows use of different heuristics for ordering of finding different words
															 {static(Default), mrv(minimum remaining values), deg(degree/most constraining variable), mrv+deg(mrv but ties are broken by degree)}
	
  -vo or --value-order :Allows for the use of different heuristics for ordering of values tried from the domain of a variable{static(Default), lcv(Least constraining value)}
  
  -lfc or --limited-forward-check :Indicates limited forward checking when checking for value consistency{Disabled by default}
  