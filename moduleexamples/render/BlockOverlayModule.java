package com.aqua.hud.modules.impl.render;

import java.util.Objects;
import com.aqua.hud.gui.DisplaySpace;
import com.aqua.hud.gui.AquaUI;
import com.aqua.hud.modules.Category;
import com.aqua.hud.modules.Module;
import com.aqua.hud.modules.settings.BooleanSetting;
import com.aqua.hud.modules.settings.ColorSetting;
import com.aqua.hud.modules.settings.ModeSetting;
import com.aqua.hud.modules.settings.NumberSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;

public class BlockOverlayModule extends Module {
   private final ColorSetting overlayColor = (ColorSetting)this.register(new ColorSetting("color", "Overlay color", 6, 182, 212, 255));
   private final ColorSetting outlineColor = (ColorSetting)this.register(new ColorSetting("outline_color", "Outline color", 103, 232, 249, 255));
   private final NumberSetting fillTransparency = (NumberSetting)this.register(new NumberSetting("fill_transparency", "Fill opacity: 0 is invisible and 255 is fully opaque", (double)48.0F, (double)0.0F, (double)255.0F, (double)1.0F));
   private final NumberSetting outlineTransparency = (NumberSetting)this.register(new NumberSetting("outline_transparency", "Border opacity: 0 is invisible and 255 is fully opaque", (double)220.0F, (double)0.0F, (double)255.0F, (double)1.0F));
   private final BooleanSetting showOutline = (BooleanSetting)this.register(new BooleanSetting("show_outline", "Show outline", true));
   private final BooleanSetting showFill = (BooleanSetting)this.register(new BooleanSetting("show_fill", "Show filled overlay", true));
   private final NumberSetting outlineWidth = (NumberSetting)this.register(new NumberSetting("outline_width", "Outline thickness", (double)2.0F, (double)1.0F, (double)5.0F, (double)0.5F));
   private final NumberSetting expandAmount = (NumberSetting)this.register(new NumberSetting("expand", "Expand overlay (prevents z-fighting)", 0.002, (double)0.0F, 0.01, 0.001));
   private final BooleanSetting showHud = (BooleanSetting)this.register(new BooleanSetting("show_hud", "Show block info HUD", true));
   private final BooleanSetting showIcon = (BooleanSetting)this.register(new BooleanSetting("show_icon", "Show block icon", true));
   private final BooleanSetting showBlockName = (BooleanSetting)this.register(new BooleanSetting("show_name", "Show block name", true));
   private final BooleanSetting showModName = (BooleanSetting)this.register(new BooleanSetting("show_mod", "Show mod/namespace", true));
   private final BooleanSetting showCoords = (BooleanSetting)this.register(new BooleanSetting("show_coords", "Show block coordinates", false));
   private final BooleanSetting hudBackground = (BooleanSetting)this.register(new BooleanSetting("hud_background", "Show HUD background", true));
   private final ColorSetting hudBgColor = (ColorSetting)this.register(new ColorSetting("hud_bg_color", "HUD background color", 0, 0, 0, 180));
   private final NumberSetting hudBgOpacity = (NumberSetting)this.register(new NumberSetting("hud_bg_opacity", "HUD background opacity", (double)128.0F, (double)0.0F, (double)255.0F, (double)1.0F));
   private final ColorSetting hudTextColor = (ColorSetting)this.register(new ColorSetting("hud_text_color", "HUD text color", 255, 255, 255, 255));
   private final ColorSetting hudModColor = (ColorSetting)this.register(new ColorSetting("hud_mod_color", "Mod name color", 100, 100, 255, 255));
   private final ModeSetting hudPosition = (ModeSetting)this.register(new ModeSetting("hud_position", "HUD position", "Custom", new String[]{"Custom", "Top Center", "Top Left", "Top Right", "Bottom Center", "Bottom Left", "Bottom Right"}));
   private final NumberSetting hudOffsetX = (NumberSetting)this.register(new NumberSetting("hud_offset_x", "HUD X offset", (double)0.0F, (double)-500.0F, (double)500.0F, (double)1.0F));
   private final NumberSetting hudOffsetY = (NumberSetting)this.register(new NumberSetting("hud_offset_y", "HUD Y offset", (double)50.0F, (double)-500.0F, (double)500.0F, (double)1.0F));
   private String cachedBlockName = "";
   private String cachedModName = "";
   private BlockPos cachedBlockPos = null;
   private ItemStack cachedItemStack;

