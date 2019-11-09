import java.util.ArrayList;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;



public class Utils {
    public static ArrayList<Process> sortProcesses(ArrayList<Process> list){
        boolean noSwaps = false;
        if(list.size() < 2){
            return list;
        }
        Process a, b;

        while(!noSwaps){
            noSwaps = true;
            for(int i = 0; i < list.size()-1; i++){
                a = list.get(i);
                b = list.get(i+1);

                if (a.a > b.a){
                    list.set(i, b);
                    list.set(i+1, a);
                    noSwaps = false;
                }

            }
        }
        return list;
    }

    //prints process stats and returns average turnaround and wait times
    public static float[] printProcesses(ArrayList<Process> list){
        Process process;
        float[] output = new float[2];
        for (int i = 0; i < list.size(); i++){
            process = list.get(i);
            System.out.printf("Process%2d:%n", i);
            System.out.printf("\t\t(A,B,C,IO) = (%d,%d,%d,%d)%n", process.a, process.b, process.c, process.IO);
            System.out.printf("\t\tFinishing Time:%6d%n", process.finishingTime);
            System.out.printf("\t\tTurnaround Time:%5d%n", process.turnaroundTime);
            System.out.printf("\t\tIO Time:%13d%n", process.IOTime);
            System.out.printf("\t\tWaiting Time:%8d%n%n", process.waitingTime);

            output[0] += process.turnaroundTime;
            output[1] += process.waitingTime;
        }

        output[0] /= list.size();
        output[1] /= list.size();

        return output;
    }

    //select shortest job for PSJF
    public static Process shortestJob(ArrayList<Process> list){
        int indexOfShortest = 0;
        int min = Integer.MAX_VALUE;
        for(int i = 0; i < list.size(); i++){
            if(list.get(i).remainingCpuTime < min){
                indexOfShortest = i;
                min = list.get(i).remainingCpuTime;
            }
        }
        return list.remove(indexOfShortest);
    }



    //use instance of Utils object for RNG
    private Scanner fileIn;

    public Utils() throws FileNotFoundException{
        this.fileIn = new Scanner(new File ("random-numbers"));
    }

    public int randomOS(int u){
        return 1 + (fileIn.nextInt() % u);
    }
}
