//Jackson Mishuk

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

/*
	**NOTE: The code seems to be fully consistent with your examples, except for the xword2 with large dictionary examples. 
					I am assuming this has to do with the arbitrary nature of mrv and mrv+deg when they are both the same in ordering
	
	The file Search.java contains all of the necessary code.
	
	This program allows you to give an input file with a puzzle layout and word bank The program will find a solution for the puzzle using the words given if there is one.  
	
	Command line arguments include: -d(File location of dictionary)*, -p(File location of puzzle)*, -v(Verbosity Level), -vs or --variable-selection(Variable selection, -vo or --value-order(Value order), -lfc or --limited-forward-check(Limited forward check)
	**NOTE: All command line arguments marked with star are required**
	
	EXAMPLE OF CMD ARGUEMENT: java Solve.java -d ../a02-data/dictionary-large.txt -p ../a02-data/xword01.txt -v 2 -vs mrv --value-order lcv
	
	-d*															:String reference for dictionary file
	-p*															:String reference for puzzle file
	-v 															:The amount of information about the search that you want to display(Higher the number the more information that will display)
																	{0(Default), 1, 2}
	-vs  or --variable-selection 		:Allows use of different heuristics for ordering of finding different words
																	{static(Default), mrv(minimum remaining values), deg(degree/most constraining variable), mrv+deg(mrv but ties are broken by degree)}
	-vo  or --value-order 			 		:Allows for the use of different heuristics for ordering of values tried from the domain of a variable
																	{static(Default), lcv(Least constraining value)}
	-lfc or --limited-forward-check :Indicates limited forward checking when checking for value consistency
																	{Disabled by default}
*/

public class Solve {
	
	public static void main(String[] args) {
		
		String[] arg = {"-d", "a02-data/dictionary-medium.txt", "-p", "a02-data/xword02.txt", "-v", "1", "-vs", "mrv"};
		
		Problem P = new Problem(arg);
		
		if(P.dictionaryFile==null || P.puzzleFile==null) {
			System.out.println("Either the dictionary file or the puzzle file can not be found\n");
			return;
		}
		
		if(!loadDictionary(P) || !loadPuzzle(P)) {
			return;
		}
		
		if(!cspLoad(P)) {
			return;
		}
		
		if(P.verbosityLvl >= 1) {
			System.out.println("* Attempting to solve crossword puzzle...\n");
		}
		if(!cspSolve(P)) {
			return;
		}
		
	}

