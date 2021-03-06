package org.ivis.layout.util;

/**
 * This class provides two methods that sort the given array of
 * doubles with the quicksort algorithm. One method is static sorted,
 * i.e. the order of elements of the input array is preserved. An
 * array of indices is sorted instead. Other method is non-static
 * sorter, i.e. the elements of the input array are sorted and thus
 * they loose the initial order.
 */

public class PolyominoQuickSort
{

//----------------------------------------------------------------------
// Section: Public area
//----------------------------------------------------------------------

    /**
     * This method is a static sorter.
     *  Paramethers:
     *      n - is the number of doubles to be sorted;
     *      key - is array of doubles to be sorted. The order of elements
     *          in this array is preserved;
     *      index - in this array the sorted order of elements is
     *          returned. index[i] = j means that i-th greatest element
     *          is j-th element of array key.
     */

    public void sort(int n,
                        double key[],
                        int index[])
    {
        // store all paramethers as the instance variables
        this.n = n;
        this.key = key;
        this.index = index;

        // make the initial order of elements

        for (int i = 0; i < n; i++)
            index[i] = i;

        // call the recursive sorting method on the whole array
        recSortStatic(0,n-1);

        // do the final sort with the insertion sorting algorithm
        // because the recursive quicksort routine do not sort intervals
        // less than 10

        insertionSortStatic();
    }


    /**
     * This method is a special sorter that sorts an integer array.
     * The method is special because the elements a and b of the array
     * are not compared as integer values. The elements of other array
     * (array of keys) with indices a and b are compared instead. If two
     * elements have equal keys, then the relative ordering of these
     * elements is preserved.
     *  Paramethers:
     *      n - is the number of elements in the integer array;
     *      index - this is the integer array to be sorted;
     *      key - this array acts as keys for comparison of two integer
     *              elements. It is assumed that elements of the array
     *              index are in the range [0 .. key.length-1].
     */

    public void indexSort(int n,
                            int index[],
                            double key[])
    {
        // We want to reduce the problem to the simple static sorting.
        // First a mapping f from the set {0,..,n-1} to the set of
        // elements is defined by f(i) -> index[i]. Then the problem
        // simply reduces to the static sort where i-th key value is
        // key[f(i)].

        // allocate memory for the temporary arrays for the static sort
        int tempIndex[] = new int[n];
        double tempKey[] = new double[n];

        // this array will store the copy of array index
        int oldIndex[] = new int[n];

        for (int i = 0; i < n; i++)
        {
            // store the key for static sort
            tempKey[i] = key[index[i]];

            // remember the i-th value of the index array
            oldIndex[i] = index[i];
        }

        // sort the auxiliary arrays
        sort(n,tempKey,tempIndex);

        // and put the result back in the index array

        for (int i = 0; i < n; i++)
            index[i] = oldIndex[tempIndex[i]];
    }


    /**
     * This method is a non-static sorter.
     *
     * @param n - number of doubles to be sorted (from 0 to n-1);
     * @param key - array of doubles to be sorted.
     */
    public void sort(int n, double key[])
    {
        // store all parameters as the instance state variables
        this.n = n;
        this.key = key;

        // call the recursive sorting method on the whole array
        recSortNonStatic(0,n-1);

        // do the final sort with the insertion sorting algorithm
        // because the recursive quicksort routine do not sort intervals
        // less than 10
        insertionSortNonStatic();
    }


//----------------------------------------------------------------------
// Section: Methods for static sorter
//----------------------------------------------------------------------

    /**
     * This method do the recursive sorting with the quicksort
     * algorithm for static sorter. The interval to be sorted is
     * specified by the parameters.
     */
    private void recSortStatic(int left, int right)
    {
        // return immediately if the interval specified is too short
        if (left+10 > right)
            return;

        // choose the pivot and swap it with the last element of the given interval
        int pivot = median3Static(left,right);

        // iterate through the given interval from both ends simultaneously
        int i = left;
        int j = right-1;
        while(true) {
            // increment i while i-th element is less than pivot
            do i++; while (cmp(index[i],pivot) == -1);

            // decrement j while j-th element is greater than pivot
            do j--; while (cmp(index[j],pivot) == 1);

            // if i and j cross then we are done, otherwise swap i-th and j-th elements
            if (i < j)
                swapStatic(i,j);
            else
                break;
        }

        // place the pivot in the right place
        swapStatic(i,right-1);

        // recursively sort both parts to the left and to the right from the pivot
        recSortStatic(left,i-1);
        recSortStatic(i+1,right);
    }


    /**
     * This method chooses the middle element of left, right and center
     * element of given interval and returns the index of this element.
     * This method is used by static sorter.
     */

