package org.jbpm.integration.spec.service;

import javax.management.ObjectName;

import org.jboss.bpm.api.model.Node;
import org.jboss.bpm.api.model.ProcessDefinition;
import org.jboss.bpm.api.model.Signal;
import org.jboss.bpm.api.model.Signal.SignalType;
import org.jboss.bpm.api.model.builder.SignalBuilder;
import org.jboss.bpm.api.service.ProcessDefinitionService;
import org.jboss.bpm.api.service.ProcessEngine;
import org.jboss.bpm.api.service.SignalBuilderService;
import org.jboss.bpm.api.service.SignalService;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.GraphElement;
import org.jbpm.graph.def.Node.NodeType;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.integration.spec.model.ProcessDefinitionImpl;
import org.jbpm.signal.EventService;
import org.jbpm.svc.Service;
import org.jbpm.svc.ServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventServiceFactory implements ServiceFactory
{
  // provide logging
  final static Logger log = LoggerFactory.getLogger(EventServiceFactory.class);

  private static final long serialVersionUID = 1L;

  private ProcessEngine engine;

  public EventServiceFactory(ProcessEngine engine)
  {
    this.engine = engine;
  }

  public Service openService()
  {
    return new EventServiceImpl();
  }

  public void close()
  {
  }

  class EventServiceImpl implements EventService
  {
    private static final long serialVersionUID = 1L;

    public void fireEvent(String eventType, GraphElement graphElement, ExecutionContext executionContext)
    {
      SignalBuilderService sigBuilderService = engine.getService(SignalBuilderService.class);
      SignalService sigService = engine.getService(SignalService.class);

      if (sigBuilderService != null && sigService != null)
      {
        SignalBuilder sigBuilder = sigBuilderService.getSignalBuilder();

        SignalType sigType = getSignalType(eventType, graphElement);
        ObjectName fromRef = getFromRef(graphElement);
        if (sigType != null && fromRef != null)
        {
          Signal signal = sigBuilder.newSignal(sigType, fromRef, null);
          sigService.throwSignal(signal);
        }
        else
        {
          log.debug("Cannot map to signal: [" + eventType + "," + graphElement + "]");
        }
      }
    }

    private ObjectName getFromRef(GraphElement graphElement)
    {
      ObjectName fromRef = null;
      
      ProcessDefinitionService pdService = engine.getService(ProcessDefinitionService.class);
      org.jbpm.graph.def.ProcessDefinition oldProcDef = graphElement.getProcessDefinition();
      ObjectName pdKey = ProcessDefinitionImpl.getKey(oldProcDef);
      ProcessDefinition procDef = pdService.getProcessDefinition(pdKey);
      
      if (graphElement instanceof org.jbpm.graph.def.ProcessDefinition)
      {
        fromRef = pdKey;
      }
      else if (graphElement instanceof org.jbpm.graph.def.Node)
      {
        org.jbpm.graph.def.Node oldNode = (org.jbpm.graph.def.Node)graphElement;
        Node node = procDef.getNode(oldNode.getNameExt());
        fromRef = node.getKey();
      }
      
      return fromRef;
    }

    private SignalType getSignalType(String eventType, GraphElement graphElement)
    {
      NodeType nodeType = null;
      if (graphElement instanceof org.jbpm.graph.def.Node)
      {
        org.jbpm.graph.def.Node oldNode = (org.jbpm.graph.def.Node)graphElement;
        nodeType = oldNode.getNodeType();
      }

      SignalType sigType = null;
      if (Event.EVENTTYPE_PROCESS_START.equals(eventType))
      {
        sigType = SignalType.SYSTEM_PROCESS_ENTER;
      }
      else if (Event.EVENTTYPE_PROCESS_END.equals(eventType))
      {
        sigType = SignalType.SYSTEM_PROCESS_EXIT;
      }
      else if (Event.EVENTTYPE_BEFORE_SIGNAL.equals(eventType) && nodeType == NodeType.StartState)
      {
        sigType = SignalType.SYSTEM_START_EVENT_ENTER;
      }
      else if (Event.EVENTTYPE_NODE_LEAVE.equals(eventType) && nodeType == NodeType.StartState)
      {
        sigType = SignalType.SYSTEM_START_EVENT_EXIT;
      }
      else if (Event.EVENTTYPE_NODE_ENTER.equals(eventType) && nodeType == NodeType.EndState)
      {
        sigType = SignalType.SYSTEM_END_EVENT_ENTER;
      }
      else if (Event.EVENTTYPE_NODE_LEAVE.equals(eventType) && nodeType == NodeType.EndState)
      {
        sigType = SignalType.SYSTEM_END_EVENT_EXIT;
      }
      else if (Event.EVENTTYPE_NODE_ENTER.equals(eventType) && nodeType == NodeType.State)
      {
        sigType = SignalType.SYSTEM_TASK_ENTER;
      }
      else if (Event.EVENTTYPE_NODE_LEAVE.equals(eventType) && nodeType == NodeType.State)
      {
        sigType = SignalType.SYSTEM_TASK_EXIT;
      }
      return sigType;
    }

    public void close()
    {
    }
  }
}
