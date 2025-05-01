package wrimsv2.evaluator;

public class LoopIndex {
	private String name;
	private int value;
	private boolean indexStart;
	public int start;
	public int end;
	public int step;
	
	public LoopIndex(String name, int value, boolean indexStart, int start, int end, int step){
		this.name=name;
		this.value=value;
		this.indexStart=indexStart;
		this.start=start;
		this.end=end;
		this.step=step;
	}
	
	public void setName(String name){
		this.name=name;
	}
	
	public void setValue(int value){
		this.value=value;
	}
	
	public String getName(){
		return name;
	}
	
	public int getValue(){
		return value;
	}
	
	public boolean getIndexStart(){
		return indexStart;
	}
	
	public void setIndexStart(boolean indexStart){
		this.indexStart=indexStart;
	}
}
