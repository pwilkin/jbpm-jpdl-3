package org.jbpm.context.exe;

import java.lang.reflect.Proxy;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import org.jbpm.context.def.ContextDefinition;
import org.jbpm.context.exe.converter.SerializableToByteArrayConverter;
import org.jbpm.context.exe.matcher.HibernateLongIdMatcher;
import org.jbpm.context.exe.matcher.HibernateStringIdMatcher;
import org.jbpm.context.exe.variableinstance.ByteArrayInstance;
import org.jbpm.context.exe.variableinstance.HibernateLongInstance;
import org.jbpm.db.AbstractDbTestCase;
import org.jbpm.db.hibernate.ConverterEnumType;
import org.jbpm.db.hibernate.Converters;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;

/**
 * regressions of the Hibernate 6 port in storing process variables.
 */
public class Hibernate6VariableDbTest extends AbstractDbTestCase {

  ProcessDefinition processDefinition;
  ProcessInstance processInstance;
  ContextInstance contextInstance;

  protected void setUp() throws Exception {
    super.setUp();
    processDefinition = new ProcessDefinition(getName());
    processDefinition.addDefinition(new ContextDefinition());
    graphSession.saveProcessDefinition(processDefinition);
    processInstance = new ProcessInstance(processDefinition);
    contextInstance = processInstance.getContextInstance();
  }

  protected void tearDown() throws Exception {
    newTransaction();
    graphSession.deleteProcessDefinition(processDefinition.getId());
    super.tearDown();
  }

  /** Hibernate 6 getMetamodel().entity() throws for non-entities; the matchers must just say no. */
  public void testIdMatchersRejectNonEntities() {
    HashMap<String, String> map = new HashMap<>();
    assertFalse(new HibernateLongIdMatcher().matches(map));
    assertFalse(new HibernateStringIdMatcher().matches(map));
    assertFalse(new HibernateLongIdMatcher().matches("text"));
    assertTrue(new HibernateLongIdMatcher().matches(processDefinition));
    assertFalse(new HibernateStringIdMatcher().matches(processDefinition));
  }

  public void testNonEntityAndEntityVariablesRoundTrip() {
    HashMap<String, String> map = new HashMap<>();
    map.put("k", "v");
    contextInstance.setVariable("map", map);
    contextInstance.setVariable("str", "text");
    contextInstance.setVariable("pd", processDefinition);

    processInstance = saveAndReload(processInstance);
    contextInstance = processInstance.getContextInstance();

    assertEquals(map, contextInstance.getVariable("map"));
    assertEquals("text", contextInstance.getVariable("str"));
    assertEquals(processDefinition.getId(), ((ProcessDefinition) contextInstance.getVariable("pd")).getId());

    TokenVariableMap variables = contextInstance.getTokenVariableMap(processInstance.getRootToken());
    VariableInstance mapInstance = variables.getVariableInstance("map");
    assertTrue(mapInstance instanceof ByteArrayInstance);
    assertTrue(mapInstance.converter instanceof SerializableToByteArrayConverter);
    assertTrue(variables.getVariableInstance("pd") instanceof HibernateLongInstance);
  }

  /** hbm2ddl must create CONVERTER_ as CHAR(1) like jBPM 3 schemas, not CHAR(255). */
  public void testConverterColumnHasOneChar() {
    contextInstance.setVariable("map", new HashMap<String, String>());
    jbpmContext.save(processInstance);
    newTransaction();

    final int[] columnSize = { -1 };
    session.doWork(connection -> {
      DatabaseMetaData metaData = connection.getMetaData();
      try (ResultSet columns = metaData.getColumns(null, null, "JBPM_VARIABLEINSTANCE", "CONVERTER_")) {
        assertTrue("CONVERTER_ column not found", columns.next());
        columnSize[0] = columns.getInt("COLUMN_SIZE");
      }
    });
    assertEquals(1, columnSize[0]);

    String converterId = Converters.getConverterId(Converters.getConverterByClassName(SerializableToByteArrayConverter.class.getName()));
    Object stored = session.createNativeQuery("select CONVERTER_ from JBPM_VARIABLEINSTANCE where NAME_ = 'map'", Object.class)
      .uniqueResult();
    assertEquals(converterId, String.valueOf(stored));
  }

  /** a CONVERTER_ column created wider than one char reads back space-padded; it must still resolve. */
  public void testConverterIdIsTrimmedOnRead() throws Exception {
    final String converterId = Converters.getConverterId(Converters.getConverterByClassName(SerializableToByteArrayConverter.class.getName()));
    final Map<String, Object> state = new HashMap<>();
    ResultSet resultSet = (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { ResultSet.class },
      (proxy, method, args) -> {
        if (method.getName().equals("getString")) {
          state.put("read", Boolean.TRUE);
          return converterId + "    ";
        }
        if (method.getName().equals("wasNull")) return Boolean.FALSE;
        throw new UnsupportedOperationException(method.toString());
      });

    Object converter = new ConverterEnumType().nullSafeGet(resultSet, 1, null, null);
    assertEquals(Boolean.TRUE, state.get("read"));
    assertTrue(converter instanceof SerializableToByteArrayConverter);
  }
}
