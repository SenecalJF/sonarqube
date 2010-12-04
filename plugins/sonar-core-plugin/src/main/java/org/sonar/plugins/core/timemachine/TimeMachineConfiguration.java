/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.timemachine;

import com.google.common.collect.Lists;
import org.apache.commons.configuration.Configuration;
import org.sonar.api.BatchExtension;
import org.sonar.api.CoreProperties;

import java.util.Collections;
import java.util.List;

public class TimeMachineConfiguration implements BatchExtension {

  private static final int NUMBER_OF_VARIATION_SNAPSHOTS = 3;

  private final Configuration configuration;
  private List<PastSnapshot> projectPastSnapshots;

  public TimeMachineConfiguration(Configuration configuration, PastSnapshotFinder variationSnapshotFinder) {
    this.configuration = configuration;
    initVariationSnapshots(variationSnapshotFinder);
  }

  private void initVariationSnapshots(PastSnapshotFinder variationSnapshotFinder) {
    projectPastSnapshots = Lists.newLinkedList();
    for (int index = 1; index <= NUMBER_OF_VARIATION_SNAPSHOTS; index++) {
      PastSnapshot variationSnapshot = variationSnapshotFinder.find(configuration, index);
      if (variationSnapshot != null) {
        projectPastSnapshots.add(variationSnapshot);
      }
    }
  }

  /**
   * for unit tests
   */
  TimeMachineConfiguration(Configuration configuration, List<PastSnapshot> projectPastSnapshots) {
    this.configuration = configuration;
    this.projectPastSnapshots = projectPastSnapshots;
  }

  /**
   * for unit tests
   */
  TimeMachineConfiguration(Configuration configuration) {
    this.configuration = configuration;
    this.projectPastSnapshots = Collections.emptyList();
  }


  public boolean skipTendencies() {
    return configuration.getBoolean(CoreProperties.SKIP_TENDENCIES_PROPERTY, CoreProperties.SKIP_TENDENCIES_DEFAULT_VALUE);
  }

  public int getTendencyPeriodInDays() {
    return configuration.getInt(CoreProperties.CORE_TENDENCY_DEPTH_PROPERTY, CoreProperties.CORE_TENDENCY_DEPTH_DEFAULT_VALUE);
  }

  public List<PastSnapshot> getProjectPastSnapshots() {
    return projectPastSnapshots;
  }
}
