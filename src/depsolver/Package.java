package depsolver;

import java.util.ArrayList;
import java.util.List;

public class Package {

	private String name;
	private String version;
	private Integer size;
	private List<List<String>> depends;
	private List<String> conflicts;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Integer getSize() {
		return size;
	}

	public void setSize(Integer size) {
		this.size = size;
	}

	public List<List<String>> getDepends() {
		return depends;
	}

	public void setDepends(List<List<String>> depends) {
		this.depends = depends;
	}

	public List<String> getConflicts() {
		return conflicts;
	}

	public void setConflicts(List<String> conflicts) {
		this.conflicts = conflicts;
	}

	public Package() {
		this.depends = new ArrayList<List<String>>();
		this.conflicts = new ArrayList<String>();
	}
}
