package ellipsis.hemma.test.testcases;

import static ellipsis.common.math.VectorHelper.vector;

import java.util.Set;

import org.apache.commons.math3.linear.RealVector;

import ellipsis.hemma.Agent;
import ellipsis.hemma.test.Solution;

public abstract class TestCase
{
	/**
	 * Initialises and executes the optimisation algorithm.
	 * @param K Number of iterations.
	 */
	public void run(int K)
	{
		Set<Agent> agents = init();
		Solution solution = optimise(agents, K);
		solution.printCSV(1000, agents.size()*2);
	}

	protected abstract Set<Agent> init();

	/**
	 * Algorithm 1:
     * Repeat asynchronously (TODO currently this is implemented synchronously with agents taking turns):
     *   1. Update neighbour information through VU requests 
     *   2. Update primal variables x(k+1) from (14)
     *   3. Update dual variables λ(k+1) from (15) i
     *   4. Increase penalty multiplier α(k+1)
	 * @param agents
	 * @param K
	 * @return
	 */
	private Solution optimise(Set<Agent> agents, int K)
	{
		// Init:
		Solution sol = new Solution(); // This stores the important values from each iteration for logging.
		agents.forEach(Agent::project); // Project the state of each agent to ensure that we start from a feasible solution.
		double epsilon = 10; // Start with a high epsilon - gradient will be reduced as epsilon is reduced each iteration.
		double epsilonMultiplier = 0.999;
		sol.storeDataPoint(agents, epsilon); // Store the starting values for logging.
		
		for(int k = 0; k < K; ++k)
		{
			for (Agent agent : agents)
			{
				// TODO Update values form neighbours:
				
				// Minimise (14) (TODO move this to agent):
				/*
				 * Stochastic gradient decent until Lagrange gradient is less than epsilon,
				 * or, if projected, until the step size is below a minimum threshold
				 * which implies that the lowest point on the constraint set boundary 
				 * has been reached (in the case of the state being projected and the
				 * gradient not being able to drop below epsilon).
				 */
				RealVector grad = agent.gradient();
				double stepSize = 1.0;
				while(grad.getNorm() > epsilon && stepSize > 1e-6)
				{
					// Approximately find the best step size:
					stepSize = backtrack(sol, agents, agent); // TODO replace with local backtracking
					RealVector step = grad.mapMultiply(-stepSize);
					
					// Step the agents state:
					agent.setV(agent.getV() + step.getEntry(0));
					agent.setvMinus(agent.getvMinus() + step.getEntry(1));
					agent.setCurrent(agent.getCurrent() + step.getEntry(2));
					
					// Project back into the constraint set:
					agent.project();
					
					// Next gradient:
					grad = agent.gradient();
				}
			
				// Step dual variables (15):
				agent.stepLambda();
			
				// Step penalty multiplier if g(x) is too big:
				if(Math.abs(agent.gPlus()) > epsilon || Math.abs(agent.gMinus()) > epsilon)
					agent.stepAlpha();
			}
			
			// Save state for logging later:
			sol.storeDataPoint(agents, epsilon);
			
			// Step epsilon:
			epsilon *= epsilonMultiplier;
		}
		
		return sol;
	}
	
	private static boolean debug = false;
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
			
			if(debug) 
				System.out.println(stepSize+","+sol.lagrange(agents)+","+(lagrange - 0.5*stepSize*grad2));
		}
		
		if(debug)
		{
			double d = 1e-5;
			for(double s = 0; s < 10*d; s += d)
			{
				agent.setV(v+grad.getEntry(0)*s);
				agent.setvMinus(vminus+grad.getEntry(1)*s);
				agent.setCurrent(current+grad.getEntry(2)*s);
			
				System.out.println(-s+","+sol.lagrange(agents)+","+(lagrange + 0.5*s*grad2));
			}
		}

		agent.setV(v);
		agent.setvMinus(vminus);
		agent.setCurrent(current);
		
		if(gradNorm*stepSize <= minStep)
			return 0.0;
		else
			return stepSize;
	}
	
	/**
	 * Calculates an approximate gradient to assist in checking the correctness of the lagrange gradient.
	 */
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
}
