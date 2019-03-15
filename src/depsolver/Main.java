package depsolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

public class Main {

	public Main() {
	}

	public static void main(String[] args) throws IOException {
		// long t1 = new Date().getTime();
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
		Map<String, Set<String>> initials = new HashMap<String, Set<String>>();
		for (String initial : inits) {
			String[] decomposedInitial = Utils.decomposeConstraint(initial);
			if (initials.get(decomposedInitial[0]) == null) {
				initials.put(decomposedInitial[0], new HashSet<String>());
			}
			initials.get(decomposedInitial[0]).add(decomposedInitial[2]);
		}

		List<String> allConstraints = JSON.parseObject(Utils.readFile(args[2]), strListType);

		List<String> redo = new ArrayList<String>(allConstraints);
		Result lowestCostResult = new Result(0L);
		do {
			allConstraints = new ArrayList<String>(redo);
			redo = new ArrayList<String>();
			for (String constraint : allConstraints) {
				String[] decompsedConstraint = Utils.decomposeConstraint(constraint.substring(1));

				switch (constraint.substring(0, 1)) {
				case "+":
					List<Package> installList = new ArrayList<>();
					if (decompsedConstraint[1].equals("=")) {
						installList.add(repositories.get(decompsedConstraint[0]).get(decompsedConstraint[2]));
					} else {
						for (String version : repositories.get(decompsedConstraint[0]).keySet()) {
							if (decompsedConstraint[1].equals("")) {
								installList.add(repositories.get(decompsedConstraint[0]).get(version));
							} else {
								if (Utils.eval(version.compareTo(decompsedConstraint[2]), decompsedConstraint[1], 0)) {
									installList.add(repositories.get(decompsedConstraint[0]).get(version));
								}
							}
						}
					}

					Result minimumCostResult = new Result(Long.MAX_VALUE);
					for (Package package1 : installList) {
						Result result = new Result(0L);
						if (Utils.installPackage(result, package1, initials, repositories, allConstraints,
								new ArrayList<String>())) {
							if (minimumCostResult.getCost() > result.getCost()) {
								for (String cmd : result.getCommands()) {
									String[] decomposedCmd = Utils.decomposeConstraint(cmd);
									if (initials.get(decomposedCmd[0].substring(1)) == null) {
										initials.put(decomposedCmd[0].substring(1), new HashSet<String>());
									}
									initials.get(decomposedCmd[0].substring(1)).add(decomposedCmd[2]);
								}
								for (String cmd : lowestCostResult.getCommands()) {
									String[] decomposedCmd = Utils.decomposeConstraint(cmd);
									if (initials.get(decomposedCmd[0].substring(1)) == null) {
										initials.put(decomposedCmd[0].substring(1), new HashSet<String>());
									}
									initials.get(decomposedCmd[0].substring(1)).add(decomposedCmd[2]);
								}
								minimumCostResult.setCost(result.getCost());
								minimumCostResult.setCommands(result.getCommands());
							}
						}
					}

					if (minimumCostResult.getCost() != Long.MAX_VALUE) {
						lowestCostResult.setCost(lowestCostResult.getCost() + minimumCostResult.getCost());
						lowestCostResult.getCommands().addAll(minimumCostResult.getCommands());
					} else {
						redo.add(constraint);
					}

					break;
				case "-":
					List<Package> uninstallList = new ArrayList<>();
					if (decompsedConstraint[1].equals("=")) {
						uninstallList.add(repositories.get(decompsedConstraint[0]).get(decompsedConstraint[2]));
					} else {
						for (String version : repositories.get(decompsedConstraint[0]).keySet()) {
							if (decompsedConstraint[1].equals("")) {
								uninstallList.add(repositories.get(decompsedConstraint[0]).get(version));
							} else {
								if (Utils.eval(version.compareTo(decompsedConstraint[2]), decompsedConstraint[1], 0)) {
									uninstallList.add(repositories.get(decompsedConstraint[0]).get(version));
								}
							}
						}
					}

					for (Package package1 : uninstallList) {
						Result result = new Result(0L);
						if (Utils.uninstallPackage(result, package1, repositories, initials)) {
							lowestCostResult.setCost(lowestCostResult.getCost() + result.getCost());
							lowestCostResult.getCommands().addAll(result.getCommands());
						}
					}

					break;
				}
			}
		} while (!redo.isEmpty());

//		System.out.println(lowestCostResult.getCost());
		System.out.println(JSON.toJSONString(lowestCostResult.getCommands(), true));
		// System.out.println(new Date().getTime() - t1);
	}
}