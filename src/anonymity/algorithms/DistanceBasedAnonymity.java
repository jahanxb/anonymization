package anonymity.algorithms;

import java.io.IOException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import data.EquivalenceClass;
import data.Tuple;
import readers.DataReader;

/**
 * Clustering based algorithm for applying homogeneous k-anonymity property by local recoding.
 * @author Giannis Giannakopoulos
 *
 */

public class DistanceBasedAnonymity extends AbstractAlgorithm {

	public DistanceBasedAnonymity(String qid, EquivalenceClass data){
		super(qid.split(" "), data);
	}
	
	@Override
	public void run() {
		Set<Tuple> visited=new LinkedHashSet<Tuple>();
		EquivalenceClass notVisited = (EquivalenceClass) this.getData().clone();
		Tuple current=null;
//		this.chooseTuple(null, visited, notVisited);
		while(notVisited.size()>=this.getK()){							//this method runs as long as there exist plenty of data to be grouped
			current=chooseTuple(current, visited, notVisited);
			EquivalenceClass res = new EquivalenceClass(this.getQID());
			while(res.size()<this.getK()){
				Tuple close=null;
				try {
					close = this.getClosestTuple(current, visited, notVisited);
				} catch (Exception e) {
					System.err.println(e.getMessage()+" happened when visited vs notVisited was:\t" +visited.size()+" vs"+ notVisited.size());
					System.exit(1);
				}
				res.add(close);
				visited.add(close);
				notVisited.remove(close);
				
				EquivalenceClass temp=new EquivalenceClass();
				for(Tuple t:notVisited){
					if(temp.size()+res.size()>2*this.getK())
						break;
					if(res.containsTuple(t)){
						temp.add(t);
					}
				}
				
				notVisited.removeAll(temp);
				visited.addAll(temp);
				res.merge(temp);
			}
			
			this.addToResults(res);
		}
		for(Tuple t:notVisited)											// the remainders (something like n mod k) are grouped to the closest groups
			this.getClosestEquivalenceClass(t).add(t);
	}
	
	private Tuple chooseTuple(Tuple previous, Set<Tuple> visited, List<Tuple> notVisited){
		Tuple chosenTuple=null;
		if(previous==null)
			chosenTuple=notVisited.get(0);
		else
			chosenTuple=previous;
		chosenTuple=getMostDistantTuple(chosenTuple, visited, notVisited);
//		System.out.println(chosenTuple);
		if(chosenTuple==null && previous==null){
			System.err.println(chosenTuple);
			System.exit(1);
		}
			
		return chosenTuple;
	}
	
	private Tuple getMostDistantTuple(Tuple tuple, Set<Tuple> visited, List<Tuple> notVisited){
		Double maxDistance=Double.MIN_VALUE;
		Tuple chosen=notVisited.get(0);
		for(Tuple t:notVisited){
//			System.out.println(tuple+" vs "+t);
			Double dist = t.getEucleidianDistance(tuple, this.getQID(), this.getRanges());
			if(!visited.contains(t) && dist>maxDistance){
				chosen=t;
				maxDistance=dist;
			}
		}
		return chosen;
	}
	
	private EquivalenceClass getClosestEquivalenceClass(Tuple t){
		EquivalenceClass res=this.getResults().get(0);
		Double minNCP=Double.MAX_VALUE;
		int population=Integer.MAX_VALUE;
		for(EquivalenceClass eq:this.getResults()){
			Double current=(eq.getNCPwithOtherTuple(t, this.getQID(), this.getRanges())-eq.getNCP(this.getQID(), this.getRanges()));
			if(current<minNCP || (current==minNCP && population>eq.size())){
				res=eq;
				minNCP=current;
				population=eq.size();
			}
		}
		return res;
	}
	
	private Tuple getClosestTuple(Tuple tuple, Set<Tuple> visited, List<Tuple> notVisited) throws Exception{
		Double minDistance=Double.MAX_VALUE;
		Tuple chosen=notVisited.get(0);
		if(tuple==null)
			throw new Exception("Shit happens!!");
		for(Tuple t:notVisited){
			Double dist = t.getEucleidianDistance(tuple, this.getQID(), this.getRanges());
			if(!visited.contains(t) && dist<minDistance){
				chosen=t;
				minDistance=dist;
			}
		}
		return chosen;
	}
	

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		/*ConfReader conf = new ConfReader(args[0]);
		DataReader reader = new DataReader(conf.getValue("FILE"));
		String qid=conf.getValue("QID");
		Integer k = new Integer(conf.getValue("K")), numberOfTuples=new Integer(conf.getValue("TUPLES"));
		EquivalenceClass data = new EquivalenceClass();
		for(int i=0;i<numberOfTuples;i++)
			data.add(reader.getNextTuple());
		*/
		if(args.length<2){
			System.err.println("I need arguments (-file, -qid, -k, -tuples)");
			System.exit(1);
		}
		DataReader reader = new DataReader(AbstractAlgorithm.getArgument(args, "-file"));
		String qid=AbstractAlgorithm.getArgument(args, "-qid");
		Integer k = new Integer(AbstractAlgorithm.getArgument(args, "-k")), 
				numberOfTuples=new Integer(AbstractAlgorithm.getArgument(args, "-tuples"));
		
		EquivalenceClass data = new EquivalenceClass();
		for(int i=0;i<numberOfTuples;i++)
			data.add(reader.getNextTuple());
		AbstractAlgorithm clustering = new DistanceBasedAnonymity(qid, data);
		clustering.setK(k);
		
		double start= System.currentTimeMillis();
		clustering.run();
		AbstractAlgorithm.printResults(clustering, System.currentTimeMillis()-start);
	}

}
