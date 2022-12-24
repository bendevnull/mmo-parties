package deathtags.stats;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;

public abstract class PlayerGroup {
	/**
	 * The group leader.
	 */
	public PlayerEntity leader = null;

	/**
	 * Send a member-list update to the entire group.
	 */
	public abstract void SendUpdate();
	
	/**
	 * Send a stat update to the entire group.
	 * @param member The member in question.
	 * @param bypassLimit Whether or not it should account for the last ping.
	 */
	public abstract void SendPartyMemberData(PlayerEntity member, boolean bypassLimit);
	
	/**
	 * If the cached data, and the current data are identical.
	 * @param member The member in question.
	 * @return
	 */
	public abstract boolean IsDataDifferent(PlayerEntity member);
	
	/**
	 * If the player is a member of the player.
	 * @param member The member in question.
	 * @return
	 */
	public abstract boolean IsMember(PlayerEntity member);
	
	/**
	 * Broadcast a message to the entire group.
	 * @param message
	 */
	public abstract void Broadcast(TranslationTextComponent message);
	
	/**
	 * Get all of the players alive in the group.
	 * @return
	 */
	public abstract PlayerEntity[] GetOnlinePlayers();
	
	/**
	 * The name used to represent this kind of group in system messages.
	 * @return
	 */
	public abstract String GetGroupAlias();
	
	/**
	 * Set a specific player to the group leader.
	 * @param member
	 */
	public void MakeLeader(PlayerEntity member)
	{
		this.leader = member;
		this.Broadcast(new TranslationTextComponent("rpgparties.message.leader.make", member.getName().getString(), this.GetGroupAlias()));

		for ( PlayerEntity player : this.GetOnlinePlayers() ) SendPartyMemberData ( player, true );
		SendUpdate();
	}
}
