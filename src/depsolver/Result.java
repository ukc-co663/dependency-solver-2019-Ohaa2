package depsolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Result {

	private long cost = 0L;
	private Map<String, Map<String, Integer>> conflicts;
	private List<String> commands;

	public long getCost() {
		return cost;
	}

	public void setCost(long cost) {
		this.cost = cost;
	}

	public Map<String, Map<String, Integer>> getConflicts() {
		return conflicts;
	}

	public void setConflicts(Map<String, Map<String, Integer>> conflicts) {
		this.conflicts = conflicts;
	}

	public List<String> getCommands() {
		return commands;
	}

	public void setCommands(List<String> commands) {
		this.commands = commands;
	}

	public Result() {
		this.commands = new ArrayList<String>();
		this.conflicts = new HashMap<String, Map<String, Integer>>();
	}
}
