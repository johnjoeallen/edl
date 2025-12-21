package com.edl.core;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

public final class YamlLoader {
  public YamlDocument load(Path path) throws IOException {
    LoaderOptions options = new LoaderOptions();
    options.setAllowDuplicateKeys(false);
    Yaml yaml = new Yaml(new SafeConstructor(options));
    try (Reader reader = Files.newBufferedReader(path)) {
      Node node = yaml.compose(reader);
      if (node == null) {
        return new YamlDocument(null, Map.of());
      }
      Map<String, Mark> marks = new LinkedHashMap<>();
      Object data = build(node, "", marks);
      return new YamlDocument(data, marks);
    }
  }

  private Object build(Node node, String path, Map<String, Mark> marks) {
    if (node instanceof MappingNode mappingNode) {
      LinkedHashMap<String, Object> map = new LinkedHashMap<>();
      for (NodeTuple tuple : mappingNode.getValue()) {
        Node keyNode = tuple.getKeyNode();
        if (!(keyNode instanceof ScalarNode scalarKey)) {
          continue;
        }
        String key = scalarKey.getValue();
        String nextPath = path.isEmpty() ? key : path + "." + key;
        marks.put(nextPath, keyNode.getStartMark());
        Object value = build(tuple.getValueNode(), nextPath, marks);
        map.put(key, value);
      }
      return map;
    }
    if (node instanceof SequenceNode sequenceNode) {
      List<Object> list = new ArrayList<>();
      int index = 0;
      for (Node item : sequenceNode.getValue()) {
        String nextPath = path + "[" + index + "]";
        list.add(build(item, nextPath, marks));
        index += 1;
      }
      return list;
    }
    if (node instanceof ScalarNode scalarNode) {
      Tag tag = scalarNode.getTag();
      String value = scalarNode.getValue();
      if (Tag.INT.equals(tag)) {
        try {
          return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
          return value;
        }
      }
      if (Tag.BOOL.equals(tag)) {
        return Boolean.parseBoolean(value);
      }
      return value;
    }
    return null;
  }
}
