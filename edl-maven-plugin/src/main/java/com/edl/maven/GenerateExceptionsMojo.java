package com.edl.maven;

import com.edl.core.CompilationResult;
import com.edl.core.CompilerOptions;
import com.edl.core.Diagnostic;
import com.edl.core.DiagnosticSeverity;
import com.edl.core.EdlCompiler;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "generate-exceptions", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public final class GenerateExceptionsMojo extends AbstractMojo {
  @Parameter(property = "edl.specFile", required = true)
  private File specFile;

  @Parameter(property = "edl.outputDirectory", defaultValue = "${project.build.directory}/generated-sources/edl")
  private File outputDirectory;

  @Parameter(property = "edl.failOnWarnings", defaultValue = "false")
  private boolean failOnWarnings;

  @Parameter(property = "edl.generateDocs", defaultValue = "false")
  private boolean generateDocs;

  @Parameter(property = "edl.generateSpringHandler", defaultValue = "false")
  private boolean generateSpringHandler;

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  @Override
  public void execute() throws MojoExecutionException {
    if (specFile == null || !specFile.exists()) {
      throw new MojoExecutionException("EDL specFile does not exist: " + specFile);
    }

    EdlCompiler compiler = new EdlCompiler();
    CompilationResult result;
    try {
      result = compiler.compile(specFile.toPath(), outputDirectory.toPath(),
          new CompilerOptions(generateDocs, generateSpringHandler));
    } catch (Exception ex) {
      throw new MojoExecutionException("Failed to compile EDL spec", ex);
    }

    List<Diagnostic> diagnostics = result.getDiagnostics();
    for (Diagnostic diagnostic : diagnostics) {
      if (diagnostic.getSeverity() == DiagnosticSeverity.ERROR) {
        getLog().error(diagnostic.format());
      } else {
        getLog().warn(diagnostic.format());
      }
    }

    boolean hasErrors = diagnostics.stream().anyMatch(d -> d.getSeverity() == DiagnosticSeverity.ERROR);
    boolean hasWarnings = diagnostics.stream().anyMatch(d -> d.getSeverity() == DiagnosticSeverity.WARNING);
    if (hasErrors) {
      throw new MojoExecutionException("EDL compilation failed with errors");
    }
    if (hasWarnings && failOnWarnings) {
      throw new MojoExecutionException("EDL compilation failed due to warnings");
    }

    Path outputPath = outputDirectory.toPath();
    Path sourceRoot = outputPath.resolve("src").resolve("main").resolve("java");
    project.addCompileSourceRoot(sourceRoot.toString());
    getLog().info("EDL generated " + result.getGeneratedFiles().size() + " files into " + outputPath);
  }
}
