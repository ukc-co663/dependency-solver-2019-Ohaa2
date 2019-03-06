package depsolver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {

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

	public static boolean conflictsExist(Result result, Package package1, Map<String, List<String>> initialPackages,
			Map<String, Map<String, Package>> repositories) {
		for (String conflict : package1.getConflicts()) {
			if (conflict.indexOf(">=") != -1) {
				List<String> vers = initialPackages.get(conflict.substring(0, conflict.indexOf(">=")));
				int conflictVersion = Integer.parseInt(conflict.substring(conflict.indexOf(">=") + 2).replace(".", ""));
				for (String ver : vers) {
					int currVersion = Integer.parseInt(ver.replace(".", ""));
					if (currVersion >= conflictVersion) {
						Map<String, Integer> versionConflicts = result.getConflicts()
								.get(conflict.substring(0, conflict.indexOf(">=")));
						if (versionConflicts == null) {
							versionConflicts = new HashMap<String, Integer>();
						}
						if (versionConflicts.get(ver) == null) {
							versionConflicts.put(ver, 0);
						}
						versionConflicts.put(ver, versionConflicts.get(ver) + 1);
						return true;
					}
				}
			} else if (conflict.indexOf(">") != -1) {
				List<String> vers = initialPackages.get(conflict.substring(0, conflict.indexOf(">")));
				int conflictVersion = Integer.parseInt(conflict.substring(conflict.indexOf(">") + 1).replace(".", ""));
				for (String ver : vers) {
					int currVersion = Integer.parseInt(ver.replace(".", ""));
					if (currVersion > conflictVersion) {
						Map<String, Integer> versionConflicts = result.getConflicts()
								.get(conflict.substring(0, conflict.indexOf(">")));
						if (versionConflicts == null) {
							versionConflicts = new HashMap<String, Integer>();
						}
						if (versionConflicts.get(ver) == null) {
							versionConflicts.put(ver, 0);
						}
						versionConflicts.put(ver, versionConflicts.get(ver) + 1);
						return true;
					}
				}
			} else if (conflict.indexOf("<=") != -1) {
				List<String> vers = initialPackages.get(conflict.substring(0, conflict.indexOf("<=")));
				int conflictVersion = Integer.parseInt(conflict.substring(conflict.indexOf("<=") + 2).replace(".", ""));
				for (String ver : vers) {
					int currVersion = Integer.parseInt(ver.replace(".", ""));
					if (currVersion <= conflictVersion) {
						Map<String, Integer> versionConflicts = result.getConflicts()
								.get(conflict.substring(0, conflict.indexOf("<=")));
						if (versionConflicts == null) {
							versionConflicts = new HashMap<String, Integer>();
						}
						if (versionConflicts.get(ver) == null) {
							versionConflicts.put(ver, 0);
						}
						versionConflicts.put(ver, versionConflicts.get(ver) + 1);
						return true;
					}
				}
			} else if (conflict.indexOf("<") != -1) {
				List<String> vers = initialPackages.get(conflict.substring(0, conflict.indexOf("<")));
				int conflictVersion = Integer.parseInt(conflict.substring(conflict.indexOf("<") + 1).replace(".", ""));
				for (String ver : vers) {
					int currVersion = Integer.parseInt(ver.replace(".", ""));
					if (currVersion < conflictVersion) {
						Map<String, Integer> versionConflicts = result.getConflicts()
								.get(conflict.substring(0, conflict.indexOf("<")));
						if (versionConflicts == null) {
							versionConflicts = new HashMap<String, Integer>();
						}
						if (versionConflicts.get(ver) == null) {
							versionConflicts.put(ver, 0);
						}
						versionConflicts.put(ver, versionConflicts.get(ver) + 1);
						result.getConflicts().put(conflict.substring(0, conflict.indexOf("<")), versionConflicts);
						return true;
					}
				}
			} else if (conflict.indexOf("=") != -1) {
				List<String> vers = initialPackages.get(conflict.substring(0, conflict.indexOf("=")));
				int conflictVersion = Integer.parseInt(conflict.substring(conflict.indexOf("=") + 1).replace(".", ""));
				for (String ver : vers) {
					int currVersion = Integer.parseInt(ver.replace(".", ""));
					if (currVersion == conflictVersion) {
						Map<String, Integer> versionConflicts = result.getConflicts()
								.get(conflict.substring(0, conflict.indexOf("=")));
						if (versionConflicts == null) {
							versionConflicts = new HashMap<String, Integer>();
						}
						if (versionConflicts.get(ver) == null) {
							versionConflicts.put(ver, 0);
						}
						versionConflicts.put(ver, versionConflicts.get(ver) + 1);
						return true;
					}
				}
			} else {
				if (initialPackages.get(conflict) != null) {
					return true;
				}
			}
		}

		return false;
	}

	public static boolean installPackage(Result result, Package package1, Map<String, List<String>> initialPackages,
			Map<String, Map<String, Package>> repositories) {
		List<List<String>> conjuncts = package1.getDepends();
		if (conjuncts == null || conjuncts.isEmpty()) {
			if (!Utils.conflictsExist(result, package1, initialPackages, repositories)) {
				result.setCost(result.getCost() + package1.getSize());
				result.getCommands().add("+" + package1.getName() + "=" + package1.getVersion());
				return true;
			} else
				return false;
		}

		for (List<String> disjuncts : conjuncts) {
			for (String disjunct : disjuncts) {
				if (disjunct.indexOf(">=") != -1) {
					for (String version : repositories.get(disjunct.substring(0, disjunct.indexOf(">="))).keySet()) {
						if (version.compareTo(disjunct.substring(disjunct.indexOf(">=") + 2)) >= 0) {
							if (installPackage(result,
									repositories.get(disjunct.substring(0, disjunct.indexOf(">="))).get(version),
									initialPackages, repositories)) {
								for (String cmd : result.getCommands()) {
									result.getCommands().add(cmd);
								}
							}
						}
					}
				} else if (disjunct.indexOf(">") != -1) {
					for (String version : repositories.get(disjunct.substring(0, disjunct.indexOf(">"))).keySet()) {
						if (version.compareTo(disjunct.substring(disjunct.indexOf(">") + 1)) > 0) {
							if (installPackage(result,
									repositories.get(disjunct.substring(0, disjunct.indexOf(">"))).get(version),
									initialPackages, repositories)) {
								for (String cmd : result.getCommands()) {
									result.getCommands().add(cmd);
								}
							}
						}
					}
				} else if (disjunct.indexOf("<=") != -1) {
					for (String version : repositories.get(disjunct.substring(0, disjunct.indexOf("<="))).keySet()) {
						if (version.compareTo(disjunct.substring(disjunct.indexOf("<=") + 2)) <= 0) {
							if (installPackage(result,
									repositories.get(disjunct.substring(0, disjunct.indexOf("<="))).get(version),
									initialPackages, repositories)) {
								for (String cmd : result.getCommands()) {
									result.getCommands().add(cmd);
								}
							}
						}
					}
				} else if (disjunct.indexOf("<") != -1) {
					for (String version : repositories.get(disjunct.substring(0, disjunct.indexOf("<"))).keySet()) {
						if (version.compareTo(disjunct.substring(disjunct.indexOf("<") + 1)) < 0) {
							if (installPackage(result,
									repositories.get(disjunct.substring(0, disjunct.indexOf("<"))).get(version),
									initialPackages, repositories)) {
								for (String cmd : result.getCommands()) {
									result.getCommands().add(cmd);
								}
							}
						}
					}
				} else if (disjunct.indexOf("=") != -1) {
					for (String version : repositories.get(disjunct.substring(0, disjunct.indexOf("="))).keySet()) {
						if (version.compareTo(disjunct.substring(disjunct.indexOf("=") + 1)) == 0) {
							if (installPackage(result,
									repositories.get(disjunct.substring(0, disjunct.indexOf("="))).get(version),
									initialPackages, repositories)) {
								for (String cmd : result.getCommands()) {
									result.getCommands().add(cmd);
								}
							}
						}
					}
				} else {
					for (String version : repositories.get(disjunct).keySet()) {
						if (installPackage(result, repositories.get(disjunct).get(version), initialPackages,
								repositories)) {
							if (!Utils.conflictsExist(result, package1, initialPackages, repositories)) {
								result.setCost(result.getCost() + package1.getSize());
								result.getCommands().add("+" + package1.getName() + "=" + package1.getVersion());
								return true;
							}
						}
					}
				}
				
			}
		}

		return false;
	}
}
