//A custom class for putting in the priority queue of the potential improved swaps 
//Index is integer location in the production queue
//Reduction amount contains the improvement score of the swap

public class SwapPair {
 
    private int index;
    private int reductionAmount;
 
    public SwapPair(int newIndex, int newReduction){
        this.index=newIndex;
        this.reductionAmount=newReduction;
    }
 
    public int getIndex() {
        return index;
    }
 
    public int getReductionAmount() {
        return reductionAmount;
    }

    //Override method so when remove method gets called from the prority queue in main module it matches just the index value to find and make the deletion from the queue. Reduction amount is ignored
    @Override
    public boolean equals(Object o) {
        
        if (!(o instanceof SwapPair)) {
            return false;
        }

        SwapPair pair = (SwapPair) o;

        if (pair.index == this.index){
                return true;
        } else {
            return false;
        }


    }
 
}