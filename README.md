**Greedy Production Line Optimizer for Asynchronous Changeovers by Justin Conroy**
**04-07-2024**

- `src`: the folder containing the java source code. Copy this folder to your local computer. App.java is the main class holding the program to run. SwapPair.java is a custom class needed by App.java and should stay in the same folder as App.java
- `resources`: is a subfolder which holds some csv files which have the configuration data. Note: GetResourceAsStream methods are used to load the files in the program so it is path independent of where the folder is located on the local computer. It will still find the csv files as long as the `resources` subfolder stays in the folder with the .java files when copied over to the local computer.

- `enableSwapLog` constant in App.java is set to false by default as showing the whole swap queue after each pass slows stuff down. However, it was very helpful to enable while I was debugging to ensure the queue was behaving properly 

- `JobToIndex.csv`: This CSV maps each job name "A", "B", "C" etc. to an integer index value which will make it easy to look up in the job change over table CSV
- `ProductChangeOverMatrix.csv`: This CSV is the change over matrix from the problem with the headers stripped out so index of rows and columns match the lookup table map from first CSV
- `ProductionTime.csv`: The production time for each job. This CSV is a single row file just relying on the column index for identification
- `ProductionQueue.csv`: This CSV contains a single row with the Job sequence for the question in the problem
- `ProductionQueueTest.csv`: This optional CSV contains the other job sequence where the answer was given for testing purposes. To test it, change 'strProductionQueuePath' constant in code of App.java to "resources/ProductionQueueTest.csv"

- Development of this code was done with `JDK 17.0.6`

**Problem Description**

A production manager has 10 products to schedule on a single production line.  The products are labeled A through J and each product has a time to produce.

|Product        |	A|B	| C	|D	|E	| F|	G	|H	| I	| J |
|---------------|---|---|---|---|---|---|---|---|---|---|
|Time to produce|	7 |	13	| 2	| 4 |	21	| 6	| 8	| 12 |	17	|22|

There is also an asymmetric changeover matrix showing the time required to switch between one product and another as defined by the table below.  The table is read as the time to switch from the product in the row to the product in the column.  So, changing from producing A to producing B incurs a cost of 9 units of time, but changing from product B to product A incurs only 3 units of time.
|| 	A|	B	|C	|D	|E	|F	|G	|H	|I	|J |
|---|---|---|---|---|---|---|---|---|---|---|
|A|	-	|9	|13|	6|	7|	8|	3|	17|	7|	3|
|B|	3|	-|	8|	7|	9|	4|	3|	16|	3|	8|
|C|	5|	0|	-|	8|	7|	9|	4|	3|	16|	5|
|D|	7|	2|	5|	-|	4|	2|	5|	7|	2|	5|
|E|	4|	3|	4|	7|	-|	4|	3|	4|	7|	3|
|F|	2|	14|	7|	7|	5|	-|	1|	4|	6|	4|
|G|	8|	7|	9|	4|	16|	3|	-|	4|	9|	8|
|H|	11|	5|	2|	3|	2|	6|	4|	-|	9|	7|
|I|	4|	2|	12|	9|	1|	5|	5|	6|	-|	9|
|J|	9|	3|	6|	7|	8|	3|	17|	7|	3|	-|
 
We are given a starting sequence: (B, G, E, A, C, I, J, D, H, F).  It has a production time of 193 time units.
This Java program uses a greedy heuristic:
- Begins with the given sequence and makes a series of improving adjacent neighbor swaps.
- At each pass, it finds and maks only the best swap, considering all possible adjacent neighbor swaps.
- The algorithm continues to find and make improving swaps and terminate when no further improvement can be made.
- When the algorithm terminates, it prints out the best sequence and the total time units to produce it.


