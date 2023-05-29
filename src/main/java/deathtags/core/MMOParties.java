package deathtags.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.mojang.blaze3d.platform.InputConstants;
import deathtags.api.PartyHelper;
import deathtags.commands.PartyCommand;
import deathtags.config.ConfigHolder;
import deathtags.core.events.EventClient;
import deathtags.core.events.EventCommon;
import deathtags.core.events.EventServer;
import deathtags.gui.HealthBar;
import deathtags.networking.MessageGUIInvitePlayer;
import deathtags.networking.MessageOpenUI;
import deathtags.networking.MessageSendMemberData;
import deathtags.networking.MessageUpdateParty;
import deathtags.stats.Party;
import deathtags.stats.PlayerStats;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.settings.KeyBindingMap;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.lwjgl.glfw.GLFW;

@Mod(value = MMOParties.MODID)
public class MMOParties {

	public static final String MODID = "mmoparties";
	public static Party localParty = null;
	public static Map<Player, PlayerStats> PlayerStats = new HashMap<>();
	private static final String PROTOCOL_VERSION = "1";
	
	public static final SimpleChannel network = NetworkRegistry.ChannelBuilder
			.named(new ResourceLocation(MODID, "sync"))
			.clientAcceptedVersions(s -> true)
			.serverAcceptedVersions(s -> true)
			.networkProtocolVersion(() -> PROTOCOL_VERSION)
			.simpleChannel();

	public static KeyMapping OPEN_GUI_KEY;
	
	public MMOParties ()
	{
		// Construct configuration
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ConfigHolder.COMMON_SPEC);

		// Construct game events.
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::preInit);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientInit);

		MinecraftForge.EVENT_BUS.addListener(this::serverInit);
		MinecraftForge.EVENT_BUS.addListener(this::serverInitEvent);

		MinecraftForge.EVENT_BUS.register(this);
	}
	
	public void preInit(FMLCommonSetupEvent event) 
	{
		System.out.println(MODID + " is pre-loading!");

		network.registerMessage(1, MessageUpdateParty.class, MessageUpdateParty::encode, MessageUpdateParty::decode, MessageUpdateParty.Handler::handle );
		network.registerMessage(2, MessageSendMemberData.class, MessageSendMemberData::encode, MessageSendMemberData::decode, MessageSendMemberData.Handler::handle);
		network.registerMessage(3, MessageGUIInvitePlayer.class, MessageGUIInvitePlayer::encode, MessageGUIInvitePlayer::decode, MessageGUIInvitePlayer.Handler::handle);

		// Register event handlers
		MinecraftForge.EVENT_BUS.register(new EventCommon());
		MinecraftForge.EVENT_BUS.register(new EventClient());
		MinecraftForge.EVENT_BUS.register(new EventServer());
	}

	public void serverInitEvent(ServerStartingEvent event) {
		PartyHelper.Server.server = event.getServer(); // Set server instance
	}

	public void clientInit(FMLClientSetupEvent event)
	{
		HealthBar.init();
		network.registerMessage(4, MessageOpenUI.class, MessageOpenUI::encode, MessageOpenUI::decode, MessageOpenUI.Handler::handle);

		OPEN_GUI_KEY = new KeyMapping("key.opengui.desc", KeyConflictContext.UNIVERSAL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, "key.mmoparties.category"); // Open GUI on G.
		ClientRegistry.registerKeyBinding(OPEN_GUI_KEY);
	}
	
	public void serverInit(RegisterCommandsEvent event)
	{
		event.getDispatcher().register(PartyCommand.register());
		network.registerMessage(4, MessageOpenUI.class, MessageOpenUI::encode, MessageOpenUI::decode, MessageOpenUI.Handler::handleServer);
	}

	/**
	 * Get the stat value of a player by their name.
	 * @param name
	 * @return
	 */
	public static PlayerStats GetStatsByName(String name)
	{
		for (Entry<Player, deathtags.stats.PlayerStats> plr : PlayerStats.entrySet()) {
			if ( plr.getKey().getName().getContents().equals(name) )
				return plr.getValue();
		}

		return null;
	}

	/**
	 * Get the stat value of a player.
	 * @param player
	 * @return
	 */
	public static PlayerStats GetStats(Player player)
	{
		return GetStatsByName(player.getName().getString());
	}

}
