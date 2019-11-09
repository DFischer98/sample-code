public class Process {
    int a, b, c, IO, remainingBurst, remainingCpuTime, elapsedBurst;
    String state = "unstarted";
    int finishingTime = 0;
    int turnaroundTime = 0;
    int IOTime = 0;
    int waitingTime = 0;


    public Process(int a, int b, int c, int IO){
        this.a = a;
        this.b = b;
        this.c = c;
        this.IO = IO;
        remainingCpuTime = c;
    }

    //returns string of two outputs: current cycle state and next cycle state
    public String[] evaluate(int time, Utils rng){
        String output[] = new String[2];

        //unstarted but now ready
        if (this.state.equals("unstarted") && this.a <= time){
            output[0] = this.state;
            this.state = "ready";
            output[1] = this.state;
        }

        //unstarted and not ready
        else if (this.state.equals("unstarted")){
            output[0] = this.state;
            output[1] = this.state;
        }

        //ready
        else if (this.state.equals("ready")){
            output[0] = this.state;
            output[1] = this.state;
            waitingTime++;
        }

        //running
        else if (this.state.equals("running")){
            output[0] = this.state;
            remainingCpuTime--;
            remainingBurst--;

            //process terminates
            if(remainingCpuTime == 0){
                remainingBurst = 0;
                this.state = "terminated";
                output[1] = this.state;

                finishingTime = time;
                turnaroundTime = finishingTime - this.a;
            }
            //process blocked
            else if (remainingBurst == 0){
                this.state = "blocked";
                remainingBurst = rng.randomOS(IO);
                output[1] = this.state;
            }
            //still running
            else{
                output[1] = this.state;
            }
        }

        //blocked
        else if (this.state.equals("blocked")){
            output[0] = this.state;
            remainingBurst--;
            IOTime++;
            //unblocked
            if(remainingBurst == 0){
                this.state = "ready";
                output[1] = this.state;
            }
            //still blocked
            else{
                output[1] = this.state;
            }
        }

        //terminated
        else if (this.state == "terminated"){
            output[0] = "terminated";
            output[1] = "terminated";
        }

        return output;
    }

    //Round Robin process evauluation
    //returns string of two outputs: current cycle state and next cycle state
    public String[] evaluate(int time, int quantum, Utils rng){
        String output[] = new String[2];

        //unstarted but now ready
        if (this.state.equals("unstarted") && this.a <= time){
            output[0] = this.state;
            this.state = "ready";
            output[1] = this.state;
        }

        //unstarted and not ready
        else if (this.state.equals("unstarted")){
            output[0] = this.state;
            output[1] = this.state;
        }

        //ready
        else if (this.state.equals("ready")){
            output[0] = this.state;
            output[1] = this.state;
            waitingTime++;
        }

        //running
        else if (this.state.equals("running")){
            output[0] = this.state;
            remainingCpuTime--;
            remainingBurst--;
            elapsedBurst++;

            //process terminates
            if(remainingCpuTime == 0){
                remainingBurst = 0;
                this.state = "terminated";
                output[1] = this.state;

                finishingTime = time;
                turnaroundTime = finishingTime - this.a;
            }
            //process blocked
            else if (remainingBurst == 0){
                this.state = "blocked";
                remainingBurst = rng.randomOS(IO);
                output[1] = this.state;
            }
            //preempted by RR
            else if (elapsedBurst == quantum){
                this.state = "ready";
                output[1] = this.state;
            }

            //still running
            else{
                output[1] = this.state;
            }
        }

        //blocked
        else if (this.state.equals("blocked")){
            output[0] = this.state;
            remainingBurst--;
            IOTime++;
            //unblocked
            if(remainingBurst == 0){
                this.state = "ready";
                output[1] = this.state;
            }
            //still blocked
            else{
                output[1] = this.state;
            }
        }

        //terminated
        else if (this.state == "terminated"){
            output[0] = "terminated";
            output[1] = "terminated";
        }

        return output;
    }

    public void startCpuBurst(Utils rng){
        this.state = "running";
        this.remainingBurst = rng.randomOS(this.b);
        this.elapsedBurst = 0;
    }

    public void startCpuBurstRR(Utils rng){
        this.state = "running";
        if (this.remainingBurst == 0){
            this.remainingBurst = rng.randomOS(this.b);
        }
        this.elapsedBurst = 0;
    }

}
