# jDEC

<p align="center">
  <img src="https://img.shields.io/github/license/materna-se/jdec.svg?style=flat-square">
  <img src="https://img.shields.io/circleci/build/github/materna-se/jdec.svg?style=flat-square">
  <img src="https://img.shields.io/maven-central/v/com.github.materna-se/jdec?style=flat-square">
  <img src="https://img.shields.io/nexus/s/com.github.materna-se/jdec?server=https%3A%2F%2Foss.sonatype.org&label=maven-snapshot&style=flat-square">
</p>

jDEC is a Java library designed to provide a unified interface for executing decisions. The decisions can be modeled with [DMN](https://www.omg.org/spec/DMN) (Decision Model and Notation) or with Java.

## Download

jDEC is published to the Maven Central Repository. To get the release versions, you can add the following dependency to your your build management tool. In addition, snapshot versions are published at more regular intervals to the Maven Snapshot Repository. All available versions are also tagged and can be seen at https://github.com/materna-se/jdec/releases.

### Maven
```xml
<dependency>
    <groupId>com.github.materna-se</groupId>
    <artifactId>jdec</artifactId>
    <version>VERSION</version>
</dependency>
```
If you want to download the snapshot versions, the following repository must also be added:
```xml
<repository>
    <id>sonatype-snapshot-repository</id>
    <url>http://oss.sonatype.org/content/repositories/snapshots</url>
    <releases>
        <enabled>false</enabled>
    </releases>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
</repository>
```

### Gradle
```gradle
compile group: 'com.github.materna-se', name: 'jdec', version: 'VERSION'
```

## Documentation

Decisions can be modeled with DMN with applications like the [Trisotech Decision Modeler](https://www.trisotech.com/digital-modeling-suite), the [ACTICO Platform](https://www.actico.com/platform/dmn-decision-model-notation) or the [Red Hat Decision Manager](https://www.redhat.com/de/technologies/jboss-middleware/decision-manager). After the decisions are modeled, they can be exported as XML:
```xml
<definitions namespace="https://github.com/agilepro/dmn-tck" name="0003-input-data-string-allowed-values" id="_0003-input-data-string-allowed-values" xmlns="http://www.omg.org/spec/DMN/20180521/MODEL/">
    <itemDefinition name="tEmploymentStatus">
        <typeRef>string</typeRef>
        <allowedValues>
            <text>"UNEMPLOYED","EMPLOYED","SELF-EMPLOYED","STUDENT"</text>
        </allowedValues>
    </itemDefinition>
    <decision name="Employment Status Statement" id="d_EmploymentStatusStatement">
        <variable typeRef="string" name="Employment Status Statement"/>
        <informationRequirement id="f4a0451b-8db5-401a-b9b4-dc31416b6e7d">
            <requiredInput href="#i_EmploymentStatus"/>
        </informationRequirement>
        <literalExpression>
            <text>"You are " + Employment Status</text>
        </literalExpression>
    </decision>
    <inputData name="Employment Status" id="i_EmploymentStatus">
        <variable typeRef="tEmploymentStatus" name="Employment Status"/>
    </inputData>
</definitions>
```

In addition to DMN, the decisions can be modeled with Java by extending the abstract class `DecisionModel`: 
```java
package de.materna.jdec.java.test;

import de.materna.jdec.java.DecisionModel;
import de.materna.jdec.model.ComplexInputStructure;
import de.materna.jdec.model.ExecutionResult;
import de.materna.jdec.model.InputStructure;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class EmploymentStatusDecision extends DecisionModel {
	@Override
	public ComplexInputStructure getInputStructure() {
		Map<String, InputStructure> inputs = new LinkedHashMap<>();

		InputStructure inputStructure = new InputStructure("string", Arrays.asList("UNEMPLOYED", "EMPLOYED", "SELF-EMPLOYED", "STUDENT"));
		inputs.put("Employment Status", inputStructure);

		return new ComplexInputStructure("object", inputs);
	}

	@Override
	public Map<String, Object> executeDecision(Map<String, Object> inputs) {
		Map<String, Object> output = new HashMap<>();
		output.put("Employment Status Statement", "You are " + inputs.get("Employment Status"));
		return output;
	}
}
```

Once the decisions are modeled, they can be used with jDEC. For this purpose, a DecisionSession can be created with three different modes to choose from:
- `DMNDecisionSession`: Accepts only DMN decisions, components to execute Java decisions are not imported.
- `JavaDecisionSession`: Accepts only Java decisions, components to execute DMN decisions are not imported.
- `HybridDecisionSession`: Accepts DMN and Java decisions, even referencing a decision of one language in a decision of another language during execution is possible.

If `DMNDecisionSession` or `HybridDecisionSession` is used, DMN decisions can be imported and executed like this:
```java
DecisionSession decisionSession = new DMNDecisionSession();
decisionSession.importModel("https://github.com/agilepro/dmn-tck", "0003-input-data-string-allowed-values", /* Decision Source */);

Map<String, Object> inputs = new HashMap<>();
inputs.put("Employment Status", "UNEMPLOYED");

Map<String, Object> outputs = decisionSession.executeModel("https://github.com/agilepro/dmn-tck", "0003-input-data-string-allowed-values", inputs);
System.out.println("executeHashMap(): " + outputs);

Assertions.assertTrue(outputs.containsKey("Employment Status Statement"));
Assertions.assertEquals("You are UNEMPLOYED", outputs.get("Employment Status Statement"));
```

If `JavaDecisionSession` or `HybridDecisionSession` is used, Java decisions can be imported and executed like this:
```java
DecisionSession decisionSession = new JavaDecisionSession();
decisionSession.importModel("de.materna.jdec.java.test", "EmploymentStatusDecision", /* Decision Source */);

Map<String, Object> inputs = new HashMap<>();
inputs.put("Employment Status", "UNEMPLOYED");

Map<String, Object> outputs = decisionSession.executeModel("de.materna.jdec.java.test", "EmploymentStatusDecision", inputs);
System.out.println("executeHashMap(): " + outputs);

Assertions.assertTrue(outputs.containsKey("Employment Status Statement"));
Assertions.assertEquals("You are UNEMPLOYED", outputs.get("Employment Status Statement"));
```