    private int median3Static(int left,int right)
    {
        // calculates the center of the given interval
        int center = (left+right)/2;

        // if left element is greater than center element, swap them

        if (cmp(index[left],index[center]) == 1)
            swapStatic(left,center);

        // if left element is greater than right element, swap them

        if (cmp(index[left],index[right]) == 1)
            swapStatic(left,right);

        // if center element is greater than right element, swap them

        if (cmp(index[center],index[right]) == 1)
            swapStatic(center,right);

        // now the center element is less or equal than right element
        // and greater or equal than left element

        // move the center element to the right side by swaping it with
        // the one before the right element

        swapStatic(center,right-1);

        // return the pivot's index
        return index[right-1];
    }


    /**
     * This method compares two elements specified with their indices.
     * It returns -1, if i-th element is less than j-th element, and 1,
     * if j-th element is less than i-th element. If both elements are
     * equal then their indices i and j are compared. This method is
     * used only by static sorter.
     */

    private int cmp(int i,int j)
    {
        if (key[i] < key[j])
            return -1;

        if (key[i] > key[j])
            return 1;

        if (i < j)
            return -1;

        if (i > j)
            return 1;

        return 0;
    }


    /**
     * This method swaps the elements by swapping their indices. Used
     * only by static sorter.
     */

    private void swapStatic(int i,int j)
    {
        int temp = index[i];
        index[i] = index[j];
        index[j] = temp;
    }


    /**
     * This method sorts the array with the insertion sort algorithm.
     * This method is used only by static sorted and therefore it works
     * with array of indices instead of input array itself.
     */

    private void insertionSortStatic()
    {
        // iterate through all elements
        for (int i = 1; i < n; i++)
        {
            // stores the index of current element
            int temp = index[i];

            // search the right spot for current element by iterating
            // backwards and pushing greater elements one place up

            int j = i;

            while (j >= 1 && cmp(index[j-1],temp) == 1)
            {
                // j-th element is greater than current so move it up
                index[j] = index[j-1];
                j--;
            }

            // we found the new home for current element so store it
            index[j] = temp;
        }
    }


//----------------------------------------------------------------------
// Section: Methods for non-static sorter
//----------------------------------------------------------------------

    /**
     * This method do the recursive sorting with the quicksort
     * algorithm for non-static sorter. The interval to be sorted is
     * specified by the paramethers.
     */
    private void recSortNonStatic(int left,int right)
    {
        // return immediately if the interval specified is too short

        if (left+10 > right)
            return;

        // choose the pivot and swap it with the last element of
        // the given interval

        double pivot = median3NonStatic(left,right);

        // iterate through the given interval from both ends
        // simultaneously

        int i = left;
        int j = right-1;

        while(true)
        {
            // increment i while i-th element is less than pivot
            do i++; while (key[i] < pivot);

            // decrement j while j-th element is greater than pivot
            do j--; while (key[j] > pivot);

            // if i and j cross then we are done, otherwise swap i-th and j-th elements
            if (i < j)
                swapNonStatic(i,j);
            else
                break;
        }

        // place the pivot in the right place
        swapNonStatic(i,right-1);

        // recursively sort both parts to the left and to the right from the pivot
        recSortNonStatic(left,i-1);
        recSortNonStatic(i+1,right);
    }


    /**
     * This method returns the middle element of left, right and center
     * element. This method is used by non-static sorter.
     */

    private double median3NonStatic(int left,int right)
    {
        // calculates the center of the given interval
        int center = (left+right)/2;

        // if left element is greater than center element, swap them

        if (key[left] > key[center])
            swapNonStatic(left,center);

        // if left element is greater than right element, swap them

        if (key[left] > key[right])
            swapNonStatic(left,right);

        // if center element is greater than right element, swap them

        if (key[center] > key[right])
            swapNonStatic(center,right);

        // now the center element is less or equal than right element
        // and greater or equal than left element

        // move the center element to the right side by swaping it with
        // the one before the right element

        swapNonStatic(center,right-1);

        // return the pivot
        return key[right-1];
    }


    /**
     * This method swaps the elements. Used only by non-static sorter.
     */

    private void swapNonStatic(int i,int j)
    {
        double temp = key[i];
        key[i] = key[j];
        key[j] = temp;
    }


    /**
     * This method sorts the array with the insertion sort algorithm.
     * This method is used only by non-static sorted.
     */

    private void insertionSortNonStatic()
    {
        // iterate through all elements

        for (int i = 1; i < n; i++)
        {
            // stores the current element
            double temp = key[i];

            // search the right spot for current element by iterating
            // backwards and pushing greater elements one place up

            int j = i;

            while (j >= 1 && key[j-1] > temp)
            {
                // j-1-th element is greater than current so move it up
                key[j] = key[j-1];
                j--;
            }

            // we found the new home for current element so store it
            key[j] = temp;
        }
    }


//----------------------------------------------------------------------
// Section: Instance variables
//----------------------------------------------------------------------

    /**
     * This variable stores the number of elements to be sorted
     */
    private int n;

    /**
     * This array stores the elements to be sorted
     */
    private double key[];

    /**
     * This array is used by static sorter and represents the current
     * order of the input array
     */
    private int index[];
}
