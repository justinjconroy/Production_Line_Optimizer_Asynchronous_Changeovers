//Production Line Optimizer Project by Justin Conroy
//04-07-2024

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Hashtable;


public class App {
    private static final Boolean enableSwapLog = false;  //For Debugging purposes. Set to true to print the entire ranked swap queue after each pass
    
    //All CSV configuration files are stored in the resources subdirectory of the src folder. GetResourceAsStream methods are used to load the files so it is path independent of where the src folder is located
    
    //This CSV maps each job name "A", "B", "C" etc. to an integer index value which will make it easy to look up in the job modification table 
    private static final String strLookupPath = "resources/JobToIndex.csv";
    
    //This CSV is the change over matrix from the problem with the headers stripped out so index of rows and columns match the lookup table map from first CSV
    private static final String strChangeOverPath = "resources/ProductChangeOverMatrix.csv";
    
    //Production time for each job. This CSV is a single row file just relying on the column index for identification
    private static final String strProductionTimePath = "resources/ProductionTime.csv";
    
    //This CSV contains a single row with the Job order for the question in the problem. 
    private static final String strProductionQueuePath = "resources/ProductionQueue.csv";

    //A custom comparator for the SwapPair class to order them in our Priority Queue of potential pairs to swap
    //based on the ones with the most reduction to overall production time being at the top
    public static Comparator<SwapPair> reductionComparator = new Comparator<SwapPair>(){
 
        @Override
        public int compare(SwapPair p1, SwapPair p2) {
            return (int) (p1.getReductionAmount() - p2.getReductionAmount());
        }
    };
    
    public static void main(String[] args) throws Exception {
        final long startTime = System.currentTimeMillis();

        //Load all the data from the CSV files
        List<Dictionary<String, String>> dictIDtoIndex = createJobIDToIndex(strLookupPath);
   
        int[][] aChangeOver = convert2DStrToInt(get2DStrArrayFromCSV(strChangeOverPath));
        int[] aProductionTime = convert2DStrToInt(get2DStrArrayFromCSV(strProductionTimePath))[0];
        int[] aProductionSeq = convertStrToIndexes(get2DStrArrayFromCSV(strProductionQueuePath)[0], dictIDtoIndex);
        
        //Print out intial production sequence and total production time to make sure everything is correct at the beginning
        System.out.println("Initial production sequence: " + String.join(", ", convertIndexesToStr(aProductionSeq, dictIDtoIndex)));
        System.out.println("The initial total production time is " + getTotalTime(aProductionSeq, aChangeOver, aProductionTime));
        System.out.println();
        System.out.println("_________________________________________");
        
        //**The main algorithm starts here**
        //For large production sequences we don't want to cycle through the entire production sequence with each pass since we are only changing one small section around the single pair that is being flipped
        //per pass. Thus instead we will use a priority queue which will only list all the possible *improving* flips and rank order them with a custom comparator based on a calculated score.
        //After one is polled from the top of the queue to swap, only the nearby pairs in the production sequence are re-scored and then are added/deleted/modified in the swap queue based on the new score  

        //Initialize the queue
        Queue<SwapPair> queuePairSwaps = new PriorityQueue<>(aProductionSeq.length-1, reductionComparator);
        
        //Populate the initial swap queue by cycling through the entire production sequence once. **This is the only time we need to cycle through the entire production sequence 
        int intSwapScore;
        for (int i = 0; i < aProductionSeq.length-1; i++) {
            intSwapScore = getTotalSwapScore(aProductionSeq, aChangeOver, i);
            //negative value means we are reducing production time with that swap
            if (intSwapScore < 0){
                queuePairSwaps.add(new SwapPair(i, intSwapScore));    
            }
        }

        if (enableSwapLog){
            System.out.println("Initial Swap Queue:");
            printSwapQueue(queuePairSwaps);
            System.out.println("_________________________________________");
        }

        int passes = 0;
        
        ///Core loop through the priority queue of potential improving swaps
        while (queuePairSwaps.size() > 0){
            //get the best swap pair from the top of the queue
            SwapPair currentSwap = queuePairSwaps.poll();
            //The notation for currentIndex is the first Job in the pair
            int currentIndex = currentSwap.getIndex();
            int currentScore = currentSwap.getReductionAmount();
            
            if (enableSwapLog){
                System.out.println();
                System.out.println("Swapping pair at indexes " + currentIndex + " and " + (currentIndex + 1) + " to reduce production time by " + currentScore);
            
            }
            int tempIndex = aProductionSeq[currentIndex];
            aProductionSeq[currentIndex] = aProductionSeq[currentIndex+1];
            aProductionSeq[currentIndex+1] = tempIndex;

            //Re-score nearby potential swaps in the production sequence whose swap scores were affected by the swap we just did
            //then add/delete/modifiy in the swap queue based on the new calculated scores
            updateSwapQueue(aProductionSeq, aChangeOver, queuePairSwaps, currentIndex + 1);
            updateSwapQueue(aProductionSeq, aChangeOver, queuePairSwaps, currentIndex + 2);

            updateSwapQueue(aProductionSeq, aChangeOver, queuePairSwaps, currentIndex - 1);
            updateSwapQueue(aProductionSeq, aChangeOver, queuePairSwaps, currentIndex - 2);
            
            passes +=1;
            
            if (enableSwapLog){
                //Print out new modified sequence after that pass         
                System.out.println(String.join(", ", convertIndexesToStr(aProductionSeq, dictIDtoIndex)));
                System.out.println();
                System.out.println("Swap Queue after pass " + passes + ":");
                printSwapQueue(queuePairSwaps);
                System.out.println("_________________________________________________________");
            }
            
        }

        //Print out the results after all passes have been completed
        System.out.println();
        System.out.println("Completed with " + passes + " passes in " + (System.currentTimeMillis() - startTime) + " msec execution time");
        System.out.println("The final Sequence is " + String.join(", ", convertIndexesToStr(aProductionSeq, dictIDtoIndex)));
        System.out.println("The final total production time is " + getTotalTime(aProductionSeq, aChangeOver, aProductionTime));
        System.out.println("_________________________________________________________");
  
    }

