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
   LinkedList<Call> waitList2 = new LinkedList<Call> ();
   
   LinkedList<Agents> nBusyAgents1 = new LinkedList<Agents> ();
   LinkedList<Agents> nBusyAgents2 = new LinkedList<Agents> ();
   
   LinkedList<Agents> nFreeAgents1 = new LinkedList<Agents> ();
   LinkedList<Agents> nFreeAgents2 = new LinkedList<Agents> ();
   
   ArrayList<LinkedList<Agents>> nBusyAgent = new ArrayList<LinkedList<Agents>>();
   ArrayList<LinkedList<Agents>> nFreeAgent = new ArrayList<LinkedList<Agents>>();
   
    Double [][] AgentsOccupations ;

   
   ArrayList<LinkedList<Call>> waitList = new ArrayList<LinkedList<Call>>();
   
   Tally CallWaits1     = new Tally ("Waiting times1");
   Tally CallWaits2     = new Tally ("Waiting times2");
   Accumulate totWait1  = new Accumulate ("Size of queue1");
   Accumulate totWait2  = new Accumulate ("Size of queue2");
   
   Tally [] Callwait= {CallWaits1,CallWaits2};
   
   Double s=20.0/60.0;
   int []  abandons= {0,0};
   int nAgents1=31;
   int nAgents2=6;
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
	   
	   waitList.add(0, waitList1);
	   waitList.add(1, waitList2);
	   
	   nBusyAgent.add(0,nBusyAgents1);
	   nBusyAgent.add(1,nBusyAgents2);
	   
	   nFreeAgent.add(0,nFreeAgents1);
	   nFreeAgent.add(1,nFreeAgents2);
	   
	   for(int i=0; i<2;i++) {
		   genArr.add(i,new ExponentialGen (new MRG32k3a(), lambda[i]));
		   genPatience.add(i,new ExponentialGen (new MRG32k3a(), nu[i]));
	   }
	   
	   for(int i=0; i<4;i++) {
		   genServ.add(i,new ExponentialGen(new MRG32k3a(), mu[i]));
	   }
	   
	   for(int i=0; i<nAgents1;i++) {
		   nFreeAgent.get(0).addLast(new Agents(0,i));
		   //AgentsOccupations[0]= new Double [nAgents1];
	   }
	   
	   for(int i=0; i<nAgents2;i++) {
		   nFreeAgent.get(1).addLast(new Agents(1,i));
		   //AgentsOccupations[1]= new Double [nAgents2];
		   
	   }
	   
	   
   }


   // A phone call.
   class Call { 

      double arrivalTime, serviceTime, patienceTime;
      int type;

      public Call(int type) {
    	 this.type=type;
         if (nBusyAgent.get(type-1).size() < nAgents[type-1]) {           // Start service immediately.
        	Agents agent = nFreeAgent.get(type-1).removeFirst();
        	serviceTime = genServ.get(servTimeGen(type-1,agent.type)).nextDouble(); // Generate service time.
        	nBusyAgent.get(agent.type).addLast(agent);
        	
            //nBusys[type-1]++;
            Callwait[type-1].add (0.0);
            new CallCompletion(type-1,agent).schedule (serviceTime);
         }
         else if(nBusyAgent.get(type%2).size() < nAgents[type%2]) {
        	Agents agent = nFreeAgent.get(type%2).removeFirst();
         	serviceTime = genServ.get(servTimeGen(type%2,agent.type)).nextDouble(); // Generate service time.
        	nBusyAgent.get(agent.type).addLast(agent);
        	 
        	 //nBusys[type%2]++;
             Callwait[type%2].add (0.0);
             new CallCompletion(type-1,agent).schedule (serviceTime);	 
         }
         else {                         // Join the queue.
            patienceTime = generPatience(type-1);
            arrivalTime = Sim.time();
            waitList.get(type-1).addLast (this);
         }
      }
      
      public void endWait(Agents Agent) {
         double wait = Sim.time() - arrivalTime;
         if (patienceTime < wait) { // Caller has abandoned.
            abandons[type-1]++;
            wait = patienceTime;    // Effective waiting time.
         }
         else {
         	//serviceTime = genServ.get(servTimeGen(type-1,Agent.type)).nextDouble(); // Generate service time.
            nBusyAgent.get(Agent.type).addLast(Agent);
            new CallCompletion(type-1,Agent).schedule (serviceTime);
         }
         if (wait < s) nGoodQoS[type-1]++;
         Callwait[type-1].add (wait);
      }
      
     int servTimeGen(int callType,int agentType){
    	 if(callType==agentType) return (int)callType; 
    	 else if(callType < agentType) return 2;
    	 else return 3;
     }
   } 

   class Agents{
	   int type;
	   int number;
	   String name;
	   public Agents(int type,int number) {
		   this.type= type;
		   this.number=number;
		   this.name = "agent"+type;
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
	   Agents agent;
	   public CallCompletion(int type, Agents agent) {
		   this.type=type;
		   this.agent =agent;
	   }
      public void actions() {
    	  nBusyAgent.get(agent.type).remove(agent);
    	  nFreeAgent.get(agent.type).add(agent);  
    	  checkQueue(agent.type); 
    	 
    	  }
   }

   // Start answering new calls if agents are free and queue not empty.
   public void checkQueue(int type) {
      if(waitList.get(type).size() > 0 && nBusyAgent.get(type).size() < nAgents[type]) {
    	  Agents agent = nBusyAgent.get(type).removeFirst();
    	  ((Call)waitList.get(type).removeFirst()).endWait(agent);
      }
      else if (waitList.get((type+1)%2).size() > 0 && nBusyAgent.get((type+1)%2).size() < nAgents[(type+1)%2]){
    	  Agents agent = nBusyAgent.get((type+1)%2).removeFirst();
    	  ((Call)waitList.get((type+1)%2).removeFirst()).endWait(agent);
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
