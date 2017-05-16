package ellipsis.hemma;

import static ellipsis.hemma.HEMMAProtocol.HEMMAState.Idle;
import static ellipsis.hemma.HEMMAProtocol.HEMMAState.SessionInitialisation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import javax.naming.OperationNotSupportedException;

/**
 * Implementation of the HEMMA protocol.
 * @author bmillar
 *
 */
public class HEMMAProtocol
{
	public static boolean useCache = true; // FIXME testing code

	private static final int PARAM_COUNT = 11;
	
	private static final int PARAM_V = 0;
	private static final int PARAM_V_MINUS = 1;
	private static final int PARAM_POWER = 2;
	
	private static final int PARAM_LAMBDA_PLUS = 3;
	private static final int PARAM_LAMBDA_MINUS = 4;
	
	private static final int PARAM_ALPHA = 5;
	
	private static final int PARAM_G_PLUS = 6;
	private static final int PARAM_G_MINUS = 7;
	
	static class AgentCache implements IAgent
	{
		private double[] params;
		private String name;
		private IAgent agent;
		
		public ellipsis.hemma.IAgent.AgentType getType() 
		{
			return agent.getType();
		};

		AgentCache(IAgent agent, double[] params) 
		{
			this.params = params;
			this.name = agent.getName();
			this.agent = agent;
		}

		public String getName() { return name; }
		public int hashCode() { return name.hashCode(); }
		public boolean equals(Object obj) 
		{
			if(obj instanceof IAgent)
				return name.equals(((IAgent)obj).getName());
			return false;
		}
		public String toString() { return "CACHE:"+name; }
	
		public double getV()                         
		{
//if(params[PARAM_V] != agent.getV())
//	System.err.println("NOT EQUAL!");
			if(useCache)
				return (double) params[PARAM_V];
			else
				return agent.getV(); 
		}          

		public double getvMinus()                    
		{ 
			if(useCache)
				return (double) params[PARAM_V_MINUS];
			else
				return agent.getvMinus(); 
		}
		public double getPower()                     
		{ 
			if(useCache)
				return (double) params[PARAM_POWER];
			else
				return agent.getPower(); 
		}             

		public double getLambdaPlus()                
		{ 
			if(useCache)
				return (double) params[PARAM_LAMBDA_PLUS]; 
			else
				return agent.getLambdaPlus(); 
		}        
		public double getLambdaMinus()               
		{ 
			if(useCache)
				return (double) params[PARAM_LAMBDA_MINUS];
			else
				return agent.getLambdaMinus(); 
		}       

		public double getAlpha()                     
		{ 
			if(useCache)
				return (double) params[PARAM_ALPHA];
			else
				return agent.getAlpha(); 
		}             

		public double gPlus()                        
		{ 
//			if(useCache)
//				return (double) params[PARAM_G_PLUS];
//			else
//				return agent.gPlus();
boolean oldValue = useCache;
useCache = false;
double gPlus = agent.gPlus();
useCache = oldValue;
return gPlus;
		}                     
		public double gMinus()                       
		{ 
//			if(useCache)
//				return (double) params[PARAM_G_MINUS];
//			else
//				return agent.gMinus();
boolean oldValue = useCache;
useCache = false;
double gMinus = agent.gMinus();
useCache = oldValue;
return gMinus;
		}

		public HEMMAProtocol getHemmaProtocol() 
		{ 
			throw new RuntimeException(new OperationNotSupportedException()); 
		}
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
		double[] parameters;
		
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
		
		public HEMMAMessage(HEMMAProtocol source, HEMMAMessageType type, /*int ttl, */double... params)
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
	private Map<HEMMAProtocol, IAgent> neighbourCache = new HashMap<>(); // Cached values from variable updates.
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
		log("DN message received from "+neighbour.agent.getName());
		
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
		IAgent cache = new AgentCache(message.source.agent, message.parameters);
		updateCache(message.source, cache);
		
		// Respond with this agent's state:
		return new HEMMAMessage(this, HEMMAMessageType.VariableUpdate_response, parameters(message.source.agent));
	}

	protected void updateCache(HEMMAProtocol hemma, IAgent cache) 
	{
//		if(neighbourCache.contains(cache))
//			neighbourCache.remove(cache);
		
		neighbourCache.put(hemma, cache);
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
					int sessionId = (int)message.parameters[0];
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

	public Iterable<IAgent> neighbourSet()
	{
if(useCache)
		return neighbourCache.values();
else
	return connections;
		
//		return new Iterable<IAgent>() {
//			
//			@Override
//			public Iterator<IAgent> iterator() 
//			{
//				final Iterator<HEMMAProtocol> it = neighbours.iterator();
//				return new Iterator<IAgent>() {
//					
//					@Override
//					public IAgent next() 
//					{
//						// If not cached then request values from neighbour:
//						HEMMAProtocol hemma = it.next();
//						if(!neighbourCache.containsKey(hemma))
//							throw new RuntimeException("Neighbour missing from cache: "+hemma.agent.getName());
//						
//						// Otherwise use cached values:
//						IAgent agent = neighbourCache.get(hemma);
//						if(agent == null)
//							throw new RuntimeException();
//						return agent;
//					}
//					
//					@Override
//					public boolean hasNext() 
//					{
//						return it.hasNext();
//					}
//				};
//			}
//		};
	}
	
	public void updateValues()
	{
		for (HEMMAProtocol n : neighbours) 
		{
			updateValues(n);
		}
	}

	protected IAgent updateValues(HEMMAProtocol n) 
	{
		HEMMAMessage response = n.message(new HEMMAMessage(this, HEMMAProtocol.HEMMAMessageType.VariableUpdate, parameters(n.agent)));
		IAgent cache = new AgentCache(response.source.agent, response.parameters);
		updateCache(response.source, cache);
		return cache;
	}

	protected double[] parameters(IAgent wrt)
	{
		double[] params = new double[PARAM_COUNT];
		
		params[PARAM_V] = agent.getV();
		params[PARAM_V_MINUS] = agent.getvMinus();
		params[PARAM_POWER] = agent.getPower();

		params[PARAM_LAMBDA_PLUS] = agent.getLambdaPlus();
		params[PARAM_LAMBDA_MINUS] = agent.getLambdaMinus();

		params[PARAM_ALPHA] = agent.getAlpha();

		params[PARAM_G_PLUS] = agent.gPlus();
		params[PARAM_G_MINUS] = agent.gMinus();
		
		return params;
	}

	public boolean initialised() 
	{
		return neighbourCache.size() == neighbours.size();
	}
	
	void log(String message)
	{
		log.print(agent.getName());
		log.print(",");
		log.println(message);
	}
	
	@Override
	public String toString() 
	{
		return "HEMMA:"+agent.toString();
	}
}
