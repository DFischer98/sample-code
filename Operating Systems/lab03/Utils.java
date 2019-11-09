//simple class to represent resource type, tracking available and used amounts
class Resource{
    public int total, available, used, toBeAvailable;

    public Resource(int resourceCount){
        this.available = resourceCount;
        this.total = resourceCount;
    }

    //returns true upon valid resource request
    public boolean request(int amount){
        if (this.available >= amount){
            this.available -= amount;
            this.used += amount;
            return true;
        }
        return false;
    }

    //release resources (marking them unusable until further processing [AKA next cycle])
    public void release(int amount){
        this.toBeAvailable += amount;
    }

    //release resources, making them available immediately (for deadlock purposes)
    public void hardRelease(int amount){
        this.available += amount;
        this.used -= amount;
    }

    //
    public void assimilateResources(){
        this.available += this.toBeAvailable;
        this.used -= this.toBeAvailable;
        this.toBeAvailable = 0;
    }
}

//container for activity data
class Activity{
    public String activityType;
    public int delay, resourceType, amount;

    public Activity(String type, int delay, int resourceType, int amount){
        this.activityType = type;
        this.delay = delay;
        this.resourceType = resourceType;
        this.amount = amount;
    }
}
