/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.rule2.ws;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.WebService;
import org.sonar.check.Cardinality;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.qualityprofile.persistence.ActiveRuleDao;
import org.sonar.server.rule2.RuleService;
import org.sonar.server.rule2.persistence.RuleDao;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import static org.fest.assertions.Assertions.assertThat;

public class RulesWebServiceTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();


  private RulesWebService ws;
  private RuleDao ruleDao;
  private DbSession session;

  WsTester wsTester;


  @Before
  public void setUp() throws Exception {
    tester.clearDataStores();
    ruleDao = tester.get(RuleDao.class);
    ws = tester.get(RulesWebService.class);
    wsTester = new WsTester(ws);
    session = tester.get(MyBatis.class).openSession(false);
  }

  @After
  public void after(){
    session.close();
  }

  @Test
  public void define() throws Exception {

    WebService.Context context = new WebService.Context();
    ws.define(context);

    WebService.Controller controller = context.controller("api/rules2");

    assertThat(controller).isNotNull();
    assertThat(controller.actions()).hasSize(4);
    assertThat(controller.action("search")).isNotNull();
    assertThat(controller.action("show")).isNotNull();
    assertThat(controller.action("tags")).isNotNull();
    assertThat(controller.action("set_tags")).isNotNull();
  }

  @Test
  public void search_no_rules() throws Exception {

    MockUserSession.set();
    WsTester.TestRequest request = wsTester.newGetRequest("api/rules2", "search");
    System.out.println("request.toString() = " + request.toString());

    WsTester.Result result = request.execute();
    assertThat(result.outputAsString()).isEqualTo("{\"total\":0,\"rules\":[],\"activeRules\":[]}");
  }

  @Test
  public void search_2_rules() throws Exception {
    ruleDao.insert(newRuleDto(RuleKey.of("javascript", "S001")), session);
    ruleDao.insert(newRuleDto(RuleKey.of("javascript", "S002")), session);
    session.commit();

    tester.get(RuleService.class).refresh();

    MockUserSession.set();
    WsTester.TestRequest request = wsTester.newGetRequest("api/rules2", "search");
    WsTester.Result result = request.execute();
    //TODO
  }

  @Test
  public void search_active_rules() throws Exception {
    QualityProfileDto profile = newQualityProfile();
    tester.get(QualityProfileDao.class).insert(profile, session);

    RuleDto rule = newRuleDto(RuleKey.of(profile.getLanguage(), "S001"));
    ruleDao.insert(rule,  session);

    ActiveRuleDto activeRule = newActiveRule(profile, rule);
    tester.get(ActiveRuleDao.class).insert(activeRule, session);

    session.commit();
    tester.get(RuleService.class).refresh();


    MockUserSession.set();
    WsTester.TestRequest request = wsTester.newGetRequest("api/rules2", "search");
    WsTester.Result result = request.execute();

//    System.out.println("result = " + result.outputAsString());

  }


  private QualityProfileDto newQualityProfile() {
    return new QualityProfileDto()
      .setLanguage("java")
      .setName("My Profile");
  }

  private RuleDto newRuleDto(RuleKey ruleKey) {
    return new RuleDto()
      .setRuleKey(ruleKey.rule())
      .setRepositoryKey(ruleKey.repository())
      .setName("Rule " + ruleKey.rule())
      .setDescription("Description " + ruleKey.rule())
      .setStatus(RuleStatus.READY.toString())
      .setConfigKey("InternalKey" + ruleKey.rule())
      .setSeverity(Severity.INFO)
      .setCardinality(Cardinality.SINGLE)
      .setLanguage("js")
      .setRemediationFunction("linear")
      .setDefaultRemediationFunction("linear_offset")
      .setRemediationCoefficient("1h")
      .setDefaultRemediationCoefficient("5d")
      .setRemediationOffset("5min")
      .setDefaultRemediationOffset("10h")
      .setEffortToFixDescription(ruleKey.repository() + "." + ruleKey.rule() + ".effortToFix");
  }

  private ActiveRuleDto  newActiveRule(QualityProfileDto profile, RuleDto rule) {
    return ActiveRuleDto.createFor(profile, rule)
      .setInheritance("none")
      .setSeverity("BLOCKER");
  }
}