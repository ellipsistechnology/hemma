package ellipsis.genetics;

import java.util.Random;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import ellipsis.common.ArrayHelper;
import ellipsis.common.math.Sum;
import ellipsis.genetics.GeneticSolver.Creator;
import ellipsis.genetics.GeneticSolver.GeneSplicer;
import ellipsis.genetics.GeneticSolver.Mutator;

public class VectorGeneticSolver extends GeneticSolver<RealVector> 
							     implements Creator<RealVector>, GeneSplicer<RealVector>, Mutator<RealVector>
{
	private int dimension;
	private double initialLow;
	private double initialHigh;
	private Random rand = new Random();

	/**
	 * 
	 * @param dimension
	 * @param initialLow
	 * @param initialHigh
	 * @param mutationProbability
	 * @param parentSelectionPercent
	 * @param maxIterations
	 * @param minFitness
	 */
	public VectorGeneticSolver(int dimension, double initialLow, double initialHigh, double mutationProbability, double parentSelectionPercent, double maxIterations, double minFitness) 
	{
		this.dimension = dimension;
		this.initialLow = initialLow;
		this.initialHigh = initialHigh;
		
		withCreator(this);
		withEliteCount(2);
		withGeneSplicer(this);
		withMutationProbability(() -> rand.nextDouble() < mutationProbability);
		withMutator(this);
		withParentSelector(population -> population[rand.nextInt((int)(population.length*parentSelectionPercent))]);
		withStoppingCriteria((population, k) -> k > maxIterations || ArrayHelper.max(population, fitness::fitness) > minFitness);
	}
	
	/**
	 * Must set creator manually and set dimension before use.
	 * @param mutationProbability
	 * @param parentSelectionPercent
	 * @param maxIterations
	 * @param minFitness
	 */
	public VectorGeneticSolver(double mutationProbability, double parentSelectionPercent, double maxIterations, double minFitness)
	{
		this(0, 0.0, 0.0, mutationProbability, parentSelectionPercent, maxIterations, minFitness);
	}
	
	public VectorGeneticSolver withDimension(int dimension)
	{
		this.dimension = dimension;
		return this;
	}

	@Override
	public RealVector create() 
	{
		RealVector v = new ArrayRealVector(dimension);
		for (int i = 0; i < dimension; i++) 
		{
			v.setEntry(i, initialLow + rand.nextDouble()*(initialHigh-initialLow));
		}
		return v;
	}

	@Override
	public RealVector splice(RealVector father, RealVector mother) 
	{
		int n = rand.nextInt(dimension);
		return father.getSubVector(0, n).append(mother.getSubVector(n, dimension-n));
	}

	@Override
	public RealVector mutate(RealVector v) 
	{
		RealVector v2 = new ArrayRealVector(v);
		int i = rand.nextInt(v.getDimension());
		v2.setEntry(i, rand.nextDouble());
		return v2;
	}
	
	public RealVector solve(int populationSize) 
	{
		return super.solve(new RealVector[populationSize]);
	}
	
	
	//// Test Case ////
	
	private static double fitness(RealVector v)
	{
		return -Sum.sum(i -> v.getEntry(i)*v.getEntry(i), v.getDimension());
	}

	public static void main(String[] args) 
	{
		VectorGeneticSolver solver = new VectorGeneticSolver(10, 0.0, 1.0, 0.5, 0.5, 10000, -0.001);
		solver.withFitness(VectorGeneticSolver::fitness);
		solver.withLogger(System.out);
		
		RealVector fittest = solver.solve(new RealVector[10]);
		
		double fitness = fitness(fittest);
		System.out.println(fittest.getNorm()+" : "+fitness);
	}
}