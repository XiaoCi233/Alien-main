package dev.luminous.asm.mixins;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.luminous.Alien;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.mod.commands.Command;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatInputSuggestor.class)
public abstract class MixinChatInputSuggestor {
   @Final
   @Shadow
   TextFieldWidget textField;
   @Shadow
   private CompletableFuture<Suggestions> pendingSuggestions;
   @Final
   @Shadow
   private List<OrderedText> messages;
   @Unique
   private boolean showOutline = false;

   @Shadow
   public abstract void show(boolean var1);

   @Inject(at = @At("HEAD"), method = "render")
   private void onRender(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
      if (this.showOutline) {
         int x = this.textField.getX() - 3;
         int y = this.textField.getY() - 3;
         Render2DUtil.drawRect(context.getMatrices(), x, y, this.textField.getWidth() + 1, 1.0F, ClientSetting.INSTANCE.color.getValue().getRGB());
         Render2DUtil.drawRect(
            context.getMatrices(),
            x,
            y + this.textField.getHeight() + 1,
            this.textField.getWidth() + 1,
            1.0F,
            ClientSetting.INSTANCE.color.getValue().getRGB()
         );
         Render2DUtil.drawRect(context.getMatrices(), x, y, 1.0F, this.textField.getHeight() + 1, ClientSetting.INSTANCE.color.getValue().getRGB());
         Render2DUtil.drawRect(
            context.getMatrices(),
            x + this.textField.getWidth() + 1,
            y,
            1.0F,
            this.textField.getHeight() + 2,
            ClientSetting.INSTANCE.color.getValue().getRGB()
         );
      }
   }

   @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;getCursor()I", ordinal = 0), method = "refresh()V")
   private void onRefresh(CallbackInfo ci) {
      String prefix = Alien.getPrefix();
      String string = this.textField.getText();
      this.showOutline = string.startsWith(prefix);
      if (!string.isEmpty()) {
         int cursorPos = this.textField.getCursor();
         String string2 = string.substring(0, cursorPos);
         if (prefix.startsWith(string2) || string2.startsWith(prefix)) {
            int j = 0;
            Matcher matcher = Pattern.compile("(\\s+)").matcher(string2);

            while (matcher.find()) {
               j = matcher.end();
            }

            SuggestionsBuilder builder = new SuggestionsBuilder(string2, j);
            if (string2.length() < prefix.length()) {
               if (!prefix.startsWith(string2)) {
                  return;
               }

               builder.suggest(prefix);
            } else {
               if (!string2.startsWith(prefix)) {
                  return;
               }

               int count = StringUtils.countMatches(string2, " ");
               List<String> seperated = Arrays.asList(string2.split(" "));
               if (count == 0) {
                  for (Object strObj : Alien.COMMAND.getCommands().keySet().toArray()) {
                     String str = (String)strObj;
                     builder.suggest(Alien.getPrefix() + str + " ");
                  }
               } else {
                  if (seperated.isEmpty()) {
                     return;
                  }

                  Command c = Alien.COMMAND.getCommandBySyntax(((String)seperated.getFirst()).substring(prefix.length()));
                  if (c == null) {
                     this.messages.add(Text.of(" §4no commands found: §f" + ((String)seperated.getFirst()).substring(prefix.length())).asOrderedText());
                     return;
                  }

                  String[] suggestions = c.getAutocorrect(count, seperated);
                  if (suggestions == null || suggestions.length == 0) {
                     return;
                  }

                  for (String str : suggestions) {
                     builder.suggest(str + " ");
                  }
               }
            }

            this.pendingSuggestions = builder.buildFuture();
            this.show(false);
         }
      }
   }
}
