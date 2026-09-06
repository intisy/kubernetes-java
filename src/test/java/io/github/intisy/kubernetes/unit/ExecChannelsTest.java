package io.github.intisy.kubernetes.unit;

import io.github.intisy.kubernetes.exec.ExecChannels;
import io.github.intisy.kubernetes.exec.ExecIncompleteException;
import io.github.intisy.kubernetes.exec.ExecResult;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecChannelsTest {
    private byte[] frame(int channel, String payload) {
        byte[] body = payload.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[body.length + 1];
        out[0] = (byte) channel;
        System.arraycopy(body, 0, out, 1, body.length);
        return out;
    }

    @Test
    void separatesStdoutFromStderr() {
        ExecChannels channels = new ExecChannels();

        channels.accept(frame(1, "spisor-1b1-probe\n"));
        channels.accept(frame(2, "Defaulted container\n"));

        ExecResult result = channels.result();
        assertEquals("spisor-1b1-probe\n", result.stdout());
        assertEquals("Defaulted container\n", result.stderr());
    }

    @Test
    void reassemblesOutputSplitAcrossFrames() {
        ExecChannels channels = new ExecChannels();

        channels.accept(frame(1, "spisor-"));
        channels.accept(frame(1, "1b1-probe"));

        assertEquals("spisor-1b1-probe", channels.result().stdout());
    }

    @Test
    void readsExitZeroFromASuccessStatus() {
        ExecChannels channels = new ExecChannels();

        channels.accept(frame(3, "{\"metadata\":{},\"status\":\"Success\"}"));

        assertEquals(0, channels.result().exitCode());
    }

    @Test
    void readsANonZeroExitFromTheStatusCause() {
        ExecChannels channels = new ExecChannels();

        channels.accept(frame(3, "{\"status\":\"Failure\",\"reason\":\"NonZeroExitCode\","
                + "\"details\":{\"causes\":[{\"reason\":\"ExitCode\",\"message\":\"2\"}]}}"));

        assertEquals(2, channels.result().exitCode());
    }

    @Test
    void treatsAnEmptyFrameAsAKeepAliveRatherThanData() {
        ExecChannels channels = new ExecChannels();

        channels.accept(frame(1, "spisor-1b1-probe"));
        channels.accept(new byte[]{(byte) 1});

        assertEquals("spisor-1b1-probe", channels.result().stdout());
    }

    @Test
    void requireCompletedResultReturnsTheRealExitCodeOnceAStatusFrameArrived() throws Exception {
        ExecChannels channels = new ExecChannels();

        channels.accept(frame(1, "row-count: 1\n"));
        channels.accept(frame(3, "{\"status\":\"Success\"}"));

        assertTrue(channels.isCompleted());
        ExecResult result = channels.requireCompletedResult("should not be used");
        assertEquals(0, result.exitCode());
        assertEquals("row-count: 1\n", result.stdout());
    }

    @Test
    void requireCompletedResultThrowsWhenNoStatusFrameEverArrived() {
        ExecChannels channels = new ExecChannels();

        channels.accept(frame(1, "partial"));

        assertFalse(channels.isCompleted());
        ExecIncompleteException exception = assertThrows(ExecIncompleteException.class,
                () -> channels.requireCompletedResult("timed out waiting for the pod"));
        assertEquals("timed out waiting for the pod", exception.getMessage());
        assertEquals("partial", exception.partialStdout());
    }
}
