package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;

public class Friend extends Module {
   public static Friend INSTANCE;

   public Friend() {
      super("Friend", Module.Category.Misc);
      this.setChinese("好友");
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      if (nullCheck()) {
         this.disable();
      } else {
         if (mc.crosshairTarget instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof PlayerEntity player) {
            Alien.FRIEND.friend(player);
         }

         this.disable();
      }
   }
}
