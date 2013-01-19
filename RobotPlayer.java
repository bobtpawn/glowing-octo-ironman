package team077;
/**************************************************
  This is a really dumb player that researches the
  pickaxe, then maintains a minimum population
  while researching the nuke.  Soldiers move
  randomly, lay mines, and build artillery.  It
  works surprisingly well.
**************************************************/

import battlecode.common.*;
import battlecode.communication.*;
import java.util.Random;
import java.util.Arrays;
import java.lang.Math;

public class RobotPlayer{
	
	private static RobotController rc;
	private static Random numberGenerator;
	private static final	Direction[] dirs = {Direction.valueOf("NORTH"),
														  Direction.valueOf("SOUTH"),
														  Direction.valueOf("EAST"),
														  Direction.valueOf("WEST"),
														  Direction.valueOf("EAST"),
														  Direction.valueOf("NORTH_EAST"),
														  Direction.valueOf("SOUTH_EAST"),
														  Direction.valueOf("NORTH_WEST"),
														  Direction.valueOf("SOUTH_WEST")};
			
	public static void run(RobotController myRC){
		rc = myRC;
		numberGenerator = new Random(rc.getRobot().getID());
		// The robot's type will never change, so check it once, then do the appropriate thing.	
		try{
			if (rc.getType()==RobotType.SOLDIER){
				soldierCode();
			}else if(rc.getType()==RobotType.ARTILLERY){
				artilleryCode();
			}else if(rc.getType()==RobotType.SUPPLIER){
				supplierCode();
			}else{
				hqCode();
			}
		}catch (Exception e){
			System.out.println("caught exception before it killed us:");
			e.printStackTrace();
		}
	}
	
	private static void soldierCode() throws GameActionException{
		// Put something here to see if we are one of the first 5.
		if(Clock.getRoundNum()<2)
			earlySoldier(4);
		else if(Clock.getRoundNum()<1*GameConstants.HQ_SPAWN_DELAY+2)
			earlySoldier(3);
		else if(Clock.getRoundNum()<2*GameConstants.HQ_SPAWN_DELAY+2)
			earlySoldier(2);
		else if(Clock.getRoundNum()<3*GameConstants.HQ_SPAWN_DELAY+2)
			earlySoldier(1);
		else if(Clock.getRoundNum()<4*GameConstants.HQ_SPAWN_DELAY+2)
			earlySoldier(0);
			
		while(true){
			// If an enemy is next to us...
			if(rc.senseNearbyGameObjects(Robot.class, 2, rc.getTeam().opponent()).length>0){
				; // Do nothing
			// Or if an enemy is one space over, ATTACK!
			}else if(rc.senseNearbyGameObjects(Robot.class, 8, rc.getTeam().opponent()).length>0){
				attack();
			// Otherwise, if we can build an encampment...
			}else if(rc.senseEncampmentSquare(rc.getLocation())){
				makeEncampment();
			// Otherwise, lay mines.
			}else{
				if(rc.hasUpgrade(Upgrade.PICKAXE)){
					if(rc.senseMineLocations(rc.getLocation(), 1, null).length>2)
						moveRandomly();
					else
						rc.layMine();
				}else{
					if(rc.senseMine(rc.getLocation()) != null)
						moveRandomly();
					else
						rc.layMine();
				}
			}
			hold();
		}	
	}

	private static void earlySoldier(int n) throws GameActionException{
		MapLocation[] encampments = rc.senseAllEncampmentSquares();
		MapLocation   home        = rc.senseHQLocation();
		int[] indices   = new int[n];
		int[] distances = new int[n];
		
		Arrays.fill(distances, 100000);
		
		// Capture the n+1st closest encampment
		for(int i=0;i<encampments.length;++i){
			int thisIndex    = i;
			int thisDistance = home.distanceSquaredTo(encampments[i]);
			for(int j=0;j<n;++j){
				if(thisDistance<distances[j]){
					int tempIndex = thisIndex;
					int tempDistance = thisDistance;
					thisIndex = indices[j];
					thisDistance = distances[j];
					indices[j] = tempIndex;
					distances[j] = tempDistance;
				}
			}
		}
		
		MapLocation objective = encampments[indices[n-1]];
				
		while(!rc.getLocation().equals(objective)){
			Direction dir = rc.getLocation().directionTo(objective);
			if(rc.senseMine(rc.getLocation().add(dir))==Team.NEUTRAL){
				rc.defuseMine(rc.getLocation().add(dir));
				hold();
			}
			while(!rc.canMove(dir))
				dir = dir.rotateRight();
				
			rc.move(dir);
			rc.yield();
		}
		
		makeEncampment();
	}
	
