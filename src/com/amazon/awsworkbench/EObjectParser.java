package com.amazon.awsworkbench;


import java.io.ByteArrayInputStream;


import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

import org.eclipse.emf.ecore.EStructuralFeature;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazon.aws.workbench.model.awsworkbench.AppBuilder_core;
import com.amazon.aws.workbench.model.awsworkbench.Block;
import com.amazon.awsworkbench.data.ComponentObject;
import com.amazon.awsworkbench.dependency.Analyser;
import com.amazon.awsworkbench.util.ProjectUtils;

import awsworkbench.design.Activator;

public class EObjectParser {

	private String[] mandatoryFields = { "varName", "identifier" };

	private Map<String, ComponentObject> componentObjectMap = new HashMap<String, ComponentObject>();

	private Map<String, SortedSet<String>> uniqueMandatoryFieldValues = new HashMap<String, SortedSet<String>>();

	private Map<String, List<String>> classNameToVariablesMap = new HashMap<String, List<String>>();

	private Set<String> importStatements = new TreeSet<String>();

	private Set<String> cdkRepos = new TreeSet<String>();

	private Set<String> constructRepos = new TreeSet<String>();

	private StringBuilder codeOutput = new StringBuilder();

	private ComponentObject appObject;

	private Analyser graphAnalyser = new Analyser();

	private String className = new String("SampleCDK");

	private AppBuilder_core rootObject;

	public void generateCode(AppBuilder_core self) throws Exception {

		if (self.getMainClassName() == null || self.getMainClassName().trim().length() == 0)
			showError("Main Class Name not defined!!");

		if (className != null)
			this.className = self.getMainClassName();

		if (self == null)
			return;
		
		rootObject = self;

		componentObjectMap = new HashMap<String, ComponentObject>();
		uniqueMandatoryFieldValues = new HashMap<String, SortedSet<String>>();
		classNameToVariablesMap = new HashMap<String, List<String>>();
		graphAnalyser = new Analyser();

		codeOutput = new StringBuilder();
		parse(self, new ArrayList<String>(), true, null);

		buildDependencyGraph();

		System.out.println("\n\n\n");
		graphAnalyser.checkCycles();

		generateImportsAndPOMrepos();

		graphAnalyser.topologicalSort();

		String imports = getImports();

		String variables = getResourcesVariables();

		String generatedCode = getGeneratedCode();

		String assembledCode = assembleCode(imports, generatedCode, className);

		String helperCode = generateHelperClass(imports, className);

		String formattedSourceCode = getFormatterCode(assembledCode);

		String formattedHelperCode = getFormatterCode(helperCode);

		System.out.println(formattedSourceCode);

		System.out.println(formattedHelperCode);

		generateProject(formattedSourceCode, formattedHelperCode);

	}

	private String getResourcesVariables() {

		StringBuilder resourceVars = new StringBuilder();
		for (ComponentObject c : graphAnalyser.getQueue()) {

			resourceVars.append("public static " + c.getGeneratedClassName() + " " + c.getVarName() + ";\n");
		}

		resourceVars.append("\n\n");
		return resourceVars.toString();
	}

	private void generateProject(String formattedSourceCode, String formatterHelperCode) throws Exception {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(rootObject.getProjectName());

		boolean newProject = false;
		IFolder srcMainJava;
		IFolder srcMain;
		IFolder srcTest;
		IFolder src;
		IPackageFragment pack;

		IProgressMonitor monitor = new NullProgressMonitor();
		if (!project.exists()) {

			newProject = true;
			project.create(monitor);

		}

		project.open(monitor);

		IJavaProject javaProject = JavaCore.create(project);
		List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();

		if (newProject) {

			// Let's add JavaSE-1.8 to our classpath

			IExecutionEnvironmentsManager executionEnvironmentsManager = JavaRuntime.getExecutionEnvironmentsManager();
			IExecutionEnvironment[] executionEnvironments = executionEnvironmentsManager.getExecutionEnvironments();
			for (IExecutionEnvironment iExecutionEnvironment : executionEnvironments) {
				// We will look for JavaSE-1.8 as the JRE container to add to our classpath
				if ("JavaSE-1.8".equals(iExecutionEnvironment.getId())) {
					entries.add(JavaCore.newContainerEntry(JavaRuntime.newJREContainerPath(iExecutionEnvironment)));
					break;
				}
			}

			IProjectDescription description = project.getDescription();
			description.setNatureIds(new String[] { JavaCore.NATURE_ID, "org.eclipse.m2e.core.maven2Nature" });

			ArrayList<ICommand> builders = new ArrayList<ICommand>();

			final ICommand java = description.newCommand();
			java.setBuilderName(JavaCore.BUILDER_ID);
			builders.add(java);

			final ICommand mvn_schema = description.newCommand();
			mvn_schema.setBuilderName("org.eclipse.m2e.core.maven2Builder");
			builders.add(mvn_schema);

			description.setBuildSpec(builders.toArray(new ICommand[builders.size()]));

			project.setDescription(description, monitor);

			// src
			src = project.getFolder("src");
			src.create(true, true, monitor);

			// src/main
			srcMain = src.getFolder("main");
			srcMain.create(true, true, monitor);

			srcTest = src.getFolder("test");
			srcTest.create(true, true, monitor);

			// src/main/java
			srcMainJava = srcMain.getFolder("java");
			srcMainJava.create(true, true, monitor);

			srcTest.getFolder("java").create(true, true, monitor);
			project.getFolder("target").create(true, true, monitor);

			final IClasspathEntry srcClasspathEntry = JavaCore.newSourceEntry(srcMainJava.getFullPath());
			entries.add(0, srcClasspathEntry);

			javaProject.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), monitor);

