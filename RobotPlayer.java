package simpleplayer;

import battlecode.common.*;
import battlecode.communication.*;
import java.util.Random;

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
		numberGenerator = new Random();
			
		try{
			if (rc.getType()==RobotType.SOLDIER){
				soldierCode();
			}else{
				hqCode();
			}
		}catch (Exception e){
			System.out.println("caught exception before it killed us:");
			e.printStackTrace();
		}
	}

	private static void soldierCode() throws GameActionException{
		while(true){
			if(rc.hasUpgrade(Upgrade.PICKAXE)){
				if(rc.senseMineLocations(rc.getLocation(), 1, null).length>3)
					moveRandomly();
				else
					rc.layMine();
			}else{
				if(rc.senseMine(rc.getLocation()) != null)
					moveRandomly();
				else
					rc.layMine();
			}
			hold();
		}	
	}

	private static void moveRandomly() throws GameActionException{
		Direction dir;
		for(int i=1;i<8;++i){
			dir = pickRandomDirection();
			if(rc.canMove(dir)){
				MapLocation destination = rc.getLocation().add(dir);
				if(rc.senseMine(destination)==rc.getTeam() || rc.senseMine(destination)==null){
					rc.move(dir);	
					break;
				}
			}
			dir = dir.rotateLeft();
		}
		hold();
	}
	
	private static Direction pickRandomDirection(){
		return dirs[numberGenerator.nextInt(dirs.length)];
	}
	
	private static void hold() throws GameActionException{
		while(!rc.isActive())
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
		
		// Generate more robots/research nuke
		while(true){
			if(rc.senseNearbyGameObjects(Robot.class, 1000000, rc.getTeam()).length<20 &&
				rc.senseNearbyGameObjects(Robot.class, 2      , rc.getTeam()).length<5){
				for (Direction c : Direction.values()){
					if(rc.canMove(c)){
						rc.spawn(c);
						break;
					}
				}
			}else{
				rc.researchUpgrade(Upgrade.NUKE);
			}
			hold();
		}
	}
	
}
