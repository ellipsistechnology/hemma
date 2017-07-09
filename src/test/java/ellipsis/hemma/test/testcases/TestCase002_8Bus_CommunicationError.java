package ellipsis.hemma.test.testcases;

import java.util.Set;

import ellipsis.hemma.Agent;
import ellipsis.hemma.test.Solution;

public class TestCase002_8Bus_CommunicationError extends TestCase002_8Bus 
{
	private static final int SHUTDOWN_START_ITERATION = 50;
	private static final int SHUTDOWN_END_ITERATION = 450;
	private static final String SHUTDOWN_AGENT = "VC1";

	public static void main(String[] args)
	{
		new TestCase002_8Bus_CommunicationError().run(2000);
	}
	
	@Override
	protected void executeIterationForAgent(Set<Agent> agents, Agent agent, Solution sol, int k) 
	{
		if(k == SHUTDOWN_START_ITERATION && agent.getName().equals(SHUTDOWN_AGENT))
		{
			agent.pause();
		}
		else if(k == SHUTDOWN_END_ITERATION && agent.getName().equals(SHUTDOWN_AGENT))
		{
			agent.play();
		}
		else
		{
			super.executeIterationForAgent(agents, agent, sol, k);
		}
	}
}
