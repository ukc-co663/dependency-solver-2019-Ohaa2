package depsolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

public class Main {

	public Main() {
	}

	public static void main(String[] args) throws IOException {
		TypeReference<List<Package>> repoType = new TypeReference<List<Package>>() {
		};
		List<Package> repositories = JSON.parseObject(Utils.readFile(args[0]), repoType);
		Collections.sort(repositories, Collections.reverseOrder());
		// Map<String, Package> repositories = new HashMap<String, Package>();
		// for (Package package1 : repos) {
		// repositories.put(package1.getName() + "=" + package1.getVersion(), package1);
		// }

		TypeReference<List<String>> strListType = new TypeReference<List<String>>() {
		};

		List<String> initialPackages = JSON.parseObject(Utils.readFile(args[1]), strListType);

		List<String> allConstraints = JSON.parseObject(Utils.readFile(args[2]), strListType);

		boolean installed = false;
		Result finalResult = new Result(0L);
		// do {
		for (String constraint : allConstraints) {
			String[] decompsedConstraint = Utils.decomposeConstraint(constraint.substring(1), repositories);

			switch (constraint.substring(0, 1)) {
			case "+":
				Result wildCardResult = new Result(Long.MAX_VALUE);
				for (Package package1 : repositories) {
					boolean isMatched = false;
					Result result = new Result(0L);
					if (package1.getName().equals(decompsedConstraint[0])) {
						switch (decompsedConstraint[1]) {
						case ">=":
							if (package1.getVersion().compareTo(decompsedConstraint[2]) >= 0) {
								isMatched = true;
							}
							break;
						case ">":
							if (package1.getVersion().compareTo(decompsedConstraint[2]) > 0) {
								isMatched = true;
							}
							break;
						case "<=":
							if (package1.getVersion().compareTo(decompsedConstraint[2]) <= 0) {
								isMatched = true;
							}
							break;
						case "<":
							if (package1.getVersion().compareTo(decompsedConstraint[2]) < 0) {
								isMatched = true;
							}
							break;
						case "=":
							if (package1.getVersion().compareTo(decompsedConstraint[2]) == 0) {
								isMatched = true;
							}
							break;
						case "":
							isMatched = true;
							break;
						}

						if (isMatched && (installed = Utils.installPackage(result, package1, initialPackages,
								repositories, new ArrayList<String>()))) {
							if (decompsedConstraint[1].equals("")) {
								if (wildCardResult.getCost() > result.getCost()) {
									if (wildCardResult.getCost() != Long.MAX_VALUE) {
										finalResult.setCost(finalResult.getCost() - wildCardResult.getCost());
										finalResult.getCommands().removeAll(wildCardResult.getCommands());
									}
									wildCardResult = result;
								} else {
									result = new Result(0L);
								}
							}
							finalResult.setCost(finalResult.getCost() + result.getCost());
							finalResult.getCommands().addAll(result.getCommands());
						}
					}
				}
				break;
			case "-":
				for (Package pack : repositories) {
					boolean matchFound = false;
					Result result = new Result(0L);
					if (pack.getName().equals(decompsedConstraint[0])) {
						switch (decompsedConstraint[1]) {
						case ">=":
							if (pack.getVersion().compareTo(decompsedConstraint[2]) >= 0) {
								matchFound = true;
							}
							break;
						case ">":
							if (pack.getVersion().compareTo(decompsedConstraint[2]) > 0) {
								matchFound = true;
							}
							break;
						case "<=":
							if (pack.getVersion().compareTo(decompsedConstraint[2]) <= 0) {
								matchFound = true;
							}
							break;
						case "<":
							if (pack.getVersion().compareTo(decompsedConstraint[2]) < 0) {
								matchFound = true;
							}
							break;
						case "=":
							if (pack.getVersion().compareTo(decompsedConstraint[2]) == 0) {
								matchFound = true;
							}
							break;
						case "":
							matchFound = true;
							break;
						}

						if (matchFound && Utils.uninstallPackage(result, pack, initialPackages, repositories)) {
							finalResult.setCost(finalResult.getCost() + result.getCost());
							finalResult.getCommands().addAll(result.getCommands());
						}
					}
				}
				break;
			}
		}
		// if (!installed) {
		// finalResult.setCost(Utils.uninstallCost);
		// finalResult.setCommands(new LinkedHashSet<String>());
		// finalResult.getCommands().add("-" + initialPackages.get(0));
		// initialPackages.remove(initialPackages.get(0));
		// }
		// } while (!installed);

		// Utils.writeFile(args[0].substring(0, args[0].lastIndexOf(File.separator) + 1)
		// + "commands.json",
		// JSON.toJSONString(finalResult.getCommands(), true));

		System.out.println(finalResult.getCost());
		System.out.println(JSON.toJSONString(finalResult.getCommands()));
	}
}