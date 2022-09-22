package examen;
import umontreal.ssj.simevents.*;
import umontreal.ssj.rng.*;
import umontreal.ssj.randvar.*;
import umontreal.ssj.probdist.*;
import umontreal.ssj.stat.Tally;
import java.io.*;
import java.util.StringTokenizer;

import examen.QueueEv.Arrival;
import examen.QueueEv.EndOfSim;
import examen.QueueEv.initAccumulate;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CallCenter2 {

   static final double HOUR = 3600.0;  // Time is in seconds. 

   ArrayList<RandomVariateGen> genArr = new ArrayList<RandomVariateGen>();
   ArrayList<RandomVariateGen> genServ = new ArrayList<RandomVariateGen>();
   ArrayList<RandomVariateGen> genPatience = new ArrayList<RandomVariateGen>();
   
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
   
   Double s=20.0/60.0;
   int []  abandons= {0,0};
   int []nAgents= {31,6};           // Number of agents in current period.
   int [] nBusys= {0,0};             // Number of agents occupied;
   int [] nArrivals= {0,0};         // Number of arrivals today;         // Number of abandonments during the day.
   int [] nGoodQoS= {0,0};          // Number of waiting times less than s today.
   double [] nCallsExpected = {0,0}; // Expected number of calls per day.
   
   Tally statArrivals0 = new Tally ("Number of arrivals per day");
   Tally statWaits    = new Tally ("Average waiting time per customer");
   Tally statWaitsDay = new Tally ("Waiting times within a day");
   Tally statGoodQoS0  = new Tally ("Proportion of waiting times < s");
   Tally statAbandon0  = new Tally ("Proportion of calls lost");
   
   Tally statArrivals1 = new Tally ("Number of arrivals per day");
   Tally statGoodQoS1  = new Tally ("Proportion of waiting times < s");
   Tally statAbandon1  = new Tally ("Proportion of calls lost");


   //Event [] nextArrival = {new Arrival(0),new Arrival(1)};           // The next Arrival event.

   public CallCenter2 (Double [] lambda, Double [] mu, Double [] nu) throws IOException {
	   
	   for(int i=0; i<2;i++) {
		   genArr.add(i,new ExponentialGen (new MRG32k3a(), lambda[i]));
		   genPatience.add(i,new ExponentialGen (new MRG32k3a(), nu[i]));
	   }
	   
	   for(int i=0; i<4;i++) {
		   genServ.add(i,new ExponentialGen(new MRG32k3a(), mu[i]));
	   }
	   
	   waitList.add(0, waitList1);
	   waitList.add(1, waitList2);
   }


   // A phone call.
   class Call { 

      double arrivalTime, serviceTime, patienceTime;
      int type;

      public Call(int type) {
    	 this.type=type;
         serviceTime = genServ.get(type-1).nextDouble(); // Generate service time.
         if (nBusys[type-1]< nAgents[type-1]) {           // Start service immediately.
            nBusys[type-1]++;
            Callwait[type-1].add (0.0);
            new CallCompletion(type-1).schedule (serviceTime);
         }
         else if(nBusys[type%2]< nAgents[type%2]) {
        	 nBusys[type%2]++;
             Callwait[type%2].add (0.0);
             new CallCompletion(type-1).schedule (serviceTime);	 
         }
         else {                         // Join the queue.
            patienceTime = generPatience(type-1);
            arrivalTime = Sim.time();
            waitList.get(type-1).addLast (this);
         }
      }
      
      public void endWait() {
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
         new Arrival(type).schedule(genArr.get(type).nextDouble());
         nArrivals[type]++;
         new Call(type+1);               // Call just arrived.
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
      if(waitList.get(type).size() > 0 && nBusys[type] < nAgents[type]) {
    	  ((Call)waitList.get(type).removeFirst()).endWait();
      }
      else if (waitList.get((type+1)%2).size() > 0 && nBusys[(type+1)%2] < nAgents[(type+1)%2]){
    	  ((Call)waitList.get((type+1)%2).removeFirst()).endWait();
      }
         
   }
   
   class EndOfSim extends Event {
	      public void actions() {
	         Sim.stop();
	      }
	}
   // Generates the patience time for a call.
   public double generPatience(int type) { 
      return genPatience.get(type).nextDouble();
   }

   public void simulateOneDay (double timeHorizon) { 
      Sim.init();       
      statWaitsDay.init();
      new EndOfSim().schedule (timeHorizon);
      new Arrival(0).schedule (genArr.get(0).nextDouble());
      new Arrival(1).schedule (genArr.get(1).nextDouble());
      Sim.start();
      // Here the simulation is running...

      statArrivals0.add ((double)nArrivals[0]);
      statAbandon0.add ((double)abandons[0]);
      statGoodQoS0.add ((double)nGoodQoS[0]);
      
      statArrivals1.add ((double)nArrivals[1]);
      statAbandon1.add ((double)abandons[1]);
      statGoodQoS1.add ((double)nGoodQoS[1]);
   }



   static public void main (String[] args) throws IOException { 
	  Double [] lambda= {6.0,0.6};
	  Double [] mu= {0.20,0.18,0.15,0.14};
	  Double [] nu= {0.12,0.24};
	  
      CallCenter2 cc = new CallCenter2 (lambda, mu, nu); 
      for (int i=1; i <= 1000; i++)  cc.simulateOneDay(560);
      System.out.println (
         cc.statArrivals0.reportAndCIStudent (0.9) +
         cc.statGoodQoS0.reportAndCIStudent (0.9) +
         cc.statAbandon0.reportAndCIStudent (0.9)); 
      
      System.out.println("=========================================================================");
      System.out.println(cc.nArrivals[0]);
      
      System.out.println (
    	         cc.statArrivals1.reportAndCIStudent (0.9) +
    	         cc.statGoodQoS1.reportAndCIStudent (0.9) +
    	         cc.statAbandon1.reportAndCIStudent (0.9)); 
   }
}
