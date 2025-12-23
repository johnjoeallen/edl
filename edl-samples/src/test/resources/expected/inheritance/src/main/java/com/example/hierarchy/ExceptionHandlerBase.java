package com.example.hierarchy;

import java.lang.Object;
import java.lang.String;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class ExceptionHandlerBase {
  protected static final Object CONTAINER_TEMPLATE = Map.of();

  protected Map<String, Object> mapResponse(Map<String, Object> info) {
    Map<String, Object> body = new LinkedHashMap<>();
    if (info.containsKey("source")) {
      body.put("Source", info.get("source"));
    }
    if (info.containsKey("code")) {
      body.put("ReasonCode", info.get("code"));
    }
    if (info.containsKey("description")) {
      body.put("Description", info.get("description"));
    }
    if (info.containsKey("details")) {
      body.put("Details", info.get("details"));
    }
    if (info.containsKey("recoverable")) {
      body.put("Recoverable", info.get("recoverable"));
    }
    return body;
  }

  protected Object renderContainerTemplate(Object template, List<Map<String, Object>> infos) {
    if (template instanceof Map) {
      Map<String, Object> result = new LinkedHashMap<>();
      Map<String, Object> map = (Map<String, Object>) template;
      for (Map.Entry<String, Object> entry : map.entrySet()) {
        result.put(entry.getKey(), renderContainerTemplate(entry.getValue(), infos));
      }
      return result;
    }
    if (template instanceof List) {
      List list = (List) template;
      if (list.size() == 1) {
        List<Object> rendered = new ArrayList<>();
        for (Map<String, Object> info : infos) {
          rendered.add(renderValue(list.get(0), info));
        }
        return rendered;
      }
      return list;
    }
    return template;
  }

  protected Object renderValue(Object template, Map<String, Object> info) {
    if (template instanceof Map) {
      Map<String, Object> result = new LinkedHashMap<>();
      Map<String, Object> map = (Map<String, Object>) template;
      for (Map.Entry<String, Object> entry : map.entrySet()) {
        result.put(entry.getKey(), renderValue(entry.getValue(), info));
      }
      return result;
    }
    if (template instanceof List) {
      List list = (List) template;
      List<Object> rendered = new ArrayList<>();
      for (Object entry : list) {
        rendered.add(renderValue(entry, info));
      }
      return rendered;
    }
    if (template instanceof String) {
      String key = (String) template;
      if (info.containsKey(key)) {
        return info.get(key);
      }
      return key;
    }
    return template;
  }
}
