package net.minestom.server.item.meta;

import net.kyori.adventure.text.Component;
import net.minestom.server.item.Enchantment;
import net.minestom.server.item.ItemMeta;
import net.minestom.server.item.ItemMetaBuilder;
import net.minestom.server.utils.Position;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class CompassMeta extends ItemMeta implements ItemMetaBuilder.Provider<CompassMeta.Builder> {

    private final boolean lodestoneTracked;
    private final String lodestoneDimension;
    private final Position lodestonePosition;

    protected CompassMeta(ItemMetaBuilder metaBuilder,
                          boolean lodestoneTracked, String lodestoneDimension, Position lodestonePosition) {
        super(metaBuilder);
        this.lodestoneTracked = lodestoneTracked;
        this.lodestoneDimension = lodestoneDimension;
        this.lodestonePosition = lodestonePosition;
    }

    public boolean isLodestoneTracked() {
        return lodestoneTracked;
    }

    public String getLodestoneDimension() {
        return lodestoneDimension;
    }

    public Position getLodestonePosition() {
        return lodestonePosition;
    }

    public static class Builder extends ItemMetaBuilder {

        private boolean lodestoneTracked;
        private String lodestoneDimension;
        private Position lodestonePosition;

        public Builder lodestoneTracked(boolean lodestoneTracked) {
            this.lodestoneTracked = lodestoneTracked;
            return this;
        }

        public Builder lodestoneDimension(String lodestoneDimension) {
            this.lodestoneDimension = lodestoneDimension;
            return this;
        }

        public Builder lodestonePosition(Position lodestonePosition) {
            this.lodestonePosition = lodestonePosition;
            return this;
        }

        @Override
        public @NotNull Builder displayName(@Nullable Component displayName) {
            super.displayName(displayName);
            return this;
        }

        @Override
        public @NotNull Builder lore(List<@NotNull Component> lore) {
            super.lore(lore);
            return this;

        }

        @Override
        public @NotNull Builder lore(Component... lore) {
            super.lore(lore);
            return this;
        }

        @Override
        public @NotNull Builder enchantments(@NotNull Map<Enchantment, Short> enchantments) {
            super.enchantments(enchantments);
            return this;

        }

        @Override
        public @NotNull Builder enchantment(@NotNull Enchantment enchantment, short level) {
            super.enchantment(enchantment, level);
            return this;
        }

        @Override
        public @NotNull Builder clearEnchantment() {
            super.clearEnchantment();
            return this;
        }

        @Override
        public @NotNull CompassMeta build() {
            return new CompassMeta(this, lodestoneTracked, lodestoneDimension, lodestonePosition);
        }

        @Override
        public void read(@NotNull NBTCompound nbtCompound) {
            if (nbtCompound.containsKey("LodestoneTracked")) {
                this.lodestoneTracked = nbtCompound.getByte("LodestoneTracked") == 1;
            }
            if (nbtCompound.containsKey("LodestoneDimension")) {
                this.lodestoneDimension = nbtCompound.getString("LodestoneDimension");
            }
            if (nbtCompound.containsKey("LodestonePos")) {
                final NBTCompound posCompound = nbtCompound.getCompound("LodestonePos");
                final int x = posCompound.getInt("X");
                final int y = posCompound.getInt("Y");
                final int z = posCompound.getInt("Z");

                this.lodestonePosition = new Position(x, y, z);
            }
        }

        @Override
        public void write(@NotNull NBTCompound nbtCompound) {
            nbtCompound.setByte("LodestoneTracked", (byte) (lodestoneTracked ? 1 : 0));
            if (lodestoneDimension != null) {
                nbtCompound.setString("LodestoneDimension", lodestoneDimension);
            }

            if (lodestonePosition != null) {
                NBTCompound posCompound = new NBTCompound();
                posCompound.setInt("X", (int) lodestonePosition.getX());
                posCompound.setInt("Y", (int) lodestonePosition.getY());
                posCompound.setInt("Z", (int) lodestonePosition.getZ());
                nbtCompound.set("LodestonePos", posCompound);
            }
        }

        @Override
        protected void deepClone(@NotNull ItemMetaBuilder metaBuilder) {
            var compassBuilder = (CompassMeta.Builder) metaBuilder;
            compassBuilder.lodestoneTracked = lodestoneTracked;
            compassBuilder.lodestoneDimension = lodestoneDimension;
            compassBuilder.lodestonePosition = lodestonePosition;
        }

        @Override
        protected @NotNull Supplier<ItemMetaBuilder> getSupplier() {
            return Builder::new;
        }
    }
}