	/*
	 * Loads the dictionary file
	 * 
	 * Returns boolean(true if the file is valid, false if not)
	 */
	private static boolean loadDictionary(Problem P) {
		if(P.verbosityLvl >= 1) {
			System.out.printf("* Reading dictionary from [%s]\n" , P.dictionaryFile);
		}
		try (BufferedReader br = new BufferedReader(new FileReader(P.dictionaryFile))){
			
			String line = br.readLine();
			int i = 0;
			while(line != null) {
				i++;
				P.WordList.add(line);
				line = br.readLine();
			}
			if(P.verbosityLvl >= 2) {
				System.out.printf("** Dictionary has %d words\n\n" , i);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return true;
	}
	
	/*
	 * Loads the puzzle file
	 * 
	 * Returns boolean(true if the file is valid, false if not)
	 */
	private static boolean loadPuzzle(Problem P) {
		if(P.verbosityLvl >= 1) {
			System.out.printf("* Reading puzzle from [%s]\n" , P.puzzleFile);
		}
		
		File file = new File(P.puzzleFile);
		try {
			Scanner scan = new Scanner(file);
			
			String row = scan.next();
			String col = scan.next();
			
			P.PuzzleInfo = new String[Integer.valueOf(row)][Integer.valueOf(col)];
			
			StringBuilder puzzleString = new StringBuilder();
			
			if(P.verbosityLvl >= 2) {
				puzzleString.append("** Puzzle\n");
			}
			
			for(int i=0; i<P.PuzzleInfo.length; i++) {

				
				String[] info = new String[Integer.valueOf(col)];
				
				for(int j = 0; j<info.length; j++) {
					P.PuzzleInfo[i][j] = scan.next();
					if(P.verbosityLvl >= 2)
						puzzleString.append(String.format("%-2s ", P.PuzzleInfo[i][j]));
				}
				puzzleString.append("\n");
			}
			
			scan.close();
			if(P.verbosityLvl >= 2) {
				System.out.println(puzzleString.toString());
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	
	/*
	 * Loads the Constraint Satisfaction Problem 
	 * 
	 * Returns boolean(true)
	 */
	private static boolean cspLoad(Problem P) {
		int varsCreated = 0;
		int constraintsCreated = 0;
		//Increment a row of the puzzle
		for(int i = 0; i<P.PuzzleInfo.length; i++) {
		//Increment a column of the puzzle(backwards to guarentee that a down has already been seen when a constraint is being added)
			for(int j = P.PuzzleInfo[i].length - 1; j >= 0; j--) {
				if(!(P.PuzzleInfo[i][j].equals("#") || P.PuzzleInfo[i][j].equals("_"))){
					
					//Checking for down words
					if(i == 0 || P.PuzzleInfo[i-1][j].equals("#")) {
						int length = 0;
						for(int k = i; k<P.PuzzleInfo.length; k++) {
							if(P.PuzzleInfo[k][j].equals("#")) {
								break;
								
							}else {
								length++;
							}
						}
						P.VarList.put(-Integer.valueOf(P.PuzzleInfo[i][j]), new Problem.Var(Integer.valueOf(P.PuzzleInfo[i][j]),
								/*isAcross value*/false, length, P, null));
						varsCreated++;
					}
					
					//Checking for across words
					if(j == 0 || P.PuzzleInfo[i][j-1].equals("#")) {
						HashMap<Integer, int[]> Collisions = new HashMap<Integer, int[]>();
						int length = 0;
						for(int k = j; k<P.PuzzleInfo[i].length; k++) {
							if(P.PuzzleInfo[i][k].equals("#")) {
								break;
								
							}else {
								//Across words check for collisions with Down words
								for(int h = i; h>=0 && !P.PuzzleInfo[h][k].equals("#"); h--) {
									if(!P.PuzzleInfo[h][k].equals("_") && (h == 0 || P.PuzzleInfo[h-1][k].equals("#"))) {
										//Goal here is to create an entire constraint map or add a constraint within 
										constraintsCreated++;
										/*System.out.printf("%3s) Constraint: %2d Across Letter %2d; %2d Down Letter %2d\n",
												constraintsCreated, Integer.valueOf(P.PuzzleInfo[i][j]), k, Integer.valueOf(P.PuzzleInfo[h][k]), i-h);*/
										int[] downColis = {-Integer.valueOf(P.PuzzleInfo[h][k]), i-h};
										Collisions.put(k-j, downColis);
									}
								}
								length++;
							}
						}
						P.VarList.put(Integer.valueOf(P.PuzzleInfo[i][j]), new Problem.Var(Integer.valueOf(P.PuzzleInfo[i][j]),
								/*isAcross value*/true, length, P, Collisions));
						varsCreated++;
					}
				}
			}
		}
		if(P.verbosityLvl >= 1) {
			System.out.printf("* CSP has %d variables\n"
					+ "* CSP has %d constraints\n", varsCreated, constraintsCreated);
			//Change to true to make sure variables are being created correctly
			debugVals(false, P);
		}
		return true;
	}
	
	//Used for debugging csp only!
	static private void debugVals(boolean bool, Problem P) {
		if(bool) {
			System.out.println();
			Object[] keySet = P.VarList.keySet().toArray();
			for(int i = 0; i < keySet.length; i++) {
				
				Problem.Var element = P.VarList.get(keySet[i]);
				String direction = element.name > 0 ? "Across": " Down ";
				String notDirection = element.name < 0 ? "Across": "Down";
				String DomainStr = "";
				
				for(String DomainElem:element.Domain)
					DomainStr += DomainElem + ", ";
				
				String ConstraintsStr = "";
				Object[] constraintKeySet = element.Collisions.keySet().toArray();
				for(int j = 0; j<constraintKeySet.length; j++) {
					int[] constraintArr = element.Collisions.get(constraintKeySet[j]);
					if(j==0)
						ConstraintsStr += String.format(" The character %d colides with character %d of %d %S"
								, constraintKeySet[j], constraintArr[1], Math.abs(constraintArr[0]), notDirection);
					else
						ConstraintsStr += String.format(", \n \t\t\t      The character %d colides with character %d of %d %S "
								, constraintKeySet[j], constraintArr[1], Math.abs(constraintArr[0]), notDirection);
				}
				
				System.out.printf("\n##\t\t Value[%d %-6S]; Length[%d]; \n  \t\t Domain[%s]\n\n  \t\t Constraints[%s]\n", 
						Math.abs(element.name), direction, element.length, DomainStr, ConstraintsStr);
			}
		}
	}
	
	
	
	private static int recursiveCalls = 0;//This is called within the backtracking search
	
	/*
	 * Used to solve the csp
	 * 
	 * Returns boolean(true with success and false with failure)
	 */
	private static boolean cspSolve(Problem P) {
		HashMap<Problem.Var, String> Assignment = new HashMap<Problem.Var, String>();

		if(P.verbosityLvl >= 2)
			System.out.printf("** Running backtracking search...\n"
					+ "Backtrack Call:\n");
		long startTimeSearch = System.currentTimeMillis();
		//lastSystemTime = startTimeSearch;
		Assignment = backtrack(P, Assignment, 1);
		
		if(Assignment == null) {
			System.out.printf("FAILED; Solving took %dms (%d recursive calls)\n\n", 
					System.currentTimeMillis() - startTimeSearch, recursiveCalls);
			return false;
		}else {
			System.out.printf("SUCCESS! Solving took %dms (%d recursive calls)\n\n", 
					System.currentTimeMillis() - startTimeSearch, recursiveCalls);
			System.out.println(puzzleString(Assignment, P));
		}
		
		return true;
	}
	
	/*
	 * Backtrack method with recursive calls to method
	 * 
	 * Returns HashMap<Problem.Var, String>(The Assignment of a solved puzzle, or null indicating failure)
	 */

	private static HashMap<Problem.Var, String> backtrack(Problem P, HashMap<Problem.Var, String> Assignment, int tabAmt) {
		recursiveCalls++;
		
		if(Assignment.size() >= P.VarList.size()) {
			if(P.verbosityLvl >= 2) {
				for(int i = 0; i<tabAmt; i++)
					System.out.print("  ");
				System.out.println("Assignment is complete!\n");
			}
			return Assignment;
		}
		
		Problem.Var Variable = setUnassignedVariable(P, Assignment);
		char dir = Variable.name>0 ? 'a' : 'd'; 
		if(P.verbosityLvl >= 2) {
			for(int i = 0; i<tabAmt; i++)
				System.out.print("  ");
			System.out.printf("Trying values for X%d%c\n", Math.abs(Variable.name), dir);
		}
		
		if(Variable == null) 
			return null;
		
		for(Object Value:OrderDomainValues(P, Variable, Assignment)) {
			
			
			if(Variable.isConsistent((String)Value, Assignment, P)) {
				if(P.verbosityLvl >= 2) {
					for(int i = 0; i<tabAmt; i++)
						System.out.print("  ");
					System.out.printf("Assignment { X%d%c = %S } is consistent\n", Math.abs(Variable.name), dir, Value);
				}
				Assignment.put(Variable, (String)Value);
				
				if(P.verbosityLvl >= 2) {
					for(int i = 0; i<tabAmt; i++)
						System.out.print("  ");
					System.out.printf("Backtrack Call:\n");
				}
				if(backtrack(P, Assignment, tabAmt+1)/*Recursive Call*/ != null) 
					return Assignment;
				
				Assignment.remove(Variable);
			}else {
				if(P.verbosityLvl >= 2) {
					for(int i = 0; i<tabAmt; i++)
						System.out.print("  ");
					System.out.printf("Assignment { X%d%c = %S } is inconsistent\n", Math.abs(Variable.name), dir, Value);
				}
			}
		}
		if(P.verbosityLvl >= 2) {
			for(int i = 0; i<tabAmt; i++)
				System.out.print("  ");
			System.out.printf("Failed call; backtracking...\n");
		}
		return null;
	}

	/*
	 * Uses selected heuristic to order the variables
	 * 
	 * Returns Var(The Var to be solved next)
	 */
	private static Problem.Var setUnassignedVariable(Problem P, HashMap<Problem.Var, String> Assignment) {
		Integer[] keySet = P.VarList.keySet().toArray(new Integer[0]);
		if(P.variableSelection.equals("static")) {
			Arrays.sort(keySet, new Comparator<Integer>() {
				public int compare(Integer i1,Integer i2){  
					
					if(Assignment.get(P.VarList.get(i1)) != null) return -1;
					if(Assignment.get(P.VarList.get(i2)) != null) return 1;
					
					int absO1 = Math.abs(i1); int absO2 = Math.abs(i2);
					
					if(absO1>absO2) return 1;
					if(absO1<absO2) return -1;
					if(i1<i2)				return 1;
					return -1;	
				}
			});
		}else if(P.variableSelection.equals("mrv")) {
			Arrays.sort(keySet, new Comparator<Integer>() {
				public int compare(Integer i1,Integer i2){  

					Problem.Var V1 = P.VarList.get(i1);
					Problem.Var V2 = P.VarList.get(i2);
					if(Assignment.get(V1) != null && Assignment.get(V2) == null) return -1;
					if(Assignment.get(V2) != null && Assignment.get(V1) == null) return 1;
					
					int D1const = 0;
					for(String element:V1.Domain) {
						if(V1.isConsistent(element, Assignment, P)) D1const++;
					}
					
					int D2const = 0;
					for(String element:V2.Domain) {
						if(V2.isConsistent(element, Assignment, P)) D2const++;
					}
					if(D1const>D2const) 	return 1;
					if(D1const<D2const)		return -1;
					return 0;
				}
			});
		}else if(P.variableSelection.equals("deg")) {
			Arrays.sort(keySet, new Comparator<Integer>() {
				public int compare(Integer i1,Integer i2){  
					
					HashMap<Integer, int[]> C1 = P.VarList.get(i1).Collisions; 
					
					int c1Unassigned = 0;
					
					Integer[] keySet1 = C1.keySet().toArray(new Integer[0]);
					for(int i = 0; i<C1.size(); i++) {
						if(Assignment.get(P.VarList.get(C1.get(keySet1[i])[0]))==null) c1Unassigned++;
					}
					if(c1Unassigned == 0) return 1;	
					
					HashMap<Integer, int[]> C2 = P.VarList.get(i2).Collisions;
					
					if(c1Unassigned>C2.size()) return -1;
					
					int c2Unassigned = 0;
					
					Integer[] keySet2 = C2.keySet().toArray(new Integer[0]);
					for(int i = 0; i<C2.size(); i++) {
						if(Assignment.get(P.VarList.get(C2.get(keySet2[i])[0]))==null) c2Unassigned++;
					}
					
					if(c1Unassigned<c2Unassigned)		return 1;
					if(c1Unassigned>c2Unassigned) 	return -1;
					return 0;
				}
			});
		}else if(P.variableSelection.equals("mrv+deg")){
			Arrays.sort(keySet, new Comparator<Integer>() {
				public int compare(Integer i1,Integer i2){  
					Problem.Var V1 = P.VarList.get(i1);
					Problem.Var V2 = P.VarList.get(i2);
					
					int D1const = 0; int D2const = 0;
					
					if(Assignment.get(V1) != null && Assignment.get(V2) == null) return -1;
					if(Assignment.get(V2) != null && Assignment.get(V1) == null) return 1;
					if(Assignment.get(V2) != null && Assignment.get(V1) != null) {}
					else {
						for(String element:V1.Domain) {
							if(V1.isConsistent(element, Assignment, P)) D1const++;
						}
						
						for(String element:V2.Domain) {
							if(V2.isConsistent(element, Assignment, P)) D2const++;
						}
						if(D1const>D2const) 	return 1;
						if(D1const<D2const)		return -1;
					}
					
					
					HashMap<Integer, int[]> C1 = P.VarList.get(i1).Collisions; 
					HashMap<Integer, int[]> C2 = P.VarList.get(i2).Collisions;
					
					int c1Unassigned = 0;
					
					Integer[] keySet1 = C1.keySet().toArray(new Integer[0]);
					for(int i = 0; i<C1.size(); i++) {
						if(Assignment.get(P.VarList.get(C1.get(keySet1[i])[0]))==null) c1Unassigned++;
					}
					
					int c2Unassigned = 0;
					
					Integer[] keySet2 = C2.keySet().toArray(new Integer[0]);
					for(int i = 0; i<C2.size(); i++) {
						if(Assignment.get(P.VarList.get(C2.get(keySet2[i])[0]))==null) c2Unassigned++;
					}
					
					if(c1Unassigned>c2Unassigned)		return 1;
					if(c1Unassigned<c2Unassigned) 	return -1;
					return 0;
				}
			});
		}
		for(int i = 0; i < keySet.length; i++) {
			if(Assignment.get(P.VarList.get(keySet[i])) == null) {
				return P.VarList.get(keySet[i]);
			}
		}
		return null;
	}

	/*
	 * Uses selected heuristic to order the values
	 * 
	 * Returns Object[](The order of values to be checked)
	 */
	private static Object[] OrderDomainValues(Problem P, Problem.Var Variable, HashMap<Problem.Var, String> Assignment) {
		if(P.valueOrder.equals("static")) 
			Arrays.sort(Variable.Domain);
		else if(P.valueOrder.equals("lcv")) 
			Arrays.sort(Variable.Domain, new Comparator<String>() {
				public int compare(String i1,String i2){
					int count1 = 0;
					int count2 = 0;
					
					HashMap<Integer, int[]> collis = Variable.Collisions;
					Integer[] keyArr = collis.keySet().toArray(new Integer[0]);
					for(int i = 0; i<keyArr.length; i++) {
						int tempKey = keyArr[i];
						int[] tempColis = collis.get(tempKey);
						
						Problem.Var tempVar = P.VarList.get(tempColis[0]);
						
						if(Assignment.get(tempVar) != null) continue;
						char int1Char = i1.charAt(tempKey);
						char int2Char = i2.charAt(tempKey);
						
						for(int j = 0; j < tempVar.Domain.length; j++) {
							
							char checkingChar = tempVar.Domain[j].charAt(tempColis[1]);
							if(int1Char == checkingChar) {
								count1++;
							}
							if(int2Char == checkingChar) {
								count2++;
							}
						}
					}
					if(count1<count2) return 1;
					if(count1>count2) return -1;
					return 0;
				}
			});
		return Variable.Domain;
	}

	/*
	 * Calculates final puzzle after search
	 * 
	 * Returns String(Final puzzle after solve)
	 */
	private static String puzzleString(HashMap<Problem.Var, String> cspRet, Problem P) {
		StringBuilder stringRet = new StringBuilder("");
		for(int i = 0; i<P.PuzzleInfo.length; i++) {
			for(int j = 0; j<P.PuzzleInfo[i].length; j++) {
				if (Character.isDigit(P.PuzzleInfo[i][j].charAt(0))){
					addWordsRecurse(i, j, cspRet, P);
				}
			}
		}
		for(int i = 0; i<P.PuzzleInfo.length; i++) {
			for(int j = 0; j<P.PuzzleInfo[i].length; j++) {
				if(P.PuzzleInfo[i][j].equals("#")) stringRet.append(" ");
				else stringRet.append(String.format("%s", P.PuzzleInfo[i][j]));
			}
			stringRet.append("\n");
		}
		return stringRet.toString();
	}

	//Used the build puzzle string only!
	private static void addWordsRecurse(int i, int j, HashMap<Problem.Var, String> cspRet, Problem P){
		int currNum = Integer.valueOf(P.PuzzleInfo[i][j]);
		//across
		if(j == 0 || P.PuzzleInfo[i][j-1].equals("#")) {
			for(int k = j; k < P.VarList.get(currNum).length + j; k++) {
				if(Character.isDigit(P.PuzzleInfo[i][k].charAt(0)) && Integer.valueOf(P.PuzzleInfo[i][k]) != currNum){
					addWordsRecurse(i, k, cspRet, P);
				}
				P.PuzzleInfo[i][k] = String.valueOf(cspRet.get(P.VarList.get(currNum)).charAt(k-j));
			}
		}
		//down
		if(i == 0 || P.PuzzleInfo[i-1][j].equals("#")) {
			for(int k = i; k < P.VarList.get(-currNum).length + i; k++) {
				if(Character.isDigit(P.PuzzleInfo[k][j].charAt(0)) && Integer.valueOf(P.PuzzleInfo[k][j]) != currNum) {
					addWordsRecurse(k, j, cspRet, P);
				}
				P.PuzzleInfo[k][j] = String.valueOf(cspRet.get(P.VarList.get(-currNum)).charAt(k-i));
			}
		}
	}
	
	static private class Problem{
		
		//All defined by user command line argument
		private String dictionaryFile = null;
		private String puzzleFile = null;
		private int verbosityLvl = 0;
		private String variableSelection = "static";
		private String valueOrder = "static";
		private boolean limitedForwardCheck = false;
		
		private String[][] PuzzleInfo;
		private LinkedList<String> WordList = new LinkedList<String>();
		
		private HashMap<Integer, Var> VarList = new HashMap<Integer, Var>();
		
		Problem(String[] args){
			for(int j = 0; j<args.length; j++){
				switch (args[j]) {
					case "-d":
						j++;
						dictionaryFile = args[j];
						break;
					case "-p":
						j++;
						puzzleFile = args[j];
						break;
					case "-v":
						j++;
						verbosityLvl = Integer.parseInt(args[j]);
						break;
					case "-vs":
					case "--variable-selection":
						j++;
						variableSelection = args[j];
						break;
					case "-vo":
					case "--value-order":
						j++;
						valueOrder = args[j];
						break;
					case "-lfc":
					case "--limited-forward-check":
						limitedForwardCheck = true;
						break;
				}
			}
		}
		private static class Var{
			private int name;
			private int length;
			private String[] Domain;
			
			//<Int of spot collision in current var, {Var it intersects with, spot in the other word}>
			private HashMap<Integer, int[]> Collisions = new HashMap<Integer, int[]>();
			
			Var(int n, boolean a/*Is Across*/, int l, Problem P, HashMap<Integer, int[]> C){
				if(a) {
					name = n;
					//Deals with collisions here
					createCollisions(C, P);
				}else
					name = -n;
				length = l;
				createDoman(P);
			}
			
			private void createDoman(Problem P) {
				int j = 0;
				Domain = new String[P.WordList.size()];
				for(String Word:P.WordList) {
					if(Word.length() == this.length) {
						Domain[j] = Word;
						j++;
					}
				}
				Domain = Arrays.copyOf(Domain, j);
			}
			
			//Will only be used in across variables
			private void createCollisions(HashMap<Integer, int[]> C, Problem P) {
				Collisions = C;
				Object[] keySet = C.keySet().toArray();
				for(int i = 0; i < keySet.length; i++) {
					int[] collisionInfo = C.get(keySet[i]);
					P.VarList.get(collisionInfo[0]).addDownCollision(collisionInfo[1], this.name, (int)keySet[i]);
					
				}
			}
			
			//Will only be used in down variables
			private void addDownCollision(int downSpot, int acrossValue, int acrossSpot) {
				int intArr[] = {acrossValue, acrossSpot};
				this.Collisions.put(downSpot, intArr);
			}
			
			//Used to see if a value is consistent in the given assignment in this Var
			public boolean isConsistent(String Value, HashMap<Problem.Var, String> Assignment, Problem P) {
				Integer[] collisionsKeySet = Collisions.keySet().toArray(new Integer[0]);
				for(int i = 0; i<collisionsKeySet.length; i++) {
					
					int[] tempCollisionArr = Collisions.get(collisionsKeySet[i]);
					Var tempVar = P.VarList.get(tempCollisionArr[0]);
					
					if(Assignment.get(tempVar) != null &&
					Value.charAt((int)collisionsKeySet[i]) != Assignment.get(tempVar).charAt(tempCollisionArr[1])) {
						return false;
					}
					
					//Limited forward Checking
					else if(P.limitedForwardCheck) {
						for(int j = 0; j<tempVar.Domain.length; j++){
							String element = tempVar.Domain[j];
							if(Value.charAt((int)collisionsKeySet[i]) == element.charAt(tempCollisionArr[1])) {
								j=tempVar.Domain.length;
								continue;
							}
							if(j + 1 == tempVar.Domain.length) {
								return false;
							}
						}
					}
				}
				return true;
			}
		}
	}
}
