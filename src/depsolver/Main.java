package depsolver;

import java.io.IOException;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

public class Main {

	public Main() {
	}

	public static void main(String[] args) throws IOException {
		TypeReference<List<Package>> repoType = new TypeReference<List<Package>>() {
		};
		List<Package> repo = JSON.parseObject(Utils.readFile(args[0]), repoType);
		TypeReference<List<String>> strListType = new TypeReference<List<String>>() {
		};
		List<String> initial = JSON.parseObject(Utils.readFile(args[1]), strListType);
		List<String> constraints = JSON.parseObject(Utils.readFile(args[2]), strListType);

		for (Package p : repo) {
			System.out.printf("package %s version %s\n", p.getName(), p.getVersion());
			for (List<String> clause : p.getDepends()) {
				System.out.printf("  dep:");
				for (String q : clause) {
					System.out.printf(" %s", q);
				}
				System.out.printf("\n");
			}
		}
	}
}