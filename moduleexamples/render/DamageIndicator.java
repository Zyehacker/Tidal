package com.aqua.hud.modules.impl.render;

import java.util.Objects;
import com.aqua.hud.accessor.EntityRenderStateAccessor;
import com.aqua.hud.gui.DisplaySpace;
import com.aqua.hud.gui.AquaUI;
import com.aqua.hud.modules.Category;
import com.aqua.hud.modules.Module;
import com.aqua.hud.modules.settings.BooleanSetting;
import com.aqua.hud.modules.settings.ColorSetting;
import com.aqua.hud.modules.settings.ModeSetting;
import com.aqua.hud.modules.settings.NumberSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;

public class DamageIndicator extends Module {
   private final ModeSetting displayMode = (ModeSetting)this.register(new ModeSetting("mode", "Display mode", "Crosshair", new String[]{"Crosshair", "World", "Panel", "Both"}));
   private final BooleanSetting showPlayers = (BooleanSetting)this.register(new BooleanSetting("show_players", "Show for players", true));
   private final BooleanSetting showHostile = (BooleanSetting)this.register(new BooleanSetting("show_hostile", "Show for hostile mobs", true));
   private final BooleanSetting showPassive = (BooleanSetting)this.register(new BooleanSetting("show_passive", "Show for passive mobs", true));
   private final NumberSetting worldRange = (NumberSetting)this.register(new NumberSetting("world_range", "Range for world health bars", (double)24.0F, (double)8.0F, (double)64.0F, (double)4.0F));
   private final NumberSetting worldBarWidth = (NumberSetting)this.register(new NumberSetting("world_bar_width", "World health bar width", (double)1.5F, (double)0.5F, (double)3.0F, 0.1));
   private final NumberSetting worldBarHeight = (NumberSetting)this.register(new NumberSetting("world_bar_height", "World health bar height", 0.1, 0.05, 0.3, 0.01));
   private final BooleanSetting worldShowName = (BooleanSetting)this.register(new BooleanSetting("world_show_name", "Show name in world", false));
   private final BooleanSetting worldOnlyDamaged = (BooleanSetting)this.register(new BooleanSetting("world_only_damaged", "Only show when damaged", false));
   private final BooleanSetting showName = (BooleanSetting)this.register(new BooleanSetting("show_name", "Show entity name", true));
   private final BooleanSetting showHealthText = (BooleanSetting)this.register(new BooleanSetting("health_text", "Show health as text", true));
   private final BooleanSetting showPercent = (BooleanSetting)this.register(new BooleanSetting("show_percent", "Show as percentage", false));
   private final NumberSetting barWidth = (NumberSetting)this.register(new NumberSetting("bar_width", "Health bar width", (double)80.0F, (double)40.0F, (double)150.0F, (double)5.0F));
   private final NumberSetting verticalOffset = (NumberSetting)this.register(new NumberSetting("offset", "Vertical offset from crosshair", (double)50.0F, (double)20.0F, (double)100.0F, (double)5.0F));
   private final ColorSetting healthyColor = (ColorSetting)this.register(new ColorSetting("healthy_color", "Color when health is high", 100, 255, 100));
   private final ColorSetting damagedColor = (ColorSetting)this.register(new ColorSetting("damaged_color", "Color when health is low", 255, 80, 80));
   private static final int PANEL_WIDTH = 130;
   private static final int PANEL_HEIGHT = 38;
   private float displayedHealth = 0.0F;
   private float animatedHealthLag = 0.0F;
   private LivingEntity targetEntity = null;
   private long lastTargetTime = 0L;
   private float fadeAlpha = 0.0F;

