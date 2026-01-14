package dev.luminous.api.utils.math;

import dev.luminous.Alien;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.core.impl.PlayerManager;
import dev.luminous.core.impl.PlayerManager.EntityAttribute;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.combat.Criticals;
import dev.luminous.mod.modules.impl.combat.Criticals.Mode;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.function.BiFunction;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameMode;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.apache.commons.lang3.mutable.MutableInt;

public class DamageUtils {
   public static final DamageUtils.RaycastFactory HIT_FACTORY = (context, blockPos) -> {
      BlockState blockState = Wrapper.mc.world.getBlockState(blockPos);
      return blockState.getBlock().getBlastResistance() < 600.0F
         ? null
         : blockState.getCollisionShape(Wrapper.mc.world, blockPos).raycast(context.start(), context.end(), blockPos);
   };

   public static float calculateDamage(BlockPos pos, LivingEntity entity) {
      return explosionDamage(entity, null, new Vec3d(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5), 12.0F);
   }

   public static float calculateDamage(Vec3d pos, LivingEntity entity) {
      return explosionDamage(entity, null, pos, 12.0F);
   }

   public static float explosionDamage(
      LivingEntity target, Vec3d targetPos, Box targetBox, Vec3d explosionPos, float power, DamageUtils.RaycastFactory raycastFactory
   ) {
      double modDistance = distance(targetPos.x, targetPos.y, targetPos.z, explosionPos.x, explosionPos.y, explosionPos.z);
      if (modDistance > power) {
         return 0.0F;
      } else {
         double exposure = getExposure(explosionPos, targetBox, raycastFactory);
         double impact = (1.0 - modDistance / power) * exposure;
         float damage = (int)((impact * impact + impact) / 2.0 * 7.0 * 12.0 + 1.0);
         return calculateReductionsExplosion(damage, target, Wrapper.mc.world.getDamageSources().explosion(null));
      }
   }

   public static float anchorDamage(LivingEntity target, LivingEntity predict, Vec3d anchor) {
      return overridingExplosionDamage(target, predict, anchor, 10.0F, BlockPos.ofFloored(anchor), Blocks.AIR.getDefaultState());
   }

   public static float overridingExplosionDamage(
      LivingEntity target, LivingEntity predict, Vec3d explosionPos, float power, BlockPos overridePos, BlockState overrideState
   ) {
      return explosionDamage(target, predict, explosionPos, power, getOverridingHitFactory(overridePos, overrideState));
   }

   private static float explosionDamage(LivingEntity target, LivingEntity predict, Vec3d explosionPos, float power, DamageUtils.RaycastFactory raycastFactory) {
      if (target == null) {
         return 0.0F;
      } else {
         return target instanceof PlayerEntity player && getGameMode(player) == GameMode.CREATIVE
            ? 0.0F
            : explosionDamage(
               target,
               predict != null ? predict.getPos() : target.getPos(),
               predict != null ? predict.getBoundingBox() : target.getBoundingBox(),
               explosionPos,
               power,
               raycastFactory
            );
      }
   }

   public static float explosionDamage(LivingEntity target, LivingEntity predict, Vec3d explosionPos, float power) {
      if (target == null) {
         return 0.0F;
      } else {
         return target instanceof PlayerEntity player && getGameMode(player) == GameMode.CREATIVE
            ? 0.0F
            : explosionDamage(
               target,
               predict != null ? predict.getPos() : target.getPos(),
               predict != null ? predict.getBoundingBox() : target.getBoundingBox(),
               explosionPos,
               power,
               HIT_FACTORY
            );
      }
   }

   public static DamageUtils.RaycastFactory getOverridingHitFactory(BlockPos overridePos, BlockState overrideState) {
      return (context, blockPos) -> {
         BlockState blockState;
         if (blockPos.equals(overridePos)) {
            blockState = overrideState;
         } else {
            blockState = Wrapper.mc.world.getBlockState(blockPos);
            if (blockState.getBlock().getBlastResistance() < 600.0F) {
               return null;
            }
         }

         return blockState.getCollisionShape(Wrapper.mc.world, blockPos).raycast(context.start(), context.end(), blockPos);
      };
   }

   public static float getAttackDamage(LivingEntity attacker, LivingEntity target) {
      float itemDamage = (float)attacker.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
      DamageSource damageSource = attacker instanceof PlayerEntity player
         ? Wrapper.mc.world.getDamageSources().playerAttack(player)
         : Wrapper.mc.world.getDamageSources().mobAttack(attacker);
      StatusEffectInstance effect = attacker.getStatusEffect(StatusEffects.STRENGTH);
      if (effect != null) {
         itemDamage += 3.0F * (effect.getAmplifier() + 1);
      }

      float damage = modifyAttackDamage(attacker, target, attacker.getWeaponStack(), damageSource, itemDamage);
      return calculateReductions(damage, target, damageSource);
   }

