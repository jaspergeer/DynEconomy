package me.nikeswooosh.dyneconomy.listeners;

import com.palmergames.bukkit.towny.event.*;
import com.palmergames.bukkit.towny.event.town.TownMergeEvent;
import com.palmergames.bukkit.towny.object.TownBlock;
import me.nikeswooosh.dyneconomy.DynEconomy;
import me.nikeswooosh.dyneconomy.utils.Utils;
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

    /**
     *
     * @param plugin DynEconomy insta
     */
    public TownyListener(DynEconomy plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onTownDelete(PreDeleteTownEvent e) {
        double change = -1 * e.getTown().getAccount().getHoldingBalance();
        plugin.getLogger().info("Town Deletion Detected! Total Money Supply Change: " + change);
        plugin.addTotalMoney(BigDecimal.valueOf(change));
        plugin.getLogger().info("Resyncing Max Money Supply");
        plugin.syncMaxMoney();
    }

    @EventHandler
    public void onNationDelete(PreDeleteNationEvent e) {
        double change = -1 * e.getNation().getAccount().getHoldingBalance();
        plugin.getLogger().info("Nation Deletion Detected! Total Money Supply Change: " + change);
        plugin.addTotalMoney(BigDecimal.valueOf(change));
    }

    @EventHandler
    public void onNewTown(NewTownEvent e) {
        double change = -1 * Double.parseDouble(Objects.requireNonNull(plugin.getTownyConfig()
                .getString("economy.new_expand.price_new_town")));
        plugin.getLogger().info("New Town Detected! Total Money Supply Change: " + change);
        plugin.addTotalMoney(BigDecimal.valueOf(change));
        plugin.getLogger().info("Resyncing Max Money Supply");
        plugin.syncMaxMoney();
    }

    @EventHandler
    public void onNewNation(NewNationEvent e) {
        double change = -1 * Double.parseDouble(Objects.requireNonNull(plugin.getTownyConfig()
                .getString("economy.new_expand.price_new_nation")));
        plugin.getLogger().info("New Nation Detected! Total Money Supply Change: " + change);
        plugin.addTotalMoney(BigDecimal.valueOf(change));
    }

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

    @EventHandler
    public void onTownMerge(TownMergeEvent e) {
        double change = -1 * Double.parseDouble(Objects.requireNonNull(plugin.getTownyConfig()
                .getString("economy.new_expand.price_town_merge")));
        plugin.getLogger().info("Town Merge Detected! Total Money Supply Change: " + change);
        plugin.addTotalMoney(BigDecimal.valueOf(change));
    }

    @EventHandler
    public void onUpkeep(NewDayEvent e) {
        double change = -1 * (e.getNationUpkeepCollected() + e.getTownUpkeepCollected());
        plugin.getLogger().info("Upkeep collection detected! Total Money Supply Change: " + change);
        plugin.addTotalMoney(BigDecimal.valueOf(change));
    }

    @EventHandler
    public void onPlayerFirstJoin(PlayerJoinEvent e) {
        if (!e.getPlayer().hasPlayedBefore()) {
            double change = plugin.getConfig().getDouble("money_per_player");
            plugin.getLogger().info("New Player detected! Max Money Supply Change: " + change);
            plugin.addMaxMoney(change);
        }
    }

    @EventHandler
    public void onKill(PlayerDeathEvent e) {
        Player killed = e.getEntity();
        Player killer = e.getEntity().getKiller();
        BigDecimal killedBalance = plugin.getEcoAPI().getAccount(killed.getUniqueId()).getHoldings();
        if (!(killer == null)) {
            BigDecimal exchange = killedBalance.multiply(BigDecimal.valueOf(plugin.getConfig()
                    .getDouble("money_steal_percentage"))).setScale(2, RoundingMode.DOWN);
            plugin.getEcoAPI().getAccount(killed.getUniqueId()).removeHoldings(exchange);
            plugin.getEcoAPI().getAccount(killer.getUniqueId()).addHoldings(exchange);
            killed.sendMessage(Utils.chat("&6$" + exchange.toString() + " &fwere stolen from you!"));
            killer.sendMessage(Utils.chat("You stole &6$" + exchange.toString() + " &f!"));
            plugin.getLogger().info(killer.getDisplayName() + " stole $" + killedBalance.toString() + " from " +
                    killed.getDisplayName());
        }
    }
}
