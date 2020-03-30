package kbastar;

public class dSums{
	//sum of delta per collision type
	static double dNPC=0;
	static double dMov=0;
	static double dRes=0;
	
	//colission type counters
	static int nNPC=0;
	static int nMov=0;
	static int nRes=0;
	
	//weights of collision types
	static double wNPC=0.1;
	static double wMov=0.25;
	static double wRes=1.0; //same weight for resources and portals
	
	//learning rates of types
	static double aNPC=0.8;
	static double aMov=0.8;
	static double aRes=0.8; 
}
