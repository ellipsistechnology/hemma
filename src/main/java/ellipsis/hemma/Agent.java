package ellipsis.hemma;

import static ellipsis.common.math.Sum.sum;
import static ellipsis.common.math.Sum.sumV;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.linear.RealVector;

public abstract class Agent
{
	private String name;
	
	// Electrical variables:
	private double vMax, vMin, currentMax;
	private double vMinus, v, current;
	private boolean grounded = false;
	private double ySum;
	private Map<Agent, Double> conductances = new HashMap<>();
	
	// Optimisation variables:
	private double lambdaPlus, lambdaMinus, lambdaMax;
	private double alpha, alphaMax, alphaMultiplier;
	private double equalityConstraintTarget = 1e-6;

	protected AgentCommunicator communicator;
	
	
	//// Constructor ////
	
	public Agent()
	{
		communicator = new AgentCommunicator();
	}
	
	
	//// Optimisation ////
	
//	public double lagrange()
//	{
//		return 
//				cost() + 
//				gPlus()*lambdaPlus + 
//				gMinus()*lambdaMinus +
//				sum(n -> n.gPlus()*n.getLambdaPlus(), communicator.neighbourSet()) +
//				sum(n -> n.gMinus()*n.getLambdaMinus(), communicator.neighbourSet()) +
//				(alpha/2.0)*sum(n -> n.gPlus()*n.gPlus(), communicator.neighbourSet()) +
//				(alpha/2.0)*sum(n -> n.gMinus()*n.gMinus(), communicator.neighbourSet());
//	}

	public abstract double gPlus();

	public abstract double gMinus();
	
	public abstract RealVector gPlusGradient(Agent wrt);
	
	public abstract RealVector gMinusGradient(Agent wrt);

	public abstract double cost();
	
	public abstract RealVector costGradient(Agent wrt);
	
	public RealVector gradient()
	{
		RealVector costGrad = costGradient(this);
		RealVector neighbourCostGradients = sumV(n -> n.costGradient(Agent.this), communicator.neighbourSet(), 3);
		
		RealVector gPlusGrad = gPlusGradient(this).mapMultiply(lambdaPlus + alpha*gPlus());
		RealVector neighbourGPlusGrad = sumV(n -> n.gPlusGradient(Agent.this).mapMultiply(n.getLambdaPlus() + n.getAlpha()*n.gPlus()), communicator.neighbourSet(), 3);
		
		RealVector gMinusGrad = gMinusGradient(this).mapMultiply(lambdaMinus + alpha*gMinus());
		RealVector neighbourGMinusGrad = sumV(n -> n.gMinusGradient(Agent.this).mapMultiply(n.getLambdaMinus() + n.getAlpha()*n.gMinus()), communicator.neighbourSet(), 3);
		
		RealVector grad = 
				costGrad.add(neighbourCostGradients).add(
				gPlusGrad).add(neighbourGPlusGrad).add(
				gMinusGrad).add(neighbourGMinusGrad);
		
		if(grounded)
			grad.setEntry(1, 0.0); // [v, v-, I]
		
		return grad;
	}
	
	public boolean project()
	{
		boolean projected = false;
		
		// V:
		if(v < vMin)
		{
			setV(vMin);
			projected = true;
		}
		else if(v > vMax)
		{
			setV(vMax);
			projected = true;
		}
		
		// V-
		if(vMinus < 0)
		{
			setvMinus(0);
			projected = true;
		}
		
		// Current:
		if(current < -currentMax)
		{
			setCurrent(-currentMax);
			projected = true;
		}
		else if(current > currentMax)
		{
			setCurrent(currentMax);
			projected = true;
		}
		
		return projected;
	}

	public void minimise()
	{
		RealVector grad = gradient();
		double stepSize = backtrack(grad);
		RealVector step = grad.mapMultiply(stepSize);
		
		setV(getV() + step.getEntry(0));
		setvMinus(getvMinus() + step.getEntry(1));
		setCurrent(getCurrent() + step.getEntry(2));
	}

