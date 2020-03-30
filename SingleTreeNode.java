package kbastar;

//import pavlos_97.Agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import core.game.Event;
import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Utils;
import tools.Vector2d;
//import tools.pathfinder.AStar;
//import tools.pathfinder.Node;
//import tools.pathfinder.PathFinder;
import tracks.singlePlayer.tools.Heuristics.SimpleStateHeuristic;
import tracks.singlePlayer.tools.Heuristics.StateHeuristic;
import tracks.singlePlayer.tools.Heuristics.WinScoreHeuristic;

public class SingleTreeNode
{
    private final double HUGE_NEGATIVE = -10000000.0;
    private final double HUGE_POSITIVE =  10000000.0;
    public double epsilon = 1e-6;
//    public double egreedyEpsilon = 0.05;
    public SingleTreeNode parent;
    public SingleTreeNode[] children;
    public double totValue;
    public int nVisits;
    public Random m_rnd;
    public int m_depth;
    protected double[] bounds = new double[]{Double.MAX_VALUE, -Double.MAX_VALUE};
    public int childIdx;
    public double scoreLength;
    
    public int num_actions;
    Types.ACTIONS[] actions;
    public int ROLLOUT_DEPTH = 40;
    public double K = Math.sqrt(2);
    public double total_rollouts = 0;
    public int hit_times = 0;
    
    public StateObservation rootState;
    public StateObservation Root;
    public StateObservation parentState;
    
    public StateHeuristic heuristic;
    //protected PathFinder pathf;
    public double sqScore;
    public int i = 0;
    
    
    //hold observations when collisions happen
    ArrayList<Observation> lNPC = new ArrayList<Observation>();
    ArrayList<Observation> lMov = new ArrayList<Observation>();
    ArrayList<Observation> lRes = new ArrayList<Observation>();
	dSums deltaSums = new dSums();
//    double rdelta;
    //--------------------------------------------------
//    //transposition table
//	public HashMap<StateObservation, Double> h = 
//            new HashMap<StateObservation, Double>();
//    //--------------------------------------------------
    public ArrayList<StateObservation> soList;
	
	
	
    public SingleTreeNode(Random rnd, int num_actions, Types.ACTIONS[] actions, StateObservation state, StateObservation pstate) {
        this(null, -1, rnd, num_actions, actions, state, pstate);
    }

    public SingleTreeNode(SingleTreeNode parent, int childIdx, Random rnd, int num_actions, Types.ACTIONS[] actions, StateObservation state, StateObservation pstate) {
    	this.Root = state.copy();
    	this.parentState = pstate;
    	
        this.parent = parent;
        this.m_rnd = rnd;
        this.num_actions = num_actions;
        this.actions = actions;
//        this.h = h;
        children = new SingleTreeNode[num_actions];
        totValue = 0.0;
        this.childIdx = childIdx;
        if(parent != null)
            m_depth = parent.m_depth+1;
        else
            m_depth = 0;
    }


    public void mctsSearch(ElapsedCpuTimer elapsedTimer) {

    	
    	
        double avgTimeTaken = 0;
        double acumTimeTaken = 0;
        long remaining = elapsedTimer.remainingTimeMillis();
        int numIters = 0;

        int remainingLimit = 5;
        while(remaining > 2*avgTimeTaken && remaining > remainingLimit){
        //while(numIters < Agent.MCTS_ITERATIONS){

            StateObservation state = rootState.copy();
            
            ElapsedCpuTimer elapsedTimerIteration = new ElapsedCpuTimer();
            SingleTreeNode selected = treePolicy(state);
            double delta = selected.rollOut(state);

            //------------------
            
            backUp(selected, delta);

            numIters++;
            acumTimeTaken += (elapsedTimerIteration.elapsedMillis()) ;
            avgTimeTaken  = acumTimeTaken/numIters;
            remaining = elapsedTimer.remainingTimeMillis();
        }
        
//        System.out.println("Total Rollouts: "+ total_rollouts);
    }

