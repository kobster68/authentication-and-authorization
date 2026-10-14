package edu.rit.swen755.auth.docs;

import edu.rit.swen755.auth.access.AccessRule;
import edu.rit.swen755.auth.access.AccessRules;
import edu.rit.swen755.auth.authn.DemoUser;
import edu.rit.swen755.auth.authn.DemoUsers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Acceptance tests for the README and the design document (slice 4): the README's URL and user
 * tables cannot drift from the code, every section file exists with its heading, and no TODO
 * marker is left. Maven runs tests from the project root, so the paths are relative to it.
 */
@Disabled("TODO(slice 4: Mallikarjuna) — enable when implementing")
class DocsConsistencyTest {

    private static final Path README = Path.of("README.md");
    private static final Path SECTIONS = Path.of("docs", "sections");

    private static final Map<String, String> SECTION_HEADINGS = new LinkedHashMap<>();

    static {
        SECTION_HEADINGS.put("01-purpose.md", "# 1. Purpose and requirements");
        SECTION_HEADINGS.put("02-scenarios.md", "# 2. Security scenarios");
        SECTION_HEADINGS.put("03-tactics.md", "# 3. Tactics and where they live");
        SECTION_HEADINGS.put("04-privilege-matrix.md", "# 4. Privilege matrix");
        SECTION_HEADINGS.put("05-security-view.md", "# 5. Security view");
        SECTION_HEADINGS.put("06-sequence-diagrams.md", "# 6. Sequence diagrams");
        SECTION_HEADINGS.put("07-tactics-questionnaire.md", "# 7. Tactics questionnaire");
        SECTION_HEADINGS.put("08-decisions.md", "# 8. Design decisions");
        SECTION_HEADINGS.put("09-verification.md", "# 9. Verification");
    }

    @Test
    void readmeListsEveryUrlPattern() throws IOException {
        String readme = Files.readString(README);
        for (AccessRule rule : AccessRules.all()) {
            assertTrue(readme.contains(rule.pattern()), "README is missing the URL " + rule.pattern());
        }
    }

    @Test
    void readmeListsEveryUser() throws IOException {
        String readme = Files.readString(README);
        for (DemoUser user : DemoUsers.all()) {
            assertTrue(readme.contains("`" + user.username() + "`"), "README is missing the user " + user.username());
        }
    }

    @Test
    void everySectionExistsWithItsHeading() throws IOException {
        for (Map.Entry<String, String> section : SECTION_HEADINGS.entrySet()) {
            Path file = SECTIONS.resolve(section.getKey());
            assertTrue(Files.isRegularFile(file), "missing " + file);
            assertTrue(Files.readAllLines(file).contains(section.getValue()),
                    file + " should contain the heading line '" + section.getValue() + "'");
        }
    }

    @Test
    void noTodoMarkersRemainOutsideTheVerificationSection() throws IOException {
        List<Path> files = new ArrayList<>(List.of(README));
        for (String name : SECTION_HEADINGS.keySet()) {
            if (!name.equals("09-verification.md")) {
                files.add(SECTIONS.resolve(name));
            }
        }
        assertNoTodo(files);
    }

    @Test
    @Disabled("TODO(integration) — enable in the integration pull request, once the verification section is filled")
    void noTodoMarkersRemainAnywhere() throws IOException {
        List<Path> files = new ArrayList<>(List.of(README));
        for (String name : SECTION_HEADINGS.keySet()) {
            files.add(SECTIONS.resolve(name));
        }
        assertNoTodo(files);
    }

    private static void assertNoTodo(List<Path> files) throws IOException {
        for (Path file : files) {
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                assertTrue(!lines.get(i).contains("TODO("), file + ":" + (i + 1) + " still has a TODO marker: " + lines.get(i));
            }
        }
    }
}