    //A function to score a possible pair swap. Calulates change over time difference not just for that pair but also the new potential pairs created behind and in front of it
    //Sums these 3 change over time differences together for a total score. Negative is good as this indicates reduction in time. Positive is bad as we are increasing production time     
    public static int getTotalSwapScore(int[] aProductionSeq, int[][] aChangeOver, int index){
       //BGEA for example where we are looking at GE swap. The notation for index is the first Job in the pair so in this case the index of the swap is 1. B=0, G=1, E=2, A=3
    
       //calculate the difference in change over time if we swap to this pair  (GE to EG)
       int intSwapScore = aChangeOver[aProductionSeq[index+1]][aProductionSeq[index]] - aChangeOver[aProductionSeq[index]][aProductionSeq[index+1]];
       
       //but we also changed the pair combination just before it in the sequence so need to calculate change over difference for that too (BG to BE)
       if ((index-1) > -1){    
            intSwapScore += aChangeOver[aProductionSeq[index-1]][aProductionSeq[index+1]] - aChangeOver[aProductionSeq[index-1]][aProductionSeq[index]];
        }
       
       //and also changed the pair just in front as well (EA to GA)
       if ((index+2) < aProductionSeq.length){    
            intSwapScore += aChangeOver[aProductionSeq[index]][aProductionSeq[index+2]] - aChangeOver[aProductionSeq[index+1]][aProductionSeq[index+2]];
        }

        return intSwapScore;
    }

    //A function to re-score a pair in the production sequence and add/delete/modifiy in the swap queue based on the new score
    //The notation for index is the first Job in the pair
    public static boolean updateSwapQueue(int[] aProductionSeq, int[][] aChangeOver, Queue<SwapPair> qSwaps, int index){

        if (((index+1) < aProductionSeq.length) && (index >= 0)){
            
            //This pair might not even exist in queue but will remove it if it does. If we have to modify an existing one, we still need to delete and add back to queue with new score 
            qSwaps.remove(new SwapPair(index,0));   //reduction amount in class is ignored for this remove operation. It is only looking at whether the index exists and matches. This custom match is set due to an override equals method in SwapPair class
            
            int intScore = getTotalSwapScore(aProductionSeq, aChangeOver, index);
            //If improving score add to the swap queue
            if (intScore < 0){
                qSwaps.add(new SwapPair(index, intScore));    
            }
            return true;
        }else{
            return false;
        }
    }

