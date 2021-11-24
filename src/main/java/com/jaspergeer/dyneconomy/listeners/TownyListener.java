package com.jaspergeer.dyneconomy.listeners;

import com.jaspergeer.dyneconomy.DynEconomy;
import com.jaspergeer.dyneconomy.utils.Utils;
import com.palmergames.bukkit.towny.event.*;
import com.palmergames.bukkit.towny.event.town.TownMergeEvent;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class TownyListener implements Listener {

    private DynEconomy plugin;

    public TownyListener(DynEconomy plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("Registered Listeners");
    }

    /**
     * When a town is deleted, remove the money kept in the town bank from our total count.
     * @param e PreDeleteTownEvent that triggered this
     */
    @EventHandler
    public void onTownDelete(PreDeleteTownEvent e) {
        double change = -1 * e.getTown().getAccount().getHoldingBalance();
        plugin.getLogger().info("Town Deletion Detected! Total Money Supply Change: " + change);
        plugin.addTotalMoney(BigDecimal.valueOf(change));
        plugin.getLogger().info("Resyncing Max Money Supply");
        plugin.syncMaxMoney();
    }

    /**
     * When a nation is deleted, remove the money kept in the nation bank from our total count.
     * @param e PreDeleteNationEvent that triggered this
     */
    @EventHandler
    public void onNationDelete(PreDeleteNationEvent e) {
        double change = -1 * e.getNation().getAccount().getHoldingBalance();
        plugin.getLogger().info("Nation Deletion Detected! Total Money Supply Change: " + change);
        plugin.addTotalMoney(BigDecimal.valueOf(change));
    }

    /**
     * When a town is created, remove the money spent to create the town from our total count.
     * @param e NewTownEvent that triggered this
     */
    @EventHandler
    public void onNewTown(NewTownEvent e) {
        double change = -1 * Double.parseDouble(Objects.requireNonNull(plugin.getTownyConfig()
                .getString("economy.new_expand.price_new_town")));
        plugin.getLogger().info("New Town Detected! Total Money Supply Change: " + change);
        plugin.addTotalMoney(BigDecimal.valueOf(change));
        plugin.getLogger().info("Resyncing Max Money Supply");
        plugin.syncMaxMoney();
    }

    /**
     * When a new nation is craeted, remove the money spent to create the nation from our total
     * count.
     * @param e NewNationEvent that triggered this
     */
    @EventHandler
    public void onNewNation(NewNationEvent e) {
        double change = -1 * Double.parseDouble(Objects.requireNonNull(plugin.getTownyConfig()
                .getString("economy.new_expand.price_new_nation")));
        plugin.getLogger().info("New Nation Detected! Total Money Supply Change: " + change);
        plugin.addTotalMoney(BigDecimal.valueOf(change));
    }

    /**
     * When a player claims a chunk, remove the money spent from our total count.
     * @param e TownClaimEvent that triggered this
     */
    @EventHandler
    public void onNewClaim(TownClaimEvent e) {
        TownBlock t = e.getTownBlock();
        double change = 0;
        if (t.isOutpost()) {
            change = -1 * Double.parseDouble(Objects.requireNonNull(plugin.getTownyConfig()
                    .getString("economy.new_expand.price_claim_outpost")));
        } else {
            change = -1 * Double.parseDouble(Objects.requireNonNull(plugin.getTownyConfig()
                    .getString("economy.new_expand.price_claim_townblock")));
        }
        plugin.getLogger().info("Plot Claim Detected! Total Money Supply Change: " + change);
        plugin.addTotalMoney(BigDecimal.valueOf(change));
        change = plugin.getConfig().getDouble("money_per_chunk");
        plugin.getLogger().info("Max Money Supply Change: " + change);
        plugin.addMaxMoney(change);
    }

    /**
     * When two towns merge, remove the money spent to merge from our total count.
     * @param e TownMergeEvent that triggered this
     */
    @EventHandler
    public void onTownMerge(TownMergeEvent e) {
        double change = -1 * Double.parseDouble(Objects.requireNonNull(plugin.getTownyConfig()
                .getString("economy.new_expand.price_town_merge")));
        plugin.getLogger().info("Town Merge Detected! Total Money Supply Change: " + change);
        plugin.addTotalMoney(BigDecimal.valueOf(change));
    }

    /**
     * Remove upkeep collected from our total count each in-game day.
     * @param e NewDayEvent that triggered this
     */
    @EventHandler
    public void onUpkeep(NewDayEvent e) {
        double change = -1 * (e.getNationUpkeepCollected() + e.getTownUpkeepCollected());
        plugin.getLogger().info("Upkeep collection detected! Total Money Supply Change: " + change);
        plugin.addTotalMoney(BigDecimal.valueOf(change));
    }

    /**
     * Increase maximum money by amout specified in config when a new player joins.
     * @param e PlayerJoinEvent that triggered this
     */
    @EventHandler
    public void onPlayerFirstJoin(PlayerJoinEvent e) {
        if (!e.getPlayer().hasPlayedBefore()) {
            double change = plugin.getConfig().getDouble("money_per_player");
            plugin.getLogger().info("New Player detected! Max Money Supply Change: " + change);
            plugin.addMaxMoney(change);
        }
    }

    /**
     * When a player kills another player, they 'steal' an amount of money specified in config from
     * the other player
     * @param e PlayerDeathEvent that triggered this
     */
    @EventHandler
    public void onKill(PlayerDeathEvent e) {
        Player killed = e.getEntity();
        Player killer = e.getEntity().getKiller();
        BigDecimal killedBalance =
                plugin.getEcoAPI().getAccount(killed.getUniqueId()).getHoldings();
        if (!(killer == null)) {
            BigDecimal exchange = killedBalance.multiply(BigDecimal.valueOf(plugin.getConfig()
                    .getDouble("money_steal_percentage"))).setScale(2, RoundingMode.DOWN);
            plugin.getEcoAPI().getAccount(killed.getUniqueId()).removeHoldings(exchange);
            plugin.getEcoAPI().getAccount(killer.getUniqueId()).addHoldings(exchange);
            killed.sendMessage(Utils.chat("&6$" + exchange.toString() +
                    " &fwere stolen from you!"));
            killer.sendMessage(Utils.chat("You stole &6$" + exchange.toString() + " &f!"));
            plugin.getLogger().info(killer.getDisplayName() + " stole $" + killedBalance.toString()
                    + " from " + killed.getDisplayName());
        }
    }
}
