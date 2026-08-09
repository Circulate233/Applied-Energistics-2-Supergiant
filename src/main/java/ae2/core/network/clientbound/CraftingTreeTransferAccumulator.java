package ae2.core.network.clientbound;

import java.io.ByteArrayOutputStream;

public final class CraftingTreeTransferAccumulator {
    private static final long REQUEST_ID_RANGE = (long) Integer.MAX_VALUE + 1;

    public enum Result {ACCEPTED, COMPLETE, IGNORED, ERROR}

    private int requestId = -1;
    private int expectedChunks;
    private int nextChunk;
    private int totalBytes;
    private ByteArrayOutputStream payload;
    private long lastActivityNanos;

    public record Completion(Result result, byte[] payload) {
    }

    public Result begin(int requestId, int expectedChunks, int totalBytes) {
        int requiredChunks = (totalBytes + CraftingTreeDataPacket.CHUNK_BYTES - 1)
            / CraftingTreeDataPacket.CHUNK_BYTES;
        if (requestId < 0 || expectedChunks <= 0 || expectedChunks > CraftingTreeDataPacket.MAX_CHUNKS
            || totalBytes <= 0 || totalBytes > CraftingTreeDataPacket.MAX_TOTAL_BYTES
            || expectedChunks != requiredChunks) {
            return Result.ERROR;
        }
        if (this.requestId >= 0 && !isNewer(requestId, this.requestId)) {
            return Result.IGNORED;
        }
        this.requestId = requestId;
        this.expectedChunks = expectedChunks;
        this.nextChunk = 0;
        this.totalBytes = totalBytes;
        this.payload = new ByteArrayOutputStream(totalBytes);
        this.lastActivityNanos = System.nanoTime();
        return Result.ACCEPTED;
    }

    public Result chunk(int requestId, int index, byte[] data) {
        if (requestId != this.requestId) return Result.IGNORED;
        if (payload == null || index != nextChunk || data.length == 0
            || data.length > CraftingTreeDataPacket.CHUNK_BYTES
            || payload.size() + data.length > totalBytes) {
            clearPayload();
            return Result.ERROR;
        }
        payload.writeBytes(data);
        nextChunk++;
        lastActivityNanos = System.nanoTime();
        return Result.ACCEPTED;
    }

    public Completion complete(int requestId) {
        if (requestId != this.requestId) {
            return new Completion(Result.IGNORED, null);
        }
        if (payload == null || nextChunk != expectedChunks || payload.size() != totalBytes) {
            clearPayload();
            return new Completion(Result.ERROR, null);
        }

        var result = payload.toByteArray();
        clearPayload();
        return new Completion(Result.COMPLETE, result);
    }

    public Result fail(int requestId) {
        if (requestId < 0) {
            return Result.ERROR;
        }
        if (this.requestId >= 0 && requestId != this.requestId && !isNewer(requestId, this.requestId)) {
            return Result.IGNORED;
        }
        this.requestId = requestId;
        clearPayload();
        return Result.ACCEPTED;
    }

    public boolean timeout(long nowNanos, long maxIdleNanos) {
        if (payload == null || nowNanos - lastActivityNanos < maxIdleNanos) {
            return false;
        }
        clearPayload();
        return true;
    }

    public boolean isActive() {
        return payload != null;
    }

    private static boolean isNewer(int candidate, int current) {
        long distance = (candidate - (long) current + REQUEST_ID_RANGE) % REQUEST_ID_RANGE;
        return distance > 0 && distance <= REQUEST_ID_RANGE / 2;
    }

    private void clearPayload() {
        payload = null;
        expectedChunks = 0;
        nextChunk = 0;
        totalBytes = 0;
        lastActivityNanos = 0;
    }
}
