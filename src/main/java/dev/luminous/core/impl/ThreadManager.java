package dev.luminous.core.impl;


import com.google.common.collect.Lists;
import dev.luminous.Alien;
import dev.luminous.Alien;
import dev.luminous.api.events.eventbus.EventListener;
import dev.luminous.api.events.impl.ClientTickEvent;
import dev.luminous.api.events.impl.UpdateEvent;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.render.JelloUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.mod.modules.Module;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.combat.AutoAnchor;
import dev.luminous.mod.modules.impl.combat.AutoCrystal;
import dev.luminous.mod.modules.impl.render.HoleESP;
import dev.luminous.mod.modules.impl.render.PlaceRender;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;

public class ThreadManager implements Wrapper {
   public static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
   public static ThreadManager.ClientService clientService;
   public volatile Iterable<Entity> threadSafeEntityList = Collections.emptyList();
   public volatile List<AbstractClientPlayerEntity> threadSafePlayersList = Collections.emptyList();
   public volatile boolean tickRunning = false;

   public ThreadManager() {
      this.init();
   }

   
   public void init() {
      Alien.EVENT_BUS.subscribe(this);
      clientService = new ThreadManager.ClientService();
      clientService.setName("AlienClientService");
      clientService.setDaemon(true);
      clientService.start();
   }

   public Iterable<Entity> getEntities() {
      return this.threadSafeEntityList;
   }

   public List<AbstractClientPlayerEntity> getPlayers() {
      return this.threadSafePlayersList;
   }

   public void execute(Runnable runnable) {
      EXECUTOR.execute(runnable);
   }

   @EventListener(priority = 200)
   public void onEvent(ClientTickEvent event) {
      Alien.POP.onUpdate();
      Alien.SERVER.onUpdate();
      if (event.isPre()) {
         JelloUtil.updateJello();
         this.tickRunning = true;
         BlockUtil.placedPos.forEach(pos -> PlaceRender.INSTANCE.create(pos));
         BlockUtil.placedPos.clear();
         Alien.PLAYER.onUpdate();
         if (!Module.nullCheck()) {
            Alien.EVENT_BUS.post(UpdateEvent.INSTANCE);
         }
      } else {
         this.tickRunning = false;
         if (mc.world == null || mc.player == null) {
            return;
         }

         this.threadSafeEntityList = Lists.newArrayList(mc.world.getEntities());
         this.threadSafePlayersList = Lists.newArrayList(mc.world.getPlayers());
      }

      if (!clientService.isAlive() || clientService.isInterrupted()) {
         clientService = new ThreadManager.ClientService();
         clientService.setName("AlienService");
         clientService.setDaemon(true);
         clientService.start();
      }
   }

   public class ClientService extends Thread {
      public void run() {
         while (true) {
            try {
               while (ThreadManager.this.tickRunning) {
                  Thread.onSpinWait();
               }

               AutoCrystal.INSTANCE.onThread();
               HoleESP.INSTANCE.onThread();
               AutoAnchor.INSTANCE.onThread();
            } catch (Exception var2) {
               var2.printStackTrace();
               if (ClientSetting.INSTANCE.debug.getValue()) {
                  CommandManager.sendMessage("§4An error has occurred [Thread] Message: [" + var2.getMessage() + "]");
               }
            }
         }
      }
   }
}
