package ellipsis.hemma;

import org.apache.commons.math3.linear.RealVector;

public interface IAgent 
{
	String getName();
	
	/**
	 * @return g_i^+(x)
	 */
	double gPlus();

	/**
	 * @return g_i^-(x)
	 */
	double gMinus();

	/**
	 * @return \nabla_{x_i} g_i^+(x)
	 */
	RealVector gPlusGradient(IAgent wrt);

	/**
	 * @return \nabla_{x_i} g_i^-(x)
	 */
	RealVector gMinusGradient(IAgent wrt);
	
	/**
	 * @param wrt With respect to.
	 * @return \nabla_{wrt} c_i(x)
	 */
	RealVector costGradient(IAgent wrt);

	double getLambdaPlus();

    double getLambdaMinus();
    
    double getAlpha();
    
    double getV();
    
    double getvMinus();
    
    double getPower();

//	void updateValues();

	HEMMAProtocol getHemmaProtocol();
}