    public SingleTreeNode treePolicy(StateObservation state) {

        SingleTreeNode cur = this;
       
        while (!state.isGameOver() && cur.m_depth < ROLLOUT_DEPTH)
        {
            if (cur.notFullyExpanded()) {
            	parentState = state.copy(); // parent node, before expanding (St-1) = parent and St=state selected.
                return cur.expand(state);

            } else {
                SingleTreeNode next = cur.uct(state);
                cur = next;
            }
        }

        return cur;
    }


    public SingleTreeNode expand(StateObservation state) {

        int bestAction = 0;
        double bestValue = -1;

        for (int i = 0; i < children.length; i++) {
            double x = m_rnd.nextDouble();
            if (x > bestValue && children[i] == null) {
                bestAction = i;
                bestValue = x;
            }
        }

        //Roll the state
        state.advance(actions[bestAction]);

        SingleTreeNode tn = new SingleTreeNode(this,bestAction,this.m_rnd,num_actions, actions, Root, parentState);
        children[bestAction] = tn;
        return tn;
    }

    public SingleTreeNode uct(StateObservation state) {

        SingleTreeNode selected = null;
        double bestValue = -Double.MAX_VALUE;
        for (SingleTreeNode child : this.children)
        {
            double hvVal = child.totValue;
            double childValue =  hvVal / (child.nVisits + this.epsilon);

            childValue = Utils.normalise(childValue, bounds[0], bounds[1]);
            //System.out.println("norm child value: " + childValue);

            double uctValue = childValue +
                    K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + this.epsilon));

            uctValue = Utils.noise(uctValue, this.epsilon, this.m_rnd.nextDouble());     //break ties randomly

