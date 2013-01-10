// =================================================================
//   Communicator class used to transmit information between robots 
//  and HQ using a spread-spectum frequency hopping technique.  Each
//  robot should keep an instance of this class as a private member
//  called 'comms'.
//
//   Setup:
//  In the RobotPlayer constructor, include the line,
//        comms = new communicator(rc);
//
//   Usage:
//  To send a message, use the command,
//        comms.sendMessage(...);
//   You don't have to worry about which channel, just include the
//  message.  Messages must be positive and are limited to 15 bits 
//  in length, but it is up to you to ensure that you abide by this
//  limit.
//
//  To receive a message, use the command,
//        msg = comms.getMessage();
//   This returns the positive integer that was last broadcast on 
//  this robot's channel or -1 if the last message did not exist or
//  was corrupted.  Please check the sign of the returned value 
//  before you do anything with it.
// =================================================================

import battlecode.common.Clock;
import battlecode.common.GameConstants;

public class communicator{
	private static final int teamID    = 27356;
	private static final int[] hzHoppingPrimes = {373, 691, 1373, 2659, 5107, 9833, 19597};
	private static final int[] hzHoppingRoots  = { 34,  98,   88,   75,  123,   62,   140};
    private static       RobotController rc;
	private int assignedChannel = 0;
	private int hzHoppingModulus = 0;
	private int previousTimeCheck = 0;
	
	public communicator(RobotController myrc){
	    rc = myrc;
	    
		for(int i=1;hzHoppingPrimes[i]<GameConstants.BROADCAST_MAX_CHANNELS;i++){
			hzHoppingModulus = hzHoppingPrimes[i];
			hzHoppingStep    = hzHoppingRoots[i];
		}
		hzHoppingDoubleStep = (int)(((long)hzHoppingStep)^2 % hzHoppingModulus);
			
		assignedChannel = (rc.getRobot().getID()+1)*teamID;
		incrementInternalClock();
	}

	private void incrementInternalClock(){
		steps = Clock.getRoundNum() - previousTimeCheck;
		
	    if(steps==1){
		    assignedChannel = (assignedChannel * hzHoppingStep) % hzHoppingModulus;
		}
		else if(steps>1){
		    int mask = 0x1000;
		    int jump = hzHoppingStep;
		    
		    while(mask>steps)
		        mask>>>1;
		        
		    steps = steps % mask;
		    mask>>>1;
		    while(mask>1)
		    {
		        jump = (jump * jump) % hzHoppingModulus;
		        if(steps>mask)
		            jump = (jump * hzHoppingStep) % hzHoppingModulus;
		            
		        steps = steps % mask;
		        mask>>>1;
		    }
		    
		    assignedChannel = (assignedChannel * jump) % hzHoppingModulus;
		 }
		 
		 previousTimeCheck += steps; 
	}

	public int getMessage(){
	    updateInternalClock();
	    message = rc.readBroadcast(assignedChannel);
	    if(coder.validate(message))
	        return coder.decode(message);
	    else
	        return -1;  
	}

	public void sendMessage(int message){
	    updateInternalClock();
		rc.broadcast(assignedChannel,coder.encode(message));
	}
}

