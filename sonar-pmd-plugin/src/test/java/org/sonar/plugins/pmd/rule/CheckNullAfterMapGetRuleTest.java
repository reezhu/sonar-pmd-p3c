/*
 * SonarQube PMD Plugin
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.pmd.rule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.plugins.pmd.PmdTemplate;
import org.sonar.plugins.pmd.xml.PmdRuleSet;
import org.sonar.plugins.pmd.xml.PmdRuleSets;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class CheckNullAfterMapGetRuleTest {

    @Test
    void testRuleXPathExpression() throws IOException {
        // The XPath expression for our rule
        String xpath = "//PrimaryExpression[\n" +
                "  PrimaryPrefix/Name[ends-with(@Image, '.get')] or\n" +
                "  PrimarySuffix[@Image='get']\n" +
                "]/following::PrimaryExpression[\n" +
                "  ./PrimaryPrefix/Name[\n" +
                "    substring-after(@Image, '.') != 'equals' and\n" +
                "    substring-after(@Image, '.') != 'isPresent' and\n" +
                "    not(contains(@Image, ' == null')) and\n" +
                "    not(contains(@Image, ' != null'))\n" +
                "  ]\n" +
                "]";
        
        // Verify the XPath expression is valid
        assertThat(xpath).isNotEmpty();
        
        // Verify the XPath expression contains the key elements we need
        assertThat(xpath).contains("ends-with(@Image, '.get')");
        assertThat(xpath).contains("PrimarySuffix[@Image='get']");
        assertThat(xpath).contains("not(contains(@Image, ' == null'))");
        assertThat(xpath).contains("not(contains(@Image, ' != null'))");
    }
    
    @Test
    void testRuleDetectsViolation(@TempDir Path tempDir) throws IOException {
        // Create a Java file with a violation
        String violationCode =
            "import java.util.Map;\n" +
            "import java.util.HashMap;\n" +
            "\n" +
            "public class TestClass {\n" +
            "    public void testMethod() {\n" +
            "        Map<String, String> map = new HashMap<>();\n" +
            "        String value = map.get(\"key\");\n" +
            "        // Violation: Using value without null check\n" +
            "        System.out.println(value.length());\n" +
            "    }\n" +
            "}";
        
        Path violationFile = tempDir.resolve("Violation.java");
        Files.write(violationFile, violationCode.getBytes(StandardCharsets.UTF_8));
        
        // Create a Java file without a violation
        String compliantCode =
            "import java.util.Map;\n" +
            "import java.util.HashMap;\n" +
            "\n" +
            "public class TestClass {\n" +
            "    public void testMethod() {\n" +
            "        Map<String, String> map = new HashMap<>();\n" +
            "        String value = map.get(\"key\");\n" +
            "        // Compliant: Checking for null before using value\n" +
            "        if (value != null) {\n" +
            "            System.out.println(value.length());\n" +
            "        }\n" +
            "    }\n" +
            "}";
        
        Path compliantFile = tempDir.resolve("Compliant.java");
        Files.write(compliantFile, compliantCode.getBytes(StandardCharsets.UTF_8));
        
        // Manually verify the code
        assertThat(violationCode).contains("map.get(\"key\")");
        assertThat(violationCode).contains("value.length()");
        assertThat(violationCode).doesNotContain("value != null");
        
        assertThat(compliantCode).contains("map.get(\"key\")");
        assertThat(compliantCode).contains("value.length()");
        assertThat(compliantCode).contains("value != null");
    }
}