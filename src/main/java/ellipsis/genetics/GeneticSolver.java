package ellipsis.genetics;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import ellipsis.common.ArrayHelper;
import ellipsis.common.math.Sum;

public class GeneticSolver<DNA>
{
	public static interface FitnessFunction<DNA>
	{
		double fitness(DNA dna);
	}
	
	public static interface GeneSplicer<DNA>
	{
		DNA splice(DNA father, DNA mother);
	}
	
	public static interface Mutator<DNA>
	{
		DNA mutate(DNA dna);
	}
	
	public static interface Creator<DNA>
	{
		DNA create();
	}
	
	public static interface StoppingCriteria<DNA>
	{
		boolean stop(DNA[] population, int k);
	}
	
	public static interface DNASelector<DNA>
	{
		DNA select(DNA[] population);
	}
	
	public static interface ProbabilityFunction
	{
		boolean nextValue();
	}
	
	public static interface IterationListener<DNA>
	{
		void execute(DNA[] population, int k);
	}

	protected FitnessFunction<DNA> fitness;
	protected GeneSplicer<DNA> splicer;
	protected Mutator<DNA> mutator;
	protected Creator<DNA> creator;
	protected StoppingCriteria<DNA> stoppingCriteria;
	protected int eliteCount;
	protected DNASelector<DNA> parentSelector;
	protected ProbabilityFunction mutationProbability;
	protected PrintStream logger;
	protected IterationListener<DNA> iterationStartListener, iterationEndListener;
	
	public GeneticSolver()
	{
		
	}
	
	public GeneticSolver<DNA> withLogger(PrintStream logger)
	{
		this.logger = logger;
		return this;
	}
	
	public GeneticSolver<DNA> withFitness(FitnessFunction<DNA> fitness)
	{
		this.fitness = fitness;
		return this;
	}
	
	public GeneticSolver<DNA> withGeneSplicer(GeneSplicer<DNA> splicer)
	{
		this.splicer = splicer;
		return this;
	}
	
	public GeneticSolver<DNA> withMutator(Mutator<DNA> mutator)
	{
		this.mutator = mutator;
		return this;
	}
	
	public GeneticSolver<DNA> withCreator(Creator<DNA> creator)
	{
		this.creator = creator;
		return this;
	}
	
	public GeneticSolver<DNA> withStoppingCriteria(StoppingCriteria<DNA> stoppingCriteria)
	{
		this.stoppingCriteria = stoppingCriteria;
		return this;
	}
	
	public GeneticSolver<DNA> withEliteCount(int count)
	{
		this.eliteCount = count;
		return this;
	}
	
	public GeneticSolver<DNA> withParentSelector(DNASelector<DNA> selector)
	{
		this.parentSelector = selector;
		return this;
	}
	
	public GeneticSolver<DNA> withMutationProbability(ProbabilityFunction f)
	{
		this.mutationProbability = f;
		return this;
	}
	
	public GeneticSolver<DNA> withIterationStartListener(IterationListener<DNA> l)
	{
		this.iterationStartListener = l;
		return this;
	}
	
	public GeneticSolver<DNA> withIterationEndListener(IterationListener<DNA> l)
	{
		this.iterationEndListener = l;
		return this;
	}
	
	/**
	 * 
	 * @param population An empty array ready to be filled with the initial population.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public DNA solve(DNA[] population)
	{
		// Create pool:
		for (int i = 0; i < population.length; i++) 
		{
			population[i] = creator.create();
		}
		
		if(iterationStartListener != null)
			iterationStartListener.execute(population, -1);
		
		// Progress through generations:
		int k = 0;
		while(!stoppingCriteria.stop(population, k))
		{
			if(iterationStartListener != null)
				iterationStartListener.execute(population, k);
			
			// Sort according to fitness:
			Arrays.sort(population, Comparator.comparingDouble(fitness::fitness).reversed());
			
			// Choose elite DNA for next population:
			DNA[] nextGeneration = (DNA[])new Object[population.length];
			System.arraycopy(population, 0, nextGeneration, 0, eliteCount);
			
			// Breed to produce children for next generation:
			for(int i = eliteCount; i < population.length; ++i)
			{
				DNA father = parentSelector.select(population);
				DNA mother = parentSelector.select(population);
				nextGeneration[i] = splicer.splice(father, mother);
				if(mutationProbability.nextValue())
					nextGeneration[i] = mutator.mutate(nextGeneration[i]);
			}
			
			// Prepare for next generation:
			System.arraycopy(nextGeneration, 0, population, 0, population.length);
			++k;
			
			// Call listener:
			if(iterationEndListener != null)
				iterationEndListener.execute(population, k);
			
			// Log result:
			if(logger != null)
			{
				DNA fittest = ArrayHelper.maxElement(population, fitness::fitness);
				logger.print(k);
				logger.print(": ");
				logger.print(fittest);
				logger.print(" => ");
				logger.println(fitness.fitness(fittest));
			}
		}
		
		// Find fittest solution:
		DNA fittest = ArrayHelper.maxElement(population, fitness::fitness);
		return fittest;
	}
	
	
	//// Test Case ////
	
	private static final int dimension = 10;
	private static final Random rand = new Random();
	
	private static double fitness(RealVector v)
	{
		return -Sum.sum(i -> v.getEntry(i)*v.getEntry(i), v.getDimension());
	}
	
	private static RealVector randomVector()
	{
		RealVector v = new ArrayRealVector(dimension);
		for (int i = 0; i < dimension; i++) 
		{
			v.setEntry(i, rand.nextDouble());
		}
		return v;
	}
	
	private static RealVector splice(RealVector a, RealVector b)
	{
		int n = rand.nextInt(a.getDimension());
		return a.getSubVector(0, n).append(b.getSubVector(n, dimension-n));
	}
	
	private static RealVector mutate(RealVector v)
	{
		RealVector v2 = new ArrayRealVector(v);
		int i = rand.nextInt(v.getDimension());
		v2.setEntry(i, rand.nextDouble());
		return v2;
	}
	
	private static RealVector selectParent(RealVector[] population)
	{
		return population[rand.nextInt(population.length/2)];
	}
	
	public static void main(String[] args) 
	{
		GeneticSolver<RealVector> solver = new GeneticSolver<RealVector>()
				.withFitness(GeneticSolver::fitness)
				.withCreator(GeneticSolver::randomVector)
				.withEliteCount(2)
				.withGeneSplicer(GeneticSolver::splice)
				.withMutationProbability(() -> rand.nextBoolean()) // 50/50 probability
				.withMutator(GeneticSolver::mutate)
				.withParentSelector(GeneticSolver::selectParent)
				.withStoppingCriteria((population, k) -> k > 10000 || ArrayHelper.max(population, GeneticSolver::fitness) > -0.001)
				.withLogger(System.out);
		RealVector fittest = solver.solve(new RealVector[10]);
		double fitness = fitness(fittest);
		System.out.println(fittest.getNorm()+" : "+fitness);
	}
}