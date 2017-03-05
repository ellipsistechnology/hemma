package ellipsis.hemma;

import java.util.HashSet;
import java.util.Set;

public class AgentCommunicator
{
	private Set<Agent> neighbours = new HashSet<>();
	private Set<Agent> neighbourCache;

	public AgentCommunicator()
	{
		
	}

	public void addNeighbour(Agent neighbour)
	{
		neighbours.add(neighbour);
		neighbourCache = null;
	}

	/**
	 * 
	 * @return Cached values from the neighbours.
	 */
	public Set<Agent> neighbourSet()
	{
		// TODO make copies each iteration to simulate communication
		if(neighbourCache == null)
		{
			neighbourCache = new HashSet<>(neighbours);
		}
		return neighbourCache;
	}
}