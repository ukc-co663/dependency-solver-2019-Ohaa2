package depsolver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

	public static Long uninstallCost = 1000000L;

	public Utils() {
	}

	public static String readFile(String filename) throws IOException {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filename));
			StringBuilder sb = new StringBuilder();
			br.lines().forEach(line -> sb.append(line));
			return sb.toString();
		} finally {
			if (br != null) {
				br.close();
			}
		}
	}

	public static String[] decomposeConstraint(String constraint) {
		String[] constraintArray = { "", "", "" };

		Pattern pattern = Pattern.compile("(.+?)(?=(>=)|(>)|(<=)|(<)|(=))");
		Matcher matcher = pattern.matcher(constraint);

		if (matcher.find()) {
			constraintArray[0] = matcher.group();

			pattern = Pattern.compile("((>=)|(>)|(<=)|(<)|(=))+?");
			matcher = pattern.matcher(constraint);
			matcher.find();

			constraintArray[1] = matcher.group();

			constraintArray[2] = constraint.substring(matcher.end());
		} else {
			constraintArray[0] = constraint;
		}

		return constraintArray;
	}

	public static boolean conflictsExist(Package package1, Map<String, Set<String>> installedPackages) {
		if (package1.getConflicts() == null || package1.getConflicts().isEmpty()) {
			return false;
		} else {
			for (String conflict : package1.getConflicts()) {
				String[] decompsedConflict = decomposeConstraint(conflict);
				if (installedPackages.get(decompsedConflict[0]) != null) {
					if (decompsedConflict[1].equals("")) {
						return true;
					} else if (decompsedConflict[1].equals("=")) {
						if (installedPackages.get(decompsedConflict[0]).contains(decompsedConflict[2])) {
							return true;
						}
					} else {
						for (String version : installedPackages.get(decompsedConflict[0])) {
							if (Utils.eval(version.compareTo(decompsedConflict[2]), decompsedConflict[1], 0)) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;

	}

	public static boolean installPackage(Result result, Package package1, Map<String, Set<String>> installedPackages,
			Map<String, Map<String, Package>> repositories, List<String> constraints, List<String> path) {
		if (path.contains(package1.getName())) {
			return false;
		} else {
			path.add(package1.getName());
		}

		// System.out.println(path);
		boolean installed = false;
		boolean uninstalled = false;
		Result conjuct = new Result(0L), disjunct = new Result(Long.MAX_VALUE);
		if (installedPackages.get(package1.getName()) != null
				&& installedPackages.get(package1.getName()).contains(package1.getVersion())) {
			return true;
		}
		if (package1 != null && (package1.getDepends() == null || package1.getDepends().isEmpty())) {
			if (!Utils.conflictsExist(package1, installedPackages)) {
				result.setCost(result.getCost() + package1.getSize());
				result.getCommands().add("+" + package1.getName() + "=" + package1.getVersion());
				return true;
			} else {
				uninstalled = uninstallConflicts(conjuct, package1, repositories, installedPackages);
			}
		}

		List<String> localPath = new ArrayList<String>();
		for (int i = 0; i < package1.getDepends().size(); i++) {
			localPath.addAll(path);
			if (i > 0) {
				if (!localPath.isEmpty()) {
					localPath.remove(localPath.size() - 1);
				}
			}
			disjunct = new Result(Long.MAX_VALUE);
			for (int j = 0; j < package1.getDepends().get(i).size(); j++) {
				String[] decompsedDependant = Utils.decomposeConstraint(package1.getDepends().get(i).get(j));

				List<Package> installList = new ArrayList<>();
				if (decompsedDependant[1].equals("=")) {
					installList.add(repositories.get(decompsedDependant[0]).get(decompsedDependant[2]));
				} else {
					if (repositories.get(decompsedDependant[0]) != null) {
						for (String version : repositories.get(decompsedDependant[0]).keySet()) {
							if (decompsedDependant[1].equals("")) {
								installList.add(repositories.get(decompsedDependant[0]).get(version));
							} else {
								if (Utils.eval(version.compareTo(decompsedDependant[2]), decompsedDependant[1], 0)) {
									installList.add(repositories.get(decompsedDependant[0]).get(version));
								}
							}
						}
					}
				}

				Result oneDeptResult = new Result(Long.MAX_VALUE);
				for (Package deptPackage : installList) {
					Result local = new Result(0L);
					Map<String, Set<String>> existingPackages = new HashMap<String, Set<String>>();
					for (String key : installedPackages.keySet()) {
						existingPackages.put(key, new HashSet<String>());
						for (String version : installedPackages.get(key)) {
							existingPackages.get(key).add(version);
						}
					}
					if (Utils.conflictsExist(deptPackage, installedPackages)) {
						uninstallConflicts(local, deptPackage, repositories, existingPackages);
					}
					if (installed = Utils.installPackage(local, deptPackage, existingPackages, repositories,
							constraints, path)) {
						if (oneDeptResult.getCost() > local.getCost()) {
							oneDeptResult.setCost(local.getCost());
							oneDeptResult.setCommands(local.getCommands());
						}
					}
				}

				if (disjunct.getCost() > oneDeptResult.getCost()) {
					disjunct = oneDeptResult;
				} else if (disjunct.getCost() == oneDeptResult.getCost()) {
					if (constraints.contains(String
							.valueOf(oneDeptResult.getCommands().toArray()[oneDeptResult.getCommands().size() - 1]))) {
						disjunct = oneDeptResult;
					}
				}
				if (!path.isEmpty()) {
					path.remove(path.size() - 1);
				}
			}

			for (String cmd : disjunct.getCommands()) {
				if (conjuct.getCommands().contains(cmd)) {
					String[] command = decomposeConstraint(cmd.substring(1));
					disjunct.setCost(disjunct.getCost() - repositories.get(command[0]).get(command[2]).getSize());
				}
			}

			if (!disjunct.getCommands().isEmpty()) {
				conjuct.setCost(conjuct.getCost() + disjunct.getCost());
				conjuct.getCommands().addAll(disjunct.getCommands());
				for (String cmd : conjuct.getCommands()) {
					String[] decomposedCmd = Utils.decomposeConstraint(cmd);
					if (installedPackages.get(decomposedCmd[0].substring(1)) == null) {
						installedPackages.put(decomposedCmd[0].substring(1), new HashSet<String>());
					}
					installedPackages.get(decomposedCmd[0].substring(1)).add(decomposedCmd[2]);
				}
			}
		}

		result.setCost(conjuct.getCost() + package1.getSize());
		result.getCommands().addAll(conjuct.getCommands());
		result.getCommands().add("+" + package1.getName() + "=" + package1.getVersion());

		return (installed || uninstalled);
	}

	public static boolean uninstallConflicts(Result result, Package package1,
			Map<String, Map<String, Package>> repositories, Map<String, Set<String>> installedPackages) {
		if (package1 != null) {
			for (String conflict : package1.getConflicts()) {
				List<Package> uninstallList = new ArrayList<>();
				String[] decompsedConflict = decomposeConstraint(conflict);
				if (decompsedConflict[1].equals("=")) {
					uninstallList.add(repositories.get(decompsedConflict[0]).get(decompsedConflict[2]));
				} else {
					if (repositories.get(decompsedConflict[0]) != null) {
						for (String version : repositories.get(decompsedConflict[0]).keySet()) {
							if (decompsedConflict[1].equals("")) {
								uninstallList.add(repositories.get(decompsedConflict[0]).get(version));
							} else {
								if (Utils.eval(version.compareTo(decompsedConflict[2]), decompsedConflict[1], 0)) {
									uninstallList.add(repositories.get(decompsedConflict[0]).get(version));
								}
							}
						}
					}
				}

				for (Package package2 : uninstallList) {
					if (!Utils.uninstallPackage(result, package2, repositories, installedPackages)) {
						return false;
					}
				}
			}
			return true;
		}
		return false;
	}

	public static boolean uninstallPackage(Result result, Package package1,
			Map<String, Map<String, Package>> repositories, Map<String, Set<String>> installedPackages) {
		if (package1 != null) {
			if (!isUninstallpossible(package1, repositories, installedPackages))
				return false;
			if (installedPackages.get(package1.getName()) != null) {
				installedPackages.get(package1.getName()).remove(package1.getVersion());
				if (installedPackages.get(package1.getName()).size() == 0) {
					installedPackages.remove(package1.getName());
				}
				result.setCost(result.getCost() + Utils.uninstallCost);
				result.getCommands().add("-" + package1.getName() + "=" + package1.getVersion());
				return true;
			}
		}
		return false;
	}

	public static boolean isUninstallpossible(Package package1, Map<String, Map<String, Package>> repositories,
			Map<String, Set<String>> installedPackages) {
		boolean possible = true;
		Map<String, Set<String>> copy = new HashMap<String, Set<String>>(installedPackages);
		copy.get(package1.getName()).remove(package1.getVersion());
		if(copy.get(package1.getName()).isEmpty())
			copy.remove(package1.getName());
		for (String name : copy.keySet()) {
			for (String version : copy.get(name)) {
				if (repositories.get(name) != null) {
					List<List<String>> depts = repositories.get(name).get(version).getDepends();
					for (int i = 0; i < depts.size(); i++) {
						possible = false;
						for (int j = 0; j < depts.get(i).size(); j++) {
							String[] decomposedDept = Utils.decomposeConstraint(depts.get(i).get(j));
							if (copy.get(decomposedDept[0]) != null) {
								for (String v1 : copy.get(decomposedDept[0])) {
									if (Utils.eval(decomposedDept[2].compareTo(v1), decomposedDept[1], 0)) {
										possible = true;
										break;
									}
								}
								if (possible) {
									break;
								}
							}
						}
						if (!possible) {
							break;
						}
					}
				}
			}
		}

		return possible;

	}

	public static boolean eval(int value1, String oprand, int value2) {
		switch (oprand) {
		case ">=":
			return value1 >= value2;
		case ">":
			return (value1 > value2);
		case "<=":
			return (value1 <= value2);
		case "<":
			return (value1 < value2);
		case "=":
			return (value1 == value2);
		}
		return true;
	}
}
