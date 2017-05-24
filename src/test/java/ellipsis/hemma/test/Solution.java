package ellipsis.hemma.test;

import static ellipsis.common.math.Sum.sum;
import static ellipsis.common.math.VectorHelper.appendVectors;
import static ellipsis.common.math.VectorHelper.map;
import static ellipsis.common.math.VectorHelper.vector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Set;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import ellipsis.hemma.Agent;

public class Solution
{
	protected static final PrintStream out;
	static
	{
		try
		{
			File file = new File("/opt/hemma/log/data.csv");
			file.getParentFile().mkdirs();
			out = new PrintStream(file);
		} 
		catch (FileNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public ArrayList<RealVector> xs = new ArrayList<>();
	public ArrayList<Double> lagrangeValues = new ArrayList<>();
	public ArrayList<Double> costValues = new ArrayList<>();
	public ArrayList<RealVector> gradientValues = new ArrayList<>();
	public ArrayList<RealVector> gValues = new ArrayList<>();
	public ArrayList<RealVector> epsilonValues = new ArrayList<>();
	public ArrayList<Double> alphaValues = new ArrayList<>();
	public ArrayList<RealVector> lambdaValues = new ArrayList<>();
	public ArrayList<RealVector> hValues = new ArrayList<>();
	public ArrayList<Double> trueH = new ArrayList<>();

	public void storeDataPoint(Set<Agent> agents)
	{
		RealVector x = appendVectors(agents, n -> vector(n.getV(), n.getvMinus(), n.getPower()));
		xs.add(x);
		lagrangeValues.add(lagrange(agents));
		costValues.add(cost(agents));
		gradientValues.add(appendVectors(agents, n -> n.gradient()));
		gValues.add(appendVectors(agents, n -> vector(n.gPlus(), n.gMinus())));
		epsilonValues.add(vector(agents, Agent::getEpsilon));
		alphaValues.add(agents.iterator().next().getAlpha());
		lambdaValues.add(appendVectors(agents, n -> vector(n.getLambdaPlus(), n.getLambdaMinus())));
		hValues.add(vector(agents, Agent::getAverageConvergenceApproximation));
		trueH.add(sum(Agent::getPreviousConvergenceMeasure, agents)/agents.size());
	}
	
	public Double lagrange(Set<Agent> agents)
	{
		double cost = cost(agents);
		double lambdaPlusG = lambdaPlusG(agents);
		double lambdaMinusG = lambdaMinusG(agents);
		double alphaGPlus = alphaGPlus(agents);
		double alphaGMinus = alphaGMinus(agents);
		return 
			cost + 
			lambdaPlusG +
			lambdaMinusG +
			alphaGPlus +
			alphaGMinus;
	}

	public double alphaGMinus(Set<Agent> agents)
	{
		return sum(n -> n.getAlpha()*n.gMinus()*n.gMinus()/2.0, agents);
	}

	public double alphaGPlus(Set<Agent> agents)
	{
		return sum(n -> n.getAlpha()*n.gPlus()*n.gPlus()/2.0, agents);
	}

	public double lambdaMinusG(Set<Agent> agents)
	{
		return sum(n -> n.getLambdaMinus()*n.gMinus(), agents);
	}

	public double lambdaPlusG(Set<Agent> agents)
	{
		return sum(n -> n.getLambdaPlus()*n.gPlus(), agents);
	}

	public double cost(Set<Agent> agents)
	{
		return sum(Agent::cost, agents);
	}

	public int size()
	{
		return xs.size();
	}
	
	public void printCSV(int lineCount, int gDimension)
	{
		int dimension = xs.get(0).getDimension();
		int logFrequency = size()/lineCount;
		int agentCount = epsilonValues.get(0).getDimension();
		
		// Header:
		out.print("k,");
		for (int i = 0; i < dimension; i++)
		{
			out.print("x"+i+",");
		}
		out.print("c(x),L,");
		for (int i = 0; i < gDimension; i++)
		{
			out.print("g"+i+"(x),");
		}
		out.print("alpha,");
		for (int i = 0; i < agentCount; i++)
		{
			out.print("epsilon"+i+",");
		}
		for (int i = 0; i < dimension; i++)
		{
			out.print("grad_x"+i+" L(x),");
		}
		for (int i = 0; i < gDimension; i++)
		{
			out.print("lambda"+i+",");
		}
		for (int i = 0; i < agentCount; i++)
		{
			out.print("~h"+i+",");
		}
		out.print("av[h(x)]");
		out.println();
		
		// Data:
		int k = 0;
		for (RealVector x : xs)
		{
			if(k%logFrequency == 0)
			{
				out.print(k+",");
				for (int i = 0; i < dimension; i++)
				{
					out.print(x.getEntry(i)+",");
				}
				out.print(costValues.get(k)+",");
				out.print(lagrangeValues.get(k)+",");
				RealVector g = gValues.get(k);
				for (int i = 0; i < gDimension; i++)
				{
					out.print(g.getEntry(i)+",");
				}
				out.print(alphaValues.get(k)+",");
				RealVector epsilon = epsilonValues.get(k);
				for (int i = 0; i < agentCount; i++)
				{
					out.print(epsilon.getEntry(i)+",");
				}
				
				RealVector grad = gradientValues.get(k);
				for (int i = 0; i < dimension; i++)
				{
					out.print(grad.getEntry(i)+",");
				}
				RealVector lambda = lambdaValues.get(k);
				for (int i = 0; i < gDimension; i++)
				{
					out.print(lambda.getEntry(i)+",");
				}
				RealVector h = hValues.get(k);
				for (int i = 0; i < agentCount; i++)
				{
					out.print(h.getEntry(i)+",");
				}
				out.print(trueH.get(k));
				
				out.println();
			}
			
			++k;
		}
	}

	/**
	 * Returns a non-negative number representing the convergence of the algorithm.
	 * Zero represents full convergence, larger numbers represent larger distances
	 * from the solution.
	 * @return Convergence measure.
	 */
	public double convergence(double baseVoltage, double basePower) 
	{
		int K = size();
		int lastIndex = K-1;
		
		// Distance from final state at 80% through iterations:
		RealVector finalState = xs.get(lastIndex);
		RealVector _80State = xs.get((int)(K*0.8));
		double 	stateConvergence = map( // scale according to base voltage and power
					_80State.subtract(finalState), 
					(int i, double d) -> {switch(i%3)
					{
					case 0:
					case 1:
						return d/baseVoltage;
					case 2:
						return d/basePower;
					default: // invalid case since i%3 \in {0, 1, 2}
						return 0.0;
					}})
				.getNorm();
		
		// Gradient norm:
		double finalGradNorm = gradientValues.get(lastIndex).getNorm()/alphaValues.get(lastIndex);
		
		// g(x) values:
		double finalG = gValues.get(lastIndex).getNorm();
		if(Double.isNaN(finalG) || Double.isNaN(finalGradNorm) || Double.isNaN(stateConvergence))
			return Double.MAX_VALUE;
		return new ArrayRealVector(new double[]{stateConvergence, finalGradNorm, finalG}).getNorm();
	}
}