import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;



public class Scheduler {
    private static ArrayList<Process> FcfsPocesses = new ArrayList<>();
    private static ArrayList<Process> RRProcesses = new ArrayList<>();
    private static ArrayList<Process> uniProcesses = new ArrayList<>();
    private static ArrayList<Process> PsjfProcesses = new ArrayList<>();


    private static boolean verbose = false;
    private static File programInput;
    private static Scanner fileIn;

    public static void main(String[] args) throws FileNotFoundException{
        Scheduler scheduler = new Scheduler();
        //handle args
        if (args[0].equals("--verbose")){
            scheduler.verbose = true;
            programInput = new File(args[1]);
        }
        else{
            programInput = new File(args[0]);
        }

        //load in processes from file
        fileIn = new Scanner(programInput);
        int inputs = fileIn.nextInt();
        int a, b, c, IO;
        System.out.print("The original input was: ");
        for (int i = 1; i <= inputs; i++){
            a = fileIn.nextInt();
            b = fileIn.nextInt();
            c = fileIn.nextInt();
            IO = fileIn.nextInt();

            FcfsPocesses.add(new Process(a, b, c, IO));
            RRProcesses.add(new Process(a, b, c, IO));
            uniProcesses.add(new Process(a, b, c, IO));
            PsjfProcesses.add(new Process(a, b, c, IO));

            System.out.printf("%d %d %d %d  ", a, b, c, IO);
        }

        //sort processes
        FcfsPocesses = Utils.sortProcesses(FcfsPocesses);
        RRProcesses = Utils.sortProcesses(RRProcesses);
        uniProcesses = Utils.sortProcesses(uniProcesses);
        PsjfProcesses = Utils.sortProcesses(PsjfProcesses);

        //print sorted processes
        System.out.print("\nThe (sorted) input is:  ");
        for(int i = 0; i < inputs; i++){
            System.out.printf("%d %d %d %d  ", FcfsPocesses.get(i).a, FcfsPocesses.get(i).b, FcfsPocesses.get(i).c, FcfsPocesses.get(i).IO);
        }

        scheduler.runFCFS(FcfsPocesses);
        scheduler.runRR(RRProcesses);
        scheduler.runUni(uniProcesses);
        scheduler.runPSJF(PsjfProcesses);
    }

    private void runFCFS(ArrayList<Process> processes) throws FileNotFoundException{
        Utils rng = new Utils();
        boolean lastCycle = false;
        boolean currentCpuBurst = false;
        boolean firstBlock;
        int currentCycle = 0;
        int noCpuCycle = 0;
        int IOCycles = 0;
        String[] states;
        Queue<Process> readyProcesses = new LinkedList<>();

        if (verbose){
            System.out.println("\n\nDetailed printout of each process:\n");
        }

        //run cycles
        while(!lastCycle){
            lastCycle = true;
            firstBlock = true;

            //print statuses
            if (verbose) {
                System.out.printf("%nBefore cycle %4d: ", currentCycle);
                for (Process process : processes) {
                    System.out.printf("%11s%3d", process.state, process.remainingBurst);
                }
                System.out.print(".");
            }

            //iterate through processes and evaluate each
            for(int i = 0; i < processes.size(); i++) {
                states = processes.get(i).evaluate(currentCycle, rng);
                //gather processes that are ready to be run next cycle
                if (states[1].equals("ready") && !readyProcesses.contains(processes.get(i))) {
                    readyProcesses.add(processes.get(i));
                }
                //check for process currently in a CPU burst
                if (processes.get(i).state.equals("running")){
                    currentCpuBurst = true;
                }

                //check if all processes are terminated
                if (!states[1].equals("terminated")) {
                    lastCycle = false;
                }

                //update count of cycles with IO
                if (states[0].equals("blocked") && firstBlock){
                    IOCycles++;
                    firstBlock = false;
                }
            }

            //set first ready cycle to be run, if it exists and no process is currently running
            if(readyProcesses.peek() != null && !currentCpuBurst) {
                for(int i = 0; i < readyProcesses.size(); i++){
                    if (i == 0){
                        readyProcesses.poll().startCpuBurst(rng);
                    }
                }
            }
            //count cycles with no CPU activity
            else if (!currentCpuBurst && !lastCycle){
                noCpuCycle++;
            }
            currentCpuBurst = false;

            if(!lastCycle)
                currentCycle++;
        }
        System.out.println("\nThe scheduling algorithm used was First Come First Served\n");
        float[] averages = Utils.printProcesses(processes);

        //print summary
        System.out.println("Summary Data:");
        System.out.printf("\t\tFinishing Time:%15.6f\n", (float)currentCycle);
        System.out.printf("\t\tCPU Utilization:%14.6f\n", (float)(currentCycle-noCpuCycle)/currentCycle);
        System.out.printf("\t\tI/O Utilization:%14.6f\n", (float)IOCycles/currentCycle);
        System.out.printf("\t\tThroughput:%19.6f per hundred cycles\n", (processes.size()/(currentCycle/100.0)));
        System.out.printf("\t\tAverage turnaround:%11.6f\n", averages[0]);
        System.out.printf("\t\tAverage Wait:%17.6f\n", averages[1]);
    }
    //END FCFDS scheduling

