package depsolver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Utils {

	public static Long uninstallCost = 1000000L;
	public static String[] allOperators = new String[] { ">=", ">", "<=", "<", "=" };

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

	public static void writeFile(String filename, String json) throws IOException {
		FileWriter fw = null;
		try {
			fw = new FileWriter(filename);
			fw.write(json);
		} finally {
			if (fw != null) {
				fw.close();
			}
		}
	}

	public static String[] decomposeConstraint(String constraint, List<Package> repositories) {
		String localConstraint = new String(constraint);
		String[] constraintArray = new String[3];
		for (Package package1 : repositories) {
			if (localConstraint.contains(package1.getName())) {
				constraintArray[0] = package1.getName();
				break;
			} else
				constraintArray[0] = "";
		}
		for (String operator : allOperators) {
			if ((localConstraint.indexOf(operator) != -1)) {
				constraintArray[1] = operator;
				break;
			} else
				constraintArray[1] = "";
		}
		constraintArray[2] = localConstraint.replace(constraintArray[0] + constraintArray[1], "");
		return constraintArray;
	}

	public static boolean conflictsExist(Package pack, List<String> installedPackages, List<Package> repositories) {
		if (pack.getConflicts() == null || pack.getConflicts().isEmpty()) {
			return false;
		} else {
			for (String conflict : pack.getConflicts()) {
				String[] decompsedConflict = decomposeConstraint(conflict, repositories);
				for (String installed : installedPackages) {
					String[] decompsedInstalled = decomposeConstraint(installed, repositories);
					if (decompsedInstalled[0].equals(decompsedConflict[0])) {
						switch (decompsedConflict[1]) {
						case ">=":
							if (decompsedInstalled[2].compareTo(decompsedConflict[2]) >= 0) {
								return true;
							}
							break;
						case ">":
							if (decompsedInstalled[2].compareTo(decompsedConflict[2]) > 0) {
								return true;
							}
							break;
						case "<=":
							if (decompsedInstalled[2].compareTo(decompsedConflict[2]) <= 0) {
								return true;
							}
							break;
						case "<":
							if (decompsedInstalled[2].compareTo(decompsedConflict[2]) < 0) {
								return true;
							}
							break;
						case "=":
							if (decompsedInstalled[2].compareTo(decompsedConflict[2]) == 0) {
								return true;
							}
							break;
						case "":
							return true;
						}
					}
				}
			}
		}
		return false;

	}

	public static boolean installPackage(Result result, Package pack, List<String> installedPackages,
			List<Package> repositories, List<String> breakloop) {
		if (breakloop.contains(pack.getName() + "=" + pack.getVersion())) {
			return false;
		} else {
			breakloop.add(pack.getName() + "=" + pack.getVersion());
		}

		boolean installed = false;
		if (pack != null && (pack.getDepends() == null || pack.getDepends().isEmpty())) {
			if (!Utils.conflictsExist(pack, installedPackages, repositories)) {
				result.setCost(result.getCost() + pack.getSize());
				result.getCommands().add("+" + pack.getName() + "=" + pack.getVersion());
				return true;
			}
		}

		Result conjuct = new Result(0L), disjunct = new Result(Long.MAX_VALUE);
		for (int i = 0; i < pack.getDepends().size(); i++) {
			disjunct = new Result(Long.MAX_VALUE);
			for (int j = 0; j < pack.getDepends().get(i).size(); j++) {
				String[] decompsedDependant = Utils.decomposeConstraint(pack.getDepends().get(i).get(j), repositories);
				for (Package package1 : repositories) {
					boolean isMatched = false;
					Result local = new Result(0L);
					if (package1.getName().equals(decompsedDependant[0])) {
						switch (decompsedDependant[1]) {
						case ">=":
							if (package1.getVersion().compareTo(decompsedDependant[2]) >= 0) {
								isMatched = true;
							}
							break;
						case ">":
							if (package1.getVersion().compareTo(decompsedDependant[2]) > 0) {
								isMatched = true;
							}
							break;
						case "<=":
							if (package1.getVersion().compareTo(decompsedDependant[2]) <= 0) {
								isMatched = true;
							}
							break;
						case "<":
							if (package1.getVersion().compareTo(decompsedDependant[2]) < 0) {
								isMatched = true;
							}
							break;
						case "=":
							if (package1.getVersion().compareTo(decompsedDependant[2]) == 0) {
								isMatched = true;
							}
							break;
						case "":
							isMatched = true;
							break;
						}
						if (isMatched && !Utils.conflictsExist(package1, installedPackages, repositories)) {
							if ((installed = installPackage(local, package1, installedPackages, repositories,
									breakloop))) {
								if (disjunct.getCost() > local.getCost()) {
									for (String cmd : disjunct.getCommands()) {
										installedPackages.remove(cmd.substring(1));
									}
									disjunct = local;
									for (String cmd : disjunct.getCommands()) {
										installedPackages.add(cmd.substring(1));
									}
								}
							}
							breakloop.remove(breakloop.size() - 1);
						}
					}
				}
			}
			for (String cmd : disjunct.getCommands()) {
				if (conjuct.getCommands().contains(cmd)) {
					String[] command = decomposeConstraint(cmd.substring(1), repositories);
					for (Package package1 : repositories) {
						if (package1.getName().equals(command[0]) && package1.getVersion().equals(command[2])) {
							disjunct.setCost(disjunct.getCost() - package1.getSize());
						}
					}
				}
			}
			conjuct.setCost(conjuct.getCost() + disjunct.getCost());
			conjuct.getCommands().addAll(disjunct.getCommands());
		}

		if (installed) {
			result.setCost(conjuct.getCost() + pack.getSize());
			result.getCommands().addAll(conjuct.getCommands());
			result.getCommands().add("+" + pack.getName() + "=" + pack.getVersion());
			return true;
		} else
			return false;
	}

	public static boolean uninstallPackage(Result result, Package pack, List<String> installedPackages,
			List<Package> repositories) {
		boolean uninstalled = false;
		if (pack != null && (pack.getDepends() == null || pack.getDepends().isEmpty())) {
			installedPackages.remove(pack.getName() + "=" + pack.getVersion());
			result.setCost(result.getCost() + Utils.uninstallCost);
			result.getCommands().add("-" + pack.getName() + "=" + pack.getVersion());
			return true;
		}
		return uninstalled;
	}
}
