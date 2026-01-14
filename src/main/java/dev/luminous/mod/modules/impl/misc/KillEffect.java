package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.DeathEvent;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public class KillEffect extends Module {
   private final BooleanSetting lightning = this.add(new BooleanSetting("Lightning", true));
   private final BooleanSetting levelUp = this.add(new BooleanSetting("LevelUp", true).setParent());
   private final SliderSetting lMaxPitch = this.add(new SliderSetting("LMaxPitch", 1.0, 0.0, 2.0, 0.1, this.levelUp::isOpen));
   private final SliderSetting lMinPitch = this.add(new SliderSetting("LMinPitch", 1.0, 0.0, 2.0, 0.1, this.levelUp::isOpen));
   private final BooleanSetting trident = this.add(new BooleanSetting("Trident", false).setParent());
   private final SliderSetting tMaxPitch = this.add(new SliderSetting("TMaxPitch", 1.0, 0.0, 2.0, 0.1, this.trident::isOpen));
   private final SliderSetting tMinPitch = this.add(new SliderSetting("TMinPitch", 1.0, 0.0, 2.0, 0.1, this.trident::isOpen));
   private final SliderSetting factor = this.add(new SliderSetting("Factor", 1.0, 1.0, 10.0, 1.0));

   public KillEffect() {
      super("KillEffect", Module.Category.Misc);
      this.setChinese("击杀效果");
   }

   @EventListener
   public void onPlayerDeath(DeathEvent event) {
      if (!nullCheck()) {
         PlayerEntity player = event.getPlayer();
         if (player != null) {
            for (int i = 0; i < this.factor.getValue(); i++) {
               this.doEffect(player);
            }
         }
      }
   }

   private void doEffect(PlayerEntity player) {
      double x = player.getX();
      double y = player.getY();
      double z = player.getZ();
      if (this.lightning.getValue()) {
         LightningEntity lightningEntity = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
         lightningEntity.updatePosition(x, y, z);
         lightningEntity.refreshPositionAfterTeleport(x, y, z);
         mc.world.addEntity(lightningEntity);
      }

      if (this.levelUp.getValue()) {
         mc.world
            .playSound(
               mc.player,
               x,
               y,
               z,
               SoundEvents.ENTITY_PLAYER_LEVELUP,
               SoundCategory.PLAYERS,
               100.0F,
               MathUtil.random(this.lMinPitch.getValueFloat(), this.lMaxPitch.getValueFloat())
            );
      }

      if (this.trident.getValue()) {
         mc.world
            .playSound(
               mc.player,
               x,
               y,
               z,
               SoundEvents.ITEM_TRIDENT_THUNDER,
               SoundCategory.MASTER,
               999.0F,
               MathUtil.random(this.tMinPitch.getValueFloat(), this.tMaxPitch.getValueFloat())
            );
      }
   }
}
