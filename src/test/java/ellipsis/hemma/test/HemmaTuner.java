package ellipsis.hemma.test;

import static ellipsis.common.ListUtil.setEach;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import ellipsis.common.ThreadPool;
import ellipsis.genetics.GeneticSolver.Creator;
import ellipsis.genetics.GeneticSolver.FitnessFunction;
import ellipsis.genetics.GeneticSolver.Mutator;
import ellipsis.genetics.VectorGeneticSolver;
import ellipsis.hemma.Agent;
import ellipsis.hemma.test.testcases.TestCase;
import ellipsis.hemma.test.testcases.TestCase002_8Bus;

/**
 * Tune HEMMA optimisation parameters with a Genetic Algorithm. 
 *                                  
 * DNA = [ initial alpha      ]     
 *       [ alpha multiplier   ]     
 *       [ lambda multiplier  ]     
 *       [ initial epsilon    ]     
 *       [ epsilon multiplier ]     
 */
public class HemmaTuner<TC extends TestCase> implements FitnessFunction<RealVector>, Creator<RealVector>, Mutator<RealVector>
{
	private Class<TC> testCaseClass;
	private int K;
	private Map<RealVector, Double> solutionFitnesses = new HashMap<>();
	
	public HemmaTuner(Class<TC> testCaseClass, int K) 
	{
		this.testCaseClass = testCaseClass;
		this.K = K;
	}
	
	@Override
	public double fitness(RealVector dna) 
	{
		Double fitness = solutionFitnesses.get(dna);
		if(fitness == null)
		{
			TestCase tc = testCase();
			Set<Agent> agents = tc.init();
			setParameters(agents, dna);
			Solution solution = tc.optimise(agents, K);
			fitness = -solution.convergence(1.0, 10.0);
			
			solutionFitnesses.put(dna, fitness);
		}
		
		return fitness;
	}

	private void setParameters(Set<Agent> agents, RealVector dna) 
	{
		setEach(agents, Agent::setAlpha,            dna.getEntry(0));
        setEach(agents, Agent::setAlphaMultiplier,  dna.getEntry(1));
        setEach(agents, Agent::setLambdaMultiplier, dna.getEntry(2));
        setEach(agents, Agent::setEpsilon,          dna.getEntry(3));
        setEach(agents, Agent::setEpsilonMultiplier,dna.getEntry(4));
	}


	protected TC testCase() 
	{
		try { return testCaseClass.newInstance(); } 
		catch (InstantiationException | IllegalAccessException e)  { throw new RuntimeException(e); }
	}

	@Override
	public RealVector create() 
	{
		RealVector dna = new ArrayRealVector(5);
		Agent agent = testCase().init().iterator().next(); // After initialisation all agents are the same so just get the first one's parameters.
		dna.setEntry(0, agent.getAlpha());             // initial alpha     
		dna.setEntry(1, agent.getAlphaMultiplier());   // alpha multiplier  
		dna.setEntry(2, agent.getLambdaMultiplier());  // lambda multiplier 
		dna.setEntry(3, agent.getEpsilon());           // initial epsilon   
		dna.setEntry(4, agent.getEpsilonMultiplier()); // epsilon multiplier
		
		return mutate(dna);
	}

	static Random mutateRand = new Random();
	@Override
	public RealVector mutate(RealVector dna) 
	{
		RealVector mutated = new ArrayRealVector(dna.getDimension());
		mutated.setEntry(0, Math.max(1e-6,     dna.getEntry(0)+(1-2*mutateRand.nextDouble())*0.1)  ); // initial alpha      - must be > 0
		mutated.setEntry(1, Math.max(1.000001, dna.getEntry(1)+(1-2*mutateRand.nextDouble())*0.001)); // alpha multiplier   - must be > 1  
		mutated.setEntry(2, Math.max(1e-3,     dna.getEntry(2)+(1-2*mutateRand.nextDouble())*0.1)  ); // lambda multiplier  - must be > 0 
		mutated.setEntry(3, Math.max(1e-0,     dna.getEntry(3)+(1-2*mutateRand.nextDouble())*0.1)  ); // initial epsilon    - must be > 0
		mutated.setEntry(4, Math.min(0.99999,  dna.getEntry(4)+(1-2*mutateRand.nextDouble())*0.01) ); // epsilon multiplier - must be < 1
		
//		mutated.setEntry(0, dna.getEntry(0)); // FIXME
//		mutated.setEntry(1, dna.getEntry(1)); // FIXME
//		mutated.setEntry(2, dna.getEntry(2)); // FIXME
//		mutated.setEntry(3, dna.getEntry(3)); // FIXME
//		mutated.setEntry(4, dna.getEntry(4)); // FIXME
		
		return mutated;
	}
	
	public void preloadFitnesses(RealVector[] population, int k)
	{
		ThreadPool pool = ThreadPool.getInstance();
		for (int i = 0; i < population.length; i++) 
		{
			final int _i = i;
			pool.queueTask(() -> fitness(population[_i]));
		}
		pool.waitForAll();
	}
	
	public void solve()
	{
		VectorGeneticSolver solver = new VectorGeneticSolver(0.5, 0.5, 10, 1e-3);
		solver.withDimension(5);
		solver.withFitness(this);
		solver.withCreator(this);
		solver.withMutator(this);
		solver.withLogger(System.out);
		solver.withIterationStartListener(this::preloadFitnesses);
		solver.withIterationEndListener((population, k) -> 
		{ 
			preloadFitnesses(population, k); 
			System.out.println("Iteration "+k+"ended"); 
		} );
		
		RealVector solution = solver.solve(10);
		System.out.println("initial alpha      = " + solution.getEntry(0));
		System.out.println("alpha multiplier   = " + solution.getEntry(1));
		System.out.println("lambda multiplier  = " + solution.getEntry(2));
		System.out.println("initial epsilon    = " + solution.getEntry(3));
		System.out.println("epsilon multiplier = " + solution.getEntry(4));
	}
	
	public static void main(String[] args) 
	{
		TestCase.minimalLogging = true;
		new HemmaTuner<TestCase002_8Bus>(TestCase002_8Bus.class, 10000).solve();
	}
}
