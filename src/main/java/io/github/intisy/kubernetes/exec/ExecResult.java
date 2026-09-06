package io.github.intisy.kubernetes.exec;

/**
 * Outcome of a pod exec: the reassembled stdout and stderr, and the process exit code.
 */
public final class ExecResult {
    private final String stdout;
    private final String stderr;
    private final int exitCode;

    public ExecResult(String stdout, String stderr, int exitCode) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.exitCode = exitCode;
    }

    public String stdout() {
        return stdout;
    }

    public String stderr() {
        return stderr;
    }

    public int exitCode() {
        return exitCode;
    }
}
