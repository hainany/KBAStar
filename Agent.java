package kbastar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
//import tools.pathfinder.PathFinder;
import tools.Vector2d;
//import tools.pathfinder.Node;
//import tools.pathfinder.PathFinder;

/**
 * Created with IntelliJ IDEA.
 * User: ssamot
 * Date: 14/11/13
 * Time: 21:45
 * This is an implementation of MCTS UCT
 */
public class Agent extends AbstractPlayer {
//    protected PathFinder pathf;

    public int num_actions;
    public Types.ACTIONS[] actions;

    protected SingleMCTSPlayer mctsPlayer;
    public static PathFinder pathf;
    
    /** The minimum knowledge-based evaluation found so far in the entire game */
    public static double MIN_KB_EVAL = -2.0;//These two are for normalization in function evalKB of SingleTreeNode
    /** The maximum knowledge-based evaluation found so far in the entire game */
    public static double MAX_KB_EVAL = 2.0;

    /**
     * Public constructor with state observation and time due.
     * @param so state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer)
    {
    	
    	
        ArrayList<Integer> list = new ArrayList<>(0);
        list.add(0); //wall
        pathf = new PathFinder(list);
        pathf.run(so);
        HashMap<Integer, ArrayList<Node>> pathCache_agent = pathf.getpathCache();//A public variable transit to SingleTreeNode
        
        
        //Get the actions in a static array.
        ArrayList<Types.ACTIONS> act = so.getAvailableActions();
        actions = new Types.ACTIONS[act.size()];
        for(int i = 0; i < actions.length; ++i)
        {
            actions[i] = act.get(i);
        }
        num_actions = actions.length;

        //Create the player.

        mctsPlayer = getPlayer(so, elapsedTimer);
    }

    public SingleMCTSPlayer getPlayer(StateObservation so, ElapsedCpuTimer elapsedTimer) {
        return new SingleMCTSPlayer(new Random(), num_actions, actions);
    }


    /**
     * Picks an action. This function is called every game step to request an
     * action from the player.
     * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {

        //Set the state observation object as the new root of the tree.
        mctsPlayer.init(stateObs);

        //Determine the action using MCTS...
        int action = mctsPlayer.run(elapsedTimer);

        //... and return it.
        return actions[action];
    }

}





