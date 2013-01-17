package battlecode.communication;
// =================================================================
//   Communicator class used to transmit information between robots 
//  and HQ using a spread-spectrum frequency hopping technique.  
//  All robots should keep an instance of this class as a private 
//  member called 'comms'.
//
//   Setup:
//  In the RobotPlayer constructor, include the line,
//        comms = new Communicator(rc);
//
//   Usage:
//  To post a message, use the command,
//        comms.sendMessage(msg);
//   You don't have to worry about which channel, just include the
//  message. Messages must be positive and are limited to 15 bits in
//  length, but it is up to you to ensure that you abide by this 
//  limit.
//
//	  If you have a specific recipient in mind, you can send a 
//	 message to their channel using the command,
//			 comms.sendMessageTo(recipient, msg);
//  remember that doing so will overwrite any previous message sent
//  to that robot.
//
//  To receive a message, use the command,
//        msg = comms.getMessage();
//   This returns the positive integer that was last broadcast on 
//  your private channel or -1 if the last message did not exist
//  or was corrupted.  Please check the sign of the returned value 
//  before you do anything with it.
//
//   Alternatively, you can get the last message broadcast in another
//  robot's private channel using the command,
//			 msg = comms.eavesdropOn(target);
//  This is the preferred mechanism for HQ to get information from a
//  robot.
// =================================================================

import battlecode.common.Clock;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;

public class Communicator{
	private static final int teamID    = 27356;
	private static final int[] hzHoppingPrimes   = {373, 691, 1373, 2659, 5107, 9833, 19597, 38723, 65287};
	private static final int[] hzHoppingRoots    = { 34,  98,   88,   75,  123,   62,   140,    60,   132};
	private static final int[] hzHoppingInverses = { 11, 275, 1295,  390, 4069,  793,  6579,  8390,  2473};
	private static       RobotController rc;
	
	private int assignedChannel = 0;
	private int hzHoppingModulus = 0;
	private int previousTimeCheck = 0;
	private int hzHoppingStep = 0;
	private int hzHoppingBackStep = 0;
	
	public Communicator(RobotController myrc){
	    rc = myrc;
	    
		for(int i=1;hzHoppingPrimes[i]<GameConstants.BROADCAST_MAX_CHANNELS;i++){
			hzHoppingModulus  = hzHoppingPrimes[i];
			hzHoppingStep     = hzHoppingRoots[i];
			hzHoppingBackStep = hzHoppingInverses[i];
		}
		
		assignedChannel = teamID;
		updateInternalClock();
	}

	private void updateInternalClock(){
		int steps = Clock.getRoundNum() - previousTimeCheck;
		
		if(steps==1){
		    assignedChannel = (assignedChannel * hzHoppingStep) % hzHoppingModulus;
		}
		else if(steps>1){
		    int mask = 0x1000;
		    int jump = hzHoppingStep;
		    
		    while(mask>steps)
		        mask = mask>>>1;
		        
		    steps = steps % mask;
		    mask = mask>>>1;
		    while(mask>1)
		    {
		        jump = (jump * jump) % hzHoppingModulus;
		        if(steps>mask)
		            jump = (jump * hzHoppingStep) % hzHoppingModulus;
		            
		        steps = steps % mask;
		        mask = mask>>>1;
		    }
		    
		    assignedChannel = (assignedChannel * jump) % hzHoppingModulus;
		 }
		 
		 previousTimeCheck = Clock.getRoundNum(); 
	}

	public int eavesdropOn(int robotID) throws GameActionException{
	    updateInternalClock();
	    int thisChannel = (assignedChannel*(robotID+1)) % hzHoppingModulus;
	    int message = rc.readBroadcast(thisChannel);
	    if(Coder.validate(message))
	        return Coder.decode(message);
	    else{
	    	message = rc.readBroadcast((thisChannel * hzHoppingBackStep) % hzHoppingModulus);
	    	if(Coder.validate(message))
	    		return Coder.decode(message);
	    	else
	    		return -1; 
	    }
	}

	public int getMessage() throws GameActionException{
		return eavesdropOn(rc.getRobot().getID());
	}
	
	public void sendMessageTo(int robotID, int message) throws GameActionException{
		updateInternalClock();
		rc.broadcast((assignedChannel*(robotID+1)) % hzHoppingModulus,Coder.encode(message));
	}
	
	public void sendMessage(int message) throws GameActionException{
		sendMessageTo(rc.getRobot().getID(),message);
	}
}

