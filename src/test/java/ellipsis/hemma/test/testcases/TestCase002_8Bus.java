package ellipsis.hemma.test.testcases;

import static ellipsis.common.ListUtil.setEach;

import java.util.Set;

import ellipsis.hemma.Agent;
import ellipsis.hemma.test.NetworkBuilder;

public class TestCase002_8Bus extends TestCase
{
	public static void main(String[] args)
	{
		new TestCase002_8Bus().run(10000);
	}

	/**
	 * Create a test case.
	 * @return
	 */
	public Set<Agent> init()
	{
		NetworkBuilder builder = new NetworkBuilder();
		
		builder.makeVCAgent("VC1", 200.0)
		       .setGrounded(true);
		builder.makeVCAgent("VC2", 165.0);
		
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
        setEach(agents, Agent::setAlpha,            1e-1);//0.1);//FIXME 3.5e-2);
        setEach(agents, Agent::setAlphaMax,         1e24);
        setEach(agents, Agent::setAlphaMultiplier,  1.001);
        setEach(agents, Agent::setLambdaMax,        1000.0);
        setEach(agents, Agent::setEpsilon,          10.0);
        setEach(agents, Agent::setEpsilonMultiplier,0.999);

		return agents;
	}
}