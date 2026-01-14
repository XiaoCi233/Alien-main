package dev.luminous.mod.modules;

import dev.luminous.Alien;
import dev.luminous.core.impl.CommandManager;
import dev.luminous.mod.Mod;
import dev.luminous.mod.modules.impl.client.AntiCheat;
import dev.luminous.mod.modules.impl.client.BaritoneModule;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.client.ClientSetting.Style;
import dev.luminous.mod.modules.impl.client.ColorsModule;
import dev.luminous.mod.modules.impl.client.HUD;
import dev.luminous.mod.modules.settings.Setting;
import dev.luminous.mod.modules.settings.impl.BindSetting;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;

public abstract class Module extends Mod {
   public final BooleanSetting drawn;
   private final List<Setting> settings = new ArrayList();
   private final String description;
   private final Module.Category category;
   private final BindSetting bindSetting;
   protected boolean state;
   private String chinese;

   public Module(String name, Module.Category category) {
      this(name, "", category);
   }

   public Module(String name, String description, Module.Category category) {
      super(name);
      this.category = category;
      this.description = description;
      this.bindSetting = this.add(new BindSetting("Key", this.isGuiModule() ? 344 : -1));
      this.drawn = this.add(new BooleanSetting("Drawn", !this.hideInModuleList()));
   }

   private boolean isGuiModule() {
      return this instanceof ClickGui;
   }

   private boolean hideInModuleList() {
      return this instanceof ColorsModule
         || this instanceof BaritoneModule
         || this instanceof AntiCheat
         || this instanceof ClientSetting
         || this instanceof HUD;
   }

   public void setChinese(String chinese) {
      this.chinese = chinese;
   }

   public String getArrayName() {
      return this.getDisplayName() + this.getArrayInfo();
   }

   public String getArrayInfo() {
      return this.getInfo() == null ? "" : " §7[§f" + this.getInfo() + "§7]";
   }

   public String getInfo() {
      return null;
   }

   public String getDisplayName() {
      return ClientSetting.INSTANCE.chinese.getValue() && this.chinese != null ? this.chinese : this.getName();
   }

   public String getDescription() {
      return this.description;
   }

   public Module.Category getCategory() {
      return this.category;
   }

   public BindSetting getBindSetting() {
      return this.bindSetting;
   }

   public boolean isOn() {
      return this.state;
   }

   public boolean isOff() {
      return !this.isOn();
   }

   public void toggle() {
      if (this.isOn()) {
         this.disable();
      } else {
         this.enable();
      }
   }

   public void enable() {
      if (!this.state) {
         if (!nullCheck() && this.drawn.getValue() && ClientSetting.INSTANCE.toggle.getValue()) {
            int id = ClientSetting.INSTANCE.onlyOne.getValue() ? -1 : this.hashCode();
            switch ((ClientSetting.Style)ClientSetting.INSTANCE.messageStyle.getValue()) {
               case Mio:
                  CommandManager.sendMessageId("§2[+] §f" + this.getDisplayName(), id);
                  break;
               case Debug:
                  CommandManager.sendMessageId(this.getCategory().name().toLowerCase() + "." + this.getDisplayName().toLowerCase() + ".§aenable", id);
                  break;
               case Lowercase:
                  CommandManager.sendMessageId(this.getDisplayName().toLowerCase() + " §aenabled", id);
                  break;
               case Melon:
                  CommandManager.sendMessageId("§b" + this.getDisplayName() + " §aEnabled.", id);
                  break;
               case Normal:
                  CommandManager.sendMessageId("§f" + this.getDisplayName() + " §aEnabled", id);
                  break;
               case Future:
                  CommandManager.sendMessageId("§7" + this.getDisplayName() + " toggled §aon", id);
                  break;
               case Chinese:
                  CommandManager.sendMessageId(this.getDisplayName() + " §a开启", id);
                  break;
               case Moon:
                  CommandManager.sendChatMessageWidthIdNoSync(
                     "§f[§b" + ClientSetting.INSTANCE.hackName.getValue() + "§f] [§3" + this.getDisplayName() + "§f] §7toggled §aon", id
                  );
                  break;
               case Earth:
                  CommandManager.sendChatMessageWidthIdNoSync("§l" + this.getDisplayName() + " §aenabled.", id);
            }
         }

         if (Alien.MODULE != null) {
            Alien.MODULE.showToggleBanner(this, true);
         }

         this.state = true;
         Alien.EVENT_BUS.subscribe(this);
         this.onToggle();
         this.onEnable();
      }
   }

