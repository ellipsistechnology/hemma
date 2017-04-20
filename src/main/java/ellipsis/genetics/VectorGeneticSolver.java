package ellipsis.genetics;

import java.util.Random;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import ellipsis.common.ArrayHelper;
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
}