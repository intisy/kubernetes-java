package io.github.intisy.kubernetes.model;

import com.google.gson.annotations.SerializedName;

/**
 * Kubernetes API server version information.
 *
 * @author Finn Birich
 */
public class VersionInfo {
    @SerializedName("major")
    private String major;

    @SerializedName("minor")
    private String minor;

    @SerializedName("gitVersion")
    private String gitVersion;

    @SerializedName("gitCommit")
    private String gitCommit;

    @SerializedName("gitTreeState")
    private String gitTreeState;

    @SerializedName("buildDate")
    private String buildDate;

    @SerializedName("goVersion")
    private String goVersion;

    @SerializedName("compiler")
    private String compiler;

    @SerializedName("platform")
    private String platform;

    public String getMajor() { return major; }
    public String getMinor() { return minor; }
    public String getGitVersion() { return gitVersion; }
    public String getGitCommit() { return gitCommit; }
    public String getGitTreeState() { return gitTreeState; }
    public String getBuildDate() { return buildDate; }
    public String getGoVersion() { return goVersion; }
    public String getCompiler() { return compiler; }
    public String getPlatform() { return platform; }

    @Override
    public String toString() {
        return "VersionInfo{" +
                "gitVersion='" + gitVersion + '\'' +
                ", platform='" + platform + '\'' +
                ", goVersion='" + goVersion + '\'' +
                '}';
    }
}
