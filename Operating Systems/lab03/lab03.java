/*
 * Daniel Fischer
 * DAF452
 * OS202 Lab 03
 */

import java.util.*;
import java.io.File;



public class lab03 {
    private static File fileIn;
    private static Scanner fileScan;

    //Array to contain resource types and tasks from input
    private static Resource[] resourceTypes;

    //LinkedLists to contain task queues
    private static Task[] masterTaskList;
    private static LinkedList<Task> aliveTaskList = new LinkedList<>();
    private static LinkedList<Task> pendingTasks = new LinkedList<>();
    private static LinkedList<Task> blockedTasksToAdd = new LinkedList<>();
    private static LinkedList<Task> otherTasksToAdd = new LinkedList<>();


    public static void main(String[] args) throws java.io.FileNotFoundException{
        fileIn = new File(args[0]);
        fileScan = new Scanner(fileIn);

        boolean verbose = false;
        if (args.length > 1 && args[1].equals("--verbose"))
            verbose = true;

        //initialize array for T tasks
        masterTaskList = new Task[fileScan.nextInt()];


        //initialize R resource types
        int resourceNum = fileScan.nextInt();
        resourceTypes = new Resource[resourceNum];
        for (int i = 0; i < resourceTypes.length; i++){
            resourceTypes[i] = new Resource(fileScan.nextInt());
        }

        //initialize task objects with value of R
        for (int i = 0; i < masterTaskList.length; i++){
            masterTaskList[i] = new Task(resourceNum, verbose);
        }

        //read in activities
        String activity;
        Task currentTask;
        int taskNumber;
        while(fileScan.hasNext()){
            activity = fileScan.next();
            taskNumber = fileScan.nextInt();
            currentTask = masterTaskList[taskNumber - 1];
            currentTask.addActivity(activity, fileScan.nextInt(), fileScan.nextInt(), fileScan.nextInt());
            if(activity.equals("initiate") && !pendingTasks.contains(currentTask)){
                currentTask.taskNumber = taskNumber;
                pendingTasks.add(currentTask);
                aliveTaskList.add(currentTask);
            }
        }

        int cycleCounter = 0;
        int totalCycles = 0;
        int totalBlocked = 0;
        int result;
        boolean cycling = true;
        boolean deadlocked;
        Task toAbort;

        //run program cycles in FIFO optimistic allocation
        if (verbose)
            System.out.println("FIFO allocation:");

        while(cycling){
            deadlocked = true;
            if (verbose)
                System.out.printf("\nDuring %d-%d:\n", cycleCounter, cycleCounter + 1);
            cycleCounter++;

            //iterate through tasks and execute
            while(!pendingTasks.isEmpty()){
                currentTask = pendingTasks.poll();
                result = currentTask.executeActivity(resourceTypes, false);
                if (result != 0){
                    deadlocked = false;
                }

                //preserve FIFO priority for blocked tasks
                if (result == 0){
                    blockedTasksToAdd.add(currentTask);
                }
                //add successful tasks to back of FIFO queue
                else if (result == 1 || result == 2){
                    otherTasksToAdd.add(currentTask);
                }
                //remove task from master list if terminated
                else if (result == 3){
                    aliveTaskList.remove(currentTask);
                }

            }

            //add pending tasks in correct order (blocked first) to queue for next cycle
            pendingTasks.clear();
            pendingTasks.addAll(blockedTasksToAdd);
            pendingTasks.addAll(otherTasksToAdd);
            blockedTasksToAdd.clear();
            otherTasksToAdd.clear();

            //handle deadlocked states by aborting tasks
            while (deadlocked){
                //abort first task and check if following tasks can run
                toAbort = aliveTaskList.poll();
                pendingTasks.remove(toAbort);
                toAbort.abort(resourceTypes);
                for (Task blockedTask : pendingTasks){
                    if (blockedTask.testRequest(resourceTypes)){
                        deadlocked = false;
                    }
                }
            }


            //detect all terminated tasks
            if (pendingTasks.size() == 0) {
                System.out.println("pending = 0");
                cycling = false;
            }

            //assimilate released resources for availability next cycle
            for (Resource resource : resourceTypes){
                resource.assimilateResources();
            }

            //print out resource information
            if (verbose) {
                for (int i = 0; i < resourceTypes.length; i++) {
                    System.out.printf("%d resources belonging to resource type %d are available for cycle %d.\n", resourceTypes[i].available, i + 1, cycleCounter);
                }
            }
        }

        //output stats
        System.out.printf("\n                Cycles      Blocked     Waiting\n");
        for (Task task : masterTaskList){
            if (!task.aborted){
                System.out.printf("Task %d %15d %12d %10.2f%%\n", task.taskNumber, task.cycles, task.blocked, ((float) task.blocked/task.cycles)*100);
                totalCycles += task.cycles;
                totalBlocked += task.blocked;
            }
            else
                System.out.printf("Task %d ABORTED.      -            -           -\n", task.taskNumber);

        }
        System.out.printf("Total %16d %12d %10.2f%%\n\n", totalCycles, totalBlocked, ((float) totalBlocked/totalCycles)*100);

        //BANKERS ALGORITHM
        System.out.println("Banker's Algorithm (error messages, if any, go below):");
        fileScan.close();
        fileScan = new Scanner(fileIn);

        //initialize array for T tasks
        masterTaskList = new Task[fileScan.nextInt()];


        //initialize R resource types
        resourceNum = fileScan.nextInt();
        resourceTypes = new Resource[resourceNum];
        for (int i = 0; i < resourceTypes.length; i++){
            resourceTypes[i] = new Resource(fileScan.nextInt());
        }

        //initialize task objects with value of R
        for (int i = 0; i < masterTaskList.length; i++){
            masterTaskList[i] = new Task(resourceNum, verbose);
        }

        //read in activities
        int delay, resourceType, resourceAmount;
        while(fileScan.hasNext()){
            //collect variables for each activity
            activity = fileScan.next();
            taskNumber = fileScan.nextInt();
            delay = fileScan.nextInt();
            resourceType = fileScan.nextInt();
            resourceAmount = fileScan.nextInt();
            currentTask = masterTaskList[taskNumber - 1];

            //ERROR CHECK: Claim exceeds available resources
            if (activity.equals("initiate") && resourceAmount > resourceTypes[resourceType-1].available){
                System.out.printf("\tTask %d's claim for resource %d exceeds available amount.\n", taskNumber, resourceType);
                currentTask.aborted = true;
                currentTask.taskNumber = taskNumber;
                continue;
            }
            //Add activity to task
            currentTask.addActivity(activity, delay, resourceType, resourceAmount);
            //add task to lists
            if(activity.equals("initiate") && !pendingTasks.contains(currentTask)){
                currentTask.taskNumber = taskNumber;
                pendingTasks.add(currentTask);
                aliveTaskList.add(currentTask);
            }
        }

        cycleCounter = 0;
        cycling = true;

        //run program cycles with Banker's Algorithm
        while(cycling){
            if (verbose)
                System.out.printf("\nDuring %d-%d:\n", cycleCounter, cycleCounter + 1);
            cycleCounter++;

            //iterate through tasks and execute
            while(!pendingTasks.isEmpty()){
                currentTask = pendingTasks.poll();

                //block activities in unsafe states

                //check for safe states given every resource type
                boolean safeState = true;
                for (int i = 1; i <= resourceTypes.length; i++){
                    safeState &= checkSafetyAfterRequest(aliveTaskList, currentTask, resourceTypes, i);
                }
                //if unsafe, block task
                if (currentTask.nextActivity().activityType.equals("request")
                        && currentTask.nextActivity().delay == 0
                        && !safeState){
                    if (verbose)
                        System.out.printf("\tTask %d blocked due to unsafe state.\n", currentTask.taskNumber);
                    currentTask.blocked++;
                    currentTask.cycles++;
                    blockedTasksToAdd.add(currentTask);
                }
                else{
                    result = currentTask.executeActivity(resourceTypes, true);

                    //preserve FIFO priority for blocked tasks
                    if (result == 0){
                        blockedTasksToAdd.add(currentTask);
                    }
                    //add successful tasks to back of FIFO queue
                    else if (result == 1 || result == 2){
                        otherTasksToAdd.add(currentTask);
                    }
                    //remove task from master list if terminated
                    else if (result == 3){
                        aliveTaskList.remove(currentTask);
                    }
                }
            }

            //add pending tasks in correct order (blocked first) to queue for next cycle
            pendingTasks.clear();
            pendingTasks.addAll(blockedTasksToAdd);
            pendingTasks.addAll(otherTasksToAdd);
            blockedTasksToAdd.clear();
            otherTasksToAdd.clear();

            //detect all terminated tasks
            if (pendingTasks.size() == 0)
                cycling = false;

            //assimilate released resources for availability next cycle
            for (Resource resource : resourceTypes){
                resource.assimilateResources();
            }

            //print out resource information
            if (verbose) {
                for (int i = 0; i < resourceTypes.length; i++) {
                    System.out.printf("%d resources belonging to resource type %d are available for cycle %d.\n", resourceTypes[i].available, i + 1, cycleCounter);
                }
            }
        }

        totalCycles = 0;
        totalBlocked = 0;

        //output stats
        System.out.printf("\n                Cycles      Blocked     Waiting\n");
        for (Task task : masterTaskList){
            if (!task.aborted){
                System.out.printf("Task %d %15d %12d %10.2f%%\n", task.taskNumber, task.cycles, task.blocked, ((float) task.blocked/task.cycles)*100);
                totalCycles += task.cycles;
                totalBlocked += task.blocked;
            }
            else
                System.out.printf("Task %d ABORTED.      -            -           -\n", task.taskNumber);
        }
        System.out.printf("Total %16d %12d %10.2f%%\n", totalCycles, totalBlocked, ((float) totalBlocked/totalCycles)*100);

    }

