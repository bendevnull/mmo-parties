package deathtags.gui;

import java.util.Random;

import deathtags.core.ConfigHandler;
import deathtags.networking.BuilderData;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import deathtags.core.MMOParties;
import deathtags.stats.PartyMemberData;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.common.MinecraftForge;

/**
 * The UI handler that controls the creation and rendering of all party elements
 * Each bar to be rendered is created on mod initialization.
 * @see deathtags.api.compatibility.CompatibilityHelper
 */
@Mod.EventBusSubscriber
public class PartyList {

    public static final ResourceLocation TEXTURE_ICON = new ResourceLocation(MMOParties.MODID,
        "textures/icons.png");

    public static final ResourceLocation HEART_TEXTURE = new ResourceLocation("minecraft", "textures/gui/icons.png");

    private static Minecraft mc;
    private static int updateCounter = 0;
    private static Random random;

    private static boolean renderAscending = false;
    private static boolean renderOpposite = false;

    public static NuggetBar[] nuggetBars = new NuggetBar[] {};

    public PartyList() {
        super();
        random = new Random();
    }

    /**
     * Get an X-Y offset based off of the config's UI Anchor value.
     * @return X and Y
     */
    public static UISpec GetAnchorOffset()
    {
        ScaledResolution window = new ScaledResolution(mc);
        switch (ConfigHandler.Client_Options.anchorPoint) {
            case "top-left":
                return new UISpec(8, 0);

            case "bottom-left":
                renderAscending = true;
                return new UISpec(8, window.getScaledHeight());

            case "top-right":
                return new UISpec(window.getScaledWidth() - 5, 0);

            case "bottom-right":
                renderAscending = true;
                return new UISpec(window.getScaledWidth(), window.getScaledHeight() - 20);

            default:
                System.out.println("Invalid anchor position selected.");
                break;
        }

        return new UISpec(0,0);
    }

    /**
     * Draw a nugget-bar with consideration to the compact settings and render restrictions
     * @param current
     * @param max
     * @param UI
     * @param backgroundOffset
     * @param halfOffset
     * @param compact
     * @param render
     * @return
     */
    public static int Draw(float current, float max, UISpec UI, int backgroundOffset, int halfOffset, boolean compact, boolean render) {
        if (render == false) return -1; // Don't render. Used for config values and what not.

        if (!compact) return DrawNuggetBar(current, max, UI, backgroundOffset, halfOffset);
        else return DrawNuggetBarCompact(current, UI, backgroundOffset);
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Pre event) {
        if ((event.getType() == ElementType.HEALTH || event.getType() == ElementType.ARMOR) && ConfigHandler.Client_Options.hideGUI) {
            event.setCanceled(true);
            return;
        }

        if (event.getType() != ElementType.TEXT)
            return;

        int lastOffset = 0;

        GL11.glScalef(1, 1, 1);

        mc = Minecraft.getMinecraft();
        
        /**
         * Party display.
         */
        if (MMOParties.localParty != null && MMOParties.localParty.local_players.size() >= 1) {
            int pN = 0;

            for (PartyMemberData data : MMOParties.localParty.data.values()) {
                // Hide yourself if the option is enabled.
                if (ConfigHandler.Client_Options.hideSelf && data.name.equals(mc.player.getName())) continue;

                // Render a new player and track the additional offset for the next player.
                lastOffset += RenderMember(data, lastOffset, pN, MMOParties.localParty.local_players.size() > 4
                        || ConfigHandler.Client_Options.useSimpleUI == true);
                pN++;
            }
        }
        
        mc.getTextureManager().bindTexture(Gui.ICONS);
    }

    /**
     * Draw a sectioned "nugget bar" in a compacted setting (icon + number) at a specified position.
     * Positioning of the element is automatically handled and supplied in the UI argument.
     * @param current
     * @param UI
     * @param backgroundOffset
     * @return
     */
    public static int DrawNuggetBarCompact(float current, UISpec UI, int backgroundOffset) {
        int left = UI.x;
        int top = UI.y;
        int startX = left;
        int startY = top;

        mc.getTextureManager().bindTexture(UI.texture); // Bind the appropriate texture

        mc.ingameGUI.drawTexturedModalRect(startX, startY, backgroundOffset, UI.texture_y, 9, 9);
        mc.ingameGUI.drawTexturedModalRect(startX, startY, UI.texture_x, UI.texture_y, 9, 9);

        mc.fontRenderer.drawString(String.format("%s", Math.floor(current)), startX + 12, startY, 0xFFFFFF);

        return 6;
    }

    /**
     * An interface for rendering nugget bars.
     * The Render method returns an offset for the next bar.
     * @since 2.3.0
     */
    public interface NuggetBar {
        int Render(BuilderData data, int xOffset, int yOffset, boolean compact);
    }

