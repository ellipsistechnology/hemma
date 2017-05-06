package ellipsis.hemma;

import static ellipsis.hemma.HEMMAProtocol.HEMMAState.Idle;
import static ellipsis.hemma.HEMMAProtocol.HEMMAState.SessionInitialisation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

/**
 * Implementation of the HEMMA protocol.
 * @author bmillar
 *
 */
public class HEMMAProtocol
{	
	public static enum HEMMAState
	{
		Idle,
		SessionComplete,
		SessionInitialisation,
		SessionExecution
	}
	
	public static enum HEMMAMessageType
	{
		DiscoverNeighbours,
		IdentifyNeighbour,
		StartSession,
		CancelSession,
		FinishSession,
		VariableUpdate,
		
		StartSession_reject,
		StartSession_accepted,
		FinishSession_reject,
		FinishSession_accepted
	}
	
	public static class HEMMAMessage
	{
		HEMMAProtocol source;
		HEMMAMessageType type;
		int ttl;
		int sessionId;
		
		public HEMMAMessage(HEMMAProtocol source, HEMMAMessageType type)
		{
			this.source = source;
			this.type = type;
		}
		
		public HEMMAMessage(HEMMAProtocol source, HEMMAMessageType type, int ttl)
		{
			this(source, type);
			this.ttl = ttl;
		}
		
		public HEMMAMessage(HEMMAProtocol source, HEMMAMessageType type, int ttl, int sessionId)
		{
			this(source, type, ttl);
			this.sessionId = sessionId;
		}
	}
	
	private static PrintStream log;
	{
		try
		{
			File file = new File("/opt/hemma/log/protocol.log");
			file.getParentFile().mkdirs();
			log = new PrintStream(file);
		}
		catch (FileNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private HEMMAState state = Idle;
	private Queue<HEMMAMessage> messageQueue = new LinkedList<>();
	private Set<Agent> connections = new HashSet<Agent>();
	private Set<HEMMAProtocol> neighbours;
	private Agent agent;
	
	public HEMMAProtocol(Agent agent) 
	{
		this.agent = agent;
	}
	
	public void message(HEMMAMessage message) 
	{
		synchronized(messageQueue)
		{
			messageQueue.add(message);
		}
	}
	
	/**
	 * Used to identify an agents neighbours. 
	 * Can only be sent in the Idle state. 
	 * Prior to sending, the agents neighbour 
	 * list is cleared. Upon receiving a 
	 * DN request an agent will immediately respond
	 * with an IN message addressed to the source 
	 * of the DN request and only on the interface 
	 * on which the DN request was received. DN 
	 * requests are never forwarded (sent with TTL=1).
	 * 
	 * Boradcast.
	 */
	public void discoverNeighbours(HEMMAProtocol neighbour)
	{
		// Send IN in response:
		HEMMAMessage message = new HEMMAMessage(this, HEMMAMessageType.IdentifyNeighbour, 1);
		neighbour.message(message);
	}
	
	/**
	 * Used to inform neighbouring agents of the senders 
	 * presence. Upon receiving an IN message the sender 
	 * will be added to the recipients neighbour list. 
	 * IN requests are never forwarded (sent with TTL=1).
	 * 
	 * Single recipient.
	 */
	public void identifyNeighbour(HEMMAProtocol neighbour)
	{
		neighbours.add(neighbour);
	}
	
	/**
	 * Request that a new optimisation session begin. 
	 * This can only be sent if the agent is not currently 
	 * in an optimisation session. Each new request will 
	 * be associated with a randomly generated unique identifier. 
	 * Optimisation will begin once all neighbours have 
	 * accepted the SS request.
	 * 
	 * Neighbours.
	 */
	public void startSession(int sessionId)
	{
		switch(state)
		{
		case Idle:
			state = SessionInitialisation;
		case SessionInitialisation:
			// TODO check if current session is superceded and accept or reject accordingly
			// TODO send SS on to neighbours if accepting the request
			// TODO send accept response if accepting the request
		default:
			break;
		}
	}
	
	/**
	 * Cancel the optimisation associated with a particular 
	 * session ID.
	 * 
	 * Broadcast.
	 */
	public void cancelSession(int sessionId)
	{
		
	}
	
	/**
	 * Request that the session be completed. The optimisation 
	 * will be concluded once all neighbours have accepted the 
	 * FS request.
	 * 
	 * Neighbours.
	 */
	public void finishSession()
	{
		
	}
	
	/**
	 * Exchange state variables with a neighbour. The 
	 * senders optimisation state variables are 
	 * attached to the request and the receiver will 
	 * attach its variables to the response.
	 * 
	 * Single recipient.
	 */
	public void variableUpdate()
	{
		
	}
	
	Random rand = new Random(0);
	public void execute()
	{
		// TODO update variables form neighbours
		
		do
		{
			// State machine:
			switch (state)
			{
			case Idle:
				// TODO handle state change to trigger a session start request
				//startSession(rand.nextInt());
				break;
			case SessionExecution:
				// TODO handle completion criteria met
				// state = HEMMAState.SessionComplete;
				// TODO send FS to neighbours
				break;
			default:
				throw new RuntimeException("Unsupported state found during state engine execution: "+state);
			}
			
			// Process incoming messages: 
			HEMMAMessage message;
			synchronized(messageQueue)
			{
				message = messageQueue.poll();
			}
			if(message != null)
			{
				log.println(message.type+" received from "+message.source);
				
				switch (message.type)
				{
				case DiscoverNeighbours:
					discoverNeighbours(message.source);
					break;
				case CancelSession:
					cancelSession(message.sessionId);
					break;
				case FinishSession:
					
					break;
				case FinishSession_accepted:
					
					break;
				case FinishSession_reject:
					
					break;
				case IdentifyNeighbour:
					identifyNeighbour(message.source);
					break;
				case StartSession:
					
					break;
				case StartSession_accepted:
					
					break;
				case StartSession_reject:
					
					break;
				case VariableUpdate:
					
					break;
				default:
					break;
				}
			}
		} while(!messageQueue.isEmpty());
	}

	public void init()
	{
		if(state == Idle)
		{
			neighbours = new HashSet<HEMMAProtocol>();
			HEMMAMessage message = new HEMMAMessage(this, HEMMAMessageType.DiscoverNeighbours, 1);
			broadcast(message);
		}
	}

	protected void broadcast(HEMMAMessage message) 
	{
		for (Agent agent : connections) 
		{
			agent.getHemmaProtocol().message(message);
		}
	}

	/**
	 * Physically connects this agent to another agent.
	 * @param neighbour
	 */
	public void connectToNeighbour(Agent neighbour) 
	{
		connections.add(neighbour);
	}
	
	// FIXME this should be sending cached data
	public Set<Agent> neighbourSet()
	{
		return connections;
	}
}
