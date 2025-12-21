package com.edl.core;

import java.util.Map;
import org.yaml.snakeyaml.error.Mark;

public final class YamlDocument {
  private final Object data;
  private final Map<String, Mark> marks;

  public YamlDocument(Object data, Map<String, Mark> marks) {
    this.data = data;
    this.marks = marks;
  }

  public Object getData() {
    return data;
  }

  public Map<String, Mark> getMarks() {
    return marks;
  }
}
