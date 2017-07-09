package ellipsis.hemma.test.testcases;

import static ellipsis.common.math.VectorHelper.vector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Set;

import org.apache.commons.math3.linear.RealVector;

import ellipsis.hemma.Agent;
import ellipsis.hemma.HEMMAProtocol;
import ellipsis.hemma.test.Solution;

public abstract class TestCase
{
	public static boolean minimalLogging = false;
	private static final PrintStream log;
	static
	{
		try
		{
			File file = new File("/opt/hemma/log/hemma.log");
			file.getParentFile().mkdirs();
			log = new PrintStream(file);
		} 
		catch (FileNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Initialises and executes the optimisation algorithm.
	 * @param K Number of iterations.
	 */
	public void run(int K)
	{
		log.println("\nBeginning simulation with K="+K);
		Set<Agent> agents = init();
		agents.forEach(Agent::switchOn);
		Solution solution = optimise(agents, K);
		HEMMAProtocol.useCache = false;
		solution.printCSV(1000, agents.size()*2);
	}

	public abstract Set<Agent> init();

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
	public Solution optimise(Set<Agent> agents, int K)
	{
		// Init:
		Solution sol = new Solution(); // This stores the important values from each iteration for logging.
		agents.forEach(Agent::project); // Project the state of each agent to ensure that we start from a feasible solution.
		sol.storeDataPoint(agents); // Store the starting values for logging.
		
		for(int k = 0; k < K; ++k)
		{
			if(!minimalLogging) 
				log.print(".");
			if(k % 1000 == 0)
			{
				log.print(k);
				if(minimalLogging)
					log.print(",");
			}
			
			executeIteration(agents, sol, k);
		}
		
		log.println("Simulation complete.");
		return sol;
	}

	protected void executeIteration(Set<Agent> agents, Solution sol, int k) 
	{
		for (Agent agent : agents)
		{
			executeIterationForAgent(agents, agent, sol, k);
		}
		
		// Execute messages before getting data point:
		for (Agent agent : agents)
			agent.getHemmaProtocol().execute(k);
		
		// Save state for logging later:
		sol.storeDataPoint(agents);
	}

	protected void executeIterationForAgent(Set<Agent> agents, Agent agent, Solution sol, int k) 
	{
		double epsilon = agent.getEpsilon();
		
		agent.getHemmaProtocol().execute(k);
		
		if(!agent.getHemmaProtocol().getState().equals(HEMMAProtocol.HEMMAState.SessionExecution))
			return;
		
		// Minimise (14) (TODO move this to agent):
		
		// Check that neighbours are known:
		if(!agent.getHemmaProtocol().initialised())
			return;
		
		RealVector previousState = agent.state();
		
		/*
		 * Stochastic gradient decent until Lagrange gradient is less than epsilon,
		 * or, if projected, until the step length is below a minimum threshold
		 * which implies that the lowest point on the constraint set boundary 
		 * has been reached (in the case of the state being projected and the
		 * gradient not being able to drop below epsilon).
		 */
		int maxGradDecIterations = 100;
		for(int i = 0; i < 3; ++i) // one dimension at a time - this is much faster due to a steep, curved Lagrange function 
		{
			RealVector grad = grad(agent, i);
			double stepLength = 1.0; // how far we have stepped (if too small then no point in continuing - usually an issue due to projection)
			int j = 0;
			while(grad.getNorm() > epsilon && stepLength > 1e-6 && j < maxGradDecIterations)
			{
				++j;

				// Approximately find the best step size:
				double stepSize = backtrack(sol, agents, agent, grad); // TODO replace with local backtracking
				RealVector step = grad.mapMultiply(-stepSize);
				
				// Step the agents state:
				RealVector oldState = agent.state();
				agent.setState(oldState.add(step));
				
				// Project back into the constraint set:
				agent.project();
				
				// Update neighbours' g(x) values:
				RealVector newState = agent.state();
				RealVector delta = newState.subtract(oldState);
				agent.getHemmaProtocol().updateNeighbourG(delta.toArray());
				
				// Check how far we've stepped:
				stepLength = delta.getNorm();
				
				// Next gradient:
				grad = grad(agent, i);
			}
		}
		
		// Step dual variables (15) and penalty multiplier if g(x) is too big:
		double targetG = Math.max(1e-3, epsilon);
		if(Math.abs(agent.gPlus()) > targetG || Math.abs(agent.gMinus()) > targetG)
		{
			agent.stepLambda();
			agent.stepAlpha();
		}
		
		// Step epsilon:
		agent.stepEpsilon();
		
		// Inform neighbours of values:
		agent.updateValues();
		
		// Update average convergence estaimte:
		agent.updateConvergence(previousState);
	}

	public RealVector grad(Agent agent, int i)
	{
		RealVector grad = agent.gradient();
		for (int j = 0; j < grad.getDimension(); j++)
		{
			if(j != i && i != -1)
				grad.setEntry(j, 0.0);
		}
		return grad;
	}
	
	private static boolean debug = false;
	/**
	 * Approximately search for the best step size based on the given step direction.
	 * This method only returns the step size and does not change the agent's state.
	 * Ref. http://stanford.edu/~boyd/cvxbook/bv_cvxslides.pdf slide 10-6.
	 * @param sol Solution; used for calculating the Lagrange function.
	 * @param agents List of all agents; used for calculating the Lagrange function.
	 * @param agent The agent whose is stepping.
	 * @param grad The gradient at the current state.
	 * @return The step size.
	 */
	private double backtrack(Solution sol, Set<Agent> agents, Agent agent, RealVector grad)
	{
boolean oldCacheValue = HEMMAProtocol.useCache;
HEMMAProtocol.useCache = false;
		double lagrange = sol.lagrange(agents);
		double stepSize = 2;
		double grad2 = grad.dotProduct(grad);
		
		double v = agent.getV();
		double vminus = agent.getvMinus();
		double power = agent.getPower();
		
		double minStep = 1e-24;
		double gradNorm = grad.getNorm();
		while(
				sol.lagrange(agents) > lagrange - stepSize*0.5*grad2 && 
				gradNorm*stepSize > minStep)
		{
			stepSize *= 0.5;
			RealVector step = grad.mapMultiply(-stepSize);
			agent.setV(v + step.getEntry(0));
			agent.setvMinus(vminus + step.getEntry(1));
			agent.setPower(power + step.getEntry(2));
			
			if(debug) 
				System.out.println(stepSize+","+sol.lagrange(agents)+","+(lagrange - 0.5*stepSize*grad2));
		}
		
		boolean improved = sol.lagrange(agents) < lagrange;
		
		if(debug)
		{
			double d = 1e-12;
			for(double s = 0; s < 10*d; s += d)
			{
				agent.setV(v+grad.getEntry(0)*s);
				agent.setvMinus(vminus+grad.getEntry(1)*s);
				agent.setPower(power+grad.getEntry(2)*s);
			
				System.out.println(-s+","+sol.lagrange(agents)+","+(lagrange + 0.5*s*grad2));
			}
		}

		agent.setV(v);
		agent.setvMinus(vminus);
		agent.setPower(power);
		
HEMMAProtocol.useCache = oldCacheValue;
		
		if(gradNorm*stepSize <= minStep && !improved)
			return 0.0;
		else
			return stepSize;
	}
	
	/**
	 * Calculates an approximate gradient to assist in checking the correctness of the Lagrange gradient.
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
			double p = agent.getPower();
			agent.setPower(p - delta);
			double left = sol.lagrange(agents);
			agent.setPower(p + delta);
			double right = sol.lagrange(agents);
			agent.setPower(p);
			grad2 = -(left-right)/(2*delta);
		}
		
		return vector(grad0, grad1, grad2);
	}
}
