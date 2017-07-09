package ellipsis.hemma.test.testcases;

import java.util.Set;

import ellipsis.hemma.Agent;
import ellipsis.hemma.ConstantPowerAgent;
import ellipsis.hemma.test.Solution;

public class TestCase002_8Bus_LoadChange extends TestCase002_8Bus 
{
	private static final int LOAD_CHANGE_ITERATION = 700;
	private static final String LOAD_AGENT = "CP1";
	private static final double NEW_POWER = 0;
	
	public static void main(String[] args)
	{
		new TestCase002_8Bus_LoadChange().run(2000);
	}
	
	@Override
	protected void executeIterationForAgent(Set<Agent> agents, Agent agent, Solution sol, int k) 
	{
		if(k == LOAD_CHANGE_ITERATION && agent.getName().equals(LOAD_AGENT))
		{
			((ConstantPowerAgent)agent).setConstantPower(NEW_POWER);
		}
		else
		{
			super.executeIterationForAgent(agents, agent, sol, k);
		}
	}
}