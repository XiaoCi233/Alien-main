package dev.luminous.mod.modules.impl.misc;

import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.math.Timer;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import net.minecraft.client.network.PlayerListEntry;

public class Spammer extends Module {
   private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
   public final BooleanSetting checkSelf = this.add(new BooleanSetting("CheckSelf", false));
   final StringSetting message = this.add(
      new StringSetting(
         "Message", "最强外挂Alien社区版免费试用 群\ud835\udff1\ud835\udff4\ud835\udff5\ud835\udfed\ud835\udff5\ud835\udfed\ud835\udff1\ud835\udff2\ud835\udfed"
      )
   );
   private final Random random = new Random();
   private final SliderSetting randoms = this.add(new SliderSetting("Random", 3.0, 0.0, 20.0, 1.0));
   private final SliderSetting delay = this.add(new SliderSetting("Delay", 5.0, 0.0, 60.0, 0.1).setSuffix("s"));
   private final BooleanSetting tellMode = this.add(new BooleanSetting("RandomWhisper", false));
   private final BooleanSetting autoDisable = this.add(new BooleanSetting("AutoDisable", true));
   private final Timer timer = new Timer();

   public Spammer() {
      super("Spammer", Module.Category.Misc);
      this.setChinese("自动刷屏");
   }

   @Override
   public void onLogout() {
      if (this.autoDisable.getValue()) {
         this.disable();
      }
   }

   @EventListener
   public void onUpdate(UpdateEvent event) {
      if (this.timer.passedS(this.delay.getValue())) {
         this.timer.reset();
         String randomString = this.generateRandomString(this.randoms.getValueInt());
         if (!randomString.isEmpty()) {
            randomString = " " + randomString;
         }

         if (this.tellMode.getValue()) {
            Collection<PlayerListEntry> players = mc.getNetworkHandler().getPlayerList();
            List<PlayerListEntry> list = new ArrayList(players);
            int size = list.size();
            if (size == 0) {
               return;
            }

            PlayerListEntry playerListEntry = (PlayerListEntry)list.get(this.random.nextInt(size));

            for (int i = 0;
               this.checkSelf.getValue() && Objects.equals(playerListEntry.getProfile().getName(), mc.player.getGameProfile().getName());
               playerListEntry = (PlayerListEntry)list.get(this.random.nextInt(size))
            ) {
               if (i > 50) {
                  return;
               }

               i++;
            }

            mc.getNetworkHandler().sendChatCommand("tell " + playerListEntry.getProfile().getName() + " " + this.message.getValue() + randomString);
         } else if (this.message.getValue().startsWith("/")) {
            mc.getNetworkHandler().sendCommand(this.message.getValue().replaceFirst("/", "") + randomString);
         } else {
            mc.getNetworkHandler().sendChatMessage(this.message.getValue() + randomString);
         }
      }
   }

   private String generateRandomString(int LENGTH) {
      StringBuilder sb = new StringBuilder(LENGTH);

      for (int i = 0; i < LENGTH; i++) {
         int index = this.random.nextInt("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".length());
         sb.append("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".charAt(index));
      }

      return sb.toString();
   }
}
