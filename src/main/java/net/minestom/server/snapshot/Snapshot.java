package net.minestom.server.snapshot;

import org.jetbrains.annotations.ApiStatus;

/**
 * Represents a snapshot of a game object.
 * <p>
 * Implementations must be valued-based (immutable & not relying on identity).
 */
@ApiStatus.Experimental
public sealed interface Snapshot permits
        ServerSnapshot, InstanceSnapshot,
        EntitySnapshot, ChunkSnapshot,
        SectionSnapshot, InventorySnapshot {
}