package com.wernerapps.solver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.Stack;

public class SudokuSolver
{
    //Helper functions for rowCol, rowVal, colVal, boxVal
    //Pattern of total matrix from http://www.stolaf.edu/people/hansonr/sudoku/exactcovermatrix.htm
    int val(int a) { return a % 9; }
    int row(int a) { return a / 9 / 9; }
    int col(int a) { return (a / 9) % 9; }
    int blk(int a) { return (row(a) / 3) * 3 + col(a) / 3; };
    
    
    /*
                    R1C1 R1C2
            R1C1#1  1    0
            R1C1#2  1    0
            R1C1#3  1    0
            R1C1#4  1    0
            ...
    
                    R1C1 R1C2
            R1C2#1  0    1
            R1C2#2  0    1
            R1C2#3  0    1
            R1C2#4  0    1
            ...
    */
    int rowCol(int a) { return             row(a) * 9 + col(a); }
    
    /*
    
                R1#1 R1#2
        R1C1#1  1    0
        R1C1#2  0    1
        ...
    
        R1C2#1  1    0
        R1C2#2  0    1
        ...
    
    */
    int rowVal(int a) { return     9 * 9 + row(a) * 9 + val(a); }
    
    /*
    
                C1#1 C1#2
        R1C1#1  1    0
        R1C1#2  0    1
        ...
    
        R1C2#1  1    0
        R1C2#2  0    1
        ...
    
    */
    int colVal(int a) { return 2 * 9 * 9 + col(a) * 9 + val(a); }
    
    /*
    
                B1#1 B1#2
        R1C1#1  1    0
        R1C1#2  0    1
        ...
    
        R1C2#1  1    0
        R1C2#2  0    1
        ...
    
    */
    int boxVal(int a) { return 3 * 9 * 9 + blk(a) * 9 + val(a); }
    
    public char[] solveSudoku(String input, boolean printSolutions)
    {
        assert(input.length() == 81);//"Input must be a String of length 81");
        char[] output = input.toCharArray();

        //Initialize our exact cover, we have 4 81-wide constraints
        ExactCover ec = new ExactCover(4 * 9 * 9);

        //For each row in the blank sudoku constraint, add each of the 4 constraints to the cover.
        for (int a = 0; a < 9 * 9 * 9; a++)
        {
            ec.add(a, rowCol(a));
            ec.add(a, rowVal(a));
            ec.add(a, colVal(a));
            ec.add(a, boxVal(a));
        }

        //Set initial constraints
        for (int i = 0; i < 81; i++)
        {
            if (input.charAt(i) != '.')
            {
                //Now we loop through our matrix only considering filled in values
                //Because the rows of the cover matrix go R1C1 #1 through R1C1 #9 and then down
                //we take our index out of 81 and multiply by nine to get the RxCy #1, then we add
                //input[i] - '1' to get the value of the cell, if it is a 1 we add 0 because we're
                //already at #1, but if it was a 3, '3' - '1' = 2, so RxCy#1 becomes RxCy#3
                //For each of the four constraint categories we then get the column value and then
                //cover those

                int a = 9 * i + input.charAt(i) - '1';
                ec.cover(rowCol(a));
                ec.cover(rowVal(a));
                ec.cover(colVal(a));
                ec.cover(boxVal(a));
            }
        }

        //We call solve
        if (!ec.solve())
            System.out.println("Error: there is no solution.");
        else
        {
            //There was a solution, so get it
            Stack<Integer> sol = ec.getSolution();

            while (!sol.empty())
            {
                //We reverse the math we did to take the index to row
                //We divide by 9 to go from RxCy #z to get the index
                //out of 81. We mod by 9 to get a number between 0 and 8,
                //and add '1' to get the char of a number between 1 and 9
                output[sol.peek() / 9] = (char) (sol.peek() % 9 + '1');
                sol.pop();
            }
            if (printSolutions)
            {
                //Display output
                System.out.println("--------------------------------------------------------------------------------");
    
                for (int i = 0; i < 9; i++)
                {
                    if (i % 3 == 0 && i > 0)
                        System.out.println("------+-------+------\t------+-------+------");
                    for (int k = 0; k < 9; k++)
                    {
                        if (k % 3 == 0 && k > 0)
                            System.out.print("| ");
                        char outChar = input.charAt(i * 9 + k);
                        if (outChar == '.')
                            outChar = '-';
                        System.out.print(outChar + " ");
                    }
                    System.out.print("\t");
                    for (int k = 0; k < 9; k++)
                    {
                        if (k % 3 == 0 && k > 0)
                            System.out.print("| ");
                        System.out.print(output[i * 9 + k] + " ");
                    }
                    System.out.println();
                }
            }
        }
        return output;
    }

    public static void main(String[] args) throws FileNotFoundException
    {
        File outputFile = new File("cppOutput.txt");
        File inputFile = new File("Puzzles.txt");
        Scanner inputScan = new Scanner(inputFile);   
        PrintWriter writer = new PrintWriter(outputFile);
        Scanner scan = new Scanner(System.in);
        
        long time = System.currentTimeMillis();
        int puzzles = 0;
        String input;
        System.out.print("Show output? (y/n) ");
        String temp = scan.nextLine();
        boolean showOutput = temp.toUpperCase().equals("Y");
        
        while (inputScan.hasNextLine())
        {
            input = inputScan.nextLine();
            writer.println(new SudokuSolver().solveSudoku(input, showOutput));
            puzzles++;
            
            if (showOutput)
            {
                System.out.print("Continue? (y/n) ");
                temp = scan.nextLine();
                if (temp.charAt(0) != 'y')
                    break;
            }
        }
        if (!showOutput)
            System.out.println("Took: " + (System.currentTimeMillis() - time) + " millis for " + puzzles + " puzzles");
        scan.close();
        inputScan.close();
        writer.close();
    }
}