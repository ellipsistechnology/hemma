package ellipsis.hemma;

import static ellipsis.common.math.Sum.sum;
import static ellipsis.common.math.VectorHelper.vector;

import org.apache.commons.math3.linear.RealVector;

public class ConstantCurrentAgent extends Agent
{
	public ConstantCurrentAgent(double current)
	{
		super.setCurrent(current);
	}
	
	@Override
	public void setCurrent(double current)
	{
		// Cannot change current.
	}

	@Override
	public double cost()
	{
		return 0;
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
		return vector(0.0, 0.0, 0.0);
	}
	
	@Override
	public RealVector gPlusGradient(Agent wrt)
	{
		if(wrt == this)
			return vector(conductanceSum(), conductanceSum(), 0.0);
		else
			return vector(-conductance(wrt), -conductance(wrt), 0.0);
	}
	
	@Override
	public RealVector gMinusGradient(Agent wrt)
	{
		if(wrt == this)
			return vector(0.0, -conductanceSum(), 0.0);
		else
			return vector(0.0, conductance(wrt), 0.0);
	}
}