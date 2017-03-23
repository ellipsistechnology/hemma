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
		new TestCase001_2BusVCCC().run();
	}
	
	public void run()
	{
		Set<Agent> agents = init();
		int K = 10000;
		Solution solution = optimise(agents, K);
		solution.printCSV(1000, agents.size()*2);
	}

	private Solution optimise(Set<Agent> agents, int K)
	{
		// Init:
		Solution sol = new Solution();
		agents.forEach(Agent::project);
		double epsilon = 10;
		double epsilonMultiplier = 0.999;//0.998;
		sol.storeDataPoint(agents, epsilon);
		
		for(int k = 0; k < K; ++k)
		{
if(k == 3000)
{
	@SuppressWarnings("unused")
	int i = 0;
}
			for (Agent agent : agents)
			{
				// TODO Update values form neighbours:
//if(k > 3000 && agent instanceof ConstantPowerAgent)
//	continue;
				
				// Minimise:
				boolean projected = false;
				RealVector grad = agent.gradient();
				double stepSize = 1.0;
				while(!projected && grad.getNorm() > epsilon && stepSize > 0.0)
				{
					stepSize = backtrack(sol, agents, agent); // TODO replace with local backtracking
					RealVector step = grad.mapMultiply(-stepSize);
					
					agent.setV(agent.getV() + step.getEntry(0));
					agent.setvMinus(agent.getvMinus() + step.getEntry(1));
					agent.setCurrent(agent.getCurrent() + step.getEntry(2));
					
					projected = agent.project();
					
					grad = agent.gradient();
				}
			
				// Step dual variables:
				agent.stepLambda();
			
				// Step penalty multiplier:
				if(Math.abs(agent.gPlus()) > epsilon || Math.abs(agent.gMinus()) > epsilon)
					agent.stepAlpha();
			}
			
			sol.storeDataPoint(agents, epsilon);
			
			// Step epsilon:
			epsilon *= epsilonMultiplier;
		}
		
		return sol;
	}
	
	private double backtrack(Solution sol, Set<Agent> agents, Agent agent)
	{
		RealVector grad = agent.gradient();
		double lagrange = sol.lagrange(agents);
		double stepSize = 2;
		double grad2 = grad.dotProduct(grad);
		
		double v = agent.getV();
		double vminus = agent.getvMinus();
		double current = agent.getCurrent();
		
		double minStep = 1e-9;
		double gradNorm = grad.getNorm();
		while(
				sol.lagrange(agents) > lagrange - stepSize*0.5*grad2 && 
				gradNorm*stepSize > minStep)
		{
			stepSize *= 0.5;
			RealVector step = grad.mapMultiply(-stepSize);
			agent.setV(v + step.getEntry(0));
			agent.setvMinus(vminus + step.getEntry(1));
			agent.setCurrent(current + step.getEntry(2));
//System.out.println(stepSize+","+sol.lagrange(agents)+","+(lagrange - 0.5*stepSize*grad2));
		}
		
//double d = 1e-5;
//for(double s = 0; s < 10*d; s += d)
//{
//	agent.setV(v+grad.getEntry(0)*s);
//	agent.setvMinus(vminus+grad.getEntry(1)*s);
//	agent.setCurrent(current+grad.getEntry(2)*s);
//
//	System.out.println(-s+","+sol.lagrange(agents)+","+(lagrange + 0.5*s*grad2));
//}

		agent.setV(v);
		agent.setvMinus(vminus);
		agent.setCurrent(current);
		
		if(gradNorm*stepSize <= minStep)
			return 0.0;
		else
			return stepSize;
	}
	
	RealVector approxGradient(Solution sol, Agent agent, Set<Agent> agents)
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

	private Set<Agent> init()
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