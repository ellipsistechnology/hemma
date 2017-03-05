package ellipsis.hemma;

import static ellipsis.common.math.Sum.sum;
import static ellipsis.common.math.VectorHelper.vector;

import org.apache.commons.math3.linear.RealVector;

public class VoltageControlledAgent extends Agent
{
	@Override
	public double cost()
	{
		double v = getV();
		double I = getCurrent();
		return v*v + I*I;
	}

	@Override
	public double gPlus()
	{
		double vMinus = getvMinus();
		double v = getV();
		return sum(n -> (v + vMinus - n.getvMinus() - n.getV())*conductance(n), communicator.neighbourSet()) - getCurrent();
	}

	@Override
	public double gMinus()
	{
		double vMinus = getvMinus();
		return sum(n -> (n.getvMinus() - vMinus)*conductance(n), communicator.neighbourSet()) - getCurrent();
	}
	
	@Override
	public RealVector costGradient(Agent wrt)
	{
		if(wrt == this)
		{
			double I = getCurrent();
			double v = getV();
			return vector(2*v, 0.0, 2*I);
		} 
		else
			return vector(0.0, 0.0, 0.0);
	}
	
	@Override
	public RealVector gPlusGradient(Agent wrt)
	{
		if(wrt == this)
			return vector(conductanceSum(), conductanceSum(), -1.0);
		else
			return vector(-conductance(wrt), -conductance(wrt), 0.0);
	}
	
	@Override
	public RealVector gMinusGradient(Agent wrt)
	{
		if(wrt == this)
			return vector(0.0, -conductanceSum(), -1.0);
		else
			return vector(0.0, conductance(wrt), 0.0);
	}
}