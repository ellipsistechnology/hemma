package ellipsis.hemma;

import static ellipsis.common.math.Sum.sum;
import static ellipsis.common.math.Sum.sumV;
import static ellipsis.common.math.VectorHelper.vector;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.linear.RealVector;

public abstract class Agent implements IAgent
{
	private String name;
	
	// Electrical variables:
	private double vMax, vMin, powerMax;
	private double vMinus, v, power;
	private boolean grounded = false;
	private double ySum;
	private Map<IAgent, Double> conductances = new HashMap<>();
	
	// Optimisation variables:
	private double lambdaPlus, lambdaMinus, lambdaMax, lambdaMultiplier = 1.0;
	private double alpha, alphaMax, alphaMultiplier;
	private double epsilon, epsilonMultiplier;

	// HEMMA protocol variables:
//	protected AgentCommunicator communicator;
	protected HEMMAProtocol hemmaProtocol;
	
	
	//// Initialisation ////
	
	public Agent()
	{
//		communicator = new AgentCommunicator();
		hemmaProtocol = new HEMMAProtocol(this);
	}
	
	public void switchOn()
	{
		hemmaProtocol.init();
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

	/**
	 * @return c_i(x)
	 */
	public abstract double cost();
	
	/**
	 * Lagrange gradient with respect to this agent's state: [v, v-, p].
	 * @return \nabla_i L(x,\lambda)
	 */
	public RealVector gradient()
	{
		// Cost gradients:
		// \nabla_i \sum c_i(x)
		RealVector costGrad = costGradient(this);
		RealVector neighbourCostGradients = sumV(n -> n.costGradient(Agent.this), hemmaProtocol.neighbourSet(), 3); // this will always return zero now since the cost only uses local variables
		
		// Penalty gradients for positive power flow:
		// \nabla_i \sum \nabla_i g^+_i(x) (\lambda^+ + \alpha g^+_i(x))
		RealVector gPlusGrad = gPlusGradient(this).mapMultiply(lambdaPlus + alpha*gPlus());
		RealVector neighbourGPlusGrad = sumV(n -> n.gPlusGradient(Agent.this).mapMultiply(n.getLambdaPlus() + n.getAlpha()*n.gPlus()), hemmaProtocol.neighbourSet(), 3);

		// Penalty gradients for negative power flow:
		// \nabla_i \sum \nabla_i g^-_i(x) (\lambda^+ + \alpha g^-_i(x))
		RealVector gMinusGrad = gMinusGradient(this).mapMultiply(lambdaMinus + alpha*gMinus());
		RealVector neighbourGMinusGrad = sumV(n -> n.gMinusGradient(Agent.this).mapMultiply(n.getLambdaMinus() + n.getAlpha()*n.gMinus()), hemmaProtocol.neighbourSet(), 3);
		
		RealVector grad = 
				costGrad.add(neighbourCostGradients).add(
				gPlusGrad).add(neighbourGPlusGrad).add(
				gMinusGrad).add(neighbourGMinusGrad);
		
		// If grounded then set v- = 0:
		if(grounded)
			grad.setEntry(1, 0.0); // [v, v-, p]

		return grad;
	}
	
	/**
	 * Project the current state of this agent back into the feasible set.
	 * x_i := P_X{x_i} (14)
	 * @return
	 */
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
		
		// Power:
		if(power < -powerMax)
		{
			setPower(-powerMax);
			projected = true;
		}
		else if(power > powerMax)
		{
			setPower(powerMax);
			projected = true;
		}
		
		return projected;
	}

	/**
	 * TODO Currently this is handled by the TeseCase.
	 */
	public void minimise()
	{
		RealVector grad = gradient();
		double stepSize = backtrack(grad);
		RealVector step = grad.mapMultiply(stepSize);
		
		setV(getV() + step.getEntry(0));
		setvMinus(getvMinus() + step.getEntry(1));
		setPower(getPower() + step.getEntry(2));
	}

	/**
	 * TODO Currently handled by the TestCase.
	 * @param grad
	 * @return
	 */
	private double backtrack(RealVector grad)
	{
		return 0; // TODO
	}