   public DamageIndicator() {
      super("DamageIndicator", "Shows health of entities", Category.HUD);
      BooleanSetting var10000 = this.showPercent;
      BooleanSetting var10001 = this.showHealthText;
      Objects.requireNonNull(var10001);
      var10000.visibleWhen(var10001::isEnabled);
      this.worldRange.visibleWhen(() -> ((String)this.displayMode.getValue()).equals("World"));
      this.worldBarWidth.visibleWhen(() -> ((String)this.displayMode.getValue()).equals("World"));
      this.worldBarHeight.visibleWhen(() -> ((String)this.displayMode.getValue()).equals("World"));
      this.worldShowName.visibleWhen(() -> ((String)this.displayMode.getValue()).equals("World"));
      this.worldOnlyDamaged.visibleWhen(() -> ((String)this.displayMode.getValue()).equals("World"));
      this.verticalOffset.visibleWhen(() -> {
         String m = (String)this.displayMode.getValue();
         return m.equals("Crosshair") || m.equals("Both");
      });
      this.barWidth.visibleWhen(() -> {
         String m = (String)this.displayMode.getValue();
         return m.equals("Crosshair") || m.equals("Both");
      });
      this.showName.visibleWhen(() -> !((String)this.displayMode.getValue()).equals("World"));
      this.showHealthText.visibleWhen(() -> !((String)this.displayMode.getValue()).equals("World"));
   }

   public int getHudWidth() {
      return 130;
   }

   public int getHudHeight() {
      return 38;
   }

   public boolean isHudVisible() {
      String mode = (String)this.displayMode.getValue();
      return mode.equals("Panel") || mode.equals("Both");
   }

