package ellipsis.hemma.test.testcases;

import static ellipsis.common.ListUtil.setEach;

import java.util.Set;

import ellipsis.hemma.Agent;
import ellipsis.hemma.test.NetworkBuilder;

public class TestCase002_8Bus extends TestCase
{
	public static void main(String[] args)
	{
		new TestCase002_8Bus().run(2000);
	}

	/**
	 * Create a test case.
	 * @return
	 */
	public Set<Agent> init()
	{
		NetworkBuilder builder = new NetworkBuilder();
		
		builder.makeVCAgent("VC1", 250.0)
		       .setGrounded(true);
		builder.makeVCAgent("VC2", 170.0);
		
        builder.makeCPAgents("CP", 4, -50.0);
        builder.makeCCAgents("CC", 2, -6.0);
        
        // Setup network connections:
        builder.link("VC1", "CC1", 1.0);
        builder.link("CC1", "CC2", 1.0);
        builder.link("VC1", "VC2", 1.0);
        builder.link("VC2", "CP1", 1.0);
        builder.link("CP1", "CP2", 1.0);
        builder.link("CP2", "CP3", 1.0);
        builder.link("CP3", "CP4", 1.0);

        Set<Agent> agents = builder.agentSet();
        
        // Set optimization parameters:
        /* 1: {0.7437170678; 1.0022891615; 0.0880506421; 0.000001; 0.999999} => -0.45799673392666035 */
        /* 2: {1.0335558193; 1.0032247029; 0.0516760301; 0.000001; 0.9919932057} => -0.0052001592456112165 */
        /* 3: {0.7437170678; 1.0022891615; 0.0880506421; 0.000001; 0.9924475277} => -0.004149376395503751 */
        /* 4: {0.7291686544; 1.0205134478; 0.1663869982; 0.0594168354; 0.995573057} => -0.0014379205815923106 */
        /* 33: {0.080287964; 1.022639996; 0.1791033455; 0.107317332; 0.9913526304} => -0.0011627986176832824 */
        setEach(agents, Agent::setAlpha,             0.1);  //0.7291686544); //1.0335558193); //0.7437170678);
        setEach(agents, Agent::setAlphaMax,          1e24);         
        setEach(agents, Agent::setAlphaMultiplier,   1.02); //1.0205134478); //1.0032247029); //1.0022891615);
        setEach(agents, Agent::setLambdaMax,         1000.0);       
        setEach(agents, Agent::setLambdaMultiplier,  0.15); //0.1663869982); //0.0516760301); //0.0880506421);
        setEach(agents, Agent::setEpsilon,           0.1);  //0.0594168354); //0.000001);     //0.000001);    
        setEach(agents, Agent::setEpsilonMultiplier, 0.999);//0.995573057);  //0.9919932057); //0.999999);    

		return agents;
	}
}