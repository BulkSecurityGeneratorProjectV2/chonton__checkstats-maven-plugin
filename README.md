# checkstats-maven-plugin
Keep your Jacoco, Findbugs, Pmd, or Cpd error statistics decreasing as code is added.

When starting a new maven project, it's relatively straight-forward to enforce code hygiene.  You add unit and integration tests to verify code functionality; add checkstyle, findbugs, cpd, and pmd to guard code quality, and add jacoco to validate code coverage.  Finally, you add rules for each plugin to enforce minimum standards.

When you inherit a project that doesn't have these basics, it's difficult to start enforcing minimum standards.  Anyone on the project can add new code without new unit tests.  It's tough to determine what's legacy code which gets a free pass and what's new code which must adhere to the new standards.  Typically, we give up in despair and the code continues in it's sorry state.

Checkstats helps you climb out of this state by failing any build which does not keep or decrease the prior build's quality statistics.  The supported statistics are checkstyle's error count, cpd's duplicate count,  pmd's violations, findbug's bugCount, and jacoco's missed coverage percentage.

Checkstats provides a single goal, "check".  This goal parses the xml output reports of checkstyle, cpd, pmd, findbugs, and jacoco.  To ensure these reports are available, the "check" goal is attached by default to the "verify" phase.  The supported reports should be attached to earlier phases, usually "validate", or "prepare-package"/"post-integration-test" for jacoco.

If a report exists, checkstats compares the code quality statistic
with the statistic from the prior successful build. Once all quality statistics are successfully verified, they are saved as an attached maven artifact with classifier "stats".  This attached artifact will be saved as part of the deploy phase.  All of a development team and automation will share the code quality statistics.  Because it is an attached artifact, the quality metric baseline is reset with each project version change.

Following is a typical use of checkstats:

```xml
  <build>
    <plugins>

      <plugin>
        <groupId>org.honton.chas</groupId>
        <artifactId>checkstats-maven-plugin</artifactId>
        <version>0.0.1</version>
        <executions>
          <execution>
            <goals>
              <goal>check</goal>
            </goals>
          </execution>
        </executions>
      </plugin>

      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-checkstyle-plugin</artifactId>
        <version>2.17</version>
        <executions>
          <execution>
            <goals>
              <goal>check</goal>
            </goals>
            <phase>validate</phase>
            <configuration>
              <configLocation>google_checks.xml</configLocation>
            </configuration>
          </execution>
        </executions>
      </plugin>

      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-pmd-plugin</artifactId>
        <version>3.5</version>
        <configuration>
          <skipEmptyReport>false</skipEmptyReport>
        </configuration>
        <executions>
          <execution>
            <id>cpd</id>
            <goals>
              <goal>cpd</goal>
            </goals>
            <phase>validate</phase>
          </execution>
          <execution>
            <id>pmd</id>
            <goals>
              <goal>pmd</goal>
            </goals>
            <phase>validate</phase>
          </execution>
        </executions>
      </plugin>

      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>findbugs-maven-plugin</artifactId>
        <version>3.0.2</version>
        <configuration>
          <effort>Max</effort>
          <threshold>Low</threshold>
        </configuration>
        <executions>
          <execution>
            <id>analyze-compile</id>
            <goals>
              <goal>findbugs</goal>
            </goals>
            <phase>process-classes</phase>
          </execution>
        </executions>
      </plugin>

      <plugin>
        <groupId>org.jacoco</groupId>
        <artifactId>jacoco-maven-plugin</artifactId>
        <version>0.7.5.201505241946</version>
        <executions>
          <execution>
            <id>default-prepare-agent</id>
            <goals>
              <goal>prepare-agent</goal>
            </goals>
          </execution>
          <execution>
            <id>default-report</id>
            <goals>
              <goal>report</goal>
            </goals>
            <phase>prepare-package</phase>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
```
