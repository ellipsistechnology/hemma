package ellipsis.hemma;


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

	double getLambdaPlus();

    double getLambdaMinus();
    
    double getAlpha();
    
    double getV();
    
    double getvMinus();
    
    double getPower();

//	void updateValues();

	HEMMAProtocol getHemmaProtocol();
}