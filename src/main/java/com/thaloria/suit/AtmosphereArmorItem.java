package com.thaloria.suit;

import com.thaloria.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class AtmosphereArmorItem extends ArmorItem {

    public AtmosphereArmorItem(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                @Nullable Level level,
                                List<Component> tooltip,
                                TooltipFlag flag) {

        int oxygen = SuitOxygenUtil.getOxygen(stack);

        tooltip.add(Component.literal("Oxygen: " + oxygen + " / 100")
                .withStyle(ChatFormatting.AQUA));

        super.appendHoverText(stack, level, tooltip, flag);
    }
}