    //check for safety given rules of banker's algorithm
    private static boolean checkSafetyAfterRequest(LinkedList<Task> tasks, Task currentTask, Resource[] resourceTypes, int resourceType){
        //get resource type and amount we are checking for safety with
        Activity nextActivity = currentTask.nextActivity();
        //shallow copy tasks so we can remove them via bankers algorithm
        LinkedList<Task> modifiableTasks = (LinkedList) tasks.clone();
        int resourcesAvailable = resourceTypes[resourceType - 1].available;

        //look for task P which can be satisfied
        for (Task task : tasks){
            //such P found
            if (task.equals(currentTask) && (task.claims[resourceType - 1] - task.usedResources[resourceType - 1]) <= resourcesAvailable
                    || task.claims[resourceType - 1] <= resourcesAvailable){
                modifiableTasks.remove(task);
                resourcesAvailable += task.usedResources[resourceType - 1];
                return checkSafetyRecursive(modifiableTasks, currentTask, resourceType, resourcesAvailable);
            }
        }
        //no tasks where claim <= available found, state not safe
        return false;
    }

    //helper function for recursively determining state
    //simulates aborting tasks with claims <= available and then checking again if claims <= available
    private static boolean checkSafetyRecursive(LinkedList<Task> modifiableTasks, Task currentTask, int resourceType, int resourcesAvailable){
        if (modifiableTasks.isEmpty())
            return true;
        for (Task task : modifiableTasks){
            if (task.equals(currentTask) && (task.claims[resourceType - 1] - task.usedResources[resourceType - 1]) <= resourcesAvailable
                    || task.claims[resourceType - 1] <= resourcesAvailable){
                modifiableTasks.remove(task);
                resourcesAvailable += task.usedResources[resourceType - 1];
                return checkSafetyRecursive(modifiableTasks, currentTask, resourceType, resourcesAvailable);
            }
        }
        //no tasks where claim <= available found, state not safe
        return false;
    }

}


