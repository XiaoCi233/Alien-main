package dev.luminous.mod.modules.impl.player;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import java.awt.Color;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Box;

public class AirPlace extends Module {
   public static AirPlace INSTANCE;
   public final BooleanSetting module = this.add(new BooleanSetting("Module", true));
   public final BooleanSetting grimBypass = this.add(new BooleanSetting("GrimBypass", false));
   public final BooleanSetting crossHair = this.add(new BooleanSetting("Crosshair", true).setParent());
   private final SliderSetting range = this.add(new SliderSetting("Range", 5.0, 0.0, 6.0, this.crossHair::isOpen));
   private final ColorSetting fill = this.add(new ColorSetting("Fill", new Color(255, 0, 0, 50), this.crossHair::isOpen).injectBoolean(true));
   private final ColorSetting box = this.add(new ColorSetting("Box", new Color(255, 0, 0, 100), this.crossHair::isOpen).injectBoolean(true));
   private BlockHitResult hit;
   private int cooldown;

   public AirPlace() {
      super("AirPlace", Module.Category.Player);
      this.setChinese("空气放置");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.crossHair.getValue()) {
         if (this.cooldown > 0) {
            this.cooldown--;
         }

         if (mc.getCameraEntity().raycast(this.range.getValue(), 0.0F, false) instanceof BlockHitResult bhr) {
            this.hit = bhr;
         } else {
            this.hit = null;
         }

         if (this.hit == null
            || !mc.world.getBlockState(this.hit.getBlockPos()).getBlock().equals(Blocks.AIR)
            || !(mc.player.getMainHandStack().getItem() instanceof BlockItem)) {
            return;
         }

         boolean main = mc.player.getMainHandStack().getItem() instanceof BlockItem;
         if (mc.options.useKey.isPressed() && main && this.cooldown <= 0) {
            BlockUtil.airPlace(this.hit.getBlockPos(), false);
            this.cooldown = 2;
         }
      }
   }

   @Override
   public void onRender3D(MatrixStack stack) {
      if (this.crossHair.getValue()) {
         if (this.hit == null
            || !mc.world.getBlockState(this.hit.getBlockPos()).getBlock().equals(Blocks.AIR)
            || !(mc.player.getMainHandStack().getItem() instanceof BlockItem)) {
            return;
         }

         Render3DUtil.draw3DBox(
            stack, new Box(this.hit.getBlockPos()), this.fill.getValue(), this.box.getValue(), this.box.booleanValue, this.fill.booleanValue
         );
      }
   }
}
