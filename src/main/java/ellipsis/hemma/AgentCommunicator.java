package ellipsis.hemma;

import java.util.HashSet;
import java.util.Set;

public class AgentCommunicator
{
	private Set<IAgent> neighbours = new HashSet<>(); // physical link to neighbours
	private Set<IAgent> neighbourCache;

	public AgentCommunicator()
	{
		
	}

	public void addNeighbour(IAgent neighbour)
	{
		neighbours.add(neighbour);
		neighbourCache = null;
		
		// TODO if this is the first neighbour then we are connected to a network
		// for the first time and need to send out a neighbour discovery request:
	}

	/**
	 * 
	 * @return Cached values from the neighbours.
	 */
	public Set<IAgent> neighbourSet()
	{
		// TODO make copies each iteration to simulate communication
		if(neighbourCache == null)
		{
			neighbourCache = new HashSet<>(neighbours);
		}
		return neighbourCache;
	}
}