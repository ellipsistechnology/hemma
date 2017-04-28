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
		builder.makeVCAgent("VC2", 200.0);
		
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
        /* Best from tuning:
			initial alpha      = 7.3978384550794E-4
			alpha multiplier   = 1.000001
			lambda multiplier  = 1.1855488320556264
			initial epsilon    = 10.208361642475733
			epsilon multiplier = 0.9833867364998466
         */
        setEach(agents, Agent::setAlpha,             1e-0);
        setEach(agents, Agent::setAlphaMax,          1e24);
        setEach(agents, Agent::setAlphaMultiplier,   1.001);
        setEach(agents, Agent::setLambdaMax,         1000.0);
        setEach(agents, Agent::setLambdaMultiplier,  4e-3);
        setEach(agents, Agent::setEpsilon,           1e-6);
        setEach(agents, Agent::setEpsilonMultiplier, 1.0);//0.999);

		return agents;
	}
}