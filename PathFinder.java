package kbastar;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by dperez on 14/01/16.
 */

//This file is partly from the original GVGAI codes of tool.PathFinder 
//and modified to be applied in this controller.
public class PathFinder {

    public AStar astar;
    public StateObservation state;

    public boolean VERBOSE = false;//false

    //All types are obstacles except the ones included in this array
    public ArrayList<Integer> obstacleItypes;

    public ArrayList<Observation> grid[][];


    private static int[] x_arrNeig = null;
    private static int[] y_arrNeig = null;

    public PathFinder(ArrayList<Integer> obstacleItypes)
    {
        this.obstacleItypes = obstacleItypes;
    }

    public void run(StateObservation stateObs)
    {
        this.state = stateObs;
        this.grid = stateObs.getObservationGrid();
        this.astar = new AStar(this);

        init();
        runAll();

        if(VERBOSE)
        {
            for(Integer pathId : astar.pathCache.keySet())
            {
            	//System.out.println("??????"+astar.pathCache.keySet().size());
                ArrayList<Node> nodes = astar.pathCache.get(pathId);
                astar.printPath(pathId, nodes);
            }
        }
    }
    
    ////////for inint run
    
    
    
    public HashMap<Integer, ArrayList<Node>> getpathCache() { ///////
    	return astar.pathCache;
    }

    private void init()
    {
        if(x_arrNeig == null)
        {
            //TODO: This is a bit of a hack, it wouldn't work with other (new) action sets.
            ArrayList<Types.ACTIONS> actions = this.state.getAvailableActions();
            if(actions.size() == 3)
            {
                //left, right
                x_arrNeig = new int[]{-1, 1};
                y_arrNeig = new int[]{0,  0};
            }else
            {
                //up, down, left, right
                x_arrNeig = new int[]{0,    0,    -1,    1};
                y_arrNeig = new int[]{-1,   1,     0,    0};
            }
        }
    }

    private void runAll()
    {
        for(int i = 0; i < grid.length; ++i)
        {
            for(int j = 0; j < grid[i].length; ++j)
            {
                boolean obstacleCell = isObstacle(i,j);
                //if(!obstacleCell)//Calculate every path no matter one material is obstacle or not.
                //{
                    if(VERBOSE) System.out.println("Running from (" + i +  "," + j + ")");
                    runAll(i,j);
                //}

            }
        }
    }

    public ArrayList<Node> getPath(Vector2d start, Vector2d end)
    {
        return astar.getPath(new Node(start), new Node(end));
    }

    private void runAll(int i, int j) {
        Node start = new Node(new Vector2d(i,j));
        Node goal = null; //To get all routes.

        astar.findPath(start, goal);
    }
    
    //This is a new function constructed by one existing function( runAll() ) and the codes from original GVGAI toolkit tool.PathFinder
    public ArrayList<Node> runAllAStar(StateObservation stateObs,Vector2d start, Vector2d goal ) { 
    	this.state = stateObs;
        this.grid = stateObs.getObservationGrid();
        this.astar = new AStar(this);
        init();
        astar.getPath(new Node(start), new Node(goal));
        return astar.findPath(new Node(start), new Node(goal));
    }



    private boolean isObstacle(int row, int col)
    {
        if(row<0 || row>=grid.length) return true;
        if(col<0 || col>=grid[row].length) return true;

        for(Observation obs : grid[row][col])
        {
        	//System.out.println(obs.position+" "+obs.itype+" "+grid[row][col]);
            if(obstacleItypes.contains(obs.itype))
                return true;
        }

        return false;

    }

    public ArrayList<Node> getNeighbours(Node node) {
        ArrayList<Node> neighbours = new ArrayList<Node>();
        int x = (int) (node.position.x);
        int y = (int) (node.position.y);

        for(int i = 0; i < x_arrNeig.length; ++i)
        {
            if(!isObstacle(x+x_arrNeig[i], y+y_arrNeig[i]))
            {
                neighbours.add(new Node(new Vector2d(x+x_arrNeig[i], y+y_arrNeig[i])));
            }
        }

        return neighbours;
    }


}
