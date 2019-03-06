package depsolver;

import java.util.LinkedHashSet;
import java.util.Set;

public class Result {

	private Long cost;
	private Set<String> commands;

	public Long getCost() {
		return cost;
	}

	public void setCost(Long cost) {
		this.cost = cost;
	}

	public Set<String> getCommands() {
		return commands;
	}

	public void setCommands(Set<String> commands) {
		this.commands = commands;
	}

	public Result(Long cost) {
		this.cost = cost;
		this.commands = new LinkedHashSet<String>();
	}
}