    private void runRR(ArrayList<Process> processes) throws FileNotFoundException{
        Utils rng = new Utils();
        boolean lastCycle = false;
        boolean currentCpuBurst = false;
        boolean firstBlock;
        int currentCycle = 0;
        int noCpuCycle = 0;
        int IOCycles = 0;
        int quantum = 2;
        String[] states;
        Queue<Process> readyProcesses = new LinkedList<>();

        if (verbose){
            System.out.println("\n\nDetailed printout of each process:\n");
        }

        //run cycles
        while(!lastCycle){
            lastCycle = true;
            firstBlock = true;

            //print statuses
            if (verbose) {
                System.out.printf("%nBefore cycle %4d: ", currentCycle);
                for (Process process : processes) {
                    //make sure remaining burst time for running processes is capped by quantum
                    System.out.printf("%11s%3d", process.state, (process.state.equals("running") && process.remainingBurst > 1) ? quantum - process.elapsedBurst : ((process.state.equals("blocked") || process.state.equals("running")) ? process.remainingBurst : 0));
                }
                System.out.print(".");
            }

            //iterate through processes and evaluate each
            for(int i = 0; i < processes.size(); i++) {
                states = processes.get(i).evaluate(currentCycle, quantum, rng);
                //gather processes that are ready to be run next cycle
                if (states[1].equals("ready") && !readyProcesses.contains(processes.get(i))) {
                    readyProcesses.add(processes.get(i));
                }
                //check for process currently in a CPU burst
                if (processes.get(i).state.equals("running")){
                    currentCpuBurst = true;
                }

                //check if all processes are terminated
                if (!states[1].equals("terminated")) {
                    lastCycle = false;
                }

                //update count of cycles with IO
                if (states[0].equals("blocked") && firstBlock){
                    IOCycles++;
                    firstBlock = false;
                }
            }

            //set first ready cycle to be run, if it exists and no process is currently running
            if(readyProcesses.peek() != null && !currentCpuBurst) {
                for(int i = 0; i < readyProcesses.size(); i++){
                    if (i == 0){
                        readyProcesses.poll().startCpuBurstRR(rng);
                    }
                }
            }
            //count cycles with no CPU activity
            else if (!currentCpuBurst && !lastCycle){
                noCpuCycle++;
            }
            currentCpuBurst = false;

            if(!lastCycle)
                currentCycle++;
        }
        System.out.println("\nThe scheduling algorithm used was Round Robin\n");
        float[] averages = Utils.printProcesses(processes);

        //print summary
        System.out.println("Summary Data:");
        System.out.printf("\t\tFinishing Time:%15.6f\n", (float)currentCycle);
        System.out.printf("\t\tCPU Utilization:%14.6f\n", (float)(currentCycle-noCpuCycle)/currentCycle);
        System.out.printf("\t\tI/O Utilization:%14.6f\n", (float)IOCycles/currentCycle);
        System.out.printf("\t\tThroughput:%19.6f per hundred cycles\n", (processes.size()/(currentCycle/100.0)));
        System.out.printf("\t\tAverage turnaround:%11.6f\n", averages[0]);
        System.out.printf("\t\tAverage Wait:%17.6f\n", averages[1]);
    }
    //END RR scheduling

