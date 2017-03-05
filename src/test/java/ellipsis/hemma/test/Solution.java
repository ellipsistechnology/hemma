package ellipsis.hemma.test;

import static ellipsis.common.math.Sum.sum;
import static ellipsis.common.math.VectorHelper.appendVectors;
import static ellipsis.common.math.VectorHelper.vector;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Set;

import org.apache.commons.math3.linear.RealVector;

import ellipsis.hemma.Agent;

public class Solution
{	
	public ArrayList<RealVector> xs = new ArrayList<>();
	public ArrayList<Double> lagrangeValues = new ArrayList<>();
	public ArrayList<Double> costValues = new ArrayList<>();
	public ArrayList<RealVector> gradientValues = new ArrayList<>();
	public ArrayList<RealVector> gValues = new ArrayList<>();
	public ArrayList<Double> epsilonValues = new ArrayList<>();
	public ArrayList<Double> alphaValues = new ArrayList<>();
	public ArrayList<RealVector> lambdaValues = new ArrayList<>();

	public void storeDataPoint(Set<Agent> agents)
	{
		RealVector x = appendVectors(agents, n -> vector(n.getV(), n.getvMinus(), n.getCurrent()));
		xs.add(x);
		lagrangeValues.add(lagrange(agents));
		costValues.add(sum(Agent::cost, agents));
		gradientValues.add(appendVectors(agents, n -> n.gradient()));
		gValues.add(appendVectors(agents, n -> vector(n.gPlus(), n.gMinus())));
		epsilonValues.add(0.0);// FIXME epsilon);
		alphaValues.add(agents.iterator().next().getAlpha());
		lambdaValues.add(appendVectors(agents, n -> vector(n.getLambdaPlus(), n.getLambdaMinus())));
	}
	
	public Double lagrange(Set<Agent> agents)
	{
		return 
			sum(Agent::cost, agents) + 
			sum(n -> n.getLambdaPlus()*n.gPlus(), agents) +
			sum(n -> n.getLambdaMinus()*n.gMinus(), agents) +
			sum(n -> n.getAlpha()*n.gPlus()*n.gPlus()/2.0, agents) +
			sum(n -> n.getAlpha()*n.gMinus()*n.gMinus()/2.0, agents);
	}

	public int size()
	{
		return xs.size();
	}
	
	public void printCSV(int lineCount, int gDimension)
	{
		PrintStream out = System.out;
		int dimension = xs.get(0).getDimension();
		int logFrequency = size()/lineCount;
		
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
		out.print("alpha,epsilon,");
		for (int i = 0; i < dimension; i++)
		{
			out.print("grad_x"+i+" L(x),");
		}
		for (int i = 0; i < gDimension; i++)
		{
			out.print("lambda"+i+",");
		}
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
				out.print(alphaValues.get(k)+","+epsilonValues.get(k)+",");
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
				
				out.println();
			}
			
			++k;
		}
	}
}