    //Function to print out the current swap queue in ranked order
    public static boolean printSwapQueue(Queue<SwapPair> qSwaps){
        
        //create a seperate copy so we don't actually modify the queue
        PriorityQueue<SwapPair> qCopy = new PriorityQueue<>(qSwaps);
        
        if (qCopy.isEmpty()){
            System.out.println("The queue is empty!");
            System.out.println();
            return true;
        }

        int i = 1;
        while (!qCopy.isEmpty()){
            SwapPair currentSwap = qCopy.poll();
            System.out.println(i + ".  Swap at Indexes: " + currentSwap.getIndex() + " and " + (currentSwap.getIndex() + 1) + "   Score: " + currentSwap.getReductionAmount());
            i +=1;
        } 

        System.out.println();
        return true;
    }

    //A function to calculate total production time of a given sequence including change overs
    public static int getTotalTime(int[] aProductionSeq, int[][] aChangeOver, int[] aProductTime){

        int intTotal = 0;

        for (int i=0; i < aProductionSeq.length; i++){
            intTotal += aProductTime[aProductionSeq[i]];
            if((i + 1) < aProductionSeq.length){
                intTotal += aChangeOver[aProductionSeq[i]][aProductionSeq[i+1]];
            }
        }

        return intTotal;

    }

    
    //Simple function to load CSV into a 2D string array
    //GetResourceAsStream methods are used to load the files so it is path independent of where the src folder is located on the local PC
    public static String[][] get2DStrArrayFromCSV(String fileName){
        
        String currentLine; 
        BufferedReader csvInput = null;

        try{
            InputStream csvStream = App.class.getResourceAsStream(fileName);
            csvInput = new BufferedReader(new InputStreamReader(csvStream));
        } catch(Exception e){
            System.out.println("Error opening " + fileName);
            System.exit(0);
        }

        
        List<String[]> lLines = new ArrayList<String[]>();

        try{
            while ((currentLine = csvInput.readLine()) != null) {
                lLines.add(currentLine.split(","));
            }
            csvInput.close();
        } catch (IOException e){
            System.out.println("IO Error in" + fileName);
            System.exit(0);
        }

        String[][] aString = new String[lLines.size()][0];
        lLines.toArray(aString);

        return aString;
       
    }

    //Simple function to convert 2D string array to 2D integer array
    public static int[][] convert2DStrToInt(String[][] aStr){
        
        int[][] aInt = new int[aStr.length][aStr[0].length];
        
        for (int i = 0; i < aInt.length; i++){
            for (int j = 0; j < aInt[0].length; j++){
                aInt[i][j] = Integer.parseInt(aStr[i][j]);    
            }
        }

        return aInt;
    }


    //creates two dictionaries and return them in a two element list. One where the key is the Job ID and the value is in the index. The other has the key as the job index and the value is the Job ID. 
    //Takes up twice the amount of memory but faster when trying to go from index back to to Job ID
    public static List<Dictionary<String, String>> createJobIDToIndex(String filePath){

        String[][] aData = get2DStrArrayFromCSV(filePath);
        Dictionary<String, String> dictJob = new Hashtable<>();
        Dictionary<String, String> dictIndex = new Hashtable<>();
        for (int i = 0; i < aData[0].length; i++){
            dictJob.put(aData[0][i], aData[1][i]);
            dictIndex.put(aData[1][i], aData[0][i]);
        }
        
        List<Dictionary<String, String>> dicts = new ArrayList<>();
        dicts.add(dictJob);
        dicts.add(dictIndex);
        
        return dicts;
    }

    //Function to convert a Job ID such as Ab, B, C, etc to a numerical index based on the dictionary loaded from createJobIDtoIndex method
    public static int[] convertStrToIndexes(String[] aStringID, List<Dictionary<String, String>> dictLookup){
        
        int[] aInt = new int[aStringID.length];

        for (int i = 0; i < aStringID.length; i++) {
            aInt[i] = Integer.parseInt(dictLookup.get(0).get(aStringID[i]));
        }
        return aInt;
    }
    
    
    //Function to convert a numerical index to Job ID such as Ab, B, C, etc based on the dictionary loaded from createJobIDtoIndex method
    public static String[] convertIndexesToStr(int[] aIntIndex, List<Dictionary<String, String>> dictLookup){
    
        String[] aStr = new String[aIntIndex.length];

        for (int i = 0; i < aIntIndex.length; i++){
            aStr[i] = dictLookup.get(1).get(Integer.toString(aIntIndex[i]));
        }
        return aStr;
    }
}