	private double backtrack(RealVector grad)
	{
		return 0; // TODO
	}


	public void stepLambda()
	{
		lambdaPlus += alpha*gPlus();
		lambdaMinus += alpha*gMinus();
		
		if(lambdaPlus > lambdaMax)
			lambdaPlus = lambdaMax;
		else if(lambdaPlus < -lambdaMax)
			lambdaPlus = -lambdaMax;
		
		if(lambdaMinus > lambdaMax)
			lambdaMinus = lambdaMax;
		else if(lambdaMinus < -lambdaMax)
			lambdaMinus = -lambdaMax;
	}
	
	public void stepAlpha()
	{
		if(Math.abs(gPlus()) > equalityConstraintTarget && Math.abs(gMinus()) > equalityConstraintTarget)
			alpha = alpha*alphaMultiplier;
		
		if(alpha > alphaMax)
			alpha = alphaMax;
	}

	
	//// Neighbour Management ////
	
    public void addNeighbour(Agent neighbour, double conductance)
    {
        ySum = 0;
        conductances.put(neighbour, conductance);
        communicator.addNeighbour(neighbour);
    }
    
    public double conductance(Agent neighbour)
    {
        Double y_ij = conductances.get(neighbour);
		return y_ij == null ? 0.0 : y_ij;
    }
    
    public double conductanceSum()
    {
        if(ySum == 0)
        {
            ySum = sum(neighbour -> conductance(neighbour), neighbours());
        }
        return ySum;
    }
    
    public Iterable<Agent> neighbours()
    {
        return conductances.keySet();
    }
    
    public int neighbourCount()
    {
        return conductances.size();
    }
    
    
    //// Getters/Setters ////

	public double getLambdaPlus()
	{
		return lambdaPlus;
	}

	public void setLambdaPlus(double lambdaPlus)
	{
		this.lambdaPlus = lambdaPlus;
	}

	public double getLambdaMinus()
	{
		return lambdaMinus;
	}

	public void setLambdaMinus(double lambdaMinus)
	{
		this.lambdaMinus = lambdaMinus;
	}
	public double getvMax()
	{
		return vMax;
	}
	public void setvMax(double vMax)
	{
		this.vMax = vMax;
	}
	public double getvMin()
	{
		return vMin;
	}
	public void setvMin(double vMin)
	{
		this.vMin = vMin;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public double getvMinus()
	{
		return vMinus;
	}
	public void setvMinus(double vMinus)
	{
		this.vMinus = vMinus;
	}
	public double getV()
	{
		return v;
	}
	public void setV(double v)
	{
		this.v = v;
	}
	public double getCurrent()
	{
		return current;
	}
	public void setCurrent(double current)
	{
		this.current = current;
	}
	public double getCurrentMax()
	{
		return currentMax;
	}
	public void setCurrentMax(double currentMax)
	{
		this.currentMax = currentMax;
	}
	public boolean isGrounded()
	{
		return grounded;
	}
	public void setGrounded(boolean grounded)
	{
		this.grounded = grounded;
	}


	public double getLambdaMax()
	{
		return lambdaMax;
	}


	public void setLambdaMax(double lambdaMax)
	{
		this.lambdaMax = lambdaMax;
	}


	public double getAlpha()
	{
		return alpha;
	}


	public void setAlpha(double alpha)
	{
		this.alpha = alpha;
	}


	public double getAlphaMax()
	{
		return alphaMax;
	}


	public void setAlphaMax(double alphaMax)
	{
		this.alphaMax = alphaMax;
	}


	public double getAlphaMultiplier()
	{
		return alphaMultiplier;
	}


	public void setAlphaMultiplier(double alphaMultiplier)
	{
		this.alphaMultiplier = alphaMultiplier;
	}
}