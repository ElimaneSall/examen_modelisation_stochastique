package examen;
import umontreal.ssj.simevents.*;
import umontreal.ssj.rng.*;
import umontreal.ssj.randvar.*;
import umontreal.ssj.probdist.*;
import umontreal.ssj.stat.Tally;
import java.io.*;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CallCenter2 {

   static final double HOUR = 3600.0;  // Time is in seconds. 

   RandomVariateGen [] genArr;
   RandomVariateGen [] genServ;
   RandomVariateGen [] genPatience;
   
   LinkedList<Call> waitList1 = new LinkedList<Call> ();
   LinkedList<Call> servList1 = new LinkedList<Call> ();
   LinkedList<Call> waitList2 = new LinkedList<Call> ();
   LinkedList<Call> servList2 = new LinkedList<Call> ();
   
   ArrayList<LinkedList<Call>> waitList = new ArrayList<LinkedList<Call>>();
   
   Tally CallWaits1     = new Tally ("Waiting times1");
   Tally CallWaits2     = new Tally ("Waiting times2");
   Accumulate totWait1  = new Accumulate ("Size of queue1");
   Accumulate totWait2  = new Accumulate ("Size of queue2");
   
   Tally [] Callwait= {CallWaits1,CallWaits2};
   
   int s=20;;
   int []  abandons= {0,0};
   int nAgent1, nAgent2;           // Number of agents in current period.
   int [] nBusys= {0,0};             // Number of agents occupied;
   int [] nArrivals= {0,0};         // Number of arrivals today;         // Number of abandonments during the day.
   int [] nGoodQoS= {0,0};          // Number of waiting times less than s today.
   double [] nCallsExpected = {0,0}; // Expected number of calls per day.
   
   Tally statArrivals = new Tally ("Number of arrivals per day");
   Tally statWaits    = new Tally ("Average waiting time per customer");
   Tally statWaitsDay = new Tally ("Waiting times within a day");
   Tally statGoodQoS  = new Tally ("Proportion of waiting times < s");
   Tally statAbandon  = new Tally ("Proportion of calls lost");


   Event [] nextArrival = {new Arrival(0),new Arrival(1)};           // The next Arrival event.

   public CallCenter2 (Double [] lambda, Double [] mu, Double [] nu) throws IOException {
	   for(int i=0; i<4;i++) {
		   genServ[i]=new ExponentialGen (new MRG32k3a(), mu[i]);
	   }
	   for(int i=0; i<2;i++) {
		   genArr[i]=new ExponentialGen (new MRG32k3a(), lambda[i]);
		   genPatience[i]=new ExponentialGen (new MRG32k3a(), nu[i]);
	   }
	  
	   waitList.set(0, waitList1);
	   waitList.set(1, waitList2);
   }


   // A phone call.
   class Call { 

      double arrivalTime, serviceTime, patienceTime;
      int type;

      public Call(int type) {
    	 this.type=type;
         serviceTime = genServ[type-1].nextDouble(); // Generate service time.
         if (nBusys[type-1]!=0) {           // Start service immediately.
            nBusys[type-1]++;
            nGoodQoS[type-1]++;
            Callwait[type-1].add (0.0);
            new CallCompletion(type-1).schedule (serviceTime);
         }
         else if(nBusys[type%2]!=0) {
        	 nBusys[type%2]++;
             nGoodQoS[type%2]++;
             Callwait[type%2].add (0.0);
             new CallCompletion(type-1).schedule (serviceTime);	 
         }
         else {                         // Join the queue.
            patienceTime = generPatience(type-1);
            arrivalTime = Sim.time();
            waitList.get(type-1).addLast (this);
         }
      }
      public void endWait(int type) {
         double wait = Sim.time() - arrivalTime;
         if (patienceTime < wait) { // Caller has abandoned.
            abandons[type-1]++;
            wait = patienceTime;    // Effective waiting time.
         }
         else {
            nBusys[type-1]++;
            new CallCompletion(type-1).schedule (serviceTime);
         }
         if (wait < s) nGoodQoS[type-1]++;
         Callwait[type-1].add (wait);
      }
   } 

   // Event: A call arrives.
   class Arrival extends Event {
	  int type ;
	  public Arrival(int type) {
		  this.type = type;
	  }
      public void actions() {
         nextArrival[type].schedule(genArr[type].nextDouble());
         nArrivals[type]++;
         new Call(type);               // Call just arrived.
      }
   }

   // Event: A call is completed.
   class CallCompletion extends Event {
	   int type;
	   public CallCompletion(int type) {
		   this.type=type;
	   }
      public void actions() { nBusys[type]--;   checkQueue(type); }
   }

   // Start answering new calls if agents are free and queue not empty.
   public void checkQueue(int type) {
      if(waitList.get(type).size() > 0) {
    	  ((Call)waitList.get(type).removeFirst()).endWait(type);
      }
      else if (waitList.get((type+1)%2).size() > 0){
    	  ((Call)waitList.get((type+1)%2).removeFirst()).endWait((type+1)%2);
      }
         
   }

   // Generates the patience time for a call.
   public double generPatience(int type) { 
      return genPatience[type].nextDouble();
   }

   public void simulateOneDay () { 
      Sim.init();       
      statWaitsDay.init();
      Sim.start();
      // Here the simulation is running...

      statArrivals.add ((double)nArrivals[1]);
      statAbandon.add ((double)abandons[1]);
      statGoodQoS.add ((double)nGoodQoS[1]);
   }



   static public void main (String[] args) throws IOException { 
	  Double [] lambda= {6.0,0.6};
	  Double [] mu= {0.20,0.18,0.15,0.14};
	  Double [] nu= {0.12,0.24};
	  
      CallCenter2 cc = new CallCenter2 (lambda, mu, nu); 
      for (int i=1; i <= 1000; i++)  cc.simulateOneDay();
      System.out.println (
         cc.statArrivals.reportAndCIStudent (0.9) +
         cc.statGoodQoS.reportAndCIStudent (0.9) +
         cc.statAbandon.reportAndCIStudent (0.9)); 
   }
}
