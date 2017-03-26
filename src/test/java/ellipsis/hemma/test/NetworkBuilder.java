package ellipsis.hemma.test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import ellipsis.hemma.Agent;
import ellipsis.hemma.ConstantCurrentAgent;
import ellipsis.hemma.ConstantPowerAgent;
import ellipsis.hemma.VoltageControlledAgent;

public class NetworkBuilder
{
	private static final double RESISTIVITY = 0.0132; // ohms/m
    private final static double PS_VOLTAGE = 12;
    private static final double PS_VOLTAGE_MIN = PS_VOLTAGE*0.9;
    private static final double PS_VOLTAGE_MAX = PS_VOLTAGE*1.1;
//    private static final double PS_CURRENT_MAX = 10;
    
    private final static double LED_VOLTAGE = 12;
    private final static double LED_VOLTAGE_MIN = 9;
    private final static double LED_VOLTAGE_MAX = 15;
    
    private final static double CHARGER_VOLTAGE = 12;
    private final static double CHARGER_VOLTAGE_MIN = 10;
    private final static double CHARGER_VOLTAGE_MAX = 18;
    
    private final static double MAX_POWER = 100;
    
	private HashMap<String, Agent> agents = new LinkedHashMap<>();
	private Agent context;
	
	public Set<Agent> agentSet()
	{
		return new LinkedHashSet<>(agents.values());
	}
	
	public NetworkBuilder makeCPAgent(String name, double power)
    {
        Agent cp = new ConstantPowerAgent(power); // Watts (load)
        cp.setName(name);
        cp.setvMax(CHARGER_VOLTAGE_MAX);
        cp.setvMin(CHARGER_VOLTAGE_MIN);
        cp.setV(CHARGER_VOLTAGE);
        cp.setvMinus(0.0);
        cp.setPowerMax(MAX_POWER);
        
        agents.put(name, cp);
        context = cp;
        
        return this;
    }

	public NetworkBuilder makeCCAgent(String name, double current)
    {
        Agent cc = new ConstantCurrentAgent(current); // Amps (load)
        cc.setName(name);
        cc.setvMax(LED_VOLTAGE_MAX);
        cc.setvMin(LED_VOLTAGE_MIN);
        cc.setV(LED_VOLTAGE);
        cc.setvMinus(0.0);
        cc.setPowerMax(MAX_POWER);
        
        agents.put(name, cc);
        context = cc;
        
        return this;
    }

	public NetworkBuilder makeVCAgent(String name, double maxPower)
    {
        Agent vc = new VoltageControlledAgent();
        vc.setName(name);
        vc.setvMax(PS_VOLTAGE_MAX);
        vc.setvMin(PS_VOLTAGE_MIN);
        vc.setV(PS_VOLTAGE);
        vc.setvMinus(0.0);
        vc.setPowerMax(maxPower);
        
        agents.put(name, vc);
        context = vc;
        
        return this;
    }
	
	public void link(String name1, String name2, double length)
    {
		Agent a1 = agents.get(name1); 
		Agent a2 = agents.get(name2);
		double resistance = RESISTIVITY*length;
        double conductance = 1.0/resistance;
        a1.addNeighbour(a2, conductance);
        a2.addNeighbour(a1, conductance);
    }

	public NetworkBuilder setGrounded(boolean b)
	{
		context.setGrounded(b);
		return this;
	}
}