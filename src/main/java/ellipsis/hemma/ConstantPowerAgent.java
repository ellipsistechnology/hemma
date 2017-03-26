package ellipsis.hemma;

import static ellipsis.common.math.Sum.sum;
import static ellipsis.common.math.VectorHelper.vector;

import org.apache.commons.math3.linear.RealVector;

public class ConstantPowerAgent extends Agent
{
	private double constantPower;
	
	public ConstantPowerAgent(double power)
	{
		this.constantPower = power;
	}

	@Override
	public double gPlus()
	{
		double vMinus = getvMinus();
		double v = getV();
		return v*sum(n -> (v + vMinus - n.getvMinus() - n.getV())*conductance(n), communicator.neighbourSet()) - constantPower;
	}

	@Override
	public double gMinus()
	{
		double vMinus = getvMinus();
		double v = getV();
		return v*sum(n -> (n.getvMinus() - vMinus)*conductance(n), communicator.neighbourSet()) - constantPower;
	}

	@Override
	public double cost()
	{
		return 0;
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
		{
			double sum = 2*getV()*conductanceSum() + sum(n -> (getvMinus()-n.getvMinus()-n.getV())*conductance(n), communicator.neighbourSet());
			return vector(sum, getV()*conductanceSum(), 0.0);
		} 
		else
			return vector(-getV()*conductance(wrt), -getV()*conductance(wrt), 0.0);
	}
	
	@Override
	public RealVector gMinusGradient(Agent wrt)
	{
		if(wrt == this)
		{
			double sum = sum(n -> (n.getvMinus()-getvMinus())*conductance(n), communicator.neighbourSet());
			return vector(sum, -getV()*conductanceSum(), 0.0);
		}
		else
			return vector(0.0, getV()*conductance(wrt), 0.0);
	}
}