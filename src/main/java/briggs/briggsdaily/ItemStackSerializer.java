package briggs.briggsdaily;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

public class ItemStackSerializer {
    private ItemStackSerializer() {
    }

    public static JsonElement encode(ItemStack stack, RegistryAccess registryAccess) {
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        return ItemStack.CODEC.encodeStart(ops, stack)
                .resultOrPartial(msg -> briggs.briggsdaily.BriggsDaily.LOGGER.error("ItemStack encode error: {}", msg))
                .orElseThrow(() -> new IllegalStateException("Failed to encode ItemStack"));
    }

    public static ItemStack decode(JsonElement json, RegistryAccess registryAccess) {
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        return ItemStack.CODEC.parse(ops, json)
                .resultOrPartial(msg -> briggs.briggsdaily.BriggsDaily.LOGGER.error("ItemStack decode error: {}", msg))
                .orElse(ItemStack.EMPTY);
    }
}
