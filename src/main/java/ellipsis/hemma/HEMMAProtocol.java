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

import javax.naming.OperationNotSupportedException;

import org.apache.commons.math3.linear.RealVector;

/**
 * Implementation of the HEMMA protocol.
 * @author bmillar
 *
 */
public class HEMMAProtocol
{	
	static class AgentCache implements IAgent 
	{
		private Object[] params;
		private String name;

		AgentCache(String name, Object[] params) 
		{
			this.params = params;
			this.name = name;
		}

		public String getName() { return name; }
		public int hashCode() { return name.hashCode(); }
		public boolean equals(Object obj) { return name.equals(obj); }

		public double getV()           { return (double) params[0]; }
		public double getvMinus()      { return (double) params[1]; }
		public double getPower()       { return (double) params[2]; }

		public double getLambdaPlus()  { return (double) params[3]; }
		public double getLambdaMinus() { return (double) params[4]; }

		public double getAlpha()       { return (double) params[5]; }

		public double     gPlus()                    { return (double) params[6]; }
		public double     gMinus()                   { return (double) params[7]; }

		public RealVector costGradient(IAgent wrt)   { return (RealVector) params[8]; } // TODO may need to check that this is only called with appropriate wrt

		public RealVector gPlusGradient(IAgent wrt)  { return (RealVector) params[9]; }
		public RealVector gMinusGradient(IAgent wrt) { return (RealVector) params[10]; }

		public HEMMAProtocol getHemmaProtocol() { throw new RuntimeException(new OperationNotSupportedException()); }
	}

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
		FinishSession_accepted,
		VariableUpdate_response
	}
	
	public static class HEMMAMessage
	{
		HEMMAProtocol source;
		HEMMAMessageType type;
		//int ttl;
		//int sessionId;
		Object[] parameters;
		
		public HEMMAMessage(HEMMAProtocol source, HEMMAMessageType type)
		{
			this.source = source;
			this.type = type;
		}
		
//		public HEMMAMessage(HEMMAProtocol source, HEMMAMessageType type, int ttl)
//		{
//			this(source, type);
//			this.ttl = ttl;
//		}
		
		public HEMMAMessage(HEMMAProtocol source, HEMMAMessageType type, /*int ttl, */Object... params)
		{
			this(source, type/*, ttl*/);
			this.parameters = params;
		}
		
		@Override
		public String toString() 
		{
			return "["+type+":"+source.agent.getName()+"]";
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
	private Set<IAgent> connections = new HashSet<>(); // 'Physical connections'.
	private Set<HEMMAProtocol> neighbours = new HashSet<>(); // Discovered neighbours.
	private Set<IAgent> neighbourCache = new HashSet<>(); // Cached values from variable updates.
	private IAgent agent;
	
	public HEMMAProtocol(IAgent agent)
	{
		this.agent = agent;
	}
	
	public HEMMAMessage message(HEMMAMessage message) 
	{
		switch(message.type)
		{
		// Synchronous messages:
		case VariableUpdate:
			return variableUpdate(message);

		// Asynchronous messages:
		default:
			synchronized(messageQueue)
			{
				messageQueue.add(message);
			}
			return null;
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
		log("DN message received form "+neighbour.agent.getName());
		
		// Add to known neighbours:
		neighbours.add(neighbour);
		
		// Send IN in response:
		log("Sending IN response to "+neighbour.agent.getName());
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
		log("IN message received from "+neighbour.agent.getName());
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
	public HEMMAMessage variableUpdate(HEMMAMessage message)
	{
		// Cache neighbour state:
		final Object[] params = message.parameters;
		final String name = message.source.agent.getName();
		IAgent cache = new AgentCache(name, params);
		neighbourCache.add(cache);
		
		// Respond with this agent's state:
		return new HEMMAMessage(this, HEMMAMessageType.VariableUpdate_response, parameters(message.source.agent));
	}
	
	Random rand = new Random(0);
	public void execute()
	{
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
//				log.println(message.type+" received from "+message.source);
				
				switch (message.type)
				{
				case DiscoverNeighbours:
					discoverNeighbours(message.source);
					break;
				case CancelSession:
					int sessionId = (Integer)message.parameters[0];
					cancelSession(sessionId);
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
				default:
					// N/A Handled synchronously.
					break;
				}
			}
		} while(!messageQueue.isEmpty());
	}

	public void init()
	{
		if(state == Idle)
		{
			log("Initialising HEMMA protocol; broadcasting DN message.");
			neighbours = new HashSet<HEMMAProtocol>();
			HEMMAMessage message = new HEMMAMessage(this, HEMMAMessageType.DiscoverNeighbours);
			broadcast(message);
		}
		else
		{
			log("Init called in invalid state: "+state);
		}
	}

	protected void broadcast(HEMMAMessage message) 
	{
		for (IAgent agent : connections) 
		{
			agent.getHemmaProtocol().message(message);
		}
	}

	/**
	 * Physically connects this agent to another agent.
	 * @param neighbour
	 */
	public void connectToNeighbour(IAgent neighbour) 
	{
		connections.add(neighbour);
	}

	public Set<IAgent> neighbourSet()
	{
		// FIXME
//		return cachedNeighbours;
		return connections;
	}
	
	public void updateValues()
	{
		for (HEMMAProtocol n : neighbours) 
		{
			HEMMAMessage response = n.message(new HEMMAMessage(this, HEMMAProtocol.HEMMAMessageType.VariableUpdate, parameters(n.agent)));
			neighbourCache.add(new AgentCache(response.source.agent.getName(), response.parameters));
		}
	}

	protected Object[] parameters(IAgent neighbour)
	{
		return new Object[]{
			agent.getV(), 
			agent.getvMinus(), 
			agent.getPower(), 
			
			agent.getLambdaPlus(), 
			agent.getLambdaMinus(),
			
			agent.getAlpha(),
			
			agent.gPlus(),
			agent.gMinus(),
			
			agent.costGradient(neighbour),
			
			agent.gPlusGradient(neighbour),
			agent.gMinusGradient(neighbour)
		};
	}
	
	void log(String message)
	{
		log.print(agent.getName());
		log.print(",");
		log.println(message);
	}
}