			pack = javaProject.getPackageFragmentRoot(srcMainJava).createPackageFragment(rootObject.getPackageName(),
					false, null);

			ICompilationUnit cuHelper = pack.createCompilationUnit(className + "Helper.java", formatterHelperCode,
					false, null);

			IFile cdkJsonFile = project.getFile("cdk.json");
			cdkJsonFile.create(new ByteArrayInputStream(ProjectUtils.cdkJson.getBytes()), false, monitor);

		}
		src = project.getFolder("src");
		srcMain = src.getFolder("main");
		srcMainJava = srcMain.getFolder("java");

		pack = javaProject.getPackageFragmentRoot(srcMainJava).getPackageFragment(rootObject.getPackageName());

		ICompilationUnit cu = pack.createCompilationUnit(className + ".java", formattedSourceCode, true, null);

		String pomContents = ProjectUtils.generatePOM(rootObject.getPackageName(), rootObject.getProjectName(),
				rootObject.getPackageName() + "." + className,
				constructRepos.toArray(new String[constructRepos.size()]),
				cdkRepos.toArray(new String[cdkRepos.size()]), "1.55.0");

		IFile pomFile = project.getFile("pom.xml");
		pomFile.delete(true, monitor);
		pomFile.create(new ByteArrayInputStream(pomContents.getBytes()), true, monitor);

	}

	private String getFormatterCode(String code) {

		CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(null);

		TextEdit textEdit = codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT, code, 0, code.length(), 0, null);
		IDocument doc = new Document(code);
		try {
			textEdit.apply(doc);
			return doc.get();

		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		return null;

	}

	private String assembleCode(String imports, String generatedCode, String className) {
		StringBuilder assembledCodeBuilder = new StringBuilder();

		assembledCodeBuilder.append(imports + "\n");

		assembledCodeBuilder.append("public class " + className + "{\n ");

		assembledCodeBuilder.append(getResourcesVariables());

		assembledCodeBuilder.append("public static void main(String args[]) {\n ");

		assembledCodeBuilder.append(generatedCode + "\n");

		assembledCodeBuilder.append(className + "Helper.setup();\n");

		assembledCodeBuilder.append(appObject.getVarName() + ".synth();\n } \n } \n");

		return assembledCodeBuilder.toString();
	}

	private String generateHelperClass(String imports, String className) {

		StringBuilder helperCode = new StringBuilder();

		helperCode.append(imports + "\n");

		helperCode.append(
				"// Changes made to this class will not be overwritten when " + className + " is regenerated :-) \n");

		helperCode.append("public class " + className + "Helper" + "{\n ");

		helperCode.append("public static " + className + " " + className.toLowerCase() + ";\n");

		helperCode.append(" public static void setup(){\n}\n }\n");

		return helperCode.toString();

	}

	private void generateApp() {

		String code = appObject.generateCode(componentObjectMap);

	}

	private void printComponents() {
		for (ComponentObject c : componentObjectMap.values()) {

			// System.out.println(c);
		}

	}

	private String getGeneratedCode() {

		StringBuilder generatedCodeBuilder = new StringBuilder();

		for (ComponentObject c : graphAnalyser.getQueue()) {
			String generatedCode = c.generateCode(componentObjectMap);

			generatedCodeBuilder.append(generatedCode + "\n");
		}

		return generatedCodeBuilder.toString();

	}

	private void buildDependencyGraph() throws Exception {

		Map<String, ComponentObject> graphAdditions = new HashMap<String, ComponentObject>();

		for (ComponentObject dependentObject : componentObjectMap.values()) {
			if (!graphAdditions.containsKey(dependentObject.getVarName())) {
				graphAdditions.put(dependentObject.getVarName(), dependentObject);
				graphAnalyser.addVariable(dependentObject.getVarName(), dependentObject);
			}
		}

		for (ComponentObject dependentObject : graphAdditions.values()) {
			Map<String, List<String>> dependencies = dependentObject.getDependencies();

			// System.out.println (dependentObject.getVarName() + " " + dependencies);

			for (String key : dependencies.keySet()) {
				List<String> varList = dependencies.get(key);

				for (String vars : varList) {
					if (graphAdditions.containsKey(vars)) {
						ComponentObject neededObject = graphAdditions.get(vars);
						if (key.equals(neededObject.getGeneratedClassName())) {
							graphAnalyser.addDependency(neededObject.getVarName(), dependentObject.getVarName());
						} else {
							System.out.println("Mismatch : " + vars + " does not belong to class : " + key);
							// cObject.removeDependency(key, vars);

						}

					} else {
						showError("Variable not defined: " + vars + " for class : " + key);
						dependentObject.removeDependency(key, vars);
					}
				}

			}
		}

	}

	private void generateAppAndStackClass() {
		// TODO Auto-generated method stub

	}

	private String getImports() {

		StringBuilder importsBuilder = new StringBuilder();

		importsBuilder.append("package " + rootObject.getPackageName() + ";\n\n");

		for (String statement : importStatements) {
			// System.out.print(statement);
			importsBuilder.append(statement + "\n");
		}

		return importsBuilder.toString();

	}

	private void generateImportsAndPOMrepos() {

		importStatements.add("import java.util.*;");
		for (ComponentObject currentObject : componentObjectMap.values()) {

			String genClassName = currentObject.getGeneratedClassName();

			String starImport = genClassName.substring(0, genClassName.lastIndexOf("."));
			starImport = starImport + ".*";
			importStatements.add("import " + starImport + ";");

			String constructSearch = "awsconstructs.services.";
			String cdkSearch = "awscdk.services.";

			if (genClassName.indexOf(constructSearch) != -1) {

				int startIndex = genClassName.indexOf(constructSearch) + constructSearch.length();

				constructRepos.add(genClassName.substring(startIndex, genClassName.indexOf('.', startIndex)));

			} else if (genClassName.indexOf(cdkSearch) != -1) {
				int startIndex = genClassName.indexOf(cdkSearch) + cdkSearch.length();

				cdkRepos.add(genClassName.substring(startIndex, genClassName.indexOf('.', startIndex)));

			}

		}

		System.out.println(constructRepos);
		System.out.println(cdkRepos);

	}

	public void parse(EObject currentEcoreObject, List<String> parents, boolean isApp, String parentVarname)
			throws Exception {

		if (currentEcoreObject instanceof Block) {

			Block blockObject = (Block) currentEcoreObject;

			if (blockObject.isCanDeploy()) {

				EList<EObject> children = currentEcoreObject.eContents();
				for (EObject e : children)
					parse(e, parents, false, parentVarname);

			}

		} else {

			checkMandatoryFields(currentEcoreObject);

			ComponentObject componentObject = new ComponentObject(currentEcoreObject, parents, parentVarname);
			if (isApp) {
				appObject = componentObject;

			}
			componentObjectMap.put(componentObject.getVarName(), componentObject);
			if (classNameToVariablesMap.containsKey(componentObject.getGeneratedClassName()))
				classNameToVariablesMap.get(componentObject.getGeneratedClassName()).add(componentObject.getVarName());
			else {
				classNameToVariablesMap.put(componentObject.getGeneratedClassName(), new ArrayList<String>());
				classNameToVariablesMap.get(componentObject.getGeneratedClassName()).add(componentObject.getVarName());
			}

			parents.add(componentObject.getVarName());
			EList<EObject> children = currentEcoreObject.eContents();
			for (EObject e : children)
				parse(e, parents, false, componentObject.getVarName());
			parents.remove(componentObject.getVarName());
		}

	}

	private void checkMandatoryFields(EObject self) throws Exception {

		// TODO Auto-generated method stub
		EList<EStructuralFeature> allEStructFeats = self.eClass().getEAllStructuralFeatures();

		for (EStructuralFeature eStructuralFeature : allEStructFeats) {
			if (isMandatory(eStructuralFeature)) {

				if ((self.eGet(eStructuralFeature) == null
						|| self.eGet(eStructuralFeature).toString().trim().isEmpty()))
					showError("Mandatory fields (Var Name or Identifier) are empty for some components");

				String keyName = eStructuralFeature.getName();
				String value = self.eGet(eStructuralFeature).toString().trim();

				if (uniqueMandatoryFieldValues.containsKey(keyName)) {
					if (uniqueMandatoryFieldValues.get(keyName).contains(value))
						showError("Duplicate value: " + value + " found for " + keyName);
					else
						uniqueMandatoryFieldValues.get(keyName).add(value);

				} else {

					uniqueMandatoryFieldValues.put(keyName, new TreeSet<String>());
					uniqueMandatoryFieldValues.get(keyName).add(value);

				}

			}
		}

	}

	public static void showError(String errorMessage) throws Exception {
		Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, errorMessage);
		StatusManager.getManager().handle(status, StatusManager.BLOCK);
		throw new Exception(errorMessage);

	}

	private boolean isMandatory(EStructuralFeature eStructuralFeature) {
		String fieldName = eStructuralFeature.getName().trim();
		for (String mandatoryField : mandatoryFields) {
			if (fieldName.equals(mandatoryField))
				return true;

		}
		return false;
	}

}
