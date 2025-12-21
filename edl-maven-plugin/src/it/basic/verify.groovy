import java.nio.file.Files
import java.nio.file.Paths

def generated = Paths.get(basedir.toString(), "target", "generated-sources", "edl", "com", "example", "edl", "UserNotFoundException.java")
if (!Files.exists(generated)) {
  throw new IllegalStateException("Expected generated file not found: " + generated)
}
return true