	private static void attack() throws GameActionException{
		Direction dir = rc.getLocation().directionTo(rc.senseRobotInfo(rc.senseNearbyGameObjects(Robot.class, 8, rc.getTeam().opponent())[0]).location);
		while(!rc.canMove(dir) || rc.senseMine(rc.getLocation().add(dir))==rc.getTeam().opponent() || rc.senseMine(rc.getLocation().add(dir))==Team.NEUTRAL)
			dir = dir.rotateLeft();
		rc.move(dir);
	}
	
	private static void makeEncampment() throws GameActionException{
		MapLocation home = rc.senseHQLocation();
		MapLocation away = rc.senseEnemyHQLocation();
		
		while(rc.senseCaptureCost()>rc.getTeamPower()){
			hold();
		}
		
		// Check whether we are inside an ellipse with foci at the two HQs
		if(Math.sqrt(rc.getLocation().distanceSquaredTo(home))+Math.sqrt(rc.getLocation().distanceSquaredTo(away))>
				Math.sqrt(home.distanceSquaredTo(away))+5){
			// Outside
			rc.captureEncampment(RobotType.SUPPLIER);
			hold();
		}else{
			// Inside
			rc.captureEncampment(RobotType.ARTILLERY);
			hold();
		}
	}

	private static void moveRandomly() throws GameActionException{
		Direction dir;
		int coinflip = numberGenerator.nextInt(6000);	// Determines left/right and towards enemy HQ or not
		
		if(coinflip > 1000)
			dir = pickRandomDirection();
		else
			if(Clock.getRoundNum()<1500)	// In the end game, stay close to home and let them come through the mines
				dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
			else
				dir = rc.getLocation().directionTo(rc.senseHQLocation());
			
		for(int i=1;i<8;++i){
			if(rc.canMove(dir)){
				MapLocation destination = rc.getLocation().add(dir);
				if(rc.senseMine(destination)==rc.getTeam() || rc.senseMine(destination)==null){
					rc.move(dir);	
					break;
				}
			}
			if(coinflip % 2 == 0)
				dir = dir.rotateLeft();
			else
				dir = dir.rotateRight();
		}
		hold();
	}
	
	private static Direction pickRandomDirection(){
		return dirs[numberGenerator.nextInt(dirs.length)];
	}
	
	private static void hold() throws GameActionException{
		// Useful utility function that just waits until the robot can do something again
		while(!rc.isActive())
			rc.yield();
	}
	
	private static void artilleryCode() throws GameActionException{
		// Shoot at the robot with highest health
		while(true){
			Robot[] targets = rc.senseNearbyGameObjects(Robot.class, rc.getType().attackRadiusMaxSquared, rc.getTeam().opponent());
			if(targets.length>0){
				double highestHealth = rc.senseRobotInfo(targets[0]).energon;
				int    target = 0;
				for(int i=1;i<targets.length;++i){
					double health = rc.senseRobotInfo(targets[i]).energon;
					if(health>highestHealth){
						highestHealth = health;
						target = i;
					}
				}
				rc.attackSquare(rc.senseLocationOf(targets[target]));
			}
			hold();
		}
	}
	
	private static void supplierCode() throws GameActionException{
		while(true)
			rc.yield();
	}
	
	private static void hqCode() throws GameActionException{
		// Spawn 5 robots
		for(int i=1;i<5;++i){
			for (Direction c : Direction.values()){
				if(rc.canMove(c)){
					rc.spawn(c);
					break;
				}
			}
			hold();
		}
		
		// Research pickaxe
		while(!rc.hasUpgrade(Upgrade.PICKAXE)){
			rc.researchUpgrade(Upgrade.PICKAXE);
			hold();
		}
		
		// Generate more robots whenever there are fewer than 20.
		// Research vision if we have at least 3 artillery.
		// Otherwise, research nuke.
		while(true){
			if(rc.senseNearbyGameObjects(Robot.class, 1000000, rc.getTeam()).length<20 &&
				rc.senseNearbyGameObjects(Robot.class, 2      , rc.getTeam()).length<5){
				for (Direction c : Direction.values()){
					if(rc.canMove(c)){
						rc.spawn(c);
						break;
					}
				}
			}else if(rc.senseAlliedEncampmentSquares().length>1 && !rc.hasUpgrade(Upgrade.VISION)){
				rc.researchUpgrade(Upgrade.VISION);
			}else{
				rc.setIndicatorString(1,String.valueOf(rc.senseAlliedEncampmentSquares().length));
				rc.researchUpgrade(Upgrade.NUKE);
			}
			hold();
		}
	}	
}
