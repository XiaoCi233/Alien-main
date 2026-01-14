package dev.luminous.mod.modules.impl.combat;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.world.BlockPosX;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.impl.player.PacketMine;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

public class AntiCrawl extends Module {
   public static AntiCrawl INSTANCE;
   final double[] xzOffset = new double[]{0.0, 0.3, -0.3};
   private final EnumSetting<AntiCrawl.While> whileSetting = this.add(new EnumSetting("While", AntiCrawl.While.Crawling));
   private final BooleanSetting web = this.add(new BooleanSetting("Web", true));
   public boolean work = false;

   public AntiCrawl() {
      super("AntiCrawl", Module.Category.Combat);
      this.setChinese("反趴下");
      INSTANCE = this;
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      this.work = false;
      if (!mc.player.isFallFlying()) {
         if (this.whileSetting.is(AntiCrawl.While.Always) && BlockUtil.getBlock(mc.player.getBlockPos()) != Blocks.BEDROCK
            || mc.player.isCrawling()
            || this.whileSetting.is(AntiCrawl.While.Mining) && Alien.BREAK.isMining(mc.player.getBlockPos())) {
            for (double offset : this.xzOffset) {
               for (double offset2 : this.xzOffset) {
                  BlockPos pos = new BlockPosX(mc.player.getX() + offset, mc.player.getY() + 1.2, mc.player.getZ() + offset2);
                  if (this.canBreak(pos)) {
                     PacketMine.INSTANCE.mine(pos);
                     this.work = true;
                     return;
                  }

                  if (this.web.getValue()) {
                     BlockPos web = new BlockPosX(mc.player.getX() + offset, mc.player.getY(), mc.player.getZ() + offset2);
                     if (mc.world.getBlockState(web).getBlock() == Blocks.COBWEB && this.canBreak(web)) {
                        PacketMine.INSTANCE.mine(web);
                        this.work = true;
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   private boolean canBreak(BlockPos pos) {
      return (BlockUtil.getClickSideStrict(pos) != null || pos.equals(PacketMine.getBreakPos())) && !PacketMine.unbreakable(pos) && !mc.world.isAir(pos);
   }

   private static enum While {
      Crawling,
      Mining,
      Always;
   }
}
