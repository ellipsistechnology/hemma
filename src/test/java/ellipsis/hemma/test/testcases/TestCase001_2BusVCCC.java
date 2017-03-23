package ellipsis.hemma.test.testcases;

import static ellipsis.common.ListUtil.setEach;

import java.util.Set;

import ellipsis.hemma.Agent;
import ellipsis.hemma.test.NetworkBuilder;

public class TestCase001_2BusVCCC extends TestCase
{
	public static void main(String[] args)
	{
		new TestCase001_2BusVCCC().run(10000);
	}

	/**
	 * Create a test case.
	 * @return
	 */
	protected Set<Agent> init()
	{
		NetworkBuilder builder = new NetworkBuilder();
		
		builder.makeVCAgent("PS1", 150.0)
		       .setGrounded(true);
        builder.makeCPAgent("CP1", -100.0);
        //builder.makeCCAgent("CC1", -6.0);
        
        // Setup network connections:
        builder.link("PS1", "CP1", 1.0);
        //builder.link("PS1", "CC1", 1.0);

        Set<Agent> agents = builder.agentSet();
        
        // Set optimization parameters:
        setEach(agents, Agent::setAlpha,            0.1);//FIXME 3.5e-2);
        setEach(agents, Agent::setAlphaMax,         1e24);
        setEach(agents, Agent::setAlphaMultiplier,  1.0);//FIXME 1.01);
        setEach(agents, Agent::setLambdaMax,        1000.0);

		return agents;
	}
}