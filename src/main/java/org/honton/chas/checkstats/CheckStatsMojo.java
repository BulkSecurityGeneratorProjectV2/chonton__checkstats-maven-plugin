package org.honton.chas.checkstats;

import java.io.File;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import lombok.SneakyThrows;

/**
 * verify stats have not decreased
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY)
public class CheckStatsMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Component
    private MavenProjectHelper projectHelper;

    /**
     * The artifact resolver
     */
    @Component
    private RepositorySystem repoSystem;

    /**
     * The current repository/network configuration of Maven.
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    /**
     * The project's remote repositories to use for the resolution of plugins
     * and their dependencies.
     */
    @Parameter(defaultValue = "${project.remotePluginRepositories}", readonly = true)
    private List<RemoteRepository> remoteRepos;

    /**
     * Location of the stats file.
     */
    @Parameter(defaultValue = "${project.build.directory}/stats.json", property = "output", required = true)
    private File statsFile;

    public void execute() throws MojoExecutionException {
        try {
            doWork();
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    public void doWork() throws MojoExecutionException {
        if (project.getPackaging().equals("pom")) {
            return; // no stats for aggregators
        }
        ProjectStats currentStats = aggregateCurrentStats();
        currentStats.write(statsFile);

        Artifact attachement = getAttachmentArtifact("stats");
        ProjectStats priorStats = readAttachedStats(attachement);
        if (priorStats != null) {
            logFailures(priorStats.checkIsBetter(currentStats));
        }
        projectHelper.attachArtifact(project, attachement.getExtension(), attachement.getClassifier(), statsFile);
    }

    @SneakyThrows
    private void logFailures(List<Failure> failures) {
        for (Failure failure : failures) {
            getLog().error(failure.toString());
        }
        if (!failures.isEmpty()) {
            throw new MojoExecutionException("degraded statistics");
        }
    }

    private ProjectStats aggregateCurrentStats() {
        File srcDir = new File(project.getBuild().getSourceDirectory());
        File targetDir = new File(project.getBuild().getDirectory());
        
        ProjectStats stats = new ProjectStats();
        stats.addSrcFiles(srcDir);

        for (StandardStatistic standard : StandardStatistic.values()) {
            getLog().debug("merging reportRootStat " + standard);
            Stat stat = standard.read(getLog(), srcDir, targetDir);
            if (stat != null) {
                getLog().info(standard.name() + " has " + stat.getSize() + " stats");
                stats.addStat(standard.name(), stat);
            } else {
                getLog().debug("null reportRootStat");
            }
        }
        return stats;
    }

    @Nonnull
    Artifact getAttachmentArtifact(@Nonnull String classifier) {
        return new DefaultArtifact(project.getGroupId(), project.getArtifactId(), classifier, project.getPackaging(),
                project.getVersion());
    }

    @Nullable
    ProjectStats readAttachedStats(@Nonnull Artifact artifact) {
        File statsFile = getArtifact(artifact);
        if (statsFile == null) {
            return null;
        }
        return ProjectStats.read(statsFile);
    }

    @Nullable
    File getArtifact(@Nonnull Artifact artifact) {
        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(artifact);
        request.setRepositories(remoteRepos);

        getLog().info("Resolving artifact " + artifact + " from " + remoteRepos);

        try {
            ArtifactResult result = repoSystem.resolveArtifact(repoSession, request);
            return result.getArtifact().getFile();
        } catch (ArtifactResolutionException are) {
            getLog().warn("could not find " + artifact);
            return null;
        }
    }
}
