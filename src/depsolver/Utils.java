package depsolver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

@SuppressWarnings("restriction")
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

	public static boolean conflictsExist(Package package1, Map<String, List<String>> installedPackages)
			throws ScriptException {
		if (package1.getConflicts() == null || package1.getConflicts().isEmpty()) {
			return false;
		} else {
			ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
			ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("JavaScript");
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
							if (Boolean.valueOf(String.valueOf(scriptEngine.eval(
									version.compareTo(decompsedConflict[2]) + " " + decompsedConflict[1] + " 0")))) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;

	}

	public static boolean installPackage(Result result, Package package1, Map<String, List<String>> installedPackages,
			Map<String, Map<String, Package>> repositories, List<String> path) throws ScriptException {
		if (path.contains(package1.getName())) {
			return false;
		} else {
			path.add(package1.getName());
		}

		// System.out.println(path);
		boolean installed = false;
		if (package1 != null && (package1.getDepends() == null || package1.getDepends().isEmpty())) {
			if (!Utils.conflictsExist(package1, installedPackages)) {
				result.setCost(result.getCost() + package1.getSize());
				result.getCommands().add("+" + package1.getName() + "=" + package1.getVersion());
				return true;
			} else {
				uninstallConflicts(result, package1, repositories, installedPackages);
			}
		}

		Result conjuct = new Result(0L), disjunct = new Result(Long.MAX_VALUE);
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
					ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
					ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("JavaScript");
					if (repositories.get(decompsedDependant[0]) != null) {
						for (String version : repositories.get(decompsedDependant[0]).keySet()) {
							if (decompsedDependant[1].equals("")) {
								installList.add(repositories.get(decompsedDependant[0]).get(version));
							} else {
								if (Boolean.valueOf(
										String.valueOf(scriptEngine.eval(version.compareTo(decompsedDependant[2]) + " "
												+ decompsedDependant[1] + " 0")))) {
									installList.add(repositories.get(decompsedDependant[0]).get(version));
								}
							}
						}
					}
				}

				Result oneDeptResult = new Result(Long.MAX_VALUE);
				for (Package deptPackage : installList) {
					Result local = new Result(0L);
					Map<String, List<String>> existingPackages = new HashMap<String, List<String>>();
					for (String key : installedPackages.keySet()) {
						existingPackages.put(key, new ArrayList<String>());
						for (String version : installedPackages.get(key)) {
							existingPackages.get(key).add(version);
						}
					}
					if (Utils.conflictsExist(deptPackage, installedPackages)) {
						uninstallConflicts(local, deptPackage, repositories, existingPackages);
					}
					if (installed = Utils.installPackage(local, deptPackage, existingPackages, repositories, path)) {
						if (oneDeptResult.getCost() > local.getCost()) {
							oneDeptResult.setCost(local.getCost());
							oneDeptResult.setCommands(local.getCommands());
						}
					}
				}

				if (disjunct.getCost() > oneDeptResult.getCost()) {
					disjunct = oneDeptResult;
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
			}
		}

		result.setCost(conjuct.getCost() + package1.getSize());
		result.getCommands().addAll(conjuct.getCommands());
		result.getCommands().add("+" + package1.getName() + "=" + package1.getVersion());

		return installed;
	}

	public static boolean uninstallConflicts(Result result, Package package1,
			Map<String, Map<String, Package>> repositories, Map<String, List<String>> installedPackages)
			throws ScriptException {
		if (package1 != null) {
			for (String conflict : package1.getConflicts()) {
				List<Package> uninstallList = new ArrayList<>();
				String[] decompsedConflict = decomposeConstraint(conflict);
				if (decompsedConflict[1].equals("=")) {
					uninstallList.add(repositories.get(decompsedConflict[0]).get(decompsedConflict[2]));
				} else {
					ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
					ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("JavaScript");
					for (String version : repositories.get(decompsedConflict[0]).keySet()) {
						if (decompsedConflict[1].equals("")) {
							uninstallList.add(repositories.get(decompsedConflict[0]).get(version));
						} else {
							if (Boolean.valueOf(String.valueOf(scriptEngine.eval(
									version.compareTo(decompsedConflict[2]) + " " + decompsedConflict[1] + " 0")))) {
								uninstallList.add(repositories.get(decompsedConflict[0]).get(version));
							}
						}
					}
				}

				for (Package package2 : uninstallList) {
					Utils.uninstallPackage(result, package2, installedPackages);
				}
			}
			return true;
		}
		return false;
	}

	public static boolean uninstallPackage(Result result, Package package1,
			Map<String, List<String>> installedPackages) {
		if (package1 != null) {
			installedPackages.get(package1.getName()).remove(package1.getVersion());
			if (installedPackages.get(package1.getName()).size() == 0) {
				installedPackages.remove(package1.getName());
			}
			result.setCost(result.getCost() + Utils.uninstallCost);
			result.getCommands().add("-" + package1.getName() + "=" + package1.getVersion());
			return true;
		}
		return false;
	}
}