   public BlockOverlayModule() {
      super("Block Overlay", "Highlights the block you're looking at with optional WAILA-like HUD", Category.RENDER);
      this.cachedItemStack = ItemStack.EMPTY;
      NumberSetting var10000 = this.fillTransparency;
      BooleanSetting var10001 = this.showFill;
      Objects.requireNonNull(var10001);
      var10000.visibleWhen(var10001::isEnabled);
      var10000 = this.outlineTransparency;
      var10001 = this.showOutline;
      Objects.requireNonNull(var10001);
      var10000.visibleWhen(var10001::isEnabled);
   }

   public void onRender(GuiGraphicsExtractor graphics, float tickDelta) {
      if (this.isInGame() && (Boolean)this.showHud.getValue()) {
         if (mc.hitResult != null && mc.hitResult.getType() == Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult)mc.hitResult;
            BlockPos blockPos = blockHit.getBlockPos();
            BlockState blockState = mc.level.getBlockState(blockPos);
            Block block = blockState.getBlock();
            if (!blockPos.equals(this.cachedBlockPos)) {
               this.cachedBlockPos = blockPos;
               this.cachedItemStack = new ItemStack(block.asItem());
               this.cachedBlockName = block.getName().getString();
               Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
               String namespace = blockId.getNamespace();
               this.cachedModName = this.formatModName(namespace);
            }

            int padding = 6;
            int iconSize = (Boolean)this.showIcon.getValue() ? 16 : 0;
            int iconPadding = (Boolean)this.showIcon.getValue() ? 4 : 0;
            int textHeight = 0;
            int maxTextWidth = 0;
            if ((Boolean)this.showBlockName.getValue()) {
               maxTextWidth = Math.max(maxTextWidth, mc.font.width(this.cachedBlockName));
               textHeight += 10;
            }

            if ((Boolean)this.showModName.getValue()) {
               maxTextWidth = Math.max(maxTextWidth, mc.font.width(this.cachedModName));
               textHeight += 10;
            }

            if ((Boolean)this.showCoords.getValue()) {
               String coordsText = String.format("X: %d  Y: %d  Z: %d", blockPos.getX(), blockPos.getY(), blockPos.getZ());
               maxTextWidth = Math.max(maxTextWidth, mc.font.width(coordsText));
               textHeight += 10;
            }

            int hudWidth = padding * 2 + iconSize + iconPadding + maxTextWidth;
            int hudHeight = padding * 2 + Math.max(iconSize, textHeight);
            int screenWidth = DisplaySpace.width();
            int screenHeight = DisplaySpace.height();
            String pos = (String)this.hudPosition.getValue();
            int offsetX = ((Double)this.hudOffsetX.getValue()).intValue();
            int offsetY = ((Double)this.hudOffsetY.getValue()).intValue();
            int x;
            int y;
            switch (pos) {
               case "Top Left":
                  x = 10 + offsetX;
                  y = 10 + offsetY;
                  break;
               case "Top Right":
                  x = screenWidth - hudWidth - 10 + offsetX;
                  y = 10 + offsetY;
                  break;
               case "Bottom Center":
                  x = (screenWidth - hudWidth) / 2 + offsetX;
                  y = screenHeight - hudHeight - 60 + offsetY;
                  break;
               case "Bottom Left":
                  x = 10 + offsetX;
                  y = screenHeight - hudHeight - 10 + offsetY;
                  break;
               case "Bottom Right":
                  x = screenWidth - hudWidth - 10 + offsetX;
                  y = screenHeight - hudHeight - 10 + offsetY;
                  break;
               case "Custom":
                  x = this.getHudX();
                  y = this.getHudY();
                  break;
               case "Top Center":
               default:
                  x = (screenWidth - hudWidth) / 2 + offsetX;
                  y = 10 + offsetY;
            }

            float scale = this.getHudScale();
            int backgroundColor;
            graphics.pose().pushMatrix();
            graphics.pose().translate((float)x, (float)y);
            graphics.pose().scale(scale, scale);
            graphics.pose().translate((float)(-x), (float)(-y));
            if ((Boolean)this.hudBackground.getValue()) {
               backgroundColor = this.hudBgOpacity.getIntValue() << 24 | this.hudBgColor.getRGB() & 16777215;
               AquaUI.hudPanel(graphics, x, y, hudWidth, hudHeight, this.hudBgOpacity.getIntValue());
            }

            backgroundColor = x + padding;
            int contentY = y + padding;
            if ((Boolean)this.showIcon.getValue() && !this.cachedItemStack.isEmpty()) {
               graphics.item(this.cachedItemStack, backgroundColor, contentY);
               backgroundColor += iconSize + iconPadding;
            }

            if ((Boolean)this.showIcon.getValue() && textHeight < iconSize) {
               contentY += (iconSize - textHeight) / 2;
            }

            int textColor = this.hudTextColor.getRGB() | -16777216;
            int modColor = this.hudModColor.getRGB() | -16777216;
            if ((Boolean)this.showBlockName.getValue()) {
               graphics.text(mc.font, this.cachedBlockName, backgroundColor, contentY, textColor, true);
               contentY += 10;
            }

            if ((Boolean)this.showModName.getValue()) {
               graphics.text(mc.font, this.cachedModName, backgroundColor, contentY, modColor, true);
               contentY += 10;
            }

            if ((Boolean)this.showCoords.getValue()) {
               String coordsText = String.format("X: %d  Y: %d  Z: %d", blockPos.getX(), blockPos.getY(), blockPos.getZ());
               graphics.text(mc.font, coordsText, backgroundColor, contentY, -5592406, true);
            }

            graphics.pose().popMatrix();
         }
      }
   }

   private String formatModName(String namespace) {
      if (namespace.equals("minecraft")) {
         return "Minecraft";
      } else {
         String[] parts = namespace.split("_");
         StringBuilder formatted = new StringBuilder();

         for(String part : parts) {
            if (!part.isEmpty()) {
               formatted.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
            }
         }

         return formatted.toString().trim();
      }
   }

   public int getHudWidth() {
      int padding = 6;
      int iconSize = (Boolean)this.showIcon.getValue() ? 16 : 0;
      int iconPadding = (Boolean)this.showIcon.getValue() ? 4 : 0;
      int maxTextWidth = 0;
      String blockName = this.cachedBlockName.isEmpty() ? "Block Name" : this.cachedBlockName;
      String modName = this.cachedModName.isEmpty() ? "Minecraft" : this.cachedModName;
      if ((Boolean)this.showBlockName.getValue()) {
         maxTextWidth = Math.max(maxTextWidth, mc.font.width(blockName));
      }

      if ((Boolean)this.showModName.getValue()) {
         maxTextWidth = Math.max(maxTextWidth, mc.font.width(modName));
      }

      if ((Boolean)this.showCoords.getValue()) {
         String coordsText = this.cachedBlockPos != null ? String.format("X: %d  Y: %d  Z: %d", this.cachedBlockPos.getX(), this.cachedBlockPos.getY(), this.cachedBlockPos.getZ()) : "X: 000  Y: 00  Z: 000";
         maxTextWidth = Math.max(maxTextWidth, mc.font.width(coordsText));
      }

      return padding * 2 + iconSize + iconPadding + maxTextWidth;
   }

   public int getHudHeight() {
      int padding = 6;
      int iconSize = (Boolean)this.showIcon.getValue() ? 16 : 0;
      int textHeight = 0;
      if ((Boolean)this.showBlockName.getValue()) {
         textHeight += 10;
      }

      if ((Boolean)this.showModName.getValue()) {
         textHeight += 10;
      }

      if ((Boolean)this.showCoords.getValue()) {
         textHeight += 10;
      }

      return padding * 2 + Math.max(iconSize, textHeight);
   }

   public int getOverlayColorARGB() {
      int rgb = this.overlayColor.getRGB() & 16777215;
      int alpha = ((Double)this.fillTransparency.getValue()).intValue();
      return alpha << 24 | rgb;
   }

   public int getOutlineColorARGB() {
      int rgb = this.outlineColor.getRGB() & 16777215;
      int alpha = ((Double)this.outlineTransparency.getValue()).intValue();
      return alpha << 24 | rgb;
   }

   public boolean shouldShowOutline() {
      return (Boolean)this.showOutline.getValue();
   }

   public boolean shouldShowFill() {
      return (Boolean)this.showFill.getValue();
   }

   public float getOutlineWidth() {
      return this.outlineWidth.getFloatValue();
   }

   public double getExpandAmount() {
      return (Double)this.expandAmount.getValue();
   }

   public boolean isHudVisible() {
      return (Boolean)this.showHud.getValue();
   }

   public boolean isHudEnabled() {
      return (Boolean)this.showHud.getValue();
   }

   public boolean isCustomPosition() {
      return this.hudPosition.is("Custom");
   }

   public void setHudPosition(int x, int y) {
      super.setHudPosition(x, y);
      if (!this.hudPosition.is("Custom")) {
         this.hudPosition.setValue("Custom");
      }

   }
}