	/**
	 * Move Lambda to increase the penalty. (15)
	 * \lambda^+_i := \lambda^+_i + \alpha g^+(x)
	 * \lambda^-_i := \lambda^-_i + \alpha g^-(x)
	 */
	public void stepLambda()
	{
		lambdaPlus += lambdaMultiplier*alpha*gPlus();
		lambdaMinus += lambdaMultiplier*alpha*gMinus();
		
		if(lambdaPlus > lambdaMax)
			lambdaPlus = lambdaMax;
		else if(lambdaPlus < -lambdaMax)
			lambdaPlus = -lambdaMax;
		
		if(lambdaMinus > lambdaMax)
			lambdaMinus = lambdaMax;
		else if(lambdaMinus < -lambdaMax)
			lambdaMinus = -lambdaMax;
	}
	
	/**
	 * Increase penalty multiplier.
	 */
	public void stepAlpha()
	{
		alpha = alpha*alphaMultiplier;
		
		if(alpha > alphaMax)
			alpha = alphaMax;
	}
	
	/**
	 * Decrease the target for the gradient and step.
	 */
	public void stepEpsilon()
	{
		epsilon *= epsilonMultiplier;
	}

	
	//// Neighbour Management ////
	
    public void addNeighbour(IAgent neighbour, double conductance)
    {
        ySum = 0;
        conductances.put(neighbour, conductance);
        hemmaProtocol.connectToNeighbour(neighbour);
    }
    
    public double conductance(IAgent neighbour)
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
    
    public Iterable<IAgent> neighbours()
    {
        return conductances.keySet();
    }
    
    public int neighbourCount()
    {
        return conductances.size();
    }
    
    
    //// Getters/Setters ////
    
    @Override
    public HEMMAProtocol getHemmaProtocol() 
    {
		return hemmaProtocol;
	}

    @Override
	public double getLambdaPlus()
	{
		return lambdaPlus;
	}

	public void setLambdaPlus(double lambdaPlus)
	{
		this.lambdaPlus = lambdaPlus;
	}

    @Override
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
	
	@Override
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	
	@Override
	public double getvMinus()
	{
		return vMinus;
	}
	public void setvMinus(double vMinus)
	{
		this.vMinus = vMinus;
	}
	
	@Override
	public double getV()
	{
		return v;
	}
	public void setV(double v)
	{
		this.v = v;
	}
	public boolean isGrounded()
	{
		return grounded;
	}
	public void setGrounded(boolean grounded)
	{
		this.grounded = grounded;
	}
	
	@Override
	public double getPower()
	{
		return power;
	}
	public void setPower(double power)
	{
		this.power = power;
	}
	public double getPowerMax()
	{
		return powerMax;
	}
	public void setPowerMax(double powertMax)
	{
		this.powerMax = powertMax;
	}


	public double getLambdaMax()
	{
		return lambdaMax;
	}


	public void setLambdaMax(double lambdaMax)
	{
		this.lambdaMax = lambdaMax;
	}

	public void setLambdaMultiplier(double lambdaMultiplier) {
		this.lambdaMultiplier = lambdaMultiplier;
	}
	
	public double getLambdaMultiplier() {
		return lambdaMultiplier;
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
	
	public double getEpsilon()
	{
		return epsilon;
	}
	
	public void setEpsilon(double epsilon)
	{
		this.epsilon = epsilon;
	}
	
	public double getEpsilonMultiplier()
	{
		return epsilonMultiplier;
	}
	
	public void setEpsilonMultiplier(double epsilonMultiplier)
	{
		this.epsilonMultiplier = epsilonMultiplier;
	}

	public RealVector state()
	{
		return vector(v, vMinus, power);
	}
	
	public void setState(RealVector state)
	{
		setV(state.getEntry(0));
		setvMinus(state.getEntry(1));
		setPower(state.getEntry(2));
	}
	
	public boolean updateValues()
	{
		hemmaProtocol.updateValues();
		return hemmaProtocol.initialised();
	}
	
	@Override
	public int hashCode() 
	{
		return name.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) 
	{
		return name.equals(obj);
	}
	
	@Override
	public String toString() 
	{
		return name+"[x="+state()+"|lambda={"+getLambdaPlus()+","+getLambdaMinus()+"}|alpha="+getAlpha()+"]";
	}
}