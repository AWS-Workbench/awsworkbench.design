package com.amazon.awsworkbench.util;

public class ProjectUtils {

	public static final String pomTemplate = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<project xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\"\n"
			+ "         xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
			+ "    <modelVersion>4.0.0</modelVersion>\n" + "\n" + "    <groupId>{groupID}</groupId>\n"
			+ "    <artifactId>{artifactID}</artifactId>\n" + "    <version>0.1</version>\n" + "\n"
			+ "    <properties>\n" + "        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n"
			+ "        <cdk.version>{cdk.version}</cdk.version>\n" + "    </properties>\n" + "\n" + "    <build>\n"
			+ "        <plugins>\n" + "            <plugin>\n"
			+ "                <groupId>org.apache.maven.plugins</groupId>\n"
			+ "                <artifactId>maven-compiler-plugin</artifactId>\n"
			+ "                <version>3.8.1</version>\n" + "                <configuration>\n"
			+ "                    <source>1.8</source>\n" + "                    <target>1.8</target>\n"
			+ "                </configuration>\n" + "            </plugin>\n" + "\n" + "            <plugin>\n"
			+ "                <groupId>org.codehaus.mojo</groupId>\n"
			+ "                <artifactId>exec-maven-plugin</artifactId>\n"
			+ "                <version>1.6.0</version>\n" + "                <configuration>\n"
			+ "                    <mainClass>{mainClass}</mainClass>\n" + "                </configuration>\n"
			+ "            </plugin>\n" + "        </plugins>\n" + "    </build>\n" + "\n" + "    <dependencies>\n"
			+ "        <!-- AWS Cloud Development Kit -->\n" + "{contructDependencies}\n"
			+ "        <!-- https://mvnrepository.com/artifact/junit/junit -->\n" + "        <dependency>\n"
			+ "            <groupId>junit</groupId>\n" + "            <artifactId>junit</artifactId>\n"
			+ "            <version>4.12</version>\n" + "            <scope>test</scope>\n" + "        </dependency>\n"
			+ "    </dependencies>\n" + "</project>\n";

	public static final String constructDependencyTemplate = "<dependency>\n"
			+ "    <groupId>software.amazon.awsconstructs</groupId>\n"
			+ "    <artifactId>{constructArtifact}</artifactId>\n" + "    <version>{cdk.version}</version>\n"
			+ "</dependency>\n";

	public static final String cdkDependencyTemplate = "<dependency>\n"
			+ "    <groupId>software.amazon.awscdk</groupId>\n" + "    <artifactId>{cdkArtifact}</artifactId>\n"
			+ "    <version>{cdk.version}</version>\n" + "</dependency>\n";

	public static String[] constructRepos = { "core" };
	public static String[] cdkRepos = { "ecs-patterns" };

	public static String generatePOM(String groupID, String artifactID, String mainClass,
			String[] projectConstructRepos, String[] projectCdkRepos, String version) {

		String newTemplate = new String(pomTemplate);

		newTemplate = newTemplate.replace("{groupID}", groupID);
		newTemplate = newTemplate.replace("{artifactID}", artifactID);
		newTemplate = newTemplate.replace("{mainClass}", mainClass);
		
		

		StringBuilder constructDependencies = new StringBuilder();

		for (String cdkDependency : cdkRepos) {
			constructDependencies.append(cdkDependencyTemplate.replace("{cdkArtifact}", cdkDependency));

		}

		for (String cdkDependency : projectCdkRepos) {
			constructDependencies.append(cdkDependencyTemplate.replace("{cdkArtifact}", cdkDependency));

		}

		for (String constructDependency : constructRepos) {
			constructDependencies
					.append(constructDependencyTemplate.replace("{constructArtifact}", constructDependency));

		}
		for (String constructDependency : projectConstructRepos) {
			constructDependencies
					.append(constructDependencyTemplate.replace("{constructArtifact}", constructDependency));

		}

		newTemplate = newTemplate.replace("{contructDependencies}", constructDependencies.toString());
		
		newTemplate = newTemplate.replaceAll("\\{cdk.version\\}", version);

		return newTemplate;

	}
	
	
	

}
