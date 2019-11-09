import java.util.LinkedList;
import java.util.Queue;



public class Task {
    private LinkedList<Activity> activities = new LinkedList<>();
    public Activity currentActivity;
    public int taskNumber, cycles, blocked;
    public int[] usedResources, claims;
    public boolean aborted = false;
    private boolean verbose;

    public Task(int numResourceTypes, boolean verbose){
        this.usedResources = new int[numResourceTypes];
        this.claims = new int[numResourceTypes];
        this.cycles = 0;
        this.blocked = 0;
        this.verbose = verbose;

    }

    public Activity nextActivity(){
        return activities.peek();
    }

    public void addActivity(String type, int delay, int resourceType, int amount){
        activities.add(new Activity(type, delay, resourceType, amount));
        //add to claims if type is initiate
        if (type.equals("initiate")){
            claims[resourceType - 1] = amount;
        }
    }

    /* return result of executing current activity
     * 0: unable to grant requested resources
     * 1: successful grant/release and next cycle is requesting
     * 2: delayed next cycle
     * 3: terminated
     */

    public int executeActivity(Resource[] resourceTypes, boolean usingBanker){
        boolean result;
        //fetch next activity
        currentActivity = activities.poll();
        cycles++;


        if(currentActivity.activityType.equals("initiate")) {
            if (verbose) {
                System.out.printf("\tTask %d is initiated with resource type %d.\n", taskNumber, currentActivity.resourceType);
            }
            return 1;
        }

        else if(currentActivity.activityType.equals("request")){
            if (currentActivity.delay > 0){
                currentActivity.delay--;
                if (verbose) {
                    System.out.printf("\tTask %d is delayed this cycle, %d cycles remaining.\n", taskNumber, currentActivity.delay);
                }
                activities.addFirst(currentActivity);
                return 2;
            }
            else{
                //ERROR CHECK: request during banker's algorithm exceeds claim
                if (usingBanker && usedResources[currentActivity.resourceType - 1] + currentActivity.amount > claims[currentActivity.resourceType - 1]){
                    if (verbose) {
                        System.out.printf("\tTask %d's request has exceeded its claim.", taskNumber);
                    }
                    this.abort(resourceTypes);
                    return 3;
                }

                result = resourceTypes[currentActivity.resourceType - 1].request(currentActivity.amount);
                if (result) {
                    usedResources[currentActivity.resourceType - 1] += currentActivity.amount;
                    if (verbose) {
                        System.out.printf("\tTask %d was granted %d resources of type %d.\n", taskNumber, currentActivity.amount, currentActivity.resourceType);
                    }
                    return 1;
                }
                else{
                    activities.addFirst(currentActivity);
                    if (verbose) {
                        System.out.printf("\tTask %d was unable to be granted %d resources of type %d.\n", taskNumber, currentActivity.amount, currentActivity.resourceType);
                    }
                    blocked++;
                    return 0;
                }
            }
        }
        else if (currentActivity.activityType.equals("release")){
            //check for delay
            if (currentActivity.delay > 0){
                currentActivity.delay--;
                if (verbose) {
                    System.out.printf("\tTask %d is delayed this cycle, %d cycles remaining.\n", taskNumber, currentActivity.delay);
                }
                activities.addFirst(currentActivity);
                return 2;
            }
            else {
                resourceTypes[currentActivity.resourceType - 1].release(currentActivity.amount);
                usedResources[currentActivity.resourceType - 1] -= currentActivity.amount;
                if (verbose) {
                    System.out.printf("\tTask %d has released %d resources of type %d.\n", taskNumber, currentActivity.amount, currentActivity.resourceType);
                }
                //check for successive, 0-delay terminate and execute
                if (activities.peek().activityType.equals("terminate") && activities.peek().delay == 0) {
                    if (verbose) {
                        System.out.printf("\tTask %d has terminated.\n", taskNumber);
                    }
                    return 3;
                }
                //indicate successful release
                else
                    return 1;
            }
        }

        else if (currentActivity.activityType.equals("terminate")){
            if (verbose) {
                System.out.printf("\tTask %d is delayed this cycle, %d cycles remaining.\n", taskNumber, currentActivity.delay - 1);
            }
            currentActivity.delay--;
            activities.addFirst(currentActivity);
            if (currentActivity.delay == 0){
                if (verbose) {
                    System.out.printf("\tTask %d has terminated.\n", taskNumber);
                }
                return 3;
            }
            return 2;

        }
        //should never get to here
        return 0;
    }

    //go through all resources held by this task and release them
    public void abort(Resource[] resourceTypes){
        for (int i = 0; i < usedResources.length; i++){
            if (usedResources[i] != 0){
                resourceTypes[i].hardRelease(usedResources[i]);
            }
        }
        aborted = true;
        if (verbose) {
            System.out.printf("\tTask %d has been aborted and its resources released.\n", taskNumber);
        }
    }

    //method used by deadlock resolution by returning whether the pending request can no be fulfilled, without executing it
    public boolean testRequest(Resource[] resourceTypes){
        Activity currentRequest = activities.peek();
        if (currentRequest.activityType.equals("request") && resourceTypes[currentRequest.resourceType -1].available >= currentRequest.amount)
            return true;
        else
            return false;
    }
}
