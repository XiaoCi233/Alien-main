package dev.luminous.mod.modules.impl.misc;

import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.DeathEvent;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.Module.Category;
import dev.luminous.mod.modules.settings.impl.EnumSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import java.util.List;
import java.util.Random;
import net.minecraft.entity.player.PlayerEntity;

public class AutoEZ extends Module {
   private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
   public final List<String> sex = List.of(
      "呐呐~杂鱼哥哥不会这样就被捉弄的不会说话了吧♡",
      "嘻嘻~杂鱼哥哥不会以为竖个大拇哥就能欺负我了吧~不会吧♡不会吧♡",
      "杂鱼哥哥怎么可能欺负得了别人呢~只能欺负自己哦♡~",
      "哥哥真是好欺负啊♡嘻嘻~",
      "哎♡~杂鱼说话就是无趣唉~",
      "呐呐~杂鱼哥哥发这个是想教育我吗~嘻嘻~怎么可能啊♡",
      "什么嘛~废柴哥哥会想这种事情啊~唔呃",
      "把你肮脏的目光拿开啦~很恶心哦♡",
      "咱的期待就是被你这样的笨蛋破坏了~♡"
   );
   public final List<String> bot = List.of(
      "鼠标明天到，触摸板打的",
      "转人工",
      "收徒",
      "不收徒",
      "有真人吗",
      "墨镜上车",
      "素材局",
      "不接单",
      "接单",
      "征婚",
      "4399?",
      "暂时不考虑打职业",
      "bot?",
      "叫你家大人来打",
      "假肢上门安装",
      "浪费我的网费",
      "不收残疾人",
      "下课",
      "自己找差距",
      "不接代",
      "代+",
      "这样的治好了也流口水",
      "人机",
      "人机怎么调难度啊",
      "只收不被0封的",
      "Bot吗这是",
      "领养",
      "纳亲",
      "正视差距",
      "近亲繁殖?",
      "我玩的是新手教程?",
      "来调灵敏度的",
      "来调参数的",
      "小号",
      "不是本人别加",
      "下次记得晚点玩",
      "随便玩玩,不带妹",
      "扣1上车"
   );
   private final EnumSetting<AutoEZ.Type> type = this.add(new EnumSetting("Type", AutoEZ.Type.Bot));
   final StringSetting message = this.add(new StringSetting("Message", "EZ %player%", () -> this.type.getValue() == AutoEZ.Type.Custom));
   final Random random = new Random();
   private final SliderSetting range = this.add(new SliderSetting("Range", 10.0, 0.0, 20.0, 0.1));
   private final SliderSetting randoms = this.add(new SliderSetting("Random", 3.0, 0.0, 20.0, 1.0));

   public AutoEZ() {
      super("AutoEZ", Module.Category.Misc);
      this.setChinese("自动嘲讽");
   }

   @EventListener
   public void onDeath(DeathEvent event) {
      PlayerEntity player = event.getPlayer();
      if (player != mc.player && !Alien.FRIEND.isFriend(player)) {
         if (this.range.getValue() > 0.0 && mc.player.distanceTo(player) > this.range.getValue()) {
            return;
         }

         String randomString = this.generateRandomString(this.randoms.getValueInt());
         if (!randomString.isEmpty()) {
            randomString = " " + randomString;
         }

         switch ((AutoEZ.Type)this.type.getValue()) {
            case Bot:
               mc.getNetworkHandler()
                  .sendChatMessage((String)this.bot.get(this.random.nextInt(this.bot.size() - 1)) + " " + player.getName().getString() + randomString);
               break;
            case Custom:
               mc.getNetworkHandler().sendChatMessage(this.message.getValue().replaceAll("%player%", player.getName().getString()) + randomString);
               break;
            case AutoSex:
               mc.getNetworkHandler()
                  .sendChatMessage((String)this.sex.get(this.random.nextInt(this.sex.size() - 1)) + " " + player.getName().getString() + randomString);
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

   public static enum Type {
      Bot,
      Custom,
      AutoSex;
   }
}
