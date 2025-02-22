package com.bonker.wildiron.client;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class GunAnimationState {
    public final Animation textPos = new Animation(0);
    public final Animation spin = new Animation(0);
    public final Animation pullOut = new Animation(0);
    public Component textMessage = Component.empty();
    public int textColor = 0xffffff;
    public ItemStack stack = ItemStack.EMPTY;

    public static class Animation {
        private float lastValue;
        private float value;

        public Animation(float value) {
            this.lastValue = value;
            this.value = value;
        }

        public void setValue(float value) {
            this.lastValue = this.value;
            this.value = value;
        }

        public void addValue(float addition, float min, float max) {
            setValue(Mth.clamp(value + addition, min, max));
        }

        public void reset(float value) {
            this.lastValue = value;
            this.value = value;
        }

        public float getInterpolated(float partialTick) {
            return Mth.lerp(partialTick, lastValue, value);
        }
    }
}
