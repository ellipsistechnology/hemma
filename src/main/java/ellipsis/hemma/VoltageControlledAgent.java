package ellipsis.hemma;

import static ellipsis.common.math.Sum.sum;
import static ellipsis.common.math.VectorHelper.vector;

import org.apache.commons.math3.linear.RealVector;

public class VoltageControlledAgent extends Agent
{
	private static final double A = 1.0e-2;
	
	@Override
	public AgentType getType() 
	{
		return IAgent.AgentType.VC;
	}
	
	@Override
	public double cost()
	{
		double p = getPower();
		return A*p*p;
	}

	@Override
	public double gPlus()
	{
		double vMinus = getvMinus();
		double v = getV();
		return v*sum(n -> (v + vMinus - n.getvMinus() - n.getV())*conductance(n), hemmaProtocol.neighbourSet()) - getPower();
	}

	@Override
	public double gMinus()
	{
		double vMinus = getvMinus();
		double v = getV();
		return v*sum(n -> (n.getvMinus() - vMinus)*conductance(n), hemmaProtocol.neighbourSet()) - getPower();
	}
	
	@Override
	public RealVector costGradient(IAgent wrt)
	{
		if(wrt == this)
		{
			double p = getPower();
			return vector(0.0, 0.0, 2*A*p);
		} 
		else
			return vector(0.0, 0.0, 0.0); // FIXME this should never be called
	}
	
	@Override
	public RealVector gPlusGradient(IAgent wrt)
	{
//		if(wrt == this)
//			return vector(conductanceSum(), conductanceSum(), -1.0);
//		else
//			return vector(-conductance(wrt), -conductance(wrt), 0.0);
		if(wrt == this)
		{
			double sum = 2*getV()*conductanceSum() + sum(n -> (getvMinus()-n.getvMinus()-n.getV())*conductance(n), hemmaProtocol.neighbourSet());
			return vector(sum, getV()*conductanceSum(), -1.0);
		} 
		else
			return vector(-getV()*conductance(wrt), -getV()*conductance(wrt), 0.0); // FIXME this should never be called
	}
	
	@Override
	public RealVector gMinusGradient(IAgent wrt)
	{
//		if(wrt == this)
//			return vector(0.0, -conductanceSum(), -1.0);
//		else
//			return vector(0.0, conductance(wrt), 0.0);
		if(wrt == this)
		{
			double sum = sum(n -> (n.getvMinus()-getvMinus())*conductance(n), hemmaProtocol.neighbourSet());
			return vector(sum, -getV()*conductanceSum(), -1.0);
		}
		else
			return vector(0.0, getV()*conductance(wrt), 0.0);
	}
}