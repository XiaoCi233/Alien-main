package dev.luminous.mod.modules.impl.combat;

import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class Panic extends Module {
   private final BooleanSetting restoreOnDisable;
   private final StringSetting whitelist;
   private final List<Module> disabledModules = new ArrayList();

   public Panic() {
      super("Panic", "紧急停止所有功能", Module.Category.Combat);
      this.setChinese("紧急停止所有功能");
      this.restoreOnDisable = this.add(new BooleanSetting("恢复启用", true));
      this.whitelist = this.add(new StringSetting("白名单", "ClickGui,HUD,Offhand"));
   }

   @Override
   public void onEnable() {
      this.disabledModules.clear();
      List<Module> allModules = this.getAllModules();
      String[] whitelistNames = this.whitelist.getValue().split(",");
      List<String> whitelistModules = new ArrayList();

      for (String name : whitelistNames) {
         whitelistModules.add(name.trim().toLowerCase());
      }

      for (Module module : allModules) {
         if (module != this && !this.shouldWhitelist(module, whitelistModules) && module.isOn()) {
            this.disabledModules.add(module);
            module.disable();
         }
      }

      this.sendMessage("§c已紧急停止 " + this.disabledModules.size() + " 个功能");
   }

   @Override
   public void onDisable() {
      if (this.restoreOnDisable.getValue() && !this.disabledModules.isEmpty()) {
         int restoredCount = 0;

         for (Module module : this.disabledModules) {
            if (module.isOff()) {
               module.enable();
               restoredCount++;
            }
         }

         this.sendMessage("§a已恢复 " + restoredCount + " 个功能");
      }

      this.disabledModules.clear();
   }

   @Override
   public String getInfo() {
      return this.disabledModules.isEmpty() ? "" : this.disabledModules.size() + "";
   }

   private List<Module> getAllModules() {
      List<Module> modules = new ArrayList();
      String[] moduleNames = new String[]{
         "AutoKit",
         "Bot",
         "Fonts",
         "NoTerrainScreen",
         "AutoCrystal",
         "Ambience",
         "AntiHunger",
         "AntiVoid",
         "AutoWalk",
         "VClip",
         "ExtraTab",
         "AntiWeak",
         "BedCrafter",
         "Friend",
         "AspectRatio",
         "ChunkESP",
         "Aura",
         "PistonCrystal",
         "AutoAnchor",
         "PhaseESP",
         "AutoArmor",
         "Breaker",
         "AutoLog",
         "AutoEZ",
         "SelfTrap",
         "Sorter",
         "AutoMend",
         "AutoPot",
         "AutoPush",
         "Offhand",
         "Nuker",
         "AutoTrap",
         "AutoWeb",
         "Blink",
         "ChorusControl",
         "BlockStrafe",
         "FastSwim",
         "Blocker",
         "Quiver",
         "BowBomb",
         "BreakESP",
         "Burrow",
         "Punctuation",
         "MaceSpoof",
         "CameraClip",
         "ChatAppend",
         "ClickGui",
         "InfiniteTrident",
         "ColorsModule",
         "AutoRegear",
         "LavaFiller",
         "AntiPhase",
         "Clip",
         "AntiCheat",
         "IRC",
         "ItemsCounter",
         "Fov",
         "Criticals",
         "CevBreaker",
         "Crosshair",
         "Chams",
         "AntiPacket",
         "AutoReconnect",
         "ESP",
         "HoleESP",
         "Tracers",
         "MovementSync",
         "ElytraFly",
         "PacketLogger",
         "TeleportLogger",
         "SkinFlicker",
         "EntityControl",
         "NameTags",
         "ShulkerViewer",
         "PingSpoof",
         "FakePlayer",
         "Spammer",
         "MotionCamera",
         "HighLight",
         "FastFall",
         "FastWeb",
         "Flatten",
         "Fly",
         "Yaw",
         "Freecam",
         "FreeLook",
         "TimerModule",
         "Tips",
         "ClientSetting",
         "TextRadar",
         "HUD",
         "NoResourcePack",
         "RocketExtend",
         "HoleFiller",
         "HoleSnap",
         "LogoutSpots",
         "AutoTool",
         "Trajectories",
         "KillEffect",
         "AutoPearl",
         "AntiEffects",
         "NoFall",
         "NoRender",
         "NoSlow",
         "NoSound",
         "AirPlace",
         "Xray",
         "PacketEat",
         "PacketFly",
         "PacketMine",
         "PacketControl",
         "Phase",
         "PlaceRender",
         "InteractTweaks",
         "PopChams",
         "Replenish",
         "ServerLagger",
         "Scaffold",
         "ShaderModule",
         "AntiCrawl",
         "AntiRegear",
         "SafeWalk",
         "NoJumpDelay",
         "Speed",
         "Sprint",
         "Strafe",
         "Step",
         "Surround",
         "TotemParticle",
         "Velocity",
         "ViewModel",
         "XCarry",
         "Zoom"
      };

      for (String moduleName : moduleNames) {
         Module module = this.getModuleInstance(moduleName);
         if (module != null) {
            modules.add(module);
         }
      }

      return modules;
   }

   private Module getModuleInstance(String moduleName) {
      try {
         String[] possiblePackages = new String[]{
            "dev.luminous.mod.modules.impl.combat.",
            "dev.luminous.mod.modules.impl.movement.",
            "dev.luminous.mod.modules.impl.player.",
            "dev.luminous.mod.modules.impl.render.",
            "dev.luminous.mod.modules.impl.misc.",
            "dev.luminous.mod.modules.impl.client.",
            "dev.luminous.mod.modules.impl.exploit."
         };

         for (String packageName : possiblePackages) {
            try {
               String className = packageName + moduleName;
               Class<?> clazz = Class.forName(className);
               Field instanceField = clazz.getDeclaredField("INSTANCE");
               instanceField.setAccessible(true);
               Object instance = instanceField.get(null);
               if (instance instanceof Module) {
                  return (Module)instance;
               }
            } catch (NoSuchFieldException | ClassNotFoundException var11) {
            }
         }
      } catch (Exception var12) {
      }

      return null;
   }

   private boolean shouldWhitelist(Module module, List<String> whitelistModules) {
      if (module == null) {
         return false;
      } else {
         String moduleName = module.getName().toLowerCase();

         for (String whitelistName : whitelistModules) {
            if (moduleName.equals(whitelistName.toLowerCase())) {
               return true;
            }
         }

         return false;
      }
   }

   public void panicNow() {
      if (!this.isOn()) {
         this.enable();
      }
   }

   public void restoreNow() {
      if (this.isOn()) {
         this.disable();
      }
   }
}
