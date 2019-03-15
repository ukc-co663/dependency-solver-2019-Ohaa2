package depsolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

@SuppressWarnings("restriction")
public class Main {

	public Main() {
	}

	public static void main(String[] args) throws IOException, ScriptException {
		long t1 = new Date().getTime();
		TypeReference<List<Package>> repoType = new TypeReference<List<Package>>() {
		};
		List<Package> repos = JSON.parseObject(Utils.readFile(args[0]), repoType);
		Map<String, Map<String, Package>> repositories = new HashMap<String, Map<String, Package>>();
		for (Package package1 : repos) {
			if (repositories.get(package1.getName()) == null) {
				repositories.put(package1.getName(), new HashMap<String, Package>());
			}

			repositories.get(package1.getName()).put(package1.getVersion(), package1);
		}

		TypeReference<List<String>> strListType = new TypeReference<List<String>>() {
		};

		List<String> inits = JSON.parseObject(Utils.readFile(args[1]), strListType);
		Map<String, List<String>> initials = new HashMap<String, List<String>>();
		for (String initial : inits) {
			String[] decomposedInitial = Utils.decomposeConstraint(initial);
			if (initials.get(decomposedInitial[0]) == null) {
				initials.put(decomposedInitial[0], new ArrayList<String>());
			}
			initials.get(decomposedInitial[0]).add(decomposedInitial[2]);
		}

		List<String> allConstraints = JSON.parseObject(Utils.readFile(args[2]), strListType);

		Result lowestCostResult = new Result(0L);
		for (String constraint : allConstraints) {
			String[] decompsedConstraint = Utils.decomposeConstraint(constraint.substring(1));

			switch (constraint.substring(0, 1)) {
			case "+":
				List<Package> installList = new ArrayList<>();
				if (decompsedConstraint[1].equals("=")) {
					installList.add(repositories.get(decompsedConstraint[0]).get(decompsedConstraint[2]));
				} else {
					ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
					ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("JavaScript");
					for (String version : repositories.get(decompsedConstraint[0]).keySet()) {
						if (decompsedConstraint[1].equals("")) {
							installList.add(repositories.get(decompsedConstraint[0]).get(version));
						} else {
							if (Boolean
									.valueOf(String.valueOf(scriptEngine.eval(version.compareTo(decompsedConstraint[2])
											+ " " + decompsedConstraint[1] + " 0")))) {
								installList.add(repositories.get(decompsedConstraint[0]).get(version));
							}
						}
					}
				}

				Result minimumCostResult = new Result(Long.MAX_VALUE);
				for (Package package1 : installList) {
					Result result = new Result(0L);
					if (Utils.installPackage(result, package1, initials, repositories, new ArrayList<String>())) {
						if (minimumCostResult.getCost() > result.getCost()) {
							for (String cmd : result.getCommands()) {
								String[] decomposedCmd = Utils.decomposeConstraint(cmd);
								if (initials.get(decomposedCmd[0]) == null) {
									initials.put(decomposedCmd[0], new ArrayList<String>());
								}
								initials.get(decomposedCmd[0]).add(decomposedCmd[2]);
							}
							minimumCostResult.setCost(result.getCost());
							minimumCostResult.setCommands(result.getCommands());
						}
					}
				}

				lowestCostResult.setCost(lowestCostResult.getCost() + minimumCostResult.getCost());
				lowestCostResult.getCommands().addAll(minimumCostResult.getCommands());

				break;
			case "-":
				List<Package> uninstallList = new ArrayList<>();
				if (decompsedConstraint[1].equals("=")) {
					uninstallList.add(repositories.get(decompsedConstraint[0]).get(decompsedConstraint[2]));
				} else {
					ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
					ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("JavaScript");
					for (String version : repositories.get(decompsedConstraint[0]).keySet()) {
						if (decompsedConstraint[1].equals("")) {
							uninstallList.add(repositories.get(decompsedConstraint[0]).get(version));
						} else {
							if (Boolean
									.valueOf(String.valueOf(scriptEngine.eval(version.compareTo(decompsedConstraint[2])
											+ " " + decompsedConstraint[1] + " 0")))) {
								uninstallList.add(repositories.get(decompsedConstraint[0]).get(version));
							}
						}
					}
				}

				for (Package package1 : uninstallList) {
					Result result = new Result(0L);
					if (Utils.uninstallPackage(result, package1, initials)) {
						lowestCostResult.setCost(lowestCostResult.getCost() + result.getCost());
						lowestCostResult.getCommands().addAll(result.getCommands());
					}
				}

				break;
			}
		}

		// System.out.println(lowestCostResult.getCost());
		System.out.println(JSON.toJSONString(lowestCostResult.getCommands(), true));
		// System.out.println(new Date().getTime() - t1);
	}
}