    /**
     * Handles rendering an individual party member.
     * @param data
     * @param lastOffset
     * @param pN
     * @param compact
     * @return
     */
    int RenderMember(PartyMemberData data, int lastOffset, int pN, boolean compact) {
        if (data == null) return 0; // There shouldn't be an instance where this is null, but..

        int iconRows = 0, additionalOffset = 0;
        UISpec defaultOffset = GetAnchorOffset();
        int yOffset = (15 * (pN + 1)) + lastOffset;
        int posX = defaultOffset.x;

        // Invert rendering of each player if in the bottom corners.
        if (renderAscending) yOffset = -yOffset;

        // Rendering compact or in verbose changes the amount of visible data as well as the yOffset.
        // The only bar visible within compact mode is hearts, and it's in a number form.
        if (compact) {
            yOffset = (int)(yOffset / 1.7) + 4;
            nuggetBars[1].Render(data.additionalData[1], posX, ((defaultOffset.y - 10) + yOffset), true);
            nuggetBars[2].Render(data.additionalData[2], posX + 30 + (4 * data.name.length()), ((defaultOffset.y - 10) + yOffset), true);
            return additionalOffset;
        }

        // Render each line of the list of UI elements
        // Each one is dynamically assigned at preInit by linked mods.
        for (int i=0; i < nuggetBars.length; i++) {
            if (data.additionalData[i] == null) continue;

            int rowOffset = (12 * iconRows);
            if (renderAscending) rowOffset = -rowOffset; // Reverse rendering.

            int offset = nuggetBars[i].Render(data.additionalData[i], posX, (defaultOffset.y + rowOffset) + yOffset, false);
            additionalOffset += offset;
            if (offset != -1) iconRows++;
        }

        return additionalOffset; // Return an offset calculated from number of wrapped bars to push future bars down.
    }

    /**
     * Draw a bar of nuggets based on a UI spec.
     * Position information is supplied automatically in the UI argument.
     * @param current
     * @param max
     * @param UI
     * @param backgroundOffset
     * @param halfOffset
     * @return
     */
    public static int DrawNuggetBar(float current, float max, UISpec UI, int backgroundOffset, int halfOffset) {
        UI.x = UI.x / 2;
        int length = 0, bars = 0;

        float maxLength = ConfigHandler.Client_Options.numbersAsPercentage ? 20 : max;

        // Normalize hearts into a value of 20 if the config is enabled.
        if (ConfigHandler.Client_Options.numbersAsPercentage) {
            int extraHearts = (int)((current - 20) / 2);

            // If the renderer displays as additional hearts, it shouldn't percentage the current.
            if (ConfigHandler.Client_Options.extraNumberType != "additional")
            current = (current / max) * 20;

            // Render any health that goes beyond the cap.
            RenderExtraHealth(UI, max, (extraHearts + 20) * 2);
        }

        mc.getTextureManager().bindTexture(UI.texture); // Bind the appropriate texture

        // Loop for each additional max nugget.
        for (int i = 0; i < maxLength / 2; i++) {
            int dropletHalf = i * 2 + 1;
            int nuggetX = UI.x + (length * 8), offsetY = UI.y;
            int xOffset = 4 * bars;
            if (renderOpposite) xOffset = -xOffset;

            // Randomly jiggle the health droplets at low health
            if (max > 6.0f && current <= 6.0f && updateCounter % (current * 3 + 1) == 0)
                offsetY = UI.y + (random.nextInt(3) - 1);

            DrawNugget(UI, current, dropletHalf, nuggetX, xOffset+offsetY, halfOffset, backgroundOffset);

            length ++; // increment length tracker.

            // Create a new bar when length reaches 10.
            if (length > 9) {
                bars++;
                length = 0;
            }
        }

        return (6 * (bars + 1));
    }

    /**
     * Render the health beyond maximum.
     */
    static void RenderExtraHealth(UISpec UI, float max, int extraHearts)
    {
        // Render remainder health to the side
        int remainderX = UI.x + (10 * 8) + 4;

        switch (ConfigHandler.Client_Options.extraNumberType) {
            case "percentage": // This will show the x% after bars.
                mc.fontRenderer.drawStringWithShadow(String.format("%s",Math.floor(extraHearts / max * 100)) + "%", remainderX, UI.y, 0xFFFFFF);
                break;
            case "compare": // This will show the x/x after the bars.
                mc.fontRenderer.drawStringWithShadow(String.format("%s/%s",extraHearts - 20, max), remainderX, UI.y, 0xFFFFFF);
                break;
            case "additional": // This will show +X after health.
                if (extraHearts <= 20) return;
                mc.fontRenderer.drawStringWithShadow(String.format("+%s", (extraHearts - 20) / 2), remainderX, UI.y, 0xFFFFFF);
                break;
            case "none": // This will not render anything beyond max and instead scale the health bar.
            default:
                break;
        }
    }

    /**
     * Draw an individual nugget onto the screen.
     */
    static void DrawNugget(UISpec UI, float current, int dropletHalf, int x, int y, int halfOffset, int backgroundOffset)
    {
        // Draw background
        mc.ingameGUI.drawTexturedModalRect(x, y, backgroundOffset, UI.texture_y, 9, 9);

        // Draw half or full depending on health amount.
        if ((int) current > dropletHalf) {
            mc.ingameGUI.drawTexturedModalRect(x, y, UI.texture_x, UI.texture_y, 9, 9);
        }
        else if ((int) current == dropletHalf)
            mc.ingameGUI.drawTexturedModalRect(x, y, UI.texture_x + halfOffset, UI.texture_y, 9, 9);
    }

    public static int DrawText(String text, UISpec location)
    {
        mc.fontRenderer.drawStringWithShadow(text, location.x, location.y, 0xFFFFFF);
        return 8;
    }

    public static int DrawResource(UISpec ui)
    {
        mc.getTextureManager().bindTexture(ui.texture);
        mc.ingameGUI.drawTexturedModalRect(ui.x, ui.y, ui.texture_x, ui.texture_y, ui.width, ui.height);
        mc.getTextureManager().bindTexture(Gui.ICONS);
        return ui.height;
    }

	public static void init() {
		System.out.println("Load GUI");
		MinecraftForge.EVENT_BUS.register(new PartyList());
	}
}