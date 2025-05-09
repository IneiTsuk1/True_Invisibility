package net.IneiTsuki.true_invis.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.data.TrackedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("FLAGS")
    static TrackedData<Byte> getFlagsTrackedData() {
        throw new AssertionError(); // Mixin will overwrite this
    }
}
