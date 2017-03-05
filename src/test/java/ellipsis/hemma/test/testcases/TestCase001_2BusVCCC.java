package ellipsis.hemma.test.testcases;

import static ellipsis.common.ListUtil.setEach;
import static ellipsis.common.math.VectorHelper.vector;
import java.util.Set;

import org.apache.commons.math3.linear.RealVector;

import ellipsis.hemma.Agent;
import ellipsis.hemma.test.NetworkBuilder;
import ellipsis.hemma.test.Solution;

public class TestCase001_2BusVCCC
{
	public static void main(String[] args)
	{
		Set<Agent> agents = init();
		int K = 10000;
		Solution solution = optimise(agents, K);
		solution.printCSV(1000, agents.size()*2);
	}

	private static Solution optimise(Set<Agent> agents, int K)
	{
		// Init:
		Solution sol = new Solution();
		agents.forEach(Agent::project);
		sol.storeDataPoint(agents);
		
		for(int k = 0; k < K; ++k)
		{
if(k == 15000)
{
	@SuppressWarnings("unused")
	int i = 0;
}
			for (Agent agent : agents)
			{
				// TODO Update values form neighbours:
				
				// Minimise:
//				agent.minimise();
				RealVector grad = agent.gradient();
				double stepSize = backtrack(sol, agents, agent);
				RealVector step = grad.mapMultiply(-stepSize);
				
				agent.setV(agent.getV() + step.getEntry(0));
				agent.setvMinus(agent.getvMinus() + step.getEntry(1));
				agent.setCurrent(agent.getCurrent() + step.getEntry(2));
				
				agent.project();
			
				// Step dual variables:
//				agent.stepLambda();
			
				// Step penalty multiplier:
				agent.stepAlpha();
			
				// Step epsilon:
//				epsilon *= config.epsilonMultiplier;
			}
			
			sol.storeDataPoint(agents);
		}
		
		return sol;
	}
	
	private static double backtrack(Solution sol, Set<Agent> agents, Agent agent)
	{
		RealVector grad = agent.gradient();
		double lagrange = sol.lagrange(agents);
		double stepSize = 2;
		double grad2 = grad.dotProduct(grad);
		
		double v = agent.getV();
		double vminus = agent.getvMinus();
		double current = agent.getCurrent();
		
		while(sol.lagrange(agents) > lagrange - stepSize*0.5*grad2)
		{
			stepSize *= 0.5;
			RealVector step = grad.mapMultiply(-stepSize);
			agent.setV(v + step.getEntry(0));
			agent.setvMinus(vminus + step.getEntry(1));
			agent.setCurrent(current + step.getEntry(2));
//System.out.println(stepSize+","+sol.lagrange(agents)+","+(lagrange - stepSize*grad2));
		}

//for(double d = 0; d > -1e-8; d -= 1e-9)
//{
//	System.out.println(d+","+lagrange(problem, newX, lambda, alpha)+","+(lagrange - d*grad2));
//	newX = x.add(grad.mapMultiply(-d));
//}
		agent.setV(v);
		agent.setvMinus(vminus);
		agent.setCurrent(current);
		
//System.out.println(0+","+sol.lagrange(agents)+","+lagrange);		
		
		return stepSize;
	}
	
	static RealVector approxGradient(Solution sol, Agent agent, Set<Agent> agents)
	{
		double delta = 1e-12;
		double grad0, grad1, grad2;
		
		{
			double v = agent.getV();
			agent.setV(v - delta);
			double left = sol.lagrange(agents);
			agent.setV(v + delta);
			double right = sol.lagrange(agents);
			agent.setV(v);
			grad0 = -(left-right)/(2*delta);
		}
		
		{
			double v = agent.getvMinus();
			agent.setvMinus(v - delta);
			double left = sol.lagrange(agents);
			agent.setvMinus(v + delta);
			double right = sol.lagrange(agents);
			agent.setvMinus(v);
			grad1 = -(left-right)/(2*delta);
		}
		
		{
			double I = agent.getCurrent();
			agent.setCurrent(I - delta);
			double left = sol.lagrange(agents);
			agent.setCurrent(I + delta);
			double right = sol.lagrange(agents);
			agent.setCurrent(I);
			grad2 = -(left-right)/(2*delta);
		}
		
		return vector(grad0, grad1, grad2);
	}

	private static Set<Agent> init()
	{
		NetworkBuilder builder = new NetworkBuilder();
		
		builder.makeVCAgent("PS1", 100.0)
		       .setGrounded(true);
        builder.makeCCAgent("CC1", -6.0);
        
        // Setup network connections:
        builder.link("PS1", "CC1", 1.0);

        Set<Agent> agents = builder.agentSet();
        
        // Set optimization parameters:
        setEach(agents, Agent::setAlpha,            1.0e-3);
        setEach(agents, Agent::setAlphaMax,         1e24);
        setEach(agents, Agent::setAlphaMultiplier,  1.005);
        setEach(agents, Agent::setLambdaMax,        10.0);

		return agents;
	}
}