   public static float getAttackDamage(LivingEntity attacker, LivingEntity target, ItemStack weapon) {
      EntityAttributeInstance original = attacker.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
      EntityAttributeInstance copy = new EntityAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE, o -> {});
      copy.setBaseValue(original.getBaseValue());

      for (EntityAttributeModifier modifier : original.getModifiers()) {
         copy.addTemporaryModifier(modifier);
      }
      

      copy.removeModifier(Item.BASE_ATTACK_DAMAGE_MODIFIER_ID);
      AttributeModifiersComponent attributeModifiers = (AttributeModifiersComponent)weapon.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
      if (attributeModifiers != null) {
         attributeModifiers.applyModifiers(EquipmentSlot.MAINHAND, (entry, modifier) -> {
            if (entry == EntityAttributes.GENERIC_ATTACK_DAMAGE) {
               copy.updateModifier(modifier);
            }
         });
      }

      float itemDamage = (float)copy.getValue();
      DamageSource damageSource = attacker instanceof PlayerEntity player
         ? Wrapper.mc.world.getDamageSources().playerAttack(player)
         : Wrapper.mc.world.getDamageSources().mobAttack(attacker);
      float damage = modifyAttackDamage(attacker, target, weapon, damageSource, itemDamage);
      return calculateReductions(damage, target, damageSource);
   }

   private static float modifyAttackDamage(LivingEntity attacker, LivingEntity target, ItemStack weapon, DamageSource damageSource, float damage) {
      Object2IntMap<RegistryEntry<Enchantment>> enchantments = new Object2IntOpenHashMap();
      getEnchantments(weapon, enchantments);
      float enchantDamage = 0.0F;
      int sharpness = getEnchantmentLevel(enchantments, Enchantments.SHARPNESS);
      if (sharpness > 0) {
         enchantDamage += 1.0F + 0.5F * (sharpness - 1);
      }

      int baneOfArthropods = getEnchantmentLevel(enchantments, Enchantments.BANE_OF_ARTHROPODS);
      if (baneOfArthropods > 0 && target.getType().isIn(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) {
         enchantDamage += 2.5F * baneOfArthropods;
      }

      int impaling = getEnchantmentLevel(enchantments, Enchantments.IMPALING);
      if (impaling > 0 && target.getType().isIn(EntityTypeTags.SENSITIVE_TO_IMPALING)) {
         enchantDamage += 2.5F * impaling;
      }

      int smite = getEnchantmentLevel(enchantments, Enchantments.SMITE);
      if (smite > 0 && target.getType().isIn(EntityTypeTags.SENSITIVE_TO_SMITE)) {
         enchantDamage += 2.5F * smite;
      }

      if (attacker instanceof PlayerEntity playerEntity) {
         float charge = playerEntity.getAttackCooldownProgress(0.5F);
         damage *= 0.2F + charge * charge * 0.8F;
         enchantDamage *= charge;
         if (weapon.getItem() instanceof MaceItem item) {
            float bonusDamage = item.getBonusAttackDamage(target, damage, damageSource);
            if (bonusDamage > 0.0F) {
               int density = getEnchantmentLevel(weapon, Enchantments.DENSITY);
               if (density > 0) {
                  bonusDamage += 0.5F * attacker.fallDistance;
               }

               damage += bonusDamage;
            }
         }

         if (charge > 0.9F
            && (
               attacker.fallDistance > 0.0F
                  || attacker == Wrapper.mc.player
                     && Criticals.INSTANCE.isOn()
                     && !Criticals.INSTANCE.mode.is(Criticals.Mode.Ground)
                     && (Wrapper.mc.player.isOnGround() || !Criticals.INSTANCE.onlyGround.getValue())
            )
            && (!attacker.isOnGround() || attacker == Wrapper.mc.player && Criticals.INSTANCE.isOn() && !Criticals.INSTANCE.mode.is(Criticals.Mode.Ground))
            && !attacker.isClimbing()
            && !attacker.isTouchingWater()
            && !attacker.hasStatusEffect(StatusEffects.BLINDNESS)
            && !attacker.hasVehicle()) {
            damage *= 1.5F;
         }
      }

      return damage + enchantDamage;
   }

   public static float fallDamage(LivingEntity entity) {
      if (entity instanceof PlayerEntity player && player.getAbilities().flying) {
         return 0.0F;
      } else if (!entity.hasStatusEffect(StatusEffects.SLOW_FALLING) && !entity.hasStatusEffect(StatusEffects.LEVITATION)) {
         int surface = Wrapper.mc
            .world
            .getWorldChunk(entity.getBlockPos())
            .getHeightmap(Type.MOTION_BLOCKING)
            .get(entity.getBlockX() & 15, entity.getBlockZ() & 15);
         if (entity.getBlockY() >= surface) {
            return fallDamageReductions(entity, surface);
         } else {
            BlockHitResult raycastResult = Wrapper.mc
               .world
               .raycast(
                  new RaycastContext(
                     entity.getPos(),
                     new Vec3d(entity.getX(), Wrapper.mc.world.getBottomY(), entity.getZ()),
                     ShapeType.COLLIDER,
                     FluidHandling.WATER,
                     entity
                  )
               );
            return raycastResult.getType() == net.minecraft.util.hit.HitResult.Type.MISS
               ? 0.0F
               : fallDamageReductions(entity, raycastResult.getBlockPos().getY());
         }
      } else {
         return 0.0F;
      }
   }

   private static float fallDamageReductions(LivingEntity entity, int surface) {
      int fallHeight = (int)(entity.getY() - surface + entity.fallDistance - 3.0);
      StatusEffectInstance jumpBoostInstance = entity.getStatusEffect(StatusEffects.JUMP_BOOST);
      if (jumpBoostInstance != null) {
         fallHeight -= jumpBoostInstance.getAmplifier() + 1;
      }

      return calculateReductions(fallHeight, entity, Wrapper.mc.world.getDamageSources().fall());
   }

   public static float calculateReductionsExplosion(float damage, LivingEntity entity, DamageSource damageSource) {
      if (damageSource.isScaledWithDifficulty()) {
         switch (Wrapper.mc.world.getDifficulty()) {
            case EASY:
               damage = Math.min(damage / 2.0F + 1.0F, damage);
               break;
            case HARD:
               damage *= 1.5F;
         }
      }

      damage = DamageUtil.getDamageLeft(entity, damage, damageSource, getArmor(entity), (float)getGENERIC_ARMOR_TOUGHNESS(entity));
      damage = resistanceReduction(entity, damage);
      damage = DamageUtil.getInflictedDamage(damage, getProtectionAmount(entity.getArmorItems()));
      return Math.max(damage, 0.0F);
   }

   public static float calculateReductions(float damage, LivingEntity entity, DamageSource damageSource) {
      if (damageSource.isScaledWithDifficulty()) {
         switch (Wrapper.mc.world.getDifficulty()) {
            case EASY:
               damage = Math.min(damage / 2.0F + 1.0F, damage);
               break;
            case HARD:
               damage *= 1.5F;
         }
      }

      damage = DamageUtil.getDamageLeft(entity, damage, damageSource, getArmor(entity), (float)getGENERIC_ARMOR_TOUGHNESS(entity));
      damage = resistanceReduction(entity, damage);
      damage = protectionReduction(entity, damage, damageSource);
      return Math.max(damage, 0.0F);
   }

   public static double getGENERIC_ARMOR_TOUGHNESS(LivingEntity entity) {
      if (entity instanceof PlayerEntity player) {
         PlayerManager.EntityAttribute entityAttribute = (PlayerManager.EntityAttribute)Alien.PLAYER.map.get(player);
         if (entityAttribute != null) {
            return entityAttribute.toughness();
         }
      }

      return entity.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
   }

   private static float getArmor(LivingEntity entity) {
      if (entity instanceof PlayerEntity player) {
         PlayerManager.EntityAttribute entityAttribute = (PlayerManager.EntityAttribute)Alien.PLAYER.map.get(player);
         if (entityAttribute != null) {
            return entityAttribute.armor();
         }
      }

      return (float)Math.floor(entity.getAttributeValue(EntityAttributes.GENERIC_ARMOR));
   }

   private static float protectionReduction(LivingEntity player, float damage, DamageSource source) {
      if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return damage;
      } else {
         int damageProtection = 0;

         for (ItemStack stack : player.getAllArmorItems()) {
            Object2IntMap<RegistryEntry<Enchantment>> enchantments = new Object2IntOpenHashMap();
            getEnchantments(stack, enchantments);
            int protection = getEnchantmentLevel(enchantments, Enchantments.PROTECTION);
            if (protection > 0) {
               damageProtection += protection;
            }

            int fireProtection = getEnchantmentLevel(enchantments, Enchantments.FIRE_PROTECTION);
            if (fireProtection > 0 && source.isIn(DamageTypeTags.IS_FIRE)) {
               damageProtection += 2 * fireProtection;
            }

            int blastProtection = getEnchantmentLevel(enchantments, Enchantments.BLAST_PROTECTION);
            if (blastProtection > 0 && source.isIn(DamageTypeTags.IS_EXPLOSION)) {
               damageProtection += 2 * blastProtection;
            }

            int projectileProtection = getEnchantmentLevel(enchantments, Enchantments.PROJECTILE_PROTECTION);
            if (projectileProtection > 0 && source.isIn(DamageTypeTags.IS_PROJECTILE)) {
               damageProtection += 2 * projectileProtection;
            }

            int featherFalling = getEnchantmentLevel(enchantments, Enchantments.FEATHER_FALLING);
            if (featherFalling > 0 && source.isIn(DamageTypeTags.IS_FALL)) {
               damageProtection += 3 * featherFalling;
            }
         }

         return DamageUtil.getInflictedDamage(damage, damageProtection);
      }
   }

   public static int getProtectionAmount(Iterable<ItemStack> equipment) {
      MutableInt mutableInt = new MutableInt();
      equipment.forEach(i -> mutableInt.add(getProtectionAmount(i)));
      return mutableInt.intValue();
   }

   public static int getProtectionAmount(ItemStack stack) {
      int modifierBlast = EnchantmentHelper.getLevel(
         Wrapper.mc
            .world
            .getRegistryManager()
            .getWrapperOrThrow(Enchantments.BLAST_PROTECTION.getRegistryRef())
            .getOptional(Enchantments.BLAST_PROTECTION)
            .get(),
         stack
      );
      int modifier = EnchantmentHelper.getLevel(
         Wrapper.mc.world.getRegistryManager().getWrapperOrThrow(Enchantments.PROTECTION.getRegistryRef()).getOptional(Enchantments.PROTECTION).get(), stack
      );
      return modifierBlast * 2 + modifier;
   }

   private static float resistanceReduction(LivingEntity player, float damage) {
      StatusEffectInstance resistance = player.getStatusEffect(StatusEffects.RESISTANCE);
      if (resistance != null) {
         int lvl = resistance.getAmplifier() + 1;
         damage *= 1.0F - lvl * 0.2F;
      }

      return Math.max(damage, 0.0F);
   }

   private static float getExposure(Vec3d source, Box box, DamageUtils.RaycastFactory raycastFactory) {
      if (ClientSetting.INSTANCE.optimizedCalc.getValue()) {
         int miss = 0;
         int hit = 0;

         for (int k = 0; k <= 1; k++) {
            for (int l = 0; l <= 1; l++) {
               for (int m = 0; m <= 1; m++) {
                  double n = MathHelper.lerp(k, box.minX, box.maxX);
                  double o = MathHelper.lerp(l, box.minY, box.maxY);
                  double p = MathHelper.lerp(m, box.minZ, box.maxZ);
                  Vec3d vec3d = new Vec3d(n, o, p);
                  if (raycast(vec3d, source, CombatUtil.terrainIgnore) == net.minecraft.util.hit.HitResult.Type.MISS) {
                     miss++;
                  }

                  hit++;
               }
            }
         }

         return (float)miss / hit;
      } else {
         double xDiff = box.maxX - box.minX;
         double yDiff = box.maxY - box.minY;
         double zDiff = box.maxZ - box.minZ;
         double xStep = 1.0 / (xDiff * 2.0 + 1.0);
         double yStep = 1.0 / (yDiff * 2.0 + 1.0);
         double zStep = 1.0 / (zDiff * 2.0 + 1.0);
         if (xStep > 0.0 && yStep > 0.0 && zStep > 0.0) {
            int misses = 0;
            int hits = 0;
            double xOffset = (1.0 - Math.floor(1.0 / xStep) * xStep) * 0.5;
            double zOffset = (1.0 - Math.floor(1.0 / zStep) * zStep) * 0.5;
            xStep *= xDiff;
            yStep *= yDiff;
            zStep *= zDiff;
            double startX = box.minX + xOffset;
            double startY = box.minY;
            double startZ = box.minZ + zOffset;
            double endX = box.maxX + xOffset;
            double endY = box.maxY;
            double endZ = box.maxZ + zOffset;

            for (double x = startX; x <= endX; x += xStep) {
               for (double y = startY; y <= endY; y += yStep) {
                  for (double z = startZ; z <= endZ; z += zStep) {
                     Vec3d position = new Vec3d(x, y, z);
                     if (raycast(new DamageUtils.ExposureRaycastContext(position, source), raycastFactory) == null) {
                        misses++;
                     }

                     hits++;
                  }
               }
            }

            return (float)misses / hits;
         } else {
            return 0.0F;
         }
      }
   }

   public static net.minecraft.util.hit.HitResult.Type raycast(Vec3d start, Vec3d end, boolean ignoreTerrain) {
      return (net.minecraft.util.hit.HitResult.Type)BlockView.raycast(start, end, null, (innerContext, blockPos) -> {
         BlockState blockState = Wrapper.mc.world.getBlockState(blockPos);
         if (blockState.getBlock().getBlastResistance() < 600.0F && ignoreTerrain) {
            return null;
         } else {
            BlockHitResult hitResult = blockState.getCollisionShape(Wrapper.mc.world, blockPos).raycast(start, end, blockPos);
            return hitResult == null ? null : hitResult.getType();
         }
      }, innerContext -> net.minecraft.util.hit.HitResult.Type.MISS);
   }

   public static BlockHitResult raycast(DamageUtils.ExposureRaycastContext context, DamageUtils.RaycastFactory raycastFactory) {
      return (BlockHitResult)BlockView.raycast(context.start(), context.end(), context, raycastFactory, ctx -> null);
   }

   public static int getEnchantmentLevel(ItemStack itemStack, RegistryKey<Enchantment> enchantment) {
      if (itemStack.isEmpty()) {
         return 0;
      } else {
         Object2IntMap<RegistryEntry<Enchantment>> itemEnchantments = new Object2IntArrayMap();
         getEnchantments(itemStack, itemEnchantments);
         return getEnchantmentLevel(itemEnchantments, enchantment);
      }
   }

   public static int getEnchantmentLevel(Object2IntMap<RegistryEntry<Enchantment>> itemEnchantments, RegistryKey<Enchantment> enchantment) {
      ObjectIterator var2 = Object2IntMaps.fastIterable(itemEnchantments).iterator();

      while (var2.hasNext()) {
         Object2IntMap.Entry<RegistryEntry<Enchantment>> entry = (Object2IntMap.Entry<RegistryEntry<Enchantment>>)var2.next();
         if (((RegistryEntry)entry.getKey()).matchesKey(enchantment)) {
            return entry.getIntValue();
         }
      }

      return 0;
   }

   public static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
      return Math.sqrt(squaredDistance(x1, y1, z1, x2, y2, z2));
   }

   public static GameMode getGameMode(PlayerEntity player) {
      if (player == null) {
         return null;
      } else {
         PlayerListEntry playerListEntry = Wrapper.mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
         return playerListEntry == null ? null : playerListEntry.getGameMode();
      }
   }

   public static double squaredDistanceTo(Entity entity) {
      return squaredDistanceTo(entity.getX(), entity.getY(), entity.getZ());
   }

   public static double squaredDistanceTo(BlockPos blockPos) {
      return squaredDistanceTo(blockPos.getX(), blockPos.getY(), blockPos.getZ());
   }

   public static double squaredDistanceTo(double x, double y, double z) {
      return squaredDistance(Wrapper.mc.player.getX(), Wrapper.mc.player.getY(), Wrapper.mc.player.getZ(), x, y, z);
   }

   public static double squaredDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
      double f = x1 - x2;
      double g = y1 - y2;
      double h = z1 - z2;
      return org.joml.Math.fma(f, f, org.joml.Math.fma(g, g, h * h));
   }

   public static void getEnchantments(ItemStack itemStack, Object2IntMap<RegistryEntry<Enchantment>> enchantments) {
      enchantments.clear();
      if (!itemStack.isEmpty()) {
         for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : itemStack.getItem() == Items.ENCHANTED_BOOK
            ? ((ItemEnchantmentsComponent)itemStack.get(DataComponentTypes.STORED_ENCHANTMENTS)).getEnchantmentEntries()
            : itemStack.getEnchantments().getEnchantmentEntries()) {
            enchantments.put((RegistryEntry)entry.getKey(), entry.getIntValue());
         }
      }
   }

   public record ExposureRaycastContext(Vec3d start, Vec3d end) {
   }

   @FunctionalInterface
   public interface RaycastFactory extends BiFunction<DamageUtils.ExposureRaycastContext, BlockPos, BlockHitResult> {
   }
}