    public void runUni(ArrayList<Process> processes) throws FileNotFoundException{
        Utils rng = new Utils();
        boolean lastCycle = false;
        boolean currentCpuBurst = false;
        int currentCycle = 0;
        int CpuCycles = 0;
        int IOCycles = 0;
        String[] states;
        Process nextProcess = null;
        if (verbose){
            System.out.println("\n\nDetailed printout of each process:\n");
        }

        //run cycles
        while(!lastCycle){
            lastCycle = true;

            //print statuses
            if (verbose) {
                System.out.printf("%nBefore cycle %4d: ", currentCycle);
                for (Process process : processes) {
                    System.out.printf("%11s%3d", process.state, process.remainingBurst);
                }
                System.out.print(".");
            }

            //iterate through processes and evaluate each
            for(int i = 0; i < processes.size(); i++) {
                states = processes.get(i).evaluate(currentCycle, rng);

                //check for process currently in a CPU or IO burst
                if (states[0].equals("running") || states[0].equals("blocked")){
                    //ensure currently running processes keep running
                    if (states[1].equals("running") || states[1].equals("blocked")){
                        nextProcess = processes.get(i);
                    }
                    if (states[0].equals("running")){
                        CpuCycles++;
                    }
                }
                //assign ready process to next if not one currently running
                if (states[1].equals("ready") && nextProcess == null) {
                    nextProcess = processes.get(i);
                }

                //check if all processes are terminated
                if (!states[1].equals("terminated")) {
                    lastCycle = false;
                }

                //update count of cycles with IO
                if (states[0].equals("blocked")){
                    IOCycles++;
                }
            }

            //set next running process to designated
            if (nextProcess != null && nextProcess.state.equals("ready")){
                nextProcess.startCpuBurst(rng);
            }

            nextProcess = null;

            if(!lastCycle)
                currentCycle++;
        }
        System.out.println("\nThe scheduling algorithm used was Uniprocessor\n");
        float[] averages = Utils.printProcesses(processes);

        //print summary
        System.out.println("Summary Data:");
        System.out.printf("\t\tFinishing Time:%15.6f\n", (float)currentCycle);
        System.out.printf("\t\tCPU Utilization:%14.6f\n", (float)CpuCycles/currentCycle);
        System.out.printf("\t\tI/O Utilization:%14.6f\n", (float)IOCycles/currentCycle);
        System.out.printf("\t\tThroughput:%19.6f per hundred cycles\n", (processes.size()/(currentCycle/100.0)));
        System.out.printf("\t\tAverage turnaround:%11.6f\n", averages[0]);
        System.out.printf("\t\tAverage Wait:%17.6f\n", averages[1]);
    }
    //END Uniprogrammed scheduling

    private void runPSJF(ArrayList<Process> processes) throws FileNotFoundException{
        Utils rng = new Utils();
        boolean lastCycle = false;
        boolean firstBlock;
        int currentCycle = 0;
        int CpuCycles = 0;
        int IOCycles = 0;
        String[] states;
        ArrayList<Process> readyProcesses = new ArrayList<>();
        Process currentProcess;

        if (verbose){
            System.out.println("\n\nDetailed printout of each process:\n");
        }

        //run cycles
        while(!lastCycle){
            currentProcess = null;
            lastCycle = true;
            firstBlock = true;

            //print statuses
            if (verbose) {
                System.out.printf("%nBefore cycle %4d: ", currentCycle);
                for (Process process : processes) {
                    System.out.printf("%11s%3d", process.state, process.remainingBurst);
                }
                System.out.print(".");
            }

            //iterate through processes and evaluate each
            for(int i = 0; i < processes.size(); i++) {
                states = processes.get(i).evaluate(currentCycle, rng);
                //enqueue processes in ready-list
                if(states[1].equals("ready") || states[1].equals("running")){
                    readyProcesses.add(processes.get(i));
                }
                //count cpu cycles
                if(states[0].equals("running")){
                    if(states[1].equals("running")){
                        currentProcess = processes.get(i);
                    }
                    CpuCycles++;
                }

                //check if all processes are terminated
                if (!states[1].equals("terminated")) {
                    lastCycle = false;
                }

                //update count of cycles with IO
                if (states[0].equals("blocked") && firstBlock){
                    IOCycles++;
                    firstBlock = false;
                }
            }

            //run process with lowest remaining time
            if (!readyProcesses.isEmpty()){
                Process chosen = Utils.shortestJob(readyProcesses);
                //chosen process different than current
                if (currentProcess != null && chosen != currentProcess){
                    currentProcess.state = "ready";
                    if(chosen.remainingBurst == 0){
                        chosen.startCpuBurst(rng);
                    }
                    else{
                        chosen.state = "running";
                    }
                }
                //current process doesn't exist for some reason
                else{
                    if(chosen.remainingBurst == 0){
                        chosen.startCpuBurst(rng);
                    }
                    else{
                        chosen.state = "running";
                    }
                }
            }
            readyProcesses.clear();

            if(!lastCycle)
                currentCycle++;
        }
        System.out.println("\nThe scheduling algorithm used was Preemptive Shortest Job First\n");
        float[] averages = Utils.printProcesses(processes);

        //print summary
        System.out.println("Summary Data:");
        System.out.printf("\t\tFinishing Time:%15.6f\n", (float)currentCycle);
        System.out.printf("\t\tCPU Utilization:%14.6f\n", (float)CpuCycles/currentCycle);
        System.out.printf("\t\tI/O Utilization:%14.6f\n", (float)IOCycles/currentCycle);
        System.out.printf("\t\tThroughput:%19.6f per hundred cycles\n", (processes.size()/(currentCycle/100.0)));
        System.out.printf("\t\tAverage turnaround:%11.6f\n", averages[0]);
        System.out.printf("\t\tAverage Wait:%17.6f\n", averages[1]);
    }

}