   public void disable() {
      if (this.state) {
         if (!nullCheck() && this.drawn.getValue() && ClientSetting.INSTANCE.toggle.getValue()) {
            int id = ClientSetting.INSTANCE.onlyOne.getValue() ? -1 : this.hashCode();
            switch ((ClientSetting.Style)ClientSetting.INSTANCE.messageStyle.getValue()) {
               case Mio:
                  CommandManager.sendMessageId("§4[-] §f" + this.getDisplayName(), id);
                  break;
               case Debug:
                  CommandManager.sendMessageId(this.getCategory().name().toLowerCase() + "." + this.getDisplayName().toLowerCase() + ".§cdisable", id);
                  break;
               case Lowercase:
                  CommandManager.sendMessageId(this.getDisplayName().toLowerCase() + " §cdisabled", id);
                  break;
               case Melon:
                  CommandManager.sendMessageId("§b" + this.getDisplayName() + " §cDisabled.", id);
                  break;
               case Normal:
                  CommandManager.sendMessageId("§f" + this.getDisplayName() + " §cDisabled", id);
                  break;
               case Future:
                  CommandManager.sendMessageId("§7" + this.getDisplayName() + " toggled §coff", id);
                  break;
               case Chinese:
                  CommandManager.sendMessageId(this.getDisplayName() + " §c关闭", id);
                  break;
               case Moon:
                  CommandManager.sendChatMessageWidthIdNoSync(
                     "§f[§b" + ClientSetting.INSTANCE.hackName.getValue() + "§f] [§3" + this.getDisplayName() + "§f] §7toggled §coff", id
                  );
                  break;
               case Earth:
                  CommandManager.sendChatMessageWidthIdNoSync("§l" + this.getDisplayName() + " §cdisabled.", id);
            }
         }

         if (Alien.MODULE != null) {
            Alien.MODULE.showToggleBanner(this, false);
         }

         this.state = false;
         Alien.EVENT_BUS.unsubscribe(this);
         this.onToggle();
         this.onDisable();
      }
   }

   public void sendMessage(String message) {
      CommandManager.sendMessage(message);
   }

   public void setState(boolean state) {
      if (this.state != state) {
         if (state) {
            this.enable();
         } else {
            this.disable();
         }
      }
   }

   public boolean setBind(String rkey) {
      if (rkey.equalsIgnoreCase("none")) {
         this.bindSetting.setValue(-1);
         return true;
      } else {
         int key;
         try {
            key = InputUtil.fromTranslationKey("key.keyboard." + rkey.toLowerCase()).getCode();
         } catch (NumberFormatException var4) {
            if (!nullCheck()) {
               this.sendMessage("§4Bad bind!");
            }

            return false;
         }

         if (rkey.equalsIgnoreCase("none")) {
            key = -1;
         }

         if (key == 0) {
            return false;
         } else {
            this.bindSetting.setValue(key);
            return true;
         }
      }
   }

   public void onDisable() {
   }

   public void onEnable() {
   }

   public void onToggle() {
   }

   public void onLogin() {
   }

   public void onLogout() {
   }

   public void onRender2D(DrawContext drawContext, float tickDelta) {
   }

   public void onRender3D(MatrixStack matrixStack) {
   }

   public void addSetting(Setting setting) {
      this.settings.add(setting);
   }

   public StringSetting add(StringSetting setting) {
      this.addSetting(setting);
      return setting;
   }

   public ColorSetting add(ColorSetting setting) {
      this.addSetting(setting);
      return setting;
   }

   public SliderSetting add(SliderSetting setting) {
      this.addSetting(setting);
      return setting;
   }

   public BooleanSetting add(BooleanSetting setting) {
      this.addSetting(setting);
      return setting;
   }

   public <T extends Enum<T>> EnumSetting<T> add(EnumSetting<T> setting) {
      this.addSetting(setting);
      return setting;
   }

   public BindSetting add(BindSetting setting) {
      this.addSetting(setting);
      return setting;
   }

   public List<Setting> getSettings() {
      return this.settings;
   }

   public static boolean nullCheck() {
      return mc.player == null || mc.player.input == null || mc.world == null;
   }

   public static void sendSequencedPacket(SequencedPacketCreator packetCreator) {
      if (mc.getNetworkHandler() != null && mc.world != null) {
         PendingUpdateManager pendingUpdateManager = mc.world.getPendingUpdateManager().incrementSequence();

         try {
            int i = pendingUpdateManager.getSequence();
            mc.getNetworkHandler().sendPacket(packetCreator.predict(i));
         } catch (Throwable var5) {
            if (pendingUpdateManager != null) {
               try {
                  pendingUpdateManager.close();
               } catch (Throwable var4) {
                  var5.addSuppressed(var4);
               }
            }

            throw var5;
         }

          pendingUpdateManager.close();
      }
   }

   public static enum Category {
      Combat {
         @Override
         public String getIcon() {
            return "b";
         }
      },
      Misc {
         @Override
         public String getIcon() {
            return "[";
         }
      },
      Render {
         @Override
         public String getIcon() {
            return "a";
         }
      },
      Movement {
         @Override
         public String getIcon() {
            return "8";
         }
      },
      Player {
         @Override
         public String getIcon() {
            return "5";
         }
      },
      Exploit {
         @Override
         public String getIcon() {
            return "6";
         }
      },
      Client {
         @Override
         public String getIcon() {
            return "7";
         }
      };

      public abstract String getIcon();
   }
}