   public void onTick() {
      if (!this.isInGame()) {
         this.targetEntity = null;
      } else {
         HitResult hitResult = mc.hitResult;
         if (hitResult != null && hitResult.getType() == Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult)hitResult;
            Entity entity = entityHit.getEntity();
            if (entity instanceof LivingEntity) {
               LivingEntity living = (LivingEntity)entity;
               if (this.shouldShowFor(entity)) {
                  if (this.targetEntity != living) {
                     this.displayedHealth = living.getHealth();
                     this.animatedHealthLag = living.getHealth();
                  }

                  this.targetEntity = living;
                  this.lastTargetTime = System.currentTimeMillis();
                  return;
               }
            }
         }

         if (System.currentTimeMillis() - this.lastTargetTime > 500L) {
            this.targetEntity = null;
         }

      }
   }

   public void onRender(GuiGraphicsExtractor graphics, float tickDelta) {
      if (this.isInGame() && mc.level != null && mc.player != null) {
         String mode = (String)this.displayMode.getValue();
         if (!mode.equals("World")) {
            float targetAlpha = this.targetEntity != null && !this.targetEntity.isDeadOrDying() ? 1.0F : 0.0F;
            this.fadeAlpha += (targetAlpha - this.fadeAlpha) * 0.3F;
            if (!(this.fadeAlpha < 0.01F)) {
               if (this.targetEntity != null) {
                  float currentHealth = this.targetEntity.getHealth();
                  this.displayedHealth += (currentHealth - this.displayedHealth) * 0.5F;
                  this.animatedHealthLag += (currentHealth - this.animatedHealthLag) * 0.1F;
               }

               if (mode.equals("Crosshair") || mode.equals("Both")) {
                  this.renderCrosshairIndicator(graphics);
               }

               if (mode.equals("Panel") || mode.equals("Both")) {
                  this.renderPanel(graphics);
               }

            }
         }
      }
   }

   private void renderCrosshairIndicator(GuiGraphicsExtractor graphics) {
      if (this.targetEntity != null) {
         int screenWidth = DisplaySpace.width();
         int screenHeight = DisplaySpace.height();
         int centerX = screenWidth / 2;
         int centerY = screenHeight / 2;
         float maxHealth = this.targetEntity.getMaxHealth();
         float healthPercent = Math.min(1.0F, Math.max(0.0F, this.displayedHealth / maxHealth));
         float lagPercent = Math.min(1.0F, Math.max(0.0F, this.animatedHealthLag / maxHealth));
         int barW = this.barWidth.getIntValue();
         int barH = 4;
         int yOffset = this.verticalOffset.getIntValue();
         int alpha = (int)(this.fadeAlpha * 255.0F);
         int bgAlpha = (int)(this.fadeAlpha * 200.0F);
         int indicatorX = centerX - barW / 2;
         int indicatorY = centerY - yOffset;
         if (this.showName.isEnabled()) {
            String name = this.targetEntity.getDisplayName().getString();
            int nameWidth = mc.font.width(name);
            int nameX = centerX - nameWidth / 2;
            int nameY = indicatorY - 12;
            graphics.text(mc.font, name, nameX, nameY, alpha << 24 | 16777215, true);
            indicatorY += 2;
         }

         int bgColor = bgAlpha << 24 | 1053463;
         AquaUI.roundedRect(graphics, indicatorX - 2, indicatorY - 2, barW + 4, barH + 4, 2, (int)(this.fadeAlpha * 120.0F) << 24 | 1053463);
         AquaUI.roundedRect(graphics, indicatorX, indicatorY, barW, barH, 2, bgColor);
         if (lagPercent > healthPercent) {
            int lagWidth = (int)((float)barW * lagPercent);
            int damageColor = alpha << 24 | 16724787;
            graphics.fill(indicatorX, indicatorY, indicatorX + lagWidth, indicatorY + barH, damageColor);
         }

         int healthColor = this.interpolateColor(healthPercent);
         int filledWidth = Math.max(0, (int)((float)barW * healthPercent));
         if (filledWidth > 0) {
            graphics.fill(indicatorX, indicatorY, indicatorX + filledWidth, indicatorY + barH, alpha << 24 | healthColor);
            int highlight = this.brightenColor(healthColor, 1.4F);
            graphics.fill(indicatorX, indicatorY, indicatorX + filledWidth, indicatorY + 1, alpha << 24 | highlight);
         }

         int borderAlpha = (int)(this.fadeAlpha * 120.0F);
         AquaUI.outline(graphics, indicatorX - 2, indicatorY - 2, barW + 4, barH + 4, AquaUI.withAlpha(1725422816, borderAlpha));
         if (this.showHealthText.isEnabled()) {
            String healthText = this.formatHealth(this.displayedHealth, maxHealth, healthPercent);
            int textWidth = mc.font.width(healthText);
            int textX = centerX - textWidth / 2;
            int textY = indicatorY + barH + 3;
            graphics.text(mc.font, healthText, textX, textY, alpha << 24 | 13421772, true);
         }

      }
   }

   private void renderPanel(GuiGraphicsExtractor graphics) {
      if (this.targetEntity != null) {
         float maxHealth = this.targetEntity.getMaxHealth();
         float healthPercent = Math.min(1.0F, Math.max(0.0F, this.displayedHealth / maxHealth));
         int healthColor = this.interpolateColor(healthPercent);
         double distance = (double)mc.player.distanceTo(this.targetEntity);
         int x = this.getHudX();
         int y = this.getHudY();
         float scale = this.getHudScale();
         int panelW = 130;
         int panelH = 38;
         int padding = 6;
         int alpha = (int)(this.fadeAlpha * 240.0F);
         graphics.pose().pushMatrix();
         graphics.pose().translate((float)x, (float)y);
         graphics.pose().scale(scale, scale);
         graphics.pose().translate((float)(-x), (float)(-y));
         AquaUI.hudPanel(graphics, x, y, panelW, panelH, alpha);
         graphics.fill(x, y, x + 2, y + panelH, alpha << 24 | healthColor);
         int contentX = x + padding + 2;
         int contentY = y + padding;
         int barW = panelW - padding * 2 - 2;
         String name = this.targetEntity.getDisplayName().getString();
         if (name.length() > 14) {
            name = name.substring(0, 14) + "...";
         }

         String distText = String.format("%.1fm", distance);
         graphics.text(mc.font, name, contentX, contentY, alpha << 24 | 16777215, false);
         int distWidth = mc.font.width(distText);
         graphics.text(mc.font, distText, x + panelW - padding - distWidth, contentY, alpha << 24 | 8947848, false);
         contentY += 11;
         int barH = 4;
         AquaUI.roundedRect(graphics, contentX, contentY, barW, barH, 2, -1441128412);
         int filledW = Math.max(0, (int)((float)barW * healthPercent));
         if (filledW > 0) {
            graphics.fill(contentX, contentY, contentX + filledW, contentY + barH, alpha << 24 | healthColor);
            graphics.fill(contentX, contentY, contentX + filledW, contentY + 1, alpha << 24 | this.brightenColor(healthColor, 1.3F));
         }

         contentY += 7;
         String healthStr = this.formatHealth(this.displayedHealth, maxHealth, healthPercent);
         graphics.text(mc.font, healthStr, contentX, contentY, alpha << 24 | healthColor, false);
         graphics.pose().popMatrix();
      }
   }

   public boolean isWorldMode() {
      return ((String)this.displayMode.getValue()).equals("World");
   }

   public double getWorldRange() {
      return (Double)this.worldRange.getValue();
   }

   public boolean isOnlyDamaged() {
      return this.worldOnlyDamaged.isEnabled();
   }

   public float getWorldBarWidth() {
      return ((Double)this.worldBarWidth.getValue()).floatValue();
   }

   public float getWorldBarHeight() {
      return ((Double)this.worldBarHeight.getValue()).floatValue();
   }

   public boolean shouldShowWorldName() {
      return this.worldShowName.isEnabled();
   }

   public float getEntityHealth(EntityRenderState state) {
      if (state instanceof EntityRenderStateAccessor accessor) {
         Entity entity = accessor.aqua$getEntity();
         if (entity instanceof LivingEntity living) {
            if (this.shouldShowFor(entity)) {
               return living.getHealth();
            }
         }
      }

      return -1.0F;
   }

   public float getEntityMaxHealth(EntityRenderState state) {
      if (state instanceof EntityRenderStateAccessor accessor) {
         Entity entity = accessor.aqua$getEntity();
         if (entity instanceof LivingEntity living) {
            if (this.shouldShowFor(entity)) {
               return living.getMaxHealth();
            }
         }
      }

      return 0.0F;
   }

   public int getHealthColor(float healthPercent) {
      return this.interpolateColor(healthPercent);
   }

   public boolean shouldShowPlayers() {
      return this.showPlayers.isEnabled();
   }

   public boolean shouldShowHostile() {
      return this.showHostile.isEnabled();
   }

   public boolean shouldShowPassive() {
      return this.showPassive.isEnabled();
   }

   public boolean shouldRenderWorldHealthBar(LivingEntity entity) {
      if (!this.shouldShowFor(entity)) {
         return false;
      } else if (entity.isDeadOrDying()) {
         return false;
      } else {
         return !this.worldOnlyDamaged.isEnabled() || !(entity.getHealth() >= entity.getMaxHealth());
      }
   }

   private String formatHealth(float health, float maxHealth, float healthPercent) {
      if (this.showPercent.isEnabled()) {
         return String.format("%.0f%%", healthPercent * 100.0F);
      } else {
         return (double)health == Math.floor((double)health) && (double)maxHealth == Math.floor((double)maxHealth) ? String.format("%.0f / %.0f ❤", health, maxHealth) : String.format("%.1f / %.0f ❤", health, maxHealth);
      }
   }

   private boolean shouldShowFor(Entity entity) {
      if (entity instanceof ArmorStand) {
         return false;
      } else if (entity == mc.player) {
         return false;
      } else if (entity instanceof Player) {
         return this.showPlayers.isEnabled();
      } else if (entity instanceof Monster) {
         return this.showHostile.isEnabled();
      } else {
         return entity instanceof Animal ? this.showPassive.isEnabled() : true;
      }
   }

   private int interpolateColor(float percent) {
      int r1 = this.damagedColor.getRed();
      int g1 = this.damagedColor.getGreen();
      int b1 = this.damagedColor.getBlue();
      int r2 = this.healthyColor.getRed();
      int g2 = this.healthyColor.getGreen();
      int b2 = this.healthyColor.getBlue();
      float t = percent * percent * (3.0F - 2.0F * percent);
      int r = (int)((float)r1 + (float)(r2 - r1) * t);
      int g = (int)((float)g1 + (float)(g2 - g1) * t);
      int b = (int)((float)b1 + (float)(b2 - b1) * t);
      return r << 16 | g << 8 | b;
   }

   private int brightenColor(int color, float factor) {
      int r = Math.min(255, (int)((float)(color >> 16 & 255) * factor));
      int g = Math.min(255, (int)((float)(color >> 8 & 255) * factor));
      int b = Math.min(255, (int)((float)(color & 255) * factor));
      return r << 16 | g << 8 | b;
   }
}
