package dev.luminous.mod.modules.impl.client;

import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.math.Easing;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.awt.Color;

public class ClientSetting extends Module {
   public static final Animation animation = new Animation();
   public static ClientSetting INSTANCE;
   public final EnumSetting<ClientSetting.Page> page = this.add(new EnumSetting("Page", ClientSetting.Page.Game));
   public final BooleanSetting lowVersion = this.add(new BooleanSetting("1.12", false, () -> this.page.is(ClientSetting.Page.Game)));
   public final BooleanSetting crawl = this.add(new BooleanSetting("Crawl", true, () -> this.page.is(ClientSetting.Page.Game)));
   public final BooleanSetting rotations = this.add(new BooleanSetting("ShowRotations", true, () -> this.page.is(ClientSetting.Page.Game)).setParent());
   public final BooleanSetting sync = this.add(new BooleanSetting("Sync", false, () -> this.page.is(ClientSetting.Page.Game) && this.rotations.isOpen()));
   public final BooleanSetting titleFix = this.add(new BooleanSetting("TitleFix", true, () -> this.page.is(ClientSetting.Page.Game)));
   public final BooleanSetting fuckFPSLimit = this.add(new BooleanSetting("FuckFPSLimit", true, () -> this.page.is(ClientSetting.Page.Game)));
   private final BooleanSetting portalGui = this.add(new BooleanSetting("BlockTickNausea", true, () -> this.page.is(ClientSetting.Page.Game)));
   public final BooleanSetting optimizedCalc = this.add(new BooleanSetting("OptimizedCalc", false, () -> this.page.is(ClientSetting.Page.Game)));
   public final BooleanSetting mioCompatible = this.add(new BooleanSetting("MioCompatible", false, () -> this.page.is(ClientSetting.Page.Game)));
   public final StringSetting prefix = this.add(new StringSetting("Prefix", ";", () -> this.page.is(ClientSetting.Page.Misc)));
   public final BooleanSetting chinese = this.add(new BooleanSetting("Chinese", false, () -> this.page.is(ClientSetting.Page.Misc)));
   public final BooleanSetting titleOverride = this.add(new BooleanSetting("TitleOverride", true, () -> this.page.is(ClientSetting.Page.Misc)).setParent());
   public final StringSetting windowTitle = this.add(
      new StringSetting("WindowTitle", "Alien", () -> this.page.is(ClientSetting.Page.Misc) && this.titleOverride.isOpen())
   );
   public final BooleanSetting debug = this.add(new BooleanSetting("DebugException", true, () -> this.page.is(ClientSetting.Page.Misc)));
   public final BooleanSetting caughtException = this.add(new BooleanSetting("CaughtException", false, () -> this.page.is(ClientSetting.Page.Misc)).setParent());
   public final BooleanSetting log = this.add(new BooleanSetting("Log", true, () -> this.page.is(ClientSetting.Page.Misc) && this.caughtException.isOpen()));
   private final BooleanSetting hotbar = this.add(new BooleanSetting("HotbarAnim", true, () -> this.page.is(ClientSetting.Page.Gui)));
   public final SliderSetting hotbarTime = this.add(new SliderSetting("HotbarTime", 100, 0, 1000, () -> this.page.is(ClientSetting.Page.Gui)));
   public final EnumSetting<Easing> animEase = this.add(new EnumSetting("AnimEase", Easing.CubicInOut, () -> this.page.is(ClientSetting.Page.Gui)));
   public final BooleanSetting darkening = this.add(new BooleanSetting("Darkening", true, () -> this.page.is(ClientSetting.Page.Gui)));
   public final StringSetting hackName = this.add(
      new StringSetting("Notification", "[Alien]", () -> this.page.getValue() == ClientSetting.Page.Notification)
   );
   public final ColorSetting color = this.add(new ColorSetting("Color", new Color(255, 38, 38), () -> this.page.getValue() == ClientSetting.Page.Notification));
   public final EnumSetting<ClientSetting.Style> messageStyle = this.add(
      new EnumSetting("Style", ClientSetting.Style.Mio, () -> this.page.getValue() == ClientSetting.Page.Notification)
   );
   public final BooleanSetting toggle = this.add(
      new BooleanSetting("ModuleToggle", true, () -> this.page.getValue() == ClientSetting.Page.Notification).setParent()
   );
   public final BooleanSetting onlyOne = this.add(
      new BooleanSetting("OnlyOne", false, () -> this.page.getValue() == ClientSetting.Page.Notification && this.toggle.isOpen())
   );
   public final BooleanSetting banner = this.add(
      new BooleanSetting("ToggleBanner", true, () -> this.page.getValue() == ClientSetting.Page.Notification).setParent()
   );
   public final EnumSetting<ClientSetting.BannerStyle> bannerStyle = this.add(
      new EnumSetting("BannerStyle", ClientSetting.BannerStyle.iOS, () -> this.page.getValue() == ClientSetting.Page.Notification && this.banner.isOpen())
   );
   public final EnumSetting<ClientSetting.StackDir> bannerStack = this.add(
      new EnumSetting("BannerStack", ClientSetting.StackDir.Down, () -> this.page.getValue() == ClientSetting.Page.Notification && this.banner.isOpen())
   );
   public final SliderSetting bannerFade = this.add(
      new SliderSetting("BannerFade", 160, 0, 1000, () -> this.page.getValue() == ClientSetting.Page.Notification && this.banner.isOpen())
   );
   public final SliderSetting bannerHold = this.add(
      new SliderSetting("BannerHold", 1000, 0, 3000, () -> this.page.getValue() == ClientSetting.Page.Notification && this.banner.isOpen())
   );
   public final BooleanSetting bannerSound = this.add(
      new BooleanSetting("BannerSound", true, () -> this.page.getValue() == ClientSetting.Page.Notification && this.banner.isOpen()).setParent()
   );
   public final SliderSetting bannerSoundPitch = this.add(
      new SliderSetting(
         "BannerPitch",
         1.0,
         0.5,
         2.0,
         0.05,
         () -> this.page.getValue() == ClientSetting.Page.Notification && this.banner.isOpen() && this.bannerSound.getValue()
      )
   );
   public final BooleanSetting keepHistory = this.add(new BooleanSetting("KeepHistory", true, () -> this.page.getValue() == ClientSetting.Page.ChatHud));
   public final BooleanSetting infiniteChat = this.add(new BooleanSetting("InfiniteChat", true, () -> this.page.getValue() == ClientSetting.Page.ChatHud));
   public final BooleanSetting hideIndicator = this.add(new BooleanSetting("HideIndicator", true, () -> this.page.getValue() == ClientSetting.Page.ChatHud));
   public final SliderSetting animationTime = this.add(
      new SliderSetting("AnimationTime", 300, 0, 1000, () -> this.page.getValue() == ClientSetting.Page.ChatHud)
   );
   public final EnumSetting<Easing> ease = this.add(new EnumSetting("Ease", Easing.CubicInOut, () -> this.page.getValue() == ClientSetting.Page.ChatHud));
   public final SliderSetting animateOffset = this.add(new SliderSetting("Offset", -40, -200, 100, () -> this.page.getValue() == ClientSetting.Page.ChatHud));
   public final BooleanSetting fade = this.add(new BooleanSetting("Fade", true, () -> this.page.getValue() == ClientSetting.Page.ChatHud));

   public ClientSetting() {
      super("ClientSetting", Module.Category.Client);
      this.setChinese("客户端设置");
      INSTANCE = this;
   }

   public boolean portalGui() {
      return this.isOn() && this.portalGui.getValue();
   }

   public boolean hotbar() {
      return this.isOn() && this.hotbar.getValue();
   }

   @Override
   public void enable() {
      this.state = true;
   }

   @Override
   public void disable() {
      this.state = true;
   }

   @Override
   public boolean isOn() {
      return true;
   }

   public static enum BannerStyle {
      iOS,
      Classic;
   }

   public static enum Page {
      Game,
      Gui,
      Misc,
      Notification,
      ChatHud;
   }

   public static enum StackDir {
      Down,
      Up;
   }

   public static enum Style {
      Mio,
      Debug,
      Lowercase,
      Normal,
      Future,
      Earth,
      Moon,
      Melon,
      Chinese,
      None;
   }
}
