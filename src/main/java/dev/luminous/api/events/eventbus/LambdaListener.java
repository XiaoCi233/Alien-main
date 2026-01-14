package dev.luminous.api.events.eventbus;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;

public class LambdaListener implements IListener {
   private static Method privateLookupInMethod;
   private final Class<?> target;
   private final boolean isStatic;
   private final int priority;
   private Consumer<Object> executor;

   public LambdaListener(LambdaListener.Factory factory, Class<?> klass, Object object, Method method) {
      this.target = method.getParameters()[0].getType();
      this.isStatic = Modifier.isStatic(method.getModifiers());
      this.priority = ((EventListener)method.getAnnotation(EventListener.class)).priority();

      try {
         String name = method.getName();
         MethodHandles.Lookup lookup = factory.create(privateLookupInMethod, klass);
         MethodType methodType = MethodType.methodType(void.class, method.getParameters()[0].getType());
         MethodHandle methodHandle;
         MethodType invokedType;
         if (this.isStatic) {
            methodHandle = lookup.findStatic(klass, name, methodType);
            invokedType = MethodType.methodType(Consumer.class);
         } else {
            methodHandle = lookup.findVirtual(klass, name, methodType);
            invokedType = MethodType.methodType(Consumer.class, klass);
         }

         MethodHandle lambdaFactory = LambdaMetafactory.metafactory(
               lookup, "accept", invokedType, MethodType.methodType(void.class, Object.class), methodHandle, methodType
            )
            .getTarget();
         if (this.isStatic) {
            this.executor = (Consumer)lambdaFactory.invoke();
         } else {
            this.executor = (Consumer)lambdaFactory.invoke(object);
         }
      } catch (Throwable var11) {
         var11.printStackTrace();
      }
   }

   @Override
   public void call(Object event) {
      this.executor.accept(event);
   }

   @Override
   public Class<?> getTarget() {
      return this.target;
   }

   @Override
   public int getPriority() {
      return this.priority;
   }

   @Override
   public boolean isStatic() {
      return this.isStatic;
   }

   static {
      try {
         privateLookupInMethod = MethodHandles.class.getDeclaredMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);
      } catch (NoSuchMethodException var1) {
         var1.printStackTrace();
      }
   }

   public interface Factory {
      MethodHandles.Lookup create(Method var1, Class<?> var2) throws InvocationTargetException, IllegalAccessException;
   }
}
