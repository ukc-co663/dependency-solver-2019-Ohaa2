package depsolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;

public class Main {

	public Main() {
	}

	public static void main(String[] args) throws IOException {
		long t1 = new Date().getTime();
		TypeReference<List<Package>> repoType = new TypeReference<List<Package>>() {
		};
		List<Package> repos = JSON.parseObject(Utils.readFile(args[0]), repoType);

		TypeReference<List<String>> strListType = new TypeReference<List<String>>() {
		};

		List<String> initials = JSON.parseObject(Utils.readFile(args[1]), strListType);
		Map<String, List<String>> initialPackages = new HashMap<String, List<String>>();
		for (String s : initials) {
			String[] inits = s.split("=");
			List<String> versions = initialPackages.get(inits[0]);
			if (versions == null) {
				versions = new ArrayList<String>();
			}
			versions.add(inits[1]);
			initialPackages.put(inits[0], versions);
		}

		List<String> cons = JSON.parseObject(Utils.readFile(args[2]), strListType);
		Map<String, Map<String, Map<String, List<String>>>> constraints = new HashMap<String, Map<String, Map<String, List<String>>>>();
		for (String s : cons) {
			switch (s.charAt(0)) {
			case '+':
				Map<String, Map<String, List<String>>> installs = constraints.get("+");
				if (installs == null) {
					installs = new HashMap<String, Map<String, List<String>>>();
				}

				String sign = null;
				if (s.substring(1).indexOf(">=") != -1) {
					sign = ">=";
					Map<String, List<String>> packs = installs.get(">=");
					if (packs == null) {
						packs = new HashMap<String, List<String>>();
					}
					List<String> versions = packs.get(s.substring(1, s.indexOf(">=")));
					if (versions == null) {
						versions = new ArrayList<String>();
					}
					versions.add(s.substring(s.indexOf(">=") + 1));
					packs.put(s.substring(1, s.indexOf(">=")), versions);
					installs.put(">=", packs);
				}

				if (s.substring(1).indexOf(">") != -1) {
					sign = ">";
					Map<String, List<String>> packs = installs.get(">");
					if (packs == null) {
						packs = new HashMap<String, List<String>>();
					}
					List<String> versions = packs.get(s.substring(1, s.indexOf(">")));
					if (versions == null) {
						versions = new ArrayList<String>();
					}
					versions.add(s.substring(s.indexOf(">") + 1));
					packs.put(s.substring(1, s.indexOf(">")), versions);
					installs.put(">", packs);
				}

				if (s.substring(1).indexOf("<=") != -1) {
					sign = "<=";
					Map<String, List<String>> packs = installs.get("<=");
					if (packs == null) {
						packs = new HashMap<String, List<String>>();
					}
					List<String> versions = packs.get(s.substring(1, s.indexOf("<=")));
					if (versions == null) {
						versions = new ArrayList<String>();
					}
					versions.add(s.substring(s.indexOf("<=") + 1));
					packs.put(s.substring(1, s.indexOf("<=")), versions);
					installs.put("<=", packs);
				}

				if (s.substring(1).indexOf("<") != -1) {
					sign = "<";
					Map<String, List<String>> packs = installs.get("<");
					if (packs == null) {
						packs = new HashMap<String, List<String>>();
					}
					List<String> versions = packs.get(s.substring(1, s.indexOf("<")));
					if (versions == null) {
						versions = new ArrayList<String>();
					}
					versions.add(s.substring(s.indexOf("<") + 1));
					packs.put(s.substring(1, s.indexOf("<")), versions);
					installs.put("<", packs);
				}

				if (s.substring(1).indexOf("=") != -1) {
					sign = "=";
					Map<String, List<String>> packs = installs.get("=");
					if (packs == null) {
						packs = new HashMap<String, List<String>>();
					}
					List<String> versions = packs.get(s.substring(1, s.indexOf("=")));
					if (versions == null) {
						versions = new ArrayList<String>();
					}
					versions.add(s.substring(s.indexOf("=") + 1));
					packs.put(s.substring(1, s.indexOf("=")), versions);
					installs.put("=", packs);
				}

				if (sign == null) {
					sign = ".";
					Map<String, List<String>> packs = installs.get(".");
					if (packs == null) {
						packs = new HashMap<String, List<String>>();
					}
					packs.put(s.substring(1), null);
					installs.put(".", packs);
				}

				constraints.put("+", installs);
				break;
			}
			// case '-':
			// List<String> uninstalls = constraints.get("-");
			// if (uninstalls == null) {
			// uninstalls = new ArrayList<String>();
			// }
			// uninstalls.add(s.substring(1));
			// constraints.put("-", uninstalls);
			// }
		}

		Map<String, Map<String, Package>> repositories = new HashMap<String, Map<String, Package>>();
		for (Package p : repos) {
			Map<String, Package> versions = repositories.get(p.getName());
			if (versions == null) {
				versions = new HashMap<String, Package>();
			}
			versions.put(p.getVersion(), p);
			repositories.put(p.getName(), versions);
		}
		long lowestCost = Long.MAX_VALUE;
		List<String> commands = null;
		// if (constraints.get("-") != null) {
		// for (String uninstall : constraints.get("-")) {
		//
		// }
		// }

		Result result = null;

		if (constraints.get("+") != null) {
			if (constraints.get("+").get(".") != null) {
				while (lowestCost == Long.MAX_VALUE) {
					for (String name : constraints.get("+").get(".").keySet()) {
						for (String version : repositories.get(name).keySet()) {
							result = new Result();
							if (Utils.installPackage(result, repositories.get(name).get(version), initialPackages,
									repositories)) {
								if (result.getCost() < lowestCost) {
									lowestCost = result.getCost();
									commands = new ArrayList<String>();
									for (String cmd : result.getCommands()) {
										commands.add(cmd);
									}
								}
							}
						}
					}
					if (lowestCost == Long.MAX_VALUE) {
						int maxConflicts = 0;
						for (String p1 : result.getConflicts().keySet()) {
							for (String ver : result.getConflicts().get(p1).keySet()) {
								if (maxConflicts < result.getConflicts().get(p1).get(ver).intValue()) {
									initialPackages.get(p1).remove(ver);
										
									result.setCost(1000000L);
									result.setCommands(new ArrayList<String>());
									result.getCommands().add("-" + p1 + "=" + ver);
								}
							}
						}
					}
				}
			}

		}

		System.out.println(lowestCost);
		System.out.println(JSONObject.toJSONString(commands));
		long t2 = new Date().getTime();
		System.out.println((t2 - t1) + " milliseconds");
	}
}