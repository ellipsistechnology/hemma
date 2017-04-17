package ellipsis.genetics;

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

	private FitnessFunction<DNA> fitness;
	private GeneSplicer<DNA> splicer;
	private Mutator<DNA> mutator;
	private Creator<DNA> creator;
	private StoppingCriteria<DNA> stoppingCriteria;
	private int eliteCount;
	private DNASelector<DNA> parentSelector;
	private ProbabilityFunction mutationProbability;
	
	public GeneticSolver()
	{
		
	}
	
	public GeneticSolver<DNA> withCost(FitnessFunction<DNA> fitness)
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
		
		// Progress through generations:
		int k = 0;
		while(!stoppingCriteria.stop(population, k))
		{
			// Sort according to fitness:
			Arrays.sort(population, Comparator.comparingDouble(fitness::fitness).reversed()); // FIXME this is probably in the wrong order
			
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
		}
		
		// Find fittest solution:
		DNA fittest = ArrayHelper.maxElement(population, fitness::fitness);
		return fittest;
	}
	
	
	//// Test Case ////
	
	private static final int dimension = 10;
	private static final Random rand = new Random();
	
	public static double cost(RealVector v)
	{
		return -Sum.sum(i -> v.getEntry(i)*v.getEntry(i), v.getDimension());
	}
	
	public static RealVector randomVector()
	{
		RealVector v = new ArrayRealVector(dimension);
		for (int i = 0; i < dimension; i++) 
		{
			v.setEntry(i, rand.nextDouble());
		}
		return v;
	}
	
	public static RealVector splice(RealVector a, RealVector b)
	{
		int n = rand.nextInt(a.getDimension());
		return a.getSubVector(0, n).append(b.getSubVector(n, dimension-n));
	}
	
	public static RealVector mutate(RealVector v)
	{
		RealVector v2 = new ArrayRealVector(v);
		int i = rand.nextInt(v.getDimension());
		v2.setEntry(i, rand.nextDouble());
		return v2;
	}
	
	public static RealVector selectParent(RealVector[] population)
	{
		int i = rand.nextInt(population.length/2);
		return population[i];
	}
	
	public static void main(String[] args) 
	{
		GeneticSolver<RealVector> solver = new GeneticSolver<RealVector>()
				.withCost(GeneticSolver::cost)
				.withCreator(GeneticSolver::randomVector)
				.withEliteCount(2)
				.withGeneSplicer(GeneticSolver::splice)
				.withMutationProbability(() -> rand.nextBoolean()) // 50/50 probability
				.withMutator(GeneticSolver::mutate)
				.withParentSelector(GeneticSolver::selectParent)
				.withStoppingCriteria((population, k) -> k > 10000 || ArrayHelper.max(population, GeneticSolver::cost) > -0.001);
		RealVector fittest = solver.solve(new RealVector[10]);
		double cost = cost(fittest);
		System.out.println(fittest.getNorm()+" : "+cost);
	}
}