            // small sampleRandom numbers: break ties in unexpanded nodes
            if (uctValue > bestValue) {
                selected = child;
                bestValue = uctValue;
            }
        }
        if (selected == null)
        {
            throw new RuntimeException("Warning! returning null: " + bestValue + " : " + this.children.length + " " +
            + bounds[0] + " " + bounds[1]);
        }

        //Roll the state:
        state.advance(actions[selected.childIdx]);

        return selected;
    }


    public double rollOut(StateObservation state)
    {
        int thisDepth = this.m_depth;

        while (!finishRollout(state,thisDepth)) {

            int action = m_rnd.nextInt(num_actions); //random action
            state.advance(actions[action]); //
            thisDepth++;
            total_rollouts++;
        }
        

        double delta = value(state);
        double rdelta=0; //initial
        double parentdelta=0; //initial
 
        if (parentState!=null) { //if parent exists (because first state doesn't have parent)
        	parentdelta = value(parentState);
        }
        
        if (Root!=null) {
        	rdelta = value(Root);
        }
        
        if (delta==rdelta) { //0.0 is the initial value of the current state
        	delta += evalKB(state);
        }
        
        //important occurance happened
        if (delta!=parentdelta) {
        	//get score difference
        	double scoreDif = delta - parentdelta; //delta of current minus parent delta.
        	
        	//find out the last collision
        	TreeSet<Event> treeEvent = state.getEventsHistory();
        	
        	//last element of tree is the last collision
        	if (treeEvent.size()>0) {
        		//get type of that collision
        		int type = treeEvent.last().passiveTypeId;
        		//look what list contains this collision type
        		
        		for (int i = 0; i<lNPC.size();i++) { //NPC
        			if (type==lNPC.get(i).itype) {
        				deltaSums.dNPC += scoreDif;
        				deltaSums.nNPC += 1;
        				//update weights
        				deltaSums.wNPC += (deltaSums.dNPC / deltaSums.nNPC) * deltaSums.aNPC;
        				//update learning rate
        				deltaSums.aNPC= Math.max(0.1, 0.75*deltaSums.aNPC);
        				break;
        			}
        		}
        		
        		for (int i = 0; i<lMov.size();i++) { //Movable
        			if (type==lMov.get(i).itype) {
        				deltaSums.dMov += scoreDif;
        				deltaSums.nMov += 1;
        				//update weights
        				deltaSums.wMov += (deltaSums.dMov / deltaSums.nMov) * deltaSums.aMov;
        				//update learning rate
        				deltaSums.aMov= Math.max(0.1, 0.75*deltaSums.aMov);
        				break;
        			}
        		}
        		for (int i = 0; i<lRes.size();i++) { //Resource/portal (same weight)
        			if (type==lRes.get(i).itype) {
        				deltaSums.dRes += scoreDif;
        				deltaSums.nRes += 1;
        				//update weights
        				deltaSums.wRes += (deltaSums.dRes / deltaSums.nRes) * deltaSums.aRes;
        				//update learning rate
        				deltaSums.aRes= Math.max(0.1, 0.75*deltaSums.aRes);
        				break;
        			}
        		}	
        	}
        }

        if(delta < bounds[0])
            bounds[0] = delta;
        if(delta > bounds[1])
            bounds[1] = delta;

        return delta;
    }

    //--------------VALUE-----------------------------
    public double value(StateObservation a_gameState) {
        boolean gameOver = a_gameState.isGameOver();
        Types.WINNER win = a_gameState.getGameWinner();
        double rawScore = a_gameState.getGameScore();
        
        if(gameOver && win == Types.WINNER.PLAYER_LOSES)
            rawScore += HUGE_NEGATIVE;

        if(gameOver && win == Types.WINNER.PLAYER_WINS)
            rawScore += HUGE_POSITIVE;

        return rawScore;
    }
    
   
    //use this method if eval of root is equal to eval of rollout. Add the result to
    //eval of rollout.
    public double evalKB(StateObservation a_gameState) {  
    	
    	Vector2d aPos = a_gameState.getAvatarPosition(); //avatar position
    	Vector2d aPosRoot = Root.getAvatarPosition(); //parent avatar position
    	
    	Vector2d aPos_b = a_gameState.getAvatarPosition(); //avatar position
    	Vector2d aPosRoot_b = Root.getAvatarPosition(); //parent avatar position
    	aPos_b.x = Math.max(0,(int)aPos_b.x/30);
    	aPos_b.y = Math.max(0,(int)aPos_b.y/30);
        aPosRoot_b.x = Math.max(0,(int)aPosRoot_b.x/30);
        aPosRoot_b.y = Math.max(0,(int)aPosRoot_b.y/30);
    	
    	PathFinder pathfinder = Agent.pathf;

    	
    	//WEIGHTS
    	double eval=0;
    	double discFactor = 0.0001 * Root.getGameTick(); //10^(-4) per current game tick for discount
    	
    	//GET NPC POSITIONS AND ADD THEM TO A LIST
    	if (a_gameState.getNPCPositions()!=null && a_gameState.getNPCPositions()[0].size()>0 ) {
    		lNPC.addAll(a_gameState.getNPCPositions()[0]);
    		
    		double pathsize = 0;
    		double pathRootsize = 0;
    		
        	for (int i = 0; i<lNPC.size();i++) { //NPC
        		Vector2d lNPCi = lNPC.get(i).position;
        		lNPCi.x = Math.max(0,(int)lNPCi.x/30);
        		lNPCi.y = Math.max(0,(int)lNPCi.y/30);
        		ArrayList<Node> pathfound = pathfinder.runAllAStar(a_gameState,aPos_b,lNPCi);
        		ArrayList<Node> pathfoundRoot = pathfinder.runAllAStar(a_gameState,aPosRoot_b,lNPCi);
        		if (pathfound==null) {//no way
        			pathsize=Math.sqrt(aPos.sqDist(lNPCi));
        		}
        		else {
        			pathsize=pathfound.size();
        		}
        		if (pathfoundRoot==null) {//no way
        			pathRootsize=Math.sqrt(aPosRoot.sqDist(lNPCi));
        		}
        		else {
        			pathRootsize=pathfoundRoot.size();
        		}
        	}
    		
    		//UPDATE EVAL
    		eval += (deltaSums.wNPC + discFactor) *(pathRootsize-pathsize);
        }
    	
    	//GET MOVABLE POSITIONS AND ADD THEM TO A LIST
        if (a_gameState.getMovablePositions()!=null && a_gameState.getMovablePositions()[0].size()>0) {
        	lMov.addAll(a_gameState.getMovablePositions()[0]);
    		double pathsize = 0;
    		double pathRootsize = 0;
    		
    		//Calculate path length using AStar.
        	for (int i = 0; i<lMov.size();i++) {
        		Vector2d lMovi = lMov.get(i).position;
        		lMovi.x = Math.max(0,(int)lMovi.x/30);
        		lMovi.y = Math.max(0,(int)lMovi.y/30);
        		ArrayList<Node> pathfound = pathfinder.runAllAStar(a_gameState,aPos_b,lMovi);
        		ArrayList<Node> pathfoundRoot = pathfinder.runAllAStar(a_gameState,aPosRoot_b,lMovi);
        		if (pathfound==null) {//If no way, use Euclidean distance.
        			pathsize=Math.sqrt(aPos.sqDist(lMovi));
        		}
        		else {
        			pathsize=pathfound.size();
        		}
        		if (pathfoundRoot==null) {//If no way, use Euclidean distance.
        			pathRootsize=Math.sqrt(aPosRoot.sqDist(lMovi));
        		}
        		else {
        			pathRootsize=pathfoundRoot.size();
        		}
        	}
        	
        	
        	//UPDATE EVAL
        	eval += (deltaSums.wMov + discFactor) * (pathRootsize-pathsize);
        }
        
        //GET PORTALS POSITIONS
        if (a_gameState.getPortalsPositions()!=null && a_gameState.getPortalsPositions()[0].size()>0) {
        	lRes.addAll(a_gameState.getPortalsPositions()[0]);
        	
    		double pathsize = 0;
    		double pathRootsize = 0;
    		
    		//Calculate path length using AStar.
        	for (int i = 0; i<lRes.size();i++) {
        		Vector2d lResi = lRes.get(i).position;
        		lResi.x = Math.max(0,(int)lResi.x/30);
        		lResi.y = Math.max(0,(int)lResi.y/30);
        		ArrayList<Node> pathfound = pathfinder.runAllAStar(a_gameState,aPos_b,lResi);
        		ArrayList<Node> pathfoundRoot = pathfinder.runAllAStar(a_gameState,aPosRoot_b,lResi);
        		if (pathfound==null) {//If no way, use Euclidean distance.
        			pathsize=Math.sqrt(aPos.sqDist(lResi));
        		}
        		else {
        			pathsize=pathfound.size();
        		}
        		if (pathfoundRoot==null) {
        			pathRootsize=Math.sqrt(aPosRoot.sqDist(lResi));
        		}
        		else {
        			pathRootsize=pathfoundRoot.size();
        		}
        	}
        	
        	//UPDATE EVAL
        	eval += (deltaSums.wRes + discFactor) * (pathRootsize-pathsize);
        }
        
        //GET RESOURCE POSITIONS
        if (a_gameState.getResourcesPositions()!=null && a_gameState.getResourcesPositions()[0].size()>0) {
        	lRes.addAll(a_gameState.getResourcesPositions()[0]);
        	
    		double pathsize = 0;
    		double pathRootsize = 0;
    		
    		//Calculate path length using AStar.
        	for (int i = 0; i<lRes.size();i++) {
        		Vector2d lResi = lRes.get(i).position;
        		lResi.x = Math.max(0,(int)lResi.x/30);
        		lResi.y = Math.max(0,(int)lResi.y/30);
        		ArrayList<Node> pathfound = pathfinder.runAllAStar(a_gameState,aPos_b,lResi);
        		ArrayList<Node> pathfoundRoot = pathfinder.runAllAStar(a_gameState,aPosRoot_b,lResi);
        		if (pathfound==null) {//If no way, use Euclidean distance.
        			pathsize=Math.sqrt(aPos.sqDist(lResi));
        		}
        		else {
        			pathsize=pathfound.size();
        		}
        		if (pathfoundRoot==null) {//If no way, use Euclidean distance.
        			pathRootsize=Math.sqrt(aPosRoot.sqDist(lResi));
        		}
        		else {
        			pathRootsize=pathfoundRoot.size();
        		}
        	}
        	
        	//UPDATE EVAL
        	eval += (1.0 + discFactor) * (pathRootsize-pathsize);
        }
        
        //This nomalization method comes from the paper and its original code.
        Agent.MIN_KB_EVAL = Math.min(Agent.MIN_KB_EVAL, eval);
        Agent.MAX_KB_EVAL = Math.max(Agent.MAX_KB_EVAL, eval);
		if (Agent.MIN_KB_EVAL<Agent.MAX_KB_EVAL) {
			eval = (eval-Agent.MIN_KB_EVAL)/(Agent.MAX_KB_EVAL-Agent.MIN_KB_EVAL);
		}
		else {
			eval=0.5;
		}
		eval = (1/2)*eval;
		
    	return eval; 
    }
    
    
    public double scoreDifference(StateObservation state){
    	double deltaState = value(state);
    	double deltaParent = value(parentState);
    	double deltaDif = deltaState - deltaParent;
    	return deltaDif;
    }
    
    


    public boolean finishRollout(StateObservation rollerState, int depth)
    {
        if(depth >= ROLLOUT_DEPTH)      //rollout end condition.
            return true;

        if(rollerState.isGameOver())               //end of game
            return true;

        return false;
    }

    
    public void backUp(SingleTreeNode node, double result)
    {
        SingleTreeNode n = node;
        while(n != null)
        {
            n.nVisits++;
            n.totValue += result;
            if (result < n.bounds[0]) {
                n.bounds[0] = result;
            }
            if (result > n.bounds[1]) {
                n.bounds[1] = result;
            }
            n = n.parent;
        }
    }


    public int mostVisitedAction() {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;
        boolean allEqual = true;
        double first = -1;

        for (int i=0; i<children.length; i++) {

            if(children[i] != null)
            {
                if(first == -1)
                    first = children[i].nVisits;
                else if(first != children[i].nVisits)
                {
                    allEqual = false;
                }

                double childValue = children[i].nVisits;
                childValue = Utils.noise(childValue, this.epsilon, this.m_rnd.nextDouble());     //break ties randomly
                if (childValue > bestValue) {
                    bestValue = childValue;
                    selected = i;
                }
            }
        }

        if (selected == -1)
        {
            System.out.println("Unexpected selection!");
            selected = 0;
        }else if(allEqual)
        {
            //If all are equal, we opt to choose for the one with the best Q.
            selected = bestAction();
        }
        return selected;
    }

    public int bestAction()
    {
        int selected = -1;
        double bestValue = -Double.MAX_VALUE;

        for (int i=0; i<children.length; i++) {

            if(children[i] != null) {
                //double tieBreaker = m_rnd.nextDouble() * epsilon;
                double childValue = children[i].totValue / (children[i].nVisits + this.epsilon);
                childValue = Utils.noise(childValue, this.epsilon, this.m_rnd.nextDouble());     //break ties randomly
                if (childValue > bestValue) {
                    bestValue = childValue;
                    selected = i;
                }
            }
        }

        if (selected == -1)
        {
            System.out.println("Unexpected selection!");
            selected = 0;
        }

        return selected;
    }


    public boolean notFullyExpanded() {
        for (SingleTreeNode tn : children) {
            if (tn == null) {
                return true;
            }
        }

        return false;